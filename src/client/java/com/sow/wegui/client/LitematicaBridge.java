package com.sow.wegui.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.sow.wegui.WeGuiMod;
import com.sow.wegui.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.malilib.event.RenderEventHandler;
import fi.dy.masa.malilib.interfaces.IRenderer;
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
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Map;

/**
 * 把 WorldEdit 剪贴板与选区同步到 Litematica 的渲染系统。
 * - 剪贴板：通过 SchematicPlacementManager 注入 placement，Litematica 自动渲染 ghost blocks 与 mismatch
 * - 选区框：在 malilib onRenderWorldLastAdvanced 中调用 OverlayRenderer.renderSelectionBox
 *
 * 1.21.11 适配：malilib 0.27.x 的 IRenderer 接口方法为 onRenderWorldLastAdvanced，
 * 签名 (RenderTarget, Matrix4f posMatrix, Matrix4f projMatrix, Frustum, Camera, RenderBuffers, ProfilerFiller)。
 */
public final class LitematicaBridge {
    private static final String WEGUI_PLACEMENT_NAME = "WeGui Clipboard Sync";
    private static final String REGION_NAME = "Main";

    @Nullable private static LitematicaSchematic currentSchematic;
    @Nullable private static SchematicPlacement currentPlacement;
    private static int lastClipboardHash = 0;
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
        lastClipboardHash = 0;
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
            lastClipboardHash = 0;
            return;
        }

        Map<BlockPos, BlockState> blocks = WorldEditBridge.getClipboardBlocks(mc);
        if (blocks == null || blocks.isEmpty()) {
            removeCurrentPlacement();
            lastClipboardHash = 0;
            return;
        }

        int hash = blocks.hashCode();
        if (hash == lastClipboardHash) {
            maybeResyncForOriginChange(mc, blocks);
            return;
        }
        lastClipboardHash = hash;

        BlockPos origin = AxeModeHandler.getEffectiveOrigin(mc);
        syncToLitematica(blocks, origin);
    }

    /** origin 未变化则跳过；FIXED 模式下 fixedOrigin 改变、FOLLOW_PLAYER 模式下玩家或偏移改变都会触发重同步。 */
    private static void maybeResyncForOriginChange(Minecraft mc, Map<BlockPos, BlockState> blocks) {
        BlockPos newOrigin = AxeModeHandler.getEffectiveOrigin(mc);
        if (newOrigin.equals(lastSyncedOrigin)) {
            return;
        }
        syncToLitematica(blocks, newOrigin);
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
     * 1.21.11 适配：litematica 0.25.0 的 OverlayRenderer.BoxType 是 package-private，
     * 无法从外部包访问，因此直接调用 malilib 公开的 RenderUtils.renderAreaOutline + renderBlockOutline。
     * 参数语义（与 Litematica OverlayRenderer.renderSelectionBox AREA_SELECTED 分支保持一致）：
     *   - 区域轮廓线宽 1.5f，三轴颜色 X=红/Y=绿/Z=蓝
     *   - 角点方块边框 expand=0.001f（避免 Z-fighting），线宽 2.0f，白色 */
    private static final class WeSelectionRenderer implements IRenderer {
        private static final Color4f COLOR_X = new Color4f(1.0f, 0.0625f, 0.0625f);
        private static final Color4f COLOR_Y = new Color4f(0.0625f, 1.0f, 0.0625f);
        private static final Color4f COLOR_Z = new Color4f(0.0625f, 0.0625f, 1.0f);
        private static final Color4f COLOR_CORNER = new Color4f(1.0f, 1.0f, 1.0f);

        @Override
        public void onRenderWorldLastAdvanced(RenderTarget target, Matrix4f posMatrix, Matrix4f projMatrix,
                                              Frustum frustum, Camera camera, RenderBuffers buffers,
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

            // 区域轮廓（三轴颜色，与 Litematica AREA_SELECTED 行为一致）
            RenderUtils.renderAreaOutline(pos1, pos2, 1.5f, COLOR_X, COLOR_Y, COLOR_Z);
            // 两个角点方块边框
            RenderUtils.renderBlockOutline(pos1, 0.001f, 2.0f, COLOR_CORNER);
            RenderUtils.renderBlockOutline(pos2, 0.001f, 2.0f, COLOR_CORNER);
        }
    }
}
