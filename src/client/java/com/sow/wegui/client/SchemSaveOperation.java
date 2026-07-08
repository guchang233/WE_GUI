package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.session.ClipboardHolder;
import net.minecraft.client.Minecraft;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

/**
 * /schematic save [格式] <文件名> 的直接 API 实现。
 */
public final class SchemSaveOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        Player player = WeOperationHelper.requirePlayer(mc);
        if (player == null) {
            error(mc, "无法获取 WorldEdit 玩家对象");
            return false;
        }

        var session = WeOperationHelper.requireSession(player);
        if (session == null) {
            error(mc, "无法获取 WorldEdit 会话");
            return false;
        }

        ClipboardHolder holder;
        try {
            holder = session.getClipboard();
        } catch (Exception e) {
            error(mc, "剪贴板为空");
            return false;
        }

        String formatInput;
        String filename;
        if (values.size() >= 2) {
            formatInput = values.get(0) != null ? values.get(0).trim() : "";
            filename = values.get(1) != null ? values.get(1).trim() : "";
        } else {
            formatInput = "";
            filename = values.isEmpty() || values.get(0) == null ? "" : values.get(0).trim();
        }

        if (filename.isBlank()) {
            error(mc, "请指定文件名");
            return false;
        }

        ClipboardFormat format;
        if (formatInput.isBlank()) {
            format = BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC;
        } else {
            format = ClipboardFormats.findByAlias(formatInput);
            if (format == null) {
                error(mc, "未知格式: " + formatInput);
                return false;
            }
        }

        WorldEdit worldEdit = WorldEdit.getInstance();
        File dir = worldEdit.getWorkingDirectoryPath(worldEdit.getConfiguration().saveDir).toFile();
        File file;
        try {
            file = worldEdit.getSafeSaveFile(player, dir, filename, format.getPrimaryFileExtension(),
                    ClipboardFormats.getFileExtensionArray());
        } catch (Exception e) {
            error(mc, "文件名无效: " + e.getMessage());
            return false;
        }

        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs()) {
                error(mc, "无法创建 schematic 目录");
                return false;
            }
        }

        try (FileOutputStream fos = new FileOutputStream(file);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ClipboardWriter writer = format.getWriter(bos)) {
            Clipboard clipboard = holder.getClipboard();
            Transform transform = holder.getTransform();
            Clipboard target = clipboard.transform(transform);
            writer.write(target);
            worldEdit.getSchematicsManager().update();
            feedback(mc, "已保存 schematic: " + file.getName());
            return true;
        } catch (Exception e) {
            error(mc, "保存失败: " + e.getMessage());
            return false;
        }
    }
}
