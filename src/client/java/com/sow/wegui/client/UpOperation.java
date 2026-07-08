package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.NavigationCommands;
import com.sk89q.worldedit.entity.Player;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * /up 的直接 API 实现。
 */
public final class UpOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        Player player = WeOperationHelper.requirePlayer(mc);
        if (player == null) {
            error(mc, "无法获取 WorldEdit 玩家对象");
            return false;
        }

        String flag = values.isEmpty() ? "" : values.get(0).toLowerCase();
        boolean forceFlight = flag.contains("-f");
        boolean forceGlass = flag.contains("-g");

        int distance = 5;
        if (values.size() >= 2 && !values.get(1).isBlank()) {
            try {
                distance = Integer.parseInt(values.get(1).trim());
            } catch (NumberFormatException e) {
                error(mc, "距离必须是整数");
                return false;
            }
        }

        try {
            new NavigationCommands(WorldEdit.getInstance()).up(player, distance, forceFlight, forceGlass);
            return true;
        } catch (Exception e) {
            error(mc, "执行失败: " + e.getMessage());
            return false;
        }
    }
}
