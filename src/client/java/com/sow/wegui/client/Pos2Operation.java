package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.extension.platform.permission.ActorSelectorLimits;
import com.sk89q.worldedit.math.BlockVector3;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * //pos2 的直接 API 实现。
 */
public final class Pos2Operation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        WeOperationHelper.Context ctx = WeOperationHelper.requireSessionContext(mc);
        if (ctx == null) {
            error(mc, "无法获取 WorldEdit 会话");
            return false;
        }

        BlockVector3 position = ctx.player.getLocation().toVector().toBlockPoint();
        ctx.session.getRegionSelector(ctx.world).selectSecondary(position, ActorSelectorLimits.forActor(ctx.player));
        feedback(mc, "已设置 pos2: " + position);
        return true;
    }
}
