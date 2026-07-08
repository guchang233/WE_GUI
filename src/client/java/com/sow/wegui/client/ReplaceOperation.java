package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.pattern.Pattern;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * //replace 的直接 API 实现。
 */
public final class ReplaceOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        WeOperationHelper.Context ctx = WeOperationHelper.requireRegionContext(mc);
        if (ctx == null) {
            error(mc, "无法获取选区或 WorldEdit 玩家对象");
            return false;
        }

        Mask mask;
        String patternInput;
        if ("replace_to".equals(usage.id())) {
            if (values.isEmpty() || values.get(0).isBlank()) {
                error(mc, "请指定目标图案");
                return false;
            }
            mask = Masks.alwaysTrue(); // 默认替换所有方块
            patternInput = values.get(0);
        } else {
            if (values.size() < 2 || values.get(0).isBlank() || values.get(1).isBlank()) {
                error(mc, "请指定被替换方块和目标图案");
                return false;
            }
            try {
                mask = WeOperationHelper.parseMask(values.get(0), ctx.player, ctx.world);
            } catch (Exception e) {
                error(mc, "掩码解析失败: " + e.getMessage());
                return false;
            }
            patternInput = values.get(1);
        }

        try {
            Pattern pattern = WeOperationHelper.parsePattern(patternInput, ctx.player, ctx.world);
            Mask finalMask = mask;
            return WeOperationHelper.edit(mc, ctx.player, ctx.world, es -> es.replaceBlocks(ctx.region, finalMask, pattern));
        } catch (Exception e) {
            error(mc, "执行失败: " + e.getMessage());
            return false;
        }
    }
}
