package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.function.mask.Mask;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * /removenear 的直接 API 实现。
 */
public final class RemoveNearOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        if (values.isEmpty() || values.get(0).isBlank()) {
            error(mc, "请指定要移除的方块");
            return false;
        }

        WeOperationHelper.Context ctx = WeOperationHelper.requireSessionContext(mc);
        if (ctx == null) {
            error(mc, "无法获取 WorldEdit 玩家对象");
            return false;
        }

        int range = 5;
        if (values.size() >= 2 && !values.get(1).isBlank()) {
            try {
                range = Integer.parseInt(values.get(1).trim());
            } catch (NumberFormatException e) {
                error(mc, "范围必须是整数");
                return false;
            }
        }

        try {
            Mask mask = WeOperationHelper.parseMask(values.get(0), ctx.player, ctx.world);
            int finalRange = Math.max(1, range);
            return WeOperationHelper.edit(mc, ctx.player, ctx.world, es ->
                    es.removeNear(ctx.session.getPlacementPosition(ctx.player), mask, finalRange));
        } catch (Exception e) {
            error(mc, "执行失败: " + e.getMessage());
            return false;
        }
    }
}
