package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * /removeabove 的直接 API 实现。
 */
public final class RemoveAboveOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        WeOperationHelper.Context ctx = WeOperationHelper.requireSessionContext(mc);
        if (ctx == null) {
            error(mc, "无法获取 WorldEdit 玩家对象");
            return false;
        }

        int size = 10;
        if (!values.isEmpty() && !values.get(0).isBlank()) {
            try {
                size = Integer.parseInt(values.get(0).trim());
            } catch (NumberFormatException e) {
                error(mc, "尺寸必须是整数");
                return false;
            }
        }

        int height = 5;
        if (values.size() >= 2 && !values.get(1).isBlank()) {
            try {
                height = Integer.parseInt(values.get(1).trim());
            } catch (NumberFormatException e) {
                error(mc, "高度必须是整数");
                return false;
            }
        }

        try {
            int finalSize = Math.max(1, size);
            int finalHeight = Math.max(1, height);
            return WeOperationHelper.edit(mc, ctx.player, ctx.world, es ->
                    es.removeAbove(ctx.session.getPlacementPosition(ctx.player), finalSize, finalHeight));
        } catch (Exception e) {
            error(mc, "执行失败: " + e.getMessage());
            return false;
        }
    }
}
