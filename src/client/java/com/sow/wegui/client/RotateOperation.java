package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.session.ClipboardHolder;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * //rotate 的直接 API 实现。
 */
public final class RotateOperation implements WeOperation {
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

        int y = 0, x = 0, z = 0;
        try {
            if (!values.isEmpty() && !values.get(0).isBlank()) y = Integer.parseInt(values.get(0).trim());
            if (values.size() >= 2 && !values.get(1).isBlank()) x = Integer.parseInt(values.get(1).trim());
            if (values.size() >= 3 && !values.get(2).isBlank()) z = Integer.parseInt(values.get(2).trim());
        } catch (NumberFormatException e) {
            error(mc, "角度必须是整数");
            return false;
        }

        try {
            AffineTransform transform = new AffineTransform();
            transform = transform.rotateY(-y);
            transform = transform.rotateX(-x);
            transform = transform.rotateZ(-z);
            holder.setTransform(holder.getTransform().combine(transform));
            feedback(mc, "已旋转剪贴板");
            return true;
        } catch (Exception e) {
            error(mc, "旋转失败: " + e.getMessage());
            return false;
        }
    }
}
