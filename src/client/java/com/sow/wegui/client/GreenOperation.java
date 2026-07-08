package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * /green 的直接 API 实现。
 */
public final class GreenOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        WeOperationHelper.Context ctx = WeOperationHelper.requireSessionContext(mc);
        if (ctx == null) {
            error(mc, "无法获取 WorldEdit 玩家对象");
            return false;
        }

        String flag = values.isEmpty() ? "" : values.get(0);
        boolean convertCoarse = flag.contains("-f");

        double radius = 5;
        if (values.size() >= 2 && !values.get(1).isBlank()) {
            try {
                radius = Double.parseDouble(values.get(1).trim());
            } catch (NumberFormatException e) {
                error(mc, "半径必须是数字");
                return false;
            }
        }

        try {
            double finalRadius = Math.max(1, radius);
            int height = Math.max(ctx.world.getMaxY() - ctx.world.getMinY(), 1);
            return WeOperationHelper.edit(mc, ctx.player, ctx.world, es ->
                    es.green(ctx.session.getPlacementPosition(ctx.player), finalRadius, height, !convertCoarse));
        } catch (Exception e) {
            error(mc, "执行失败: " + e.getMessage());
            return false;
        }
    }
}
