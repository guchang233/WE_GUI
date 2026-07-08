package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.RegionOperationException;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * //shift 的直接 API 实现。
 */
public final class ShiftOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        WeOperationHelper.Context ctx = WeOperationHelper.requireRegionContext(mc);
        if (ctx == null) {
            error(mc, "无法获取选区");
            return false;
        }

        if (values.isEmpty() || values.get(0).isBlank()) {
            error(mc, "请指定平移数量");
            return false;
        }

        int amount;
        try {
            amount = Integer.parseInt(values.get(0).trim());
        } catch (NumberFormatException e) {
            error(mc, "数量必须是整数");
            return false;
        }

        String dirInput = values.size() >= 2 ? values.get(1) : "";
        BlockVector3 dir = WeOperationHelper.parseDirection(dirInput, ctx.player);

        try {
            ctx.region.shift(dir.multiply(amount));
            ctx.session.getRegionSelector(ctx.world).learnChanges();
            feedback(mc, "已平移选区");
            return true;
        } catch (RegionOperationException e) {
            error(mc, "平移失败: " + e.getMessage());
            return false;
        }
    }
}
