package com.sow.wegui.client;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.sow.wegui.WeGuiMod;
import com.sow.wegui.config.Configs;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.session.ClipboardHolder;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.malilib.event.RenderEventHandler;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.render.RenderContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.state.BlockState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Map;

/**
 * 把 WorldEdit 剪贴板与选区同步到 Litematica 的渲染系统。
 * - 剪贴板：通过 SchematicPlacementManager 注入 placement，Litematica 自动渲染 ghost blocks 与 mismatch
 * - 选区框：在 malilib onRenderWorldLast 中调用 RenderUtils.renderAreaOutline + renderBlockOutline
 *
 * 边框透视（selectionBoxThroughView）：
 *   true  → 角点方块边框穿过世界方块可见（透视/x-ray）
 *   false → 角点方块边框被世界方块遮挡（不透视，默认）
 *   区域轮廓（renderAreaOutline）malilib 内部固定 NO_DEPTH_NO_CULL，始终透视，无法关闭。
 */
public final class LitematicaBridge {
    private static final String WEGUI_PLACEMENT_NAME = "WeGui Clipboard Sync";
    private static final String REGION_NAME = "Main";

    @Nullable private static LitematicaSchematic currentSchematic;
    @Nullable private static SchematicPlacement currentPlacement;
    @Nullable private static ClipboardHolder lastHolder = null;
    @Nullable private static Transform lastTransform = null;
    private static boolean registered = false;
    @Nullable private static BlockPos lastSyncedOrigin = null;

    private LitematicaBridge() {}

    public static void register() {
        if (registered) return;
        registered = true;
        ClientTickEvents.END_CLIENT_TICK.register(LitematicaBridge::onClientTick);
        RenderEventHandler.getInstance().registerWorldLastRenderer(new WeSelectionRenderer());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> onWorldDisconnect());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> onWorldJoin(client));
        WeGuiMod.LOGGER.info("[WeGui] LitematicaBridge registered, Litematica 接管渲染（含 WE 选区框）");
    }

    /** 退出存档时清理 Litematica placement 与内部同步状态，避免重进后残留旧投影。 */
    private static void onWorldDisconnect() {
        removeCurrentPlacement();
        lastHolder = null;
        lastTransform = null;
        lastSyncedOrigin = null;
        AxeModeHandler.setFixedOrigin(null);
        WeGuiMod.LOGGER.info("[WeGui] 存档断开：已清理 Litematica placement 与同步状态");
    }

    /** 进入存档时清空 WE 剪贴板，避免上一次会话的剪贴板残留导致投影错乱。 */
    private static void onWorldJoin(Minecraft mc) {
        if (mc.player == null || !WorldEditAdapter.isLoaded()) return;
        try {
            com.sk89q.worldedit.LocalSession session = WorldEditAdapter.session(mc.player);
            if (session != null) {
                session.setClipboard(null);
                WeGuiMod.LOGGER.info("[WeGui] 存档连接：已清空 WE 剪贴板");
            }
        } catch (Throwable e) {
            WeGuiMod.LOGGER.debug("[WeGui] 清空 WE 剪贴板失败: {}", e.toString());
        }
    }

    /** 导出当前 Litematica 投影为原理图文件到指定目录。成功返回 true。 */
    public static boolean exportCurrentSchematic(Path dir, String fileName) {
        LitematicaSchematic schematic = currentSchematic;
        if (schematic == null) {
            WeGuiMod.LOGGER.warn("[WeGui] 导出失败：当前没有可用的投影 schematic");
            return false;
        }
        try {
            java.nio.file.Files.createDirectories(dir);
            boolean ok = schematic.writeToFile(dir, fileName, true);
            if (ok) {
                WeGuiMod.LOGGER.info("[WeGui] 原理图已保存: {}/{}", dir, fileName);
            } else {
                WeGuiMod.LOGGER.warn("[WeGui] 原理图保存失败: {}/{}", dir, fileName);
            }
            return ok;
        } catch (Throwable e) {
            WeGuiMod.LOGGER.error("[WeGui] 原理图保存异常: {}/{}: {}", dir, fileName, e);
            return false;
        }
    }

    /** 当前是否存在可导出的 Litematica 投影。 */
    public static boolean hasExportableSchematic() {
        return currentSchematic != null;
    }

    /** 获取当前投影的 schematic 对象（仅供导出使用，不要修改）。 */
    @Nullable
    public static LitematicaSchematic getCurrentSchematic() {
        return currentSchematic;
    }

    private static void onClientTick(Minecraft mc) {
        if (mc.player == null || mc.level == null) return;

        // 关闭总开关时移除已有 placement
        if (!Configs.Generic.PASTE_PREVIEW_ENABLED.getBooleanValue()) {
            removeCurrentPlacement();
            lastHolder = null;
            lastTransform = null;
            return;
        }

        // O(1) 引用比较：检测剪贴板是否变化
        // - //copy 会创建新的 ClipboardHolder 实例（holder 引用变化）
        // - //flip / //rotate 不会创建新 holder，而是调用 holder.setTransform(...) 替换内部
        //   transform 字段（getTransform 返回的字段引用会变化）
        // 因此需要同时比较 holder 与 transform 两个引用，才能完整覆盖三种命令。
        ClipboardHolder holder = WorldEditBridge.getClipboardHolder(mc);
        if (holder == null) {
            removeCurrentPlacement();
            lastHolder = null;
            lastTransform = null;
            return;
        }

        BlockPos newOrigin = AxeModeHandler.getEffectiveOrigin(mc);
        Transform currentTransform = holder.getTransform();

        if (holder != lastHolder || currentTransform != lastTransform) {
            // 剪贴板内容或变换变化：重新读取方块并重建 schematic（O(n)，仅在 //copy、//flip、//rotate 时触发）
            Map<BlockPos, BlockState> blocks = WorldEditBridge.getClipboardBlocks(mc);
            if (blocks == null || blocks.isEmpty()) {
                removeCurrentPlacement();
                lastHolder = null;
                lastTransform = null;
                return;
            }
            lastHolder = holder;
            lastTransform = currentTransform;
            syncToLitematica(blocks, newOrigin);
        } else {
            // 剪贴板内容未变：只检查 origin 是否需要更新（O(1)）
            if (!newOrigin.equals(lastSyncedOrigin)) {
                updatePlacementOrigin(newOrigin);
            }
        }
    }

    /**
     * O(1) 更新 placement 的 origin，不重建 schematic。
     * 计算新旧 origin 的差值，应用到 placement 当前 origin 上。
     * 用于 FIXED 模式滚轮移动、FOLLOW_PLAYER 模式玩家移动时的预览跟随。
     */
    private static void updatePlacementOrigin(BlockPos newOrigin) {
        SchematicPlacement placement = currentPlacement;
        if (placement == null || lastSyncedOrigin == null) return;
        BlockPos delta = newOrigin.subtract(lastSyncedOrigin);
        if (delta.equals(BlockPos.ZERO)) return;
        BlockPos newPlacementOrigin = placement.getOrigin().offset(delta);
        placement.setOrigin(newPlacementOrigin, msg -> {});
        lastSyncedOrigin = newOrigin;
    }

    private static void syncToLitematica(Map<BlockPos, BlockState> blocks, BlockPos origin) {
        removeCurrentPlacement();

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos rel : blocks.keySet()) {
            minX = Math.min(minX, rel.getX());
            minY = Math.min(minY, rel.getY());
            minZ = Math.min(minZ, rel.getZ());
            maxX = Math.max(maxX, rel.getX());
            maxY = Math.max(maxY, rel.getY());
            maxZ = Math.max(maxZ, rel.getZ());
        }

        BlockPos minPos = new BlockPos(minX, minY, minZ);
        BlockPos maxPos = new BlockPos(maxX, maxY, maxZ);
        BlockPos worldMin = origin.offset(minPos);
        BlockPos worldMax = origin.offset(maxPos);

        AreaSelection area = new AreaSelection();
        area.setName(WEGUI_PLACEMENT_NAME);
        Box box = new Box(worldMin, worldMax, REGION_NAME);
        area.addSubRegionBox(box, true);

        LitematicaSchematic schematic = LitematicaSchematic.createEmptySchematic(area, "WeGui");
        if (schematic == null) {
            WeGuiMod.LOGGER.warn("[WeGui] createEmptySchematic 返回 null");
            return;
        }

        LitematicaBlockStateContainer container = schematic.getSubRegionContainer(REGION_NAME);
        if (container == null) {
            WeGuiMod.LOGGER.warn("[WeGui] getSubRegionContainer 返回 null");
            return;
        }

        Vec3i size = container.getSize();
        int sizeX = size.getX();
        int sizeY = size.getY();
        int sizeZ = size.getZ();

        for (Map.Entry<BlockPos, BlockState> e : blocks.entrySet()) {
            BlockPos rel = e.getKey().subtract(minPos);
            int x = rel.getX();
            int y = rel.getY();
            int z = rel.getZ();
            if (x < 0 || x >= sizeX || y < 0 || y >= sizeY || z < 0 || z >= sizeZ) {
                continue;
            }
            container.set(x, y, z, e.getValue());
        }

        BlockPos placementOrigin = worldMin;
        SchematicPlacement placement = SchematicPlacement.createFor(
            schematic, placementOrigin, WEGUI_PLACEMENT_NAME, true, true);

        SchematicPlacementManager mgr = DataManager.getSchematicPlacementManager();
        mgr.addSchematicPlacement(placement, false);

        currentSchematic = schematic;
        currentPlacement = placement;
        lastSyncedOrigin = origin;
    }

    private static void removeCurrentPlacement() {
        if (currentPlacement != null) {
            SchematicPlacementManager mgr = DataManager.getSchematicPlacementManager();
            mgr.removeSchematicPlacement(currentPlacement);
            currentPlacement = null;
            currentSchematic = null;
            lastSyncedOrigin = null;
        }
    }

    /** 用 malilib RenderUtils 渲染 WE 选区框：三轴颜色的区域轮廓 + 两个角点方块边框。
     * 26.2 适配：malilib 0.29.2 的 RenderUtils.renderAreaOutline/renderBlockOutline（5 参数版带 throughView）。
     * 参数语义（与 Litematica OverlayRenderer.renderSelectionBox AREA_SELECTED 分支保持一致）：
     *   - 区域轮廓线宽 1.5f，三轴颜色 X=红/Y=绿/Z=蓝
     *   - 角点方块边框 expand=0.001f（避免 Z-fighting），线宽 2.0f，白色
     *
     * 边框透视（selectionBoxThroughView）：
     *   - 区域轮廓：malilib renderAreaOutline 内部固定 NO_DEPTH_NO_CULL，始终透视，无法控制。
     *   - 角点方块边框：renderBlockOutline 第 5 参数 throughView 控制透视/不透视。 */
    private static final class WeSelectionRenderer implements IRenderer {
        private static final Color4f COLOR_X = new Color4f(1.0f, 0.0625f, 0.0625f);
        private static final Color4f COLOR_Y = new Color4f(0.0625f, 1.0f, 0.0625f);
        private static final Color4f COLOR_Z = new Color4f(0.0625f, 0.0625f, 1.0f);
        private static final Color4f COLOR_CORNER = new Color4f(1.0f, 1.0f, 1.0f);
        // 侧面半透明白色（与 Litematica AREA_SELECTED 的 colorArea + alpha 0.4 一致）
        private static final Color4f COLOR_AREA_SIDES = new Color4f(1.0f, 1.0f, 1.0f, 0.4f);
        private static final float LINE_WIDTH = 1.5f;

        @Override
        public void onRenderWorldLast(RenderTarget renderTarget,
                                       Matrix4fc matrix,
                                       CameraRenderState camera,
                                       Frustum frustum,
                                       RenderBuffers buffers,
                                       GpuBufferSlice slice,
                                       Vector4f vec,
                                       ProfilerFiller profiler) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;
            if (!Configs.Generic.PASTE_PREVIEW_ENABLED.getBooleanValue()) return;

            WorldEditBridge.PartialCornerPositions corners = WorldEditBridge.getPartialSelectionCorners(mc);
            if (corners == null) return;

            BlockPos pos1 = corners.pos1();
            BlockPos pos2 = corners.pos2();
            boolean hasPos1 = pos1 != null;
            boolean hasPos2 = pos2 != null;
            // 两点都未设：无选区可渲染
            if (!hasPos1 && !hasPos2) return;
            // 只设了一个点：只渲染该点的单方块白色边框，不画区域轮廓
            if (hasPos1 != hasPos2) {
                BlockPos only = hasPos1 ? pos1 : pos2;
                boolean throughView = Configs.Generic.SELECTION_BOX_THROUGH_VIEW.getBooleanValue();
                try {
                    RenderUtils.renderBlockOutline(only, 0.001f, 2.0f, COLOR_CORNER, throughView);
                } catch (Throwable ex) {
                    WeGuiMod.LOGGER.error("[WeGui] renderBlockOutline (single) failed", ex);
                }
                return;
            }

            // 两点都已设：渲染完整长方体选区（侧面 + 区域轮廓 + 两个角点方块边框）
            boolean throughView = Configs.Generic.SELECTION_BOX_THROUGH_VIEW.getBooleanValue();

            // 侧面半透明白色面（throughView=true 透视，false 不透视）
            // malilib renderAreaSides 的 throughView 参数只控制顶点排序，不控制深度测试
            // （两个 pipeline 都是 LEQUAL_DEPTH，始终被方块遮挡）。因此透视模式需自己用
            // NO_DEPTH_NO_CULL pipeline 渲染。
            try {
                if (throughView) {
                    renderAreaSidesThrough(pos1, pos2);
                } else {
                    RenderUtils.renderAreaSides(pos1, pos2, COLOR_AREA_SIDES);
                }
            } catch (Throwable ex) {
                WeGuiMod.LOGGER.error("[WeGui] renderAreaSides failed", ex);
            }

            // 区域轮廓（三色线条）
            // malilib renderAreaOutline 内部硬编码 NO_DEPTH_NO_CULL（始终透视，无法关闭）。
            // 不透视模式需自己用 LEQUAL_DEPTH pipeline 渲染。
            try {
                if (throughView) {
                    RenderUtils.renderAreaOutline(pos1, pos2, LINE_WIDTH, COLOR_X, COLOR_Y, COLOR_Z);
                } else {
                    renderAreaOutlineDepth(pos1, pos2);
                }
            } catch (Throwable ex) {
                WeGuiMod.LOGGER.error("[WeGui] renderAreaOutline failed", ex);
            }

            // 角点方块边框（throughView=true 透视，false 不透视）
            try {
                RenderUtils.renderBlockOutline(pos1, 0.001f, 2.0f, COLOR_CORNER, throughView);
                RenderUtils.renderBlockOutline(pos2, 0.001f, 2.0f, COLOR_CORNER, throughView);
            } catch (Throwable ex) {
                WeGuiMod.LOGGER.error("[WeGui] renderBlockOutline failed", ex);
            }
        }

        /**
         * 透视模式渲染选区六个半透明面：用 NO_DEPTH_NO_CULL pipeline（无深度测试、无背面剔除），
         * 面穿过世界方块可见。仿 malilib renderAreaSidesBatched 的几何计算（expand 0.002 避免 Z-fighting）。
         */
        private static void renderAreaSidesThrough(BlockPos pos1, BlockPos pos2) {
            net.minecraft.world.phys.Vec3 cam = RenderUtils.camPos();
            double expand = 0.002;
            float minX = (float) (Math.min(pos1.getX(), pos2.getX()) - cam.x - expand);
            float minY = (float) (Math.min(pos1.getY(), pos2.getY()) - cam.y - expand);
            float minZ = (float) (Math.min(pos1.getZ(), pos2.getZ()) - cam.z - expand);
            float maxX = (float) (Math.max(pos1.getX(), pos2.getX()) + 1.0 - cam.x + expand);
            float maxY = (float) (Math.max(pos1.getY(), pos2.getY()) + 1.0 - cam.y + expand);
            float maxZ = (float) (Math.max(pos1.getZ(), pos2.getZ()) + 1.0 - cam.z + expand);

            try (RenderContext ctx = new RenderContext(
                    () -> "wegui_area_sides_through",
                    MaLiLibPipelines.POSITION_COLOR_TRANSLUCENT_NO_DEPTH_NO_CULL, 0)) {
                com.mojang.blaze3d.vertex.BufferBuilder b = ctx.getBuilder();
                float r = COLOR_AREA_SIDES.r, g = COLOR_AREA_SIDES.g, bl = COLOR_AREA_SIDES.b, a = COLOR_AREA_SIDES.a;
                // 底面 (y=minY)
                quad(b, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, r, g, bl, a);
                // 顶面 (y=maxY)
                quad(b, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, r, g, bl, a);
                // 北面 (z=minZ)
                quad(b, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, r, g, bl, a);
                // 南面 (z=maxZ)
                quad(b, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, bl, a);
                // 西面 (x=minX)
                quad(b, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, r, g, bl, a);
                // 东面 (x=maxX)
                quad(b, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, r, g, bl, a);
                com.mojang.blaze3d.vertex.MeshData mesh = b.build();
                if (mesh != null) {
                    ctx.upload(mesh, false);
                    ctx.drawPost();
                    mesh.close();
                }
            } catch (Throwable ex) {
                WeGuiMod.LOGGER.error("[WeGui] renderAreaSidesThrough draw failed", ex);
            }
        }

        /**
         * 不透视模式渲染三色区域轮廓线：用 LEQUAL_DEPTH pipeline（带深度测试，被方块遮挡）。
         * 仿 malilib drawBoundingBoxEdges 实现（malilib 原方法硬编码 NO_DEPTH_NO_CULL 始终透视）。
         */
        private static void renderAreaOutlineDepth(BlockPos pos1, BlockPos pos2) {
            net.minecraft.world.phys.Vec3 cam = RenderUtils.camPos();
            float minX = (float) (Math.min(pos1.getX(), pos2.getX()) - cam.x);
            float minY = (float) (Math.min(pos1.getY(), pos2.getY()) - cam.y);
            float minZ = (float) (Math.min(pos1.getZ(), pos2.getZ()) - cam.z);
            float maxX = (float) (Math.max(pos1.getX(), pos2.getX()) + 1.0 - cam.x);
            float maxY = (float) (Math.max(pos1.getY(), pos2.getY()) + 1.0 - cam.y);
            float maxZ = (float) (Math.max(pos1.getZ(), pos2.getZ()) + 1.0 - cam.z);

            try (RenderContext ctx = new RenderContext(
                    () -> "wegui_area_outline_depth",
                    MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_LEQUAL_DEPTH, 0)) {
                com.mojang.blaze3d.vertex.BufferBuilder b = ctx.getBuilder();
                // X 方向 4 条边（红色）
                lineVert(b, minX, minY, minZ, maxX, minY, minZ, COLOR_X);
                lineVert(b, minX, maxY, minZ, maxX, maxY, minZ, COLOR_X);
                lineVert(b, minX, minY, maxZ, maxX, minY, maxZ, COLOR_X);
                lineVert(b, minX, maxY, maxZ, maxX, maxY, maxZ, COLOR_X);
                // Y 方向 4 条边（绿色）
                lineVert(b, minX, minY, minZ, minX, maxY, minZ, COLOR_Y);
                lineVert(b, maxX, minY, minZ, maxX, maxY, minZ, COLOR_Y);
                lineVert(b, minX, minY, maxZ, minX, maxY, maxZ, COLOR_Y);
                lineVert(b, maxX, minY, maxZ, maxX, maxY, maxZ, COLOR_Y);
                // Z 方向 4 条边（蓝色）
                lineVert(b, minX, minY, minZ, minX, minY, maxZ, COLOR_Z);
                lineVert(b, maxX, minY, minZ, maxX, minY, maxZ, COLOR_Z);
                lineVert(b, minX, maxY, minZ, minX, maxY, maxZ, COLOR_Z);
                lineVert(b, maxX, maxY, minZ, maxX, maxY, maxZ, COLOR_Z);
                com.mojang.blaze3d.vertex.MeshData mesh = b.build();
                if (mesh != null) {
                    ctx.upload(mesh, false);
                    ctx.drawPost();
                    mesh.close();
                }
            } catch (Throwable ex) {
                WeGuiMod.LOGGER.error("[WeGui] renderAreaOutlineDepth draw failed", ex);
            }
        }

        private static void lineVert(com.mojang.blaze3d.vertex.BufferBuilder b,
                                     float x1, float y1, float z1,
                                     float x2, float y2, float z2,
                                     Color4f color) {
            b.addVertex(x1, y1, z1).setColor(color.r, color.g, color.b, color.a).setLineWidth(LINE_WIDTH);
            b.addVertex(x2, y2, z2).setColor(color.r, color.g, color.b, color.a).setLineWidth(LINE_WIDTH);
        }

        private static void quad(com.mojang.blaze3d.vertex.BufferBuilder b,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float x3, float y3, float z3,
                                 float x4, float y4, float z4,
                                 float r, float g, float bl, float a) {
            b.addVertex(x1, y1, z1).setColor(r, g, bl, a);
            b.addVertex(x2, y2, z2).setColor(r, g, bl, a);
            b.addVertex(x3, y3, z3).setColor(r, g, bl, a);
            b.addVertex(x4, y4, z4).setColor(r, g, bl, a);
        }
    }
}
