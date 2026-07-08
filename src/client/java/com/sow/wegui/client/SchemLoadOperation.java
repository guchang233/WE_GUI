package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.session.ClipboardHolder;
import net.minecraft.client.Minecraft;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;

/**
 * /schematic load [格式] <文件名> 的直接 API 实现。
 */
public final class SchemLoadOperation implements WeOperation {
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

        ClipboardFormat format = null;
        if (!formatInput.isBlank()) {
            format = ClipboardFormats.findByAlias(formatInput);
            if (format == null) {
                error(mc, "未知格式: " + formatInput);
                return false;
            }
        }

        WorldEdit worldEdit = WorldEdit.getInstance();
        File schematicsRoot = worldEdit.getSchematicsManager().getRoot().toFile();
        File file;
        try {
            file = worldEdit.getSafeOpenFile(player, schematicsRoot, filename,
                    BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC.getPrimaryFileExtension(),
                    ClipboardFormats.getFileExtensionArray());
        } catch (Exception e) {
            error(mc, "文件名无效: " + e.getMessage());
            return false;
        }

        if (!file.exists()) {
            error(mc, "文件不存在: " + filename);
            return false;
        }

        ClipboardFormat inferred = ClipboardFormats.findByPath(file.toPath());
        if (inferred != null) {
            format = inferred;
        }
        if (format == null) {
            format = BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC;
        }

        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis);
             ClipboardReader reader = format.getReader(bis)) {
            Clipboard clipboard = reader.read();
            session.setClipboard(new ClipboardHolder(clipboard));
            feedback(mc, "已加载 schematic: " + file.getName());
            return true;
        } catch (Exception e) {
            error(mc, "加载失败: " + e.getMessage());
            return false;
        }
    }
}
