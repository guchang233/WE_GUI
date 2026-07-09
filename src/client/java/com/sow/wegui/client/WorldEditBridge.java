package com.sow.wegui.client;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sow.wegui.WeGuiMod;
import com.sow.wegui.WeStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 读取 WorldEdit 状态与剪贴板边界，用于状态栏和粘贴预览。
 */
public final class WorldEditBridge {
    private WorldEditBridge() {}

    public static WeStatus capture(Minecraft mc) {
        if (mc.player == null) return WeStatus.noWorldEdit();
        if (!WorldEditAdapter.isLoaded()) return WeStatus.noWorldEdit();

        try {
            return captureInternal(mc.player);
        } catch (Throwable e) {
            WeGuiMod.LOGGER.debug("读取 WorldEdit 状态失败: {}", e.toString());
            return WeStatus.noSelection(versionOrEmpty(), false, 0, 0, 0);
        }
    }

    private static WeStatus captureInternal(LocalPlayer player) {
        LocalSession session = WorldEditAdapter.session(player);
        String version = versionOrEmpty();
        ClipboardBounds cb = getClipboardBounds(session);

        Region region = WorldEditAdapter.selection(player);

        if (region == null) {
            return WeStatus.noSelection(version, cb != null,
                    cb == null ? 0 : cb.w, cb == null ? 0 : cb.h, cb == null ? 0 : cb.l);
        }

        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        int w = max.x() - min.x() + 1;
        int h = max.y() - min.y() + 1;
        int l = max.z() - min.z() + 1;
        long count = region.getVolume();

        return WeStatus.ready(version, region.getClass().getSimpleName(), w, h, l, count,
                cb != null, cb == null ? 0 : cb.w, cb == null ? 0 : cb.h, cb == null ? 0 : cb.l);
    }

    @Nullable
    public static ClipboardBounds getClipboardBounds(Minecraft mc) {
        if (mc.player == null) return null;
        return getClipboardBounds(WorldEditAdapter.session(mc.player));
    }

    @Nullable
    private static ClipboardBounds getClipboardBounds(@Nullable LocalSession session) {
        if (session == null) return null;
        try {
            ClipboardHolder holder = session.getClipboard();
            if (holder == null) return null;
            com.sk89q.worldedit.extent.clipboard.Clipboard clipboard = holder.getClipboard();
            BlockVector3 min = clipboard.getMinimumPoint();
            BlockVector3 max = clipboard.getMaximumPoint();
            BlockVector3 origin = clipboard.getOrigin();
            return new ClipboardBounds(
                    min.x(), min.y(), min.z(),
                    max.x() - min.x() + 1, max.y() - min.y() + 1, max.z() - min.z() + 1,
                    origin.x(), origin.y(), origin.z());
        } catch (Throwable e) {
            return null;
        }
    }

    private static String versionOrEmpty() {
        try {
            return WorldEdit.getInstance().getPlatformManager()
                    .queryCapability(Capability.USER_COMMANDS).getPlatformVersion();
        } catch (Throwable e) {
            return "";
        }
    }

    @Nullable
    public static Bounds getSelectionBounds(Minecraft mc) {
        if (mc.player == null) return null;
        return getSelectionBounds(WorldEditAdapter.selection(mc.player));
    }

    @Nullable
    private static Bounds getSelectionBounds(@Nullable Region region) {
        if (region == null) return null;
        try {
            BlockVector3 min = region.getMinimumPoint();
            BlockVector3 max = region.getMaximumPoint();
            return new Bounds(
                    min.x(), min.y(), min.z(),
                    max.x() - min.x() + 1, max.y() - min.y() + 1, max.z() - min.z() + 1);
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * 获取当前 WorldEdit 剪贴板中的方块（已转换为 Minecraft BlockState）。
     * 返回的 Map 键为剪贴板内绝对坐标对应的目标世界坐标。
     */
    @Nullable
    public static Map<BlockPos, BlockState> getClipboardBlocks(Minecraft mc) {
        if (mc.player == null) return null;
        LocalSession session = WorldEditAdapter.session(mc.player);
        if (session == null) return null;
        try {
            ClipboardHolder holder = session.getClipboard();
            if (holder == null) return null;
            com.sk89q.worldedit.extent.clipboard.Clipboard clipboard = holder.getClipboard();
            BlockVector3 min = clipboard.getMinimumPoint();
            BlockVector3 max = clipboard.getMaximumPoint();
            BlockVector3 origin = clipboard.getOrigin();
            Transform transform = holder.getTransform();

            Map<BlockPos, BlockState> result = new HashMap<>();
            for (int x = min.x(); x <= max.x(); x++) {
                for (int y = min.y(); y <= max.y(); y++) {
                    for (int z = min.z(); z <= max.z(); z++) {
                        BlockVector3 pos = BlockVector3.at(x, y, z);
                        BaseBlock base = clipboard.getFullBlock(pos);
                        if (base.getBlockType().id().equals("minecraft:air")) continue;

                        // 应用 //flip、//rotate 等变换到方块状态（朝向、半砖上下等）
                        // 使用 WorldEdit 内部 paste 时相同的 BlockTransformExtent，确保与 //paste 结果一致
                        BaseBlock transformedBase = BlockTransformExtent.transform(base, transform);
                        BlockState state = convertToMcState(transformedBase.toImmutableState());
                        if (state == null || state.isAir()) continue;

                        // 应用变换到位置（绕剪贴板 origin）
                        BlockVector3 local = pos.subtract(origin);
                        BlockVector3 transformed = transform.apply(local.toVector3()).toBlockPoint();
                        BlockPos target = new BlockPos(transformed.x(), transformed.y(), transformed.z());
                        result.put(target, state);
                    }
                }
            }
            return result;
        } catch (Throwable e) {
            WeGuiMod.LOGGER.debug("读取 WorldEdit 剪贴板方块失败: {}", e.toString());
            return null;
        }
    }

    @Nullable
    private static BlockState convertToMcState(com.sk89q.worldedit.world.block.BlockState weState) {
        try {
            String str = weState.getAsString();
            return BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, str, true).blockState();
        } catch (CommandSyntaxException e) {
            WeGuiMod.LOGGER.debug("无法解析方块状态: {}", weState.getAsString());
            return null;
        }
    }

    public record ClipboardBounds(int minX, int minY, int minZ, int w, int h, int l, int originX, int originY, int originZ) {
    }

    public record Bounds(int minX, int minY, int minZ, int w, int h, int l) {
    }

    /**
     * 获取 WorldEdit 选区的两个原始角点（pos1、pos2）。
     * 仅对 CuboidRegion 有效，其他类型返回 null。
     */
    @Nullable
    public static CornerPositions getSelectionCorners(Minecraft mc) {
        if (mc.player == null) return null;
        Region region = WorldEditAdapter.selection(mc.player);
        if (!(region instanceof CuboidRegion cuboid)) return null;
        try {
            BlockVector3 p1 = cuboid.getPos1();
            BlockVector3 p2 = cuboid.getPos2();
            return new CornerPositions(
                    new BlockPos(p1.x(), p1.y(), p1.z()),
                    new BlockPos(p2.x(), p2.y(), p2.z()));
        } catch (Throwable e) {
            return null;
        }
    }

    public record CornerPositions(BlockPos pos1, BlockPos pos2) {
    }
}
