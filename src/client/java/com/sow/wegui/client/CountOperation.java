package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.function.mask.Mask;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * //count 的直接 API 实现。
 */
public final class CountOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        WeOperationHelper.Context ctx = WeOperationHelper.requireRegionContext(mc);
        if (ctx == null) {
            error(mc, "无法获取选区");
            return false;
        }

        boolean separate = false;
        String maskInput = null;
        for (String value : values) {
            String v = value.trim();
            if ("-d".equalsIgnoreCase(v)) {
                separate = true;
            } else if (!v.isEmpty() && maskInput == null) {
                maskInput = v;
            }
        }

        if (maskInput == null || maskInput.isBlank()) {
            error(mc, "请指定要统计的方块");
            return false;
        }

        try {
            Mask mask = WeOperationHelper.parseMask(maskInput, ctx.player, ctx.world);
            int count;
            try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                    .world(ctx.world)
                    .actor(ctx.player)
                    .build()) {
                count = editSession.countBlocks(ctx.region, mask);
            }
            feedback(mc, "匹配方块数量" + (separate ? "（区分状态）" : "") + ": " + count);
            return true;
        } catch (Exception e) {
            error(mc, "统计失败: " + e.getMessage());
            return false;
        }
    }
}
