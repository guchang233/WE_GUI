package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.util.List;

/**
 * /schematic delete <文件名> 的直接 API 实现。
 */
public final class SchemDeleteOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        Player player = WeOperationHelper.requirePlayer(mc);
        if (player == null) {
            error(mc, "无法获取 WorldEdit 玩家对象");
            return false;
        }

        String filename = values.isEmpty() || values.get(0) == null ? "" : values.get(0).trim();
        if (filename.isBlank()) {
            error(mc, "请指定文件名");
            return false;
        }

        WorldEdit worldEdit = WorldEdit.getInstance();
        File dir = worldEdit.getWorkingDirectoryPath(worldEdit.getConfiguration().saveDir).toFile();
        File file;
        try {
            file = worldEdit.getSafeOpenFile(player, dir, filename, "schematic",
                    ClipboardFormats.getFileExtensionArray());
        } catch (Exception e) {
            error(mc, "文件名无效: " + e.getMessage());
            return false;
        }

        if (!file.exists()) {
            error(mc, "文件不存在: " + filename);
            return false;
        }

        if (!file.delete()) {
            error(mc, "删除失败: " + filename);
            return false;
        }

        worldEdit.getSchematicsManager().update();
        feedback(mc, "已删除 schematic: " + file.getName());
        return true;
    }
}
