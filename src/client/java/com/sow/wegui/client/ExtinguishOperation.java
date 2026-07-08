package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.function.mask.BlockTypeMask;
import com.sk89q.worldedit.world.block.BlockTypes;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * /ex 的直接 API 实现。
 */
public final class ExtinguishOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        WeOperationHelper.Context ctx = WeOperationHelper.requireSessionContext(mc);
        if (ctx == null) {
            error(mc, "无法获取 WorldEdit 玩家对象");
            return false;
        }

        int radius = 40;
        if (!values.isEmpty() && !values.get(0).isBlank()) {
            try {
                radius = Integer.parseInt(values.get(0).trim());
            } catch (NumberFormatException e) {
                error(mc, "半径必须是整数");
                return false;
            }
        }

        try {
            int finalRadius = Math.max(1, radius);
            return WeOperationHelper.edit(mc, ctx.player, ctx.world, es ->
                    es.removeNear(ctx.session.getPlacementPosition(ctx.player), new BlockTypeMask(es, BlockTypes.FIRE), finalRadius));
        } catch (Exception e) {
            error(mc, "执行失败: " + e.getMessage());
            return false;
        }
    }
}
