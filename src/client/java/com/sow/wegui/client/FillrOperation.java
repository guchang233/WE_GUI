package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.function.pattern.Pattern;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * //fillr 的直接 API 实现。
 */
public final class FillrOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        if (values.isEmpty() || values.get(0).isBlank()) {
            error(mc, "请指定填充图案");
            return false;
        }
        if (values.size() < 2 || values.get(1).isBlank()) {
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
            radius = Double.parseDouble(values.get(1).trim());
        } catch (NumberFormatException e) {
            error(mc, "半径必须是数字");
            return false;
        }

        int depth = Integer.MAX_VALUE;
        if (values.size() >= 3 && !values.get(2).isBlank()) {
            try {
                depth = Integer.parseInt(values.get(2).trim());
            } catch (NumberFormatException e) {
                error(mc, "深度必须是整数");
                return false;
            }
        }

        try {
            Pattern pattern = WeOperationHelper.parsePattern(values.get(0), ctx.player, ctx.world);
            int finalDepth = depth == Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max(1, depth);
            double finalRadius = Math.max(0, radius);
            return WeOperationHelper.edit(mc, ctx.player, ctx.world, es ->
                    es.fillXZ(ctx.session.getPlacementPosition(ctx.player), pattern, finalRadius, finalDepth, true));
        } catch (Exception e) {
            error(mc, "执行失败: " + e.getMessage());
            return false;
        }
    }
}
