package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * /replacenear 的直接 API 实现。
 */
public final class ReplaceNearOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        int rangeIndex = 0;
        int fromIndex = 1;
        int toIndex = 2;

        // 兼容 [-f] 占位参数：若第一个值是标志字符串，则后续索引后移
        if (!values.isEmpty() && values.get(0).startsWith("-")) {
            rangeIndex = 1;
            fromIndex = 2;
            toIndex = 3;
        }

        if (values.size() <= rangeIndex || values.get(rangeIndex).isBlank()) {
            error(mc, "请指定范围");
            return false;
        }
        if (values.size() <= toIndex || values.get(toIndex).isBlank()) {
            error(mc, "请指定被替换方块和目标图案");
            return false;
        }

        WeOperationHelper.Context ctx = WeOperationHelper.requireSessionContext(mc);
        if (ctx == null) {
            error(mc, "无法获取 WorldEdit 玩家对象");
            return false;
        }

        int range;
        try {
            range = Integer.parseInt(values.get(rangeIndex).trim());
        } catch (NumberFormatException e) {
            error(mc, "范围必须是整数");
            return false;
        }

        String fromInput = values.size() > fromIndex ? values.get(fromIndex) : "";
        String toInput = values.get(toIndex);

        try {
            Pattern pattern = WeOperationHelper.parsePattern(toInput, ctx.player, ctx.world);
            Mask mask = fromInput.isBlank() ? new ExistingBlockMask(ctx.world) : WeOperationHelper.parseMask(fromInput, ctx.player, ctx.world);
            int finalRange = Math.max(1, range);
            BlockVector3 base = ctx.session.getPlacementPosition(ctx.player);
            BlockVector3 min = base.subtract(finalRange, finalRange, finalRange);
            BlockVector3 max = base.add(finalRange, finalRange, finalRange);
            CuboidRegion region = new CuboidRegion(ctx.world, min, max);
            return WeOperationHelper.edit(mc, ctx.player, ctx.world, es -> es.replaceBlocks(region, mask, pattern));
        } catch (Exception e) {
            error(mc, "执行失败: " + e.getMessage());
            return false;
        }
    }
}
