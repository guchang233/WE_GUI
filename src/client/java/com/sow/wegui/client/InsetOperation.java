package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.RegionOperationException;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * //inset 的直接 API 实现。
 */
public final class InsetOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        WeOperationHelper.Context ctx = WeOperationHelper.requireRegionContext(mc);
        if (ctx == null) {
            error(mc, "无法获取选区");
            return false;
        }

        String flag = values.size() >= 1 && !values.get(0).isBlank() ? values.get(0).trim().toLowerCase() : "";
        int amountIndex = flag.isEmpty() || flag.startsWith("-") ? 1 : 0;
        if (values.size() <= amountIndex || values.get(amountIndex).isBlank()) {
            error(mc, "请指定收缩数量");
            return false;
        }

        int amount;
        try {
            amount = Integer.parseInt(values.get(amountIndex).trim());
        } catch (NumberFormatException e) {
            error(mc, "数量必须是整数");
            return false;
        }

        boolean onlyHorizontal = "-h".equals(flag);
        boolean onlyVertical = "-v".equals(flag);

        try {
            ctx.region.contract(getChanges(amount, onlyHorizontal, onlyVertical));
            ctx.session.getRegionSelector(ctx.world).learnChanges();
            feedback(mc, "已内缩选区");
            return true;
        } catch (RegionOperationException e) {
            error(mc, "内缩失败: " + e.getMessage());
            return false;
        }
    }

    private BlockVector3[] getChanges(int amount, boolean onlyHorizontal, boolean onlyVertical) {
        java.util.List<BlockVector3> changes = new java.util.ArrayList<>();
        if (!onlyHorizontal) {
            changes.add(BlockVector3.UNIT_Y.multiply(amount));
            changes.add(BlockVector3.UNIT_MINUS_Y.multiply(amount));
        }
        if (!onlyVertical) {
            changes.add(BlockVector3.UNIT_X.multiply(amount));
            changes.add(BlockVector3.UNIT_MINUS_X.multiply(amount));
            changes.add(BlockVector3.UNIT_Z.multiply(amount));
            changes.add(BlockVector3.UNIT_MINUS_Z.multiply(amount));
        }
        return changes.toArray(new BlockVector3[0]);
    }
}
