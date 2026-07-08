package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * //set 的直接 API 实现。
 */
public final class SetOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        if (values.isEmpty() || values.get(0).isBlank()) {
            error(mc, "请指定图案");
            return false;
        }

        WeOperationHelper.Context ctx = WeOperationHelper.requireRegionContext(mc);
        if (ctx == null) {
            error(mc, "无法获取选区或 WorldEdit 玩家对象");
            return false;
        }

        String patternInput = values.get(0);
        try {
            var pattern = WeOperationHelper.parsePattern(patternInput, ctx.player, ctx.world);
            return WeOperationHelper.edit(mc, ctx.player, ctx.world, es -> es.setBlocks(ctx.region, pattern));
        } catch (Exception e) {
            error(mc, "图案解析失败: " + e.getMessage());
            return false;
        }
    }
}
