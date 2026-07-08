package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.regions.RegionSelector;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * //desel 的直接 API 实现。
 */
public final class DeselOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        WeOperationHelper.Context ctx = WeOperationHelper.requireSessionContext(mc);
        if (ctx == null) {
            error(mc, "无法获取 WorldEdit 会话");
            return false;
        }

        RegionSelector selector = ctx.session.getRegionSelector(ctx.world);
        if (selector != null) {
            selector.clear();
        }
        feedback(mc, "已清除选区");
        return true;
    }
}
