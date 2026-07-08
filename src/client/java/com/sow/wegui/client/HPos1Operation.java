package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.extension.platform.permission.ActorSelectorLimits;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Location;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * //hpos1 的直接 API 实现。
 */
public final class HPos1Operation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        WeOperationHelper.Context ctx = WeOperationHelper.requireSessionContext(mc);
        if (ctx == null) {
            error(mc, "无法获取 WorldEdit 会话");
            return false;
        }

        Location target = ctx.player.getBlockTrace(300);
        if (target == null) {
            error(mc, "未指向任何方块");
            return false;
        }

        BlockVector3 position = target.toVector().toBlockPoint();
        ctx.session.getRegionSelector(ctx.world).selectPrimary(position, ActorSelectorLimits.forActor(ctx.player));
        feedback(mc, "已设置 pos1: " + position);
        return true;
    }
}
