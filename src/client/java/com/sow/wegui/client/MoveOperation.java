package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockTypes;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * //move 的直接 API 实现。
 */
public final class MoveOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        WeOperationHelper.Context ctx = WeOperationHelper.requireRegionContext(mc);
        if (ctx == null) {
            error(mc, "无法获取选区或 WorldEdit 玩家对象");
            return false;
        }

        int distance = 1;
        if (!values.isEmpty() && !values.get(0).isBlank()) {
            try {
                distance = Integer.parseInt(values.get(0).trim());
                if (distance < 1) distance = 1;
            } catch (NumberFormatException e) {
                error(mc, "距离必须是整数");
                return false;
            }
        }

        String dirInput = values.size() >= 2 ? values.get(1) : "";
        BlockVector3 dir = WeOperationHelper.parseDirection(dirInput, ctx.player);

        Pattern replacement = BlockTypes.AIR.getDefaultState();
        if (values.size() >= 3 && !values.get(2).isBlank()) {
            try {
                replacement = WeOperationHelper.parsePattern(values.get(2), ctx.player, ctx.world);
            } catch (Exception e) {
                error(mc, "填充图案解析失败: " + e.getMessage());
                return false;
            }
        }

        try {
            Pattern finalReplacement = replacement;
            int finalDistance = distance;
            return WeOperationHelper.edit(mc, ctx.player, ctx.world,
                    es -> es.moveCuboidRegion(ctx.region, dir, finalDistance, true, finalReplacement));
        } catch (Exception e) {
            error(mc, "执行失败: " + e.getMessage());
            return false;
        }
    }
}
