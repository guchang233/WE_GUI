package com.sow.wegui.client;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.fabric.FabricAdapter;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.command.tool.InvalidToolBindException;
import com.sk89q.worldedit.command.tool.SelectionWand;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import com.sow.wegui.WeGuiMod;
import com.sow.wegui.WeStatus;
import com.sow.wegui.config.Configs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 读取 WorldEdit 状态与剪贴板边界，用于状态栏和粘贴预览。
 */
public final class WorldEditBridge {
    private WorldEditBridge() {}

    private static volatile boolean versionChecked = false;
    private static String cachedVersion = "";

    /** 无剪贴板状态短期缓存：避免每帧抛 EmptyClipboardException 的开销。 */
    private static volatile long noClipboardUntilTick = 0L;

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

    /**
     * 轻量级获取当前剪贴板 holder 引用，不遍历方块。
     * 用于 LitematicaBridge 检测剪贴板是否变化（O(1) 引用比较代替 O(n) hashCode）。
     * WE 每次 //copy 会创建新的 ClipboardHolder 实例，因此 == 比较即可判断内容是否变化。
     */
    @Nullable
    public static ClipboardHolder getClipboardHolder(Minecraft mc) {
        if (mc.player == null) return null;
        LocalSession session = WorldEditAdapter.session(mc.player);
        if (session == null) return null;
        long tick = mc.level != null ? mc.level.getGameTime() : 0L;
        if (noClipboardUntilTick > 0 && tick < noClipboardUntilTick) {
            return null;
        }
        try {
            ClipboardHolder holder = session.getClipboard();
            if (holder == null) {
                noClipboardUntilTick = tick + 20;
                return null;
            }
            noClipboardUntilTick = 0L;
            return holder;
        } catch (Throwable e) {
            noClipboardUntilTick = tick + 20;
            return null;
        }
    }

    @Nullable
    private static ClipboardBounds getClipboardBounds(@Nullable LocalSession session) {
        if (session == null) return null;
        Minecraft mc = Minecraft.getInstance();
        long tick = mc.level != null ? mc.level.getGameTime() : 0L;
        if (noClipboardUntilTick > 0 && tick < noClipboardUntilTick) {
            return null;
        }
        try {
            ClipboardHolder holder = session.getClipboard();
            if (holder == null) {
                noClipboardUntilTick = tick + 20;
                return null;
            }
            com.sk89q.worldedit.extent.clipboard.Clipboard clipboard = holder.getClipboard();
            BlockVector3 min = clipboard.getMinimumPoint();
            BlockVector3 max = clipboard.getMaximumPoint();
            BlockVector3 origin = clipboard.getOrigin();
            noClipboardUntilTick = 0L;
            return new ClipboardBounds(
                    min.x(), min.y(), min.z(),
                    max.x() - min.x() + 1, max.y() - min.y() + 1, max.z() - min.z() + 1,
                    origin.x(), origin.y(), origin.z());
        } catch (Throwable e) {
            noClipboardUntilTick = tick + 20;
            return null;
        }
    }

    private static String versionOrEmpty() {
        if (versionChecked) return cachedVersion;
        try {
            cachedVersion = WorldEdit.getInstance().getPlatformManager()
                    .queryCapability(Capability.USER_COMMANDS).getPlatformVersion();
        } catch (Throwable e) {
            cachedVersion = "";
        }
        versionChecked = true;
        return cachedVersion;
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
                        try {
                            BlockVector3 pos = BlockVector3.at(x, y, z);
                            BaseBlock base = clipboard.getFullBlock(pos);

                            // 应用变换到位置（绕剪贴板 origin）
                            BlockVector3 local = pos.subtract(origin);
                            BlockVector3 transformed = transform.apply(local.toVector3()).toBlockPoint();
                            BlockPos target = new BlockPos(transformed.x(), transformed.y(), transformed.z());

                            if (base.getBlockType().id().equals("minecraft:air")) {
                                // 保留空气位置用于 mismatch 验证
                                result.put(target, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                                continue;
                            }

                            // 应用变换到方块状态
                            BaseBlock transformedBase = BlockTransformExtent.transform(base, transform);
                            BlockState state = FabricAdapter.get().toNativeBlockState(transformedBase.toImmutableState());
                            if (state == null || state.isAir()) {
                                result.put(target, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                                continue;
                            }

                            result.put(target, state);
                        } catch (Throwable e) {
                            WeGuiMod.LOGGER.debug("跳过无法转换的剪贴板方块: {}", e.toString());
                        }
                    }
                }
            }
            return result;
        } catch (Throwable e) {
            WeGuiMod.LOGGER.debug("读取 WorldEdit 剪贴板方块失败: {}", e.toString());
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

    /**
     * 获取 WorldEdit 选区中已经设置过的角点，允许只设置了一个点。
     * 仅对 CuboidRegionSelector 有效；pos2 可能为 null。
     */
    @Nullable
    public static PartialCornerPositions getPartialSelectionCorners(Minecraft mc) {
        if (mc.player == null) return null;
        LocalSession session = WorldEditAdapter.session(mc.player);
        if (session == null) return null;
        try {
            com.sk89q.worldedit.world.World weWorld = session.getSelectionWorld();
            if (weWorld == null) return null;
            RegionSelector selector = session.getRegionSelector(weWorld);
            if (!(selector instanceof CuboidRegionSelector cuboidSelector)) return null;

            CuboidRegion incomplete = cuboidSelector.getIncompleteRegion();
            if (incomplete == null) return null;
            BlockVector3 p1 = incomplete.getPos1();
            BlockVector3 p2 = incomplete.getPos2();
            return new PartialCornerPositions(
                    p1 == null ? null : new BlockPos(p1.x(), p1.y(), p1.z()),
                    p2 == null ? null : new BlockPos(p2.x(), p2.y(), p2.z()));
        } catch (Throwable e) {
            return null;
        }
    }

    public record PartialCornerPositions(@Nullable BlockPos pos1, @Nullable BlockPos pos2) {
    }

    /**
     * 读取 WorldEdit 当前的默认选区魔杖物品 ID。
     */
    public static String getWandItem() {
        try {
            String itemId = WorldEdit.getInstance().getConfiguration().wandItem;
            return itemId == null ? "" : itemId;
        } catch (Throwable e) {
            WeGuiMod.LOGGER.debug("读取 WorldEdit wandItem 失败: {}", e.toString());
            return "";
        }
    }

    /**
     * 同步设置 WorldEdit 的默认选区魔杖物品 ID。
     * 这样修改本模组的 wandItem 配置时，WorldEdit 也会真正识别新工具。
     */
    public static void setWandItem(String itemId) {
        try {
            WorldEdit.getInstance().getConfiguration().wandItem = itemId;
        } catch (Throwable e) {
            WeGuiMod.LOGGER.debug("设置 WorldEdit wandItem 失败: {}", e.toString());
        }
    }

    /**
     * 将指定物品 ID 注册为当前玩家的选区魔杖工具。
     * 只有调用 LocalSession.setTool 后，WorldEdit 才会在玩家手持该物品时响应左右键。
     */
    public static void bindWandItem(String itemId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        LocalSession session = WorldEditAdapter.session(mc.player);
        if (session == null) return;

        try {
            ItemType itemType = ItemType.REGISTRY.get(itemId);
            if (itemType == null) {
                WeGuiMod.LOGGER.debug("无法解析为 WorldEdit ItemType: {}", itemId);
                return;
            }
            session.setTool(itemType, new SelectionWand());
        } catch (InvalidToolBindException e) {
            WeGuiMod.LOGGER.debug("绑定选区魔杖失败: {}", e.toString());
        } catch (Throwable e) {
            WeGuiMod.LOGGER.debug("绑定选区魔杖时出错: {}", e.toString());
        }
    }

    /**
     * 当前是否为单人世界（本地服务端）。
     */
    public static boolean isLocalServer() {
        Minecraft mc = Minecraft.getInstance();
        return mc.getSingleplayerServer() != null;
    }

    /**
     * 是否可以直接调用 WorldEdit API 在指定坐标粘贴（单人世界且 WE 已加载）。
     */
    public static boolean canUseDirectPaste() {
        return WorldEditAdapter.isLoaded() && isLocalServer();
    }

    /**
     * 在单人世界中直接调用 WorldEdit API，在 target 位置粘贴当前剪贴板。
     * 会应用 holder 中已有的 //flip、//rotate 等变换。
     * 异步调度到服务端线程执行，避免 C2ME 等线程安全 mod 的并发修改异常。
     * 返回 true 表示已成功调度（不代表粘贴已完成），false 表示前置检查失败。
     */
    public static boolean pasteClipboardAt(LocalPlayer player, BlockPos target) {
        if (!canUseDirectPaste()) {
            WeGuiMod.LOGGER.error("[WE GUI] pasteClipboardAt: canUseDirectPaste=false");
            return false;
        }
        try {
            MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
            if (server == null) {
                WeGuiMod.LOGGER.error("[WE GUI] pasteClipboardAt: server=null");
                return false;
            }

            ServerLevel level = server.getLevel(player.level().dimension());
            if (level == null) {
                WeGuiMod.LOGGER.error("[WE GUI] pasteClipboardAt: level=null, dim={}", player.level().dimension());
                return false;
            }

            com.sk89q.worldedit.world.World weWorld = FabricAdapter.get().fromNativeWorld(level);
            LocalSession session = WorldEditAdapter.session(player);
            if (session == null) {
                WeGuiMod.LOGGER.error("[WE GUI] pasteClipboardAt: session=null, player={}", player.getName().getString());
                return false;
            }

            ClipboardHolder holder;
            try {
                holder = session.getClipboard();
            } catch (Throwable e) {
                WeGuiMod.LOGGER.error("[WE GUI] pasteClipboardAt: getClipboard 失败: {}", e.toString());
                return false;
            }
            if (holder == null) {
                WeGuiMod.LOGGER.error("[WE GUI] pasteClipboardAt: holder=null");
                return false;
            }

            final ClipboardHolder finalHolder = holder;
            final com.sk89q.worldedit.world.World finalWeWorld = weWorld;
            final LocalSession finalSession = session;
            final int tx = target.getX(), ty = target.getY(), tz = target.getZ();

            WeGuiMod.LOGGER.info("[WE GUI] pasteClipboardAt: 调度到服务端线程, target=({}, {}, {})", tx, ty, tz);
            final ServerLevel finalLevel = level;
            final boolean replaceAirOnly = Configs.PastePreview.PASTE_REPLACE_AIR_ONLY.getBooleanValue();
            server.execute(() -> {
                try (EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(finalWeWorld, -1)) {
                    // replaceAirOnly=true 时用 AirOnlyExtent 包装：只在目标位置当前为空气时放置剪贴板方块。
                    // 同时 ignoreAirBlocks(true) 跳过剪贴板中的空气方块（无需把空气“放置”到空气上）。
                    Extent targetExtent = replaceAirOnly ? new AirOnlyExtent(editSession) : editSession;
                    Operation operation = finalHolder.createPaste(targetExtent)
                            .to(BlockVector3.at(tx, ty, tz))
                            .ignoreAirBlocks(replaceAirOnly)
                            .build();
                    Operations.complete(operation);
                    finalSession.remember(editSession);
                    WeGuiMod.LOGGER.info("[WE GUI] pasteClipboardAt: 粘贴成功 at ({}, {}, {})", tx, ty, tz);

                    // 主动通知客户端方块更新：WorldEdit paste 可能绕过正常方块更新流程，
                    // 导致客户端 level.getBlockState 仍返回旧状态，使 mismatch overlay 显示错误颜色。
                    try {
                        com.sk89q.worldedit.extent.clipboard.Clipboard clipboard = finalHolder.getClipboard();
                        BlockVector3 cbMin = clipboard.getMinimumPoint();
                        BlockVector3 cbMax = clipboard.getMaximumPoint();
                        BlockVector3 cbOrigin = clipboard.getOrigin();
                        Transform transform = finalHolder.getTransform();
                        for (int x = cbMin.x(); x <= cbMax.x(); x++) {
                            for (int y = cbMin.y(); y <= cbMax.y(); y++) {
                                for (int z = cbMin.z(); z <= cbMax.z(); z++) {
                                    BlockVector3 pos = BlockVector3.at(x, y, z);
                                    BaseBlock base = clipboard.getFullBlock(pos);
                                    if (base.getBlockType().id().equals("minecraft:air")) continue;
                                    BlockVector3 local = pos.subtract(cbOrigin);
                                    BlockVector3 transformed = transform.apply(local.toVector3()).toBlockPoint();
                                    BlockPos notifyPos = new BlockPos(
                                            tx + transformed.x(),
                                            ty + transformed.y(),
                                            tz + transformed.z());
                                    var state = finalLevel.getBlockState(notifyPos);
                                    finalLevel.sendBlockUpdated(notifyPos, state, state, 3);
                                }
                            }
                        }
                    } catch (Throwable ex) {
                        WeGuiMod.LOGGER.debug("[WE GUI] 主动通知客户端方块更新失败: {}", ex.toString());
                    }
                } catch (Throwable e) {
                    WeGuiMod.LOGGER.error("[WE GUI] pasteClipboardAt: 服务端线程粘贴异常 at ({}, {}, {}): {}", tx, ty, tz, e);
                }
            });
            return true;
        } catch (Throwable e) {
            WeGuiMod.LOGGER.error("[WE GUI] pasteClipboardAt: 调度异常 at {}: {}", target, e);
            return false;
        }
    }

    /** 只允许在目标位置当前为空气时放置方块的 Extent 包装器。
     * 用于 PASTE_REPLACE_AIR_ONLY 配置：避免粘贴覆盖已有建筑。 */
    private static final class AirOnlyExtent extends AbstractDelegateExtent {
        AirOnlyExtent(Extent extent) {
            super(extent);
        }

        @Override
        public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 location, T block) throws WorldEditException {
            com.sk89q.worldedit.world.block.BlockState existing = getBlock(location);
            BlockMaterial mat = existing.getBlockType().getMaterial();
            if (mat == null || !mat.isAir()) {
                // 目标位置非空气，跳过（不替换已有方块）
                return true;
            }
            return super.setBlock(location, block);
        }
    }
}
