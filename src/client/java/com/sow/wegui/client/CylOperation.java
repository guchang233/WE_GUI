package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * //cyl 的直接 API 实现。
 */
public final class CylOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        if (values.size() < 2 || values.get(0).isBlank() || values.get(1).isBlank()) {
            error(mc, "请指定图案和半径");
            return false;
        }

        Player player = WeOperationHelper.requirePlayer(mc);
        if (player == null) {
            error(mc, "无法获取 WorldEdit 玩家对象");
            return false;
        }

        World world = player.getWorld();
        var session = WeOperationHelper.requireSession(player);
        if (session == null) {
            error(mc, "无法获取 WorldEdit 会话");
            return false;
        }

        double radius;
        try {
            radius = Double.parseDouble(values.get(1).trim());
            if (radius < 0) radius = 0;
        } catch (NumberFormatException e) {
            error(mc, "半径必须是数字");
            return false;
        }

        int height = 5;
        if (values.size() >= 3 && !values.get(2).isBlank()) {
            try {
                height = Integer.parseInt(values.get(2).trim());
            } catch (NumberFormatException e) {
                error(mc, "高度必须是整数");
                return false;
            }
        }

        String patternInput = values.get(0);
        try {
            Pattern pattern = WeOperationHelper.parsePattern(patternInput, player, world);
            BlockVector3 pos = WeOperationHelper.getPlacementPosition(player, session);
            BlockVector3 finalPos = pos;
            double finalRadius = radius;
            int finalHeight = height;
            return WeOperationHelper.edit(mc, player, world,
                    es -> es.makeCylinder(finalPos, pattern, finalRadius, finalRadius, finalHeight, true));
        } catch (Exception e) {
            error(mc, "执行失败: " + e.getMessage());
            return false;
        }
    }
}
