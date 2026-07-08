package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.entity.Player;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * //undo 的直接 API 实现。
 */
public final class UndoOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        Player player = WeOperationHelper.requirePlayer(mc);
        if (player == null) {
            error(mc, "无法获取 WorldEdit 玩家对象");
            return false;
        }

        int times = 1;
        if (!values.isEmpty() && !values.get(0).isBlank()) {
            try {
                times = Integer.parseInt(values.get(0).trim());
                if (times < 1) times = 1;
            } catch (NumberFormatException e) {
                error(mc, "步数必须是整数");
                return false;
            }
        }

        try {
            var session = WeOperationHelper.requireSession(player);
            for (int i = 0; i < times; i++) {
                session.undo(null, player);
            }
            feedback(mc, "已撤销 " + times + " 步");
            return true;
        } catch (Exception e) {
            error(mc, "撤销失败: " + e.getMessage());
            return false;
        }
    }
}
