package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.world.block.BlockTypes;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * /fixwater 的直接 API 实现。
 */
public final class FixWaterOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        if (values.isEmpty() || values.get(0).isBlank()) {
            error(mc, "请指定半径");
            return false;
        }

        WeOperationHelper.Context ctx = WeOperationHelper.requireSessionContext(mc);
        if (ctx == null) {
            error(mc, "无法获取 WorldEdit 玩家对象");
            return false;
        }

        double radius;
        try {
            radius = Double.parseDouble(values.get(0).trim());
        } catch (NumberFormatException e) {
            error(mc, "半径必须是数字");
            return false;
        }

        try {
            double finalRadius = Math.max(0, radius);
            return WeOperationHelper.edit(mc, ctx.player, ctx.world, es ->
                    es.fixLiquid(ctx.session.getPlacementPosition(ctx.player), finalRadius, BlockTypes.WATER));
        } catch (Exception e) {
            error(mc, "执行失败: " + e.getMessage());
            return false;
        }
    }
}
