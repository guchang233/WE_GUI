package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.function.pattern.Pattern;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * //overlay 的直接 API 实现。
 */
public final class OverlayOperation implements WeOperation {
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

        try {
            Pattern pattern = WeOperationHelper.parsePattern(values.get(0), ctx.player, ctx.world);
            return WeOperationHelper.edit(mc, ctx.player, ctx.world, es -> es.overlayCuboidBlocks(ctx.region, pattern));
        } catch (Exception e) {
            error(mc, "执行失败: " + e.getMessage());
            return false;
        }
    }
}
