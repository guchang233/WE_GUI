package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.session.ClipboardHolder;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * //flip 的直接 API 实现。
 */
public final class FlipOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        Player player = WeOperationHelper.requirePlayer(mc);
        if (player == null) {
            error(mc, "无法获取 WorldEdit 玩家对象");
            return false;
        }

        var session = WeOperationHelper.requireSession(player);
        ClipboardHolder holder;
        try {
            holder = session.getClipboard();
            holder.getClipboard();
        } catch (Exception e) {
            error(mc, "剪贴板为空");
            return false;
        }

        String axisInput = values.isEmpty() ? "" : values.get(0);
        BlockVector3 axis = WeOperationHelper.parseAxis(axisInput, player);

        try {
            AffineTransform transform = new AffineTransform();
            transform = transform.scale(axis.abs().multiply(-2).add(1, 1, 1).toVector3());
            holder.setTransform(holder.getTransform().combine(transform));
            feedback(mc, "已翻转剪贴板");
            return true;
        } catch (Exception e) {
            error(mc, "翻转失败: " + e.getMessage());
            return false;
        }
    }
}
