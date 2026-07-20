package com.sow.wegui.client;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.sow.wegui.WeGuiMod;
import com.sow.wegui.config.Configs;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.session.ClipboardHolder;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.render.OverlayRenderer;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.malilib.event.RenderEventHandler;
import fi.dy.masa.malilib.interfaces.IRenderer;
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
 * - 选区框：在 malilib onRenderWorldLast 中调用 OverlayRenderer.renderSelectionBox
 */
public final class LitematicaBridge {
    private static final String WEGUI_PLACEMENT_NAME = "WeGui Clipboard Sync";
    private static final String WEGUI_SELECTION_BOX_NAME = "WeGui WE Selection";
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

    /** 用 Litematica 的 OverlayRenderer 渲染 WE 选区框，BoxType=AREA_SELECTED 使用三轴颜色。
     * 每帧 new Box 避免 Box 内部派生状态跨帧不一致。
     * renderSelectionBox 的后三个 float 参数语义（经字节码反编译确认）：
     *   arg3 = pos1/pos2 方块边框的位置偏移（Litematica 传 0.001f；切勿传真实 partialTicks，会导致边框每帧放大缩小）
     *   arg4 = pos1/pos2 方块边框的线宽（Litematica 区域选区传 2.0f）
     *   arg5 = 区域轮廓线的线宽（Litematica 区域选区传 1.5f） */
    private static final class WeSelectionRenderer implements IRenderer {
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
            if (corners == null || corners.pos1() == null) {
                return;
            }

            BlockPos pos1 = corners.pos1();
            BlockPos pos2 = corners.pos2() != null ? corners.pos2() : pos1;
            Box box = new Box(pos1, pos2, WEGUI_SELECTION_BOX_NAME);

            OverlayRenderer.getInstance().renderSelectionBox(
                box,
                OverlayRenderer.BoxType.AREA_SELECTED,
                0.001f,
                2.0f, 1.5f,
                null
            );
        }
    }
}
