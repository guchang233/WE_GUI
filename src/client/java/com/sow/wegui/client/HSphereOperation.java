package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * //hsphere 的直接 API 实现。
 */
public final class HSphereOperation implements WeOperation {
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

        String raiseInput = values.size() >= 3 ? values.get(2) : "true";
        boolean raised = raiseInput.isBlank() || parseBoolean(raiseInput);

        String patternInput = values.get(0);
        try {
            Pattern pattern = WeOperationHelper.parsePattern(patternInput, player, world);
            BlockVector3 pos = WeOperationHelper.getPlacementPosition(player, session);
            if (raised) {
                pos = pos.add(0, (int) radius, 0);
            }
            BlockVector3 finalPos = pos;
            double finalRadius = radius;
            return WeOperationHelper.edit(mc, player, world,
                    es -> es.makeSphere(finalPos, pattern, finalRadius, finalRadius, finalRadius, false));
        } catch (Exception e) {
            error(mc, "执行失败: " + e.getMessage());
            return false;
        }
    }

    private boolean parseBoolean(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        String s = input.trim().toLowerCase();
        return s.equals("true") || s.equals("yes") || s.equals("y") || s.equals("1") || s.equals("是");
    }
}
