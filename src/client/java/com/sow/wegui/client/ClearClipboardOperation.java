package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.entity.Player;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * /clearclipboard 的直接 API 实现。
 */
public final class ClearClipboardOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        Player player = WeOperationHelper.requirePlayer(mc);
        if (player == null) {
            error(mc, "无法获取 WorldEdit 玩家对象");
            return false;
        }

        try {
            var session = WeOperationHelper.requireSession(player);
            session.setClipboard(null);
            feedback(mc, "已清空剪贴板");
            return true;
        } catch (Exception e) {
            error(mc, "清空剪贴板失败: " + e.getMessage());
            return false;
        }
    }
}
