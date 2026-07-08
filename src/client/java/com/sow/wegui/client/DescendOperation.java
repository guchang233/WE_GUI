package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.NavigationCommands;
import com.sk89q.worldedit.entity.Player;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * /descend 的直接 API 实现。
 */
public final class DescendOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        Player player = WeOperationHelper.requirePlayer(mc);
        if (player == null) {
            error(mc, "无法获取 WorldEdit 玩家对象");
            return false;
        }

        int levels = 1;
        if (!values.isEmpty() && !values.get(0).isBlank()) {
            try {
                levels = Integer.parseInt(values.get(0).trim());
            } catch (NumberFormatException e) {
                error(mc, "层数必须是整数");
                return false;
            }
        }

        try {
            new NavigationCommands(WorldEdit.getInstance()).descend(player, levels);
            return true;
        } catch (Exception e) {
            error(mc, "执行失败: " + e.getMessage());
            return false;
        }
    }
}
