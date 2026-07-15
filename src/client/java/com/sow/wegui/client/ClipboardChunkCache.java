package com.sow.wegui.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 剪贴板方块按 16×16×16 chunk section 分组的缓存。
 *
 * <p>对齐 Litematica 的 {@code ChunkCacheSchematic} 思路：把零散的方块按 chunk section
 * 分组，每个 section 内的方块共享一个 {@link AABB} 用于 frustum 剔除。这样在渲染时
 * 只需要遍历少量 group，而不是数千个独立方块位置。</p>
 *
 * <p>数据结构对齐 Litematica：
 * <ul>
 *   <li>{@link ChunkGroup#relPositions}：相对 paste origin 的偏移坐标，对应 Litematica 的 section-relative pos</li>
 *   <li>{@link ChunkGroup#states}：方块状态列表，与 relPositions 一一对应</li>
 *   <li>{@link ChunkGroup#worldAabb}：section 在世界坐标系下的 AABB，用于 frustum 剔除</li>
 * </ul>
 * </p>
 *
 * <p>与 Litematica 的差异：Litematica 缓存了完整的 BlockState palette 和 chunk section 数据，
 * 此处简化为只缓存方块位置和状态（WE clipboard 是只读快照，无需 palette 压缩）。</p>
 */
public final class ClipboardChunkCache {

    /**
     * 一个 16×16×16 chunk section 内方块的分组。
     * 同一 section 内的方块共享 frustum 剔除 AABB，减少逐方块 frustum 测试。
     */
    public static final class ChunkGroup {
        /** 相对 paste origin 的偏移坐标（与 origin 相加得到世界坐标） */
        public final List<BlockPos> relPositions = new ArrayList<>();
        /** 该 section 内的方块状态（与 relPositions 同顺序、同长度） */
        public final List<BlockState> states = new ArrayList<>();
        /** 该 section 在世界坐标系下的 AABB（用于 frustum 剔除） */
        public final AABB worldAabb;

        ChunkGroup(AABB worldAabb) {
            this.worldAabb = worldAabb;
        }
    }

    private Map<Long, ChunkGroup> groups = new HashMap<>();
    private BlockPos origin = BlockPos.ZERO;
    private AABB totalBox = null;

    /**
     * 用 clipboard 方块重新构建分组。
     *
     * @param blocks clipboard 方块（key = 相对 origin 的偏移，value = 方块状态）
     * @param origin paste origin（世界坐标）
     */
    public void rebuild(Map<BlockPos, BlockState> blocks, BlockPos origin) {
        this.groups = new HashMap<>();
        this.origin = origin;
        this.totalBox = null;

        if (blocks == null || blocks.isEmpty()) return;

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            BlockPos rel = entry.getKey();
            BlockState state = entry.getValue();
            if (state == null || state.isAir()) continue;

            int sectionX = rel.getX() >> 4;
            int sectionY = rel.getY() >> 4;
            int sectionZ = rel.getZ() >> 4;
            long key = SectionPos.asLong(sectionX, sectionY, sectionZ);

            ChunkGroup group = groups.computeIfAbsent(key, k -> {
                // section 在世界坐标系下的 AABB
                int sx = origin.getX() + (sectionX << 4);
                int sy = origin.getY() + (sectionY << 4);
                int sz = origin.getZ() + (sectionZ << 4);
                return new ChunkGroup(new AABB(sx, sy, sz, sx + 16, sy + 16, sz + 16));
            });

            group.relPositions.add(rel);
            group.states.add(state);

            // 计算整体 AABB
            BlockPos world = origin.offset(rel);
            minX = Math.min(minX, world.getX());
            minY = Math.min(minY, world.getY());
            minZ = Math.min(minZ, world.getZ());
            maxX = Math.max(maxX, world.getX() + 1);
            maxY = Math.max(maxY, world.getY() + 1);
            maxZ = Math.max(maxZ, world.getZ() + 1);
        }

        if (!groups.isEmpty()) {
            this.totalBox = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        }
    }

    /** 获取所有 chunk section 分组（用于遍历渲染） */
    public Collection<ChunkGroup> getGroups() {
        return groups.values();
    }

    /** 获取所有方块的整体 AABB（用于整体 frustum 剔除） */
    public AABB getTotalBox() {
        return totalBox;
    }

    /** 获取 paste origin（世界坐标） */
    public BlockPos getOrigin() {
        return origin;
    }

    /** 是否为空（无方块） */
    public boolean isEmpty() {
        return groups.isEmpty();
    }

    /** 清空缓存 */
    public void clear() {
        groups = new HashMap<>();
        totalBox = null;
    }
}
