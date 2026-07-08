package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector2;
import com.sk89q.worldedit.regions.CylinderRegion;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * /snow 的直接 API 实现。
 */
public final class SnowOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        WeOperationHelper.Context ctx = WeOperationHelper.requireSessionContext(mc);
        if (ctx == null) {
            error(mc, "无法获取 WorldEdit 玩家对象");
            return false;
        }

        double radius = 5;
        if (!values.isEmpty() && !values.get(0).isBlank()) {
            try {
                radius = Double.parseDouble(values.get(0).trim());
            } catch (NumberFormatException e) {
                error(mc, "半径必须是数字");
                return false;
            }
        }

        try {
            double finalRadius = Math.max(1, radius);
            BlockVector3 position = ctx.session.getPlacementPosition(ctx.player);
            CylinderRegion region = new CylinderRegion(
                    position,
                    Vector2.at(finalRadius, finalRadius),
                    ctx.world.getMinY(),
                    ctx.world.getMaxY()
            );
            return WeOperationHelper.edit(mc, ctx.player, ctx.world, es -> es.simulateSnow(region, false));
        } catch (Exception e) {
            error(mc, "执行失败: " + e.getMessage());
            return false;
        }
    }
}
