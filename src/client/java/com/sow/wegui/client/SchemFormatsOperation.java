package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * /schematic formats 的直接 API 实现。
 */
public final class SchemFormatsOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        feedback(mc, "可用 schematic 格式:");
        for (ClipboardFormat format : ClipboardFormats.getAll()) {
            StringBuilder sb = new StringBuilder();
            sb.append(format.getName()).append(" (");
            boolean first = true;
            for (String alias : format.getAliases()) {
                if (!first) sb.append(", ");
                sb.append(alias);
                first = false;
            }
            sb.append(")");
            feedback(mc, "  " + sb);
        }
        return true;
    }
}
