package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.world.block.BlockTypes;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * //hollow 的直接 API 实现。
 */
public final class HollowOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        WeOperationHelper.Context ctx = WeOperationHelper.requireRegionContext(mc);
        if (ctx == null) {
            error(mc, "无法获取选区或 WorldEdit 玩家对象");
            return false;
        }

        int thickness = 1;
        if (!values.isEmpty() && !values.get(0).isBlank()) {
            try {
                thickness = Integer.parseInt(values.get(0).trim());
                if (thickness < 1) thickness = 1;
            } catch (NumberFormatException e) {
                error(mc, "厚度必须是整数");
                return false;
            }
        }

        Pattern pattern;
        if (values.size() >= 2 && !values.get(1).isBlank()) {
            try {
                pattern = WeOperationHelper.parsePattern(values.get(1), ctx.player, ctx.world);
            } catch (Exception e) {
                error(mc, "图案解析失败: " + e.getMessage());
                return false;
            }
        } else {
            pattern = BlockTypes.AIR.getDefaultState();
        }

        try {
            Pattern finalPattern = pattern;
            int finalThickness = thickness;
            return WeOperationHelper.edit(mc, ctx.player, ctx.world, es -> es.hollowOutRegion(ctx.region, finalThickness, finalPattern));
        } catch (Exception e) {
            error(mc, "执行失败: " + e.getMessage());
            return false;
        }
    }
}
