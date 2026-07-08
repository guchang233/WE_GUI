package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.extension.platform.permission.ActorSelectorLimits;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.regions.selector.ExtendingCuboidRegionSelector;
import com.sk89q.worldedit.world.storage.ChunkStore;
import net.minecraft.client.Minecraft;

import java.util.List;

import static com.sk89q.worldedit.world.storage.ChunkStore.CHUNK_SHIFTS;
import static com.sk89q.worldedit.world.storage.ChunkStore.CHUNK_SHIFTS_Y;

/**
 * //chunk 的直接 API 实现。
 */
public final class ChunkSelOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        WeOperationHelper.Context ctx = WeOperationHelper.requireSessionContext(mc);
        if (ctx == null) {
            error(mc, "无法获取 WorldEdit 会话");
            return false;
        }

        boolean all = "chunk_all".equals(usage.id());
        BlockVector3 min;
        BlockVector3 max;

        if (all) {
            Region region = WeOperationHelper.requireSelection(ctx.session, ctx.world, ctx.player);
            if (region == null) {
                error(mc, "无法获取选区");
                return false;
            }

            int minChunkY = ctx.world.getMinY() >> CHUNK_SHIFTS_Y;
            int maxChunkY = ctx.world.getMaxY() >> CHUNK_SHIFTS_Y;

            BlockVector3 minChunk = ChunkStore.toChunk3d(region.getMinimumPoint()).clampY(minChunkY, maxChunkY);
            BlockVector3 maxChunk = ChunkStore.toChunk3d(region.getMaximumPoint()).clampY(minChunkY, maxChunkY);

            min = minChunk.shl(CHUNK_SHIFTS, CHUNK_SHIFTS_Y, CHUNK_SHIFTS);
            max = maxChunk.shl(CHUNK_SHIFTS, CHUNK_SHIFTS_Y, CHUNK_SHIFTS).add(15, 255, 15);
        } else {
            BlockVector3 minChunk = ChunkStore.toChunk3d(ctx.player.getLocation().toVector().toBlockPoint());
            min = minChunk.shl(CHUNK_SHIFTS, CHUNK_SHIFTS_Y, CHUNK_SHIFTS);
            max = min.add(15, 255, 15);
        }

        CuboidRegionSelector selector;
        if (ctx.session.getRegionSelector(ctx.world) instanceof ExtendingCuboidRegionSelector) {
            selector = new ExtendingCuboidRegionSelector(ctx.world);
        } else {
            selector = new CuboidRegionSelector(ctx.world);
        }
        selector.selectPrimary(min, ActorSelectorLimits.forActor(ctx.player));
        selector.selectSecondary(max, ActorSelectorLimits.forActor(ctx.player));
        ctx.session.setRegionSelector(ctx.world, selector);

        feedback(mc, "已选择" + (all ? "所有相邻区块" : "当前区块") + ": " + min + " -> " + max);
        return true;
    }
}
