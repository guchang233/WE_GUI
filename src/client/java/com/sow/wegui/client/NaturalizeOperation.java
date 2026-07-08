package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * //naturalize 的直接 API 实现。
 */
public final class NaturalizeOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        WeOperationHelper.Context ctx = WeOperationHelper.requireRegionContext(mc);
        if (ctx == null) {
            error(mc, "无法获取选区或 WorldEdit 玩家对象");
            return false;
        }

        try {
            return WeOperationHelper.edit(mc, ctx.player, ctx.world, es -> es.naturalizeCuboidBlocks(ctx.region));
        } catch (Exception e) {
            error(mc, "执行失败: " + e.getMessage());
            return false;
        }
    }
}
