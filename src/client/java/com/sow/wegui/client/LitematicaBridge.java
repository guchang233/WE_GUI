package com.sow.wegui.client;

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
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Map;

/**
 * 把 WorldEdit 剪贴板与选区同步到 Litematica 的渲染系统。
 * - 剪贴板：通过 SchematicPlacementManager 注入 placement，Litematica 自动渲染 ghost blocks 与 mismatch
 * - 选区框：在 malilib onRenderWorldLastAdvanced 中调用 RenderUtils.renderAreaOutline + renderBlockOutline
 *
 * 1.21.11 适配：malilib 0.27.x 的 IRenderer 接口方法为 onRenderWorldLastAdvanced，
 * 签名 (RenderTarget, Matrix4f posMatrix, Matrix4f projMatrix, Frustum, Camera, RenderBuffers, ProfilerFiller)。
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

    /** 进入存档时重置同步状态（不主动清空 WE 剪贴板，避免破坏 WE 会话状态导致假死）。
     *  WE 自身的 session 会在玩家退出时由 WE 自己清理，无需本 mod 介入。 */
    private static void onWorldJoin(Minecraft mc) {
        // 仅重置本 mod 的同步状态，WE 剪贴板由 WE 自己管理
        lastHolder = null;
        lastTransform = null;
        lastSyncedOrigin = null;
        WeGuiMod.LOGGER.info("[WeGui] 存档连接：已重置 Litematica 同步状态");
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

    /** 用 malilib RenderUtils 渲染 WE 选区框：三轴颜色的区域轮廓 + 半透明侧面 + 两个角点方块边框。
     * 1.21.11 适配：直接调用 malilib 公开的 RenderUtils.renderAreaOutline + renderAreaSides + renderBlockOutline。
     * 参数语义（与 Litematica OverlayRenderer.renderSelectionBox AREA_SELECTED 分支保持一致）：
     *   - 区域轮廓线宽 1.5f，三轴颜色 X=红/Y=绿/Z=蓝
     *   - 半透明侧面 alpha=0.4，白色
     *   - 角点方块边框 expand=0.001f（避免 Z-fighting），线宽 2.0f，白色
     *
     * 边框透视（selectionBoxThroughView）：
     *   - 半透明侧面：renderAreaSides 默认 LEQUAL_DEPTH（不透视），透视模式通过 GL11 禁用 depth test
     *   - 区域轮廓：malilib renderAreaOutline 内部固定 NO_DEPTH_NO_CULL，始终透视，无法关闭（malilib 0.27.0 限制）
     *   - 角点方块边框：renderBlockOutline 第 5 参数 throughView 控制透视/不透视
     * 1.21.11 的 RenderSystem 没有 enableDepthTest/disableDepthTest 方法，用 GL11 替代。 */
    private static final class WeSelectionRenderer implements IRenderer {
        private static final Color4f COLOR_X = new Color4f(1.0f, 0.0625f, 0.0625f);
        private static final Color4f COLOR_Y = new Color4f(0.0625f, 1.0f, 0.0625f);
        private static final Color4f COLOR_Z = new Color4f(0.0625f, 0.0625f, 1.0f);
        private static final Color4f COLOR_CORNER = new Color4f(1.0f, 1.0f, 1.0f);
        // 侧面半透明白色（与 Litematica AREA_SELECTED 的 colorArea + alpha 0.4 一致）
        private static final Color4f COLOR_AREA_SIDES = new Color4f(1.0f, 1.0f, 1.0f, 0.4f);

        @Override
        public void onRenderWorldLastAdvanced(RenderTarget target, Matrix4f posMatrix, Matrix4f projMatrix,
                                              Frustum frustum, Camera camera, RenderBuffers buffers,
                                              ProfilerFiller profiler) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;
            if (!Configs.Generic.PASTE_PREVIEW_ENABLED.getBooleanValue()) return;

            WorldEditBridge.PartialCornerPositions corners = WorldEditBridge.getPartialSelectionCorners(mc);
            if (corners == null || corners.pos1() == null) return;

            BlockPos pos1 = corners.pos1();
            BlockPos pos2 = corners.pos2() != null ? corners.pos2() : pos1;

            boolean throughView = Configs.Generic.SELECTION_BOX_THROUGH_VIEW.getBooleanValue();

            try {
                // 1.21.11 渲染系统说明：
                //   malilib 0.27.16 用 RenderPipeline（不是 RenderType），通过 RenderPass.setPipeline()
                //   强制覆盖 GL 状态（depth test / cull 等），外部 GL11/GlStateManager 调用无效。
                //   因此只能依赖 malilib 方法自身的 Pipeline 选择来控制透视：
                //     - renderAreaSides：内部固定 LEQUAL_DEPTH（始终不透视，无法改为透视）
                //     - renderAreaOutline：内部固定 NO_DEPTH_NO_CULL（始终透视，无法关闭）
                //     - renderBlockOutline(pos, ..., throughView)：根据 throughView 选择 Pipeline

                // 1. 半透明侧面（malilib 固定 LEQUAL_DEPTH，始终不透视）
                try {
                    RenderUtils.renderAreaSides(pos1, pos2, COLOR_AREA_SIDES, posMatrix);
                } catch (Throwable ex) {
                    WeGuiMod.LOGGER.error("[WeGui] renderAreaSides failed", ex);
                }

                // 2. 区域轮廓（红/绿/蓝三轴颜色的 12 条边）：
                //    throughView=true  → malilib renderAreaOutline（NO_DEPTH_NO_CULL，透视，穿过方块可见）
                //    throughView=false → 自定义 LEQUAL_DEPTH 渲染（被世界方块遮挡，但仍显示红绿蓝三色）
                //    （malilib renderAreaOutline 内部固定 NO_DEPTH_NO_CULL 无法关闭透视，
                //     故 throughView=false 时改用 RenderContext + DEBUG_LINES_MASA_SIMPLE_LEQUAL_DEPTH Pipeline
                //     + 反射调用 malilib private drawBoundingBoxLinesX/Y/Z 实现三轴颜色的不透视轮廓）
                if (throughView) {
                    try {
                        RenderUtils.renderAreaOutline(pos1, pos2, 1.5f, COLOR_X, COLOR_Y, COLOR_Z);
                    } catch (Throwable ex) {
                        WeGuiMod.LOGGER.error("[WeGui] renderAreaOutline failed", ex);
                    }
                } else {
                    renderAreaOutlineLequalDepth(pos1, pos2, 1.5f, COLOR_X, COLOR_Y, COLOR_Z);
                }

                // 3. 角点方块边框（renderBlockOutline 第 5 参数 throughView 控制 Pipeline：
                //    true → NO_DEPTH_NO_CULL 透视；false → LEQUAL_DEPTH 不透视）
                try {
                    RenderUtils.renderBlockOutline(pos1, 0.001f, 2.0f, COLOR_CORNER, throughView);
                    if (corners.pos2() != null) {
                        RenderUtils.renderBlockOutline(pos2, 0.001f, 2.0f, COLOR_CORNER, throughView);
                    }
                } catch (Throwable ex) {
                    WeGuiMod.LOGGER.error("[WeGui] renderBlockOutline failed", ex);
                }
            } finally {
                // 恢复 depth test 到默认启用状态（保险起见，虽然对 RenderPipeline 无效）
                org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
            }
        }

        /**
         * 用 LEQUAL_DEPTH Pipeline 渲染区域轮廓（12 条边，红/绿/蓝三轴颜色），被世界方块遮挡。
         *
         * malilib 的 RenderUtils.renderAreaOutline 内部固定使用 NO_DEPTH_NO_CULL Pipeline（始终透视），
         * 无法通过外部 GL11/GlStateManager 关闭透视。为实现在 throughView=false 时让轮廓线被方块遮挡，
         * 自己创建 RenderContext 并指定 DEBUG_LINES_MASA_SIMPLE_LEQUAL_DEPTH Pipeline，然后通过反射调用
         * malilib 的 private static 方法 drawBoundingBoxLinesX/Y/Z 画 12 条边（每轴 4 条，分别用 X/Y/Z 颜色）。
         *
         * 完全模仿 malilib drawBoundingBoxEdges 的精确流程：
         *   ctx = new RenderContext(name, LEQUAL_DEPTH)
         *   builder = ctx.getBuilder()
         *   drawBoundingBoxLinesX/Y/Z(builder, ..., colorX/Y/Z, lineWidth)
         *   rendered = builder.build()
         *   ctx.draw(rendered, false, true)
         *   rendered.close()
         *   ctx.close()
         */
        private static void renderAreaOutlineLequalDepth(BlockPos pos1, BlockPos pos2,
                                                         float lineWidth,
                                                         Color4f colorX, Color4f colorY, Color4f colorZ) {
            try {
                // 用 malilib 的 camPos() 获取相机位置（避免依赖 Camera.getPosition() 的 mappings 差异）
                Vec3 camPos = RenderUtils.camPos();
                double minX = Math.min(pos1.getX(), pos2.getX()) - camPos.x;
                double minY = Math.min(pos1.getY(), pos2.getY()) - camPos.y;
                double minZ = Math.min(pos1.getZ(), pos2.getZ()) - camPos.z;
                double maxX = Math.max(pos1.getX(), pos2.getX()) + 1.0 - camPos.x;
                double maxY = Math.max(pos1.getY(), pos2.getY()) + 1.0 - camPos.y;
                double maxZ = Math.max(pos1.getZ(), pos2.getZ()) + 1.0 - camPos.z;

                float fMinX = (float) minX, fMinY = (float) minY, fMinZ = (float) minZ;
                float fMaxX = (float) maxX, fMaxY = (float) maxY, fMaxZ = (float) maxZ;

                RenderContext ctx = new RenderContext(
                        () -> "wegui:renderAreaOutlineLequalDepth",
                        MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_LEQUAL_DEPTH);
                try {
                    com.mojang.blaze3d.vertex.BufferBuilder builder = ctx.getBuilder();

                    // 反射调用 malilib private drawBoundingBoxLinesX/Y/Z 画 12 条边
                    Method mX = getDrawBoundingBoxLinesMethod("drawBoundingBoxLinesX");
                    Method mY = getDrawBoundingBoxLinesMethod("drawBoundingBoxLinesY");
                    Method mZ = getDrawBoundingBoxLinesMethod("drawBoundingBoxLinesZ");

                    mX.invoke(null, builder, fMinX, fMinY, fMinZ, fMaxX, fMaxY, fMaxZ, colorX, lineWidth);
                    mY.invoke(null, builder, fMinX, fMinY, fMinZ, fMaxX, fMaxY, fMaxZ, colorY, lineWidth);
                    mZ.invoke(null, builder, fMinX, fMinY, fMinZ, fMaxX, fMaxY, fMaxZ, colorZ, lineWidth);

                    // build() 返回 MeshData（对应 malilib 编译时的 class_9801，运行时 named mapping 重映射为 MeshData）
                    com.mojang.blaze3d.vertex.MeshData rendered = builder.build();
                    if (rendered != null) {
                        // 完全模仿 malilib drawBoundingBoxEdges：ctx.draw(rendered, false, true)
                        ctx.draw(rendered, false, true);
                        rendered.close();
                    }
                } finally {
                    ctx.close();
                }
            } catch (Throwable ex) {
                WeGuiMod.LOGGER.error("[WeGui] renderAreaOutlineLequalDepth failed", ex);
            }
        }

        /** 反射获取 malilib RenderUtils 的 private static drawBoundingBoxLinesX/Y/Z 方法。
         *  方法签名：drawBoundingBoxLinesX(BufferBuilder, float, float, float, float, float, float, Color4f, float)
         *  注意：malilib 这些方法是 private 的，必须 setAccessible(true) 才能调用，否则 IllegalAccessException
         *  BufferBuilder 类型用 malilib 编译时的 class_287（运行时 named mapping 下就是 BufferBuilder） */
        private static Method getDrawBoundingBoxLinesMethod(String methodName) throws NoSuchMethodException {
            // malilib 编译时参数类型是 net.minecraft.class_287，运行时 Fabric loader 重映射为
            // com.mojang.blaze3d.vertex.BufferBuilder。所以 getDeclaredMethod 用 BufferBuilder.class 查找。
            Method m = RenderUtils.class.getDeclaredMethod(methodName,
                    com.mojang.blaze3d.vertex.BufferBuilder.class,
                    float.class, float.class, float.class,
                    float.class, float.class, float.class,
                    Color4f.class, float.class);
            m.setAccessible(true);
            return m;
        }
    }
}
