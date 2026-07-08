package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionSelector;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * //size 的直接 API 实现。
 */
public final class SizeOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        WeOperationHelper.Context ctx = WeOperationHelper.requireSessionContext(mc);
        if (ctx == null) {
            error(mc, "无法获取 WorldEdit 会话");
            return false;
        }

        Region region;
        try {
            region = ctx.session.getSelection(ctx.world);
        } catch (IncompleteRegionException e) {
            error(mc, "选区不完整");
            return false;
        }

        RegionSelector selector = ctx.session.getRegionSelector(ctx.world);
        feedback(mc, "选区类型: " + selector.getTypeName());

        BlockVector3 size = region.getMaximumPoint().subtract(region.getMinimumPoint()).add(1, 1, 1);
        feedback(mc, "尺寸: " + size);
        feedback(mc, "距离: " + region.getMaximumPoint().distance(region.getMinimumPoint()));
        feedback(mc, "方块数: " + region.getVolume());
        return true;
    }
}
