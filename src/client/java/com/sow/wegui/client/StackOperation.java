package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.math.BlockVector3;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * //stack 的直接 API 实现。
 */
public final class StackOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        WeOperationHelper.Context ctx = WeOperationHelper.requireRegionContext(mc);
        if (ctx == null) {
            error(mc, "无法获取选区或 WorldEdit 玩家对象");
            return false;
        }

        int count = 1;
        if (!values.isEmpty() && !values.get(0).isBlank()) {
            try {
                count = Integer.parseInt(values.get(0).trim());
                if (count < 1) count = 1;
            } catch (NumberFormatException e) {
                error(mc, "次数必须是整数");
                return false;
            }
        }

        String offsetInput = values.size() >= 2 ? values.get(1) : "";
        BlockVector3 offset = WeOperationHelper.parseDirection(offsetInput, ctx.player);

        try {
            int finalCount = count;
            return WeOperationHelper.edit(mc, ctx.player, ctx.world,
                    es -> es.stackCuboidRegion(ctx.region, offset, finalCount, true));
        } catch (Exception e) {
            error(mc, "执行失败: " + e.getMessage());
            return false;
        }
    }
}
