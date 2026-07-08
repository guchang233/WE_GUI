package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.BiomeCommands;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * //biomeinfo 的直接 API 实现。
 */
public final class BiomeInfoOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        WeOperationHelper.Context ctx = WeOperationHelper.requireSessionContext(mc);
        if (ctx == null) {
            error(mc, "无法获取 WorldEdit 玩家对象");
            return false;
        }

        String flag = values.isEmpty() ? "" : values.get(0).toLowerCase();
        boolean useLineOfSight = flag.contains("-t");
        boolean usePosition = !useLineOfSight || flag.contains("-p");

        try {
            new BiomeCommands().biomeInfo(ctx.player, ctx.world, ctx.session, useLineOfSight, usePosition);
            return true;
        } catch (WorldEditException e) {
            error(mc, "执行失败: " + e.getMessage());
            return false;
        }
    }
}
