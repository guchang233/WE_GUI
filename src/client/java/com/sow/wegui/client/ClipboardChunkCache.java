package com.sow.wegui.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 把 clipboard 方块按 16×16×16 chunk section 分组缓存。
 * clipboard 内容变化时调 rebuild 重新分组；每帧渲染时遍历 groups 做 frustum 剔除。
 */
public final class ClipboardChunkCache {

    /** 一个 chunk section 内方块的分组 */
    public static final class ChunkGroup {
        /** 相对 paste origin 的偏移坐标 */
        public final java.util.List<BlockPos> relPositions = new ArrayList<>();
        /** 该 section 在世界坐标系下的 AABB（用于 frustum 剔除） */
        public final AABB worldAabb;
        /** 该 section 内的方块状态（与 relPositions 同顺序） */
        public final java.util.List<BlockState> states = new ArrayList<>();

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

    public Collection<ChunkGroup> getGroups() {
        return groups.values();
    }

    public AABB getTotalBox() {
        return totalBox;
    }

    public BlockPos getOrigin() {
        return origin;
    }

    public boolean isEmpty() {
        return groups.isEmpty();
    }

    public void clear() {
        groups = new HashMap<>();
        totalBox = null;
    }
}
