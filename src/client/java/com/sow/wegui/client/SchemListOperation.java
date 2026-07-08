package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.internal.schematic.SchematicsManager;
import net.minecraft.client.Minecraft;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * /schematic list [页码] 的直接 API 实现。
 */
public final class SchemListOperation implements WeOperation {
    private static final int PAGE_SIZE = 9;

    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        int page = 1;
        if (!values.isEmpty() && values.get(0) != null && !values.get(0).isBlank()) {
            try {
                page = Integer.parseInt(values.get(0).trim());
                if (page < 1) page = 1;
            } catch (NumberFormatException e) {
                error(mc, "页码必须是整数");
                return false;
            }
        }

        SchematicsManager manager = WorldEdit.getInstance().getSchematicsManager();
        Set<Path> paths = manager.getSchematicPaths();
        if (paths.isEmpty()) {
            feedback(mc, "暂无 schematic 文件");
            return true;
        }

        List<Path> sorted = new ArrayList<>(paths);
        Collections.sort(sorted);

        int totalPages = Math.max(1, (sorted.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page > totalPages) page = totalPages;

        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, sorted.size());

        feedback(mc, "Schematic 列表 (第 " + page + "/" + totalPages + " 页, 共 " + sorted.size() + " 个):");
        for (int i = start; i < end; i++) {
            Path path = sorted.get(i);
            Path root = manager.getRoot();
            String display = path.startsWith(root) ? root.relativize(path).toString() : path.getFileName().toString();
            feedback(mc, "  " + (i + 1) + ". " + display);
        }
        return true;
    }
}
