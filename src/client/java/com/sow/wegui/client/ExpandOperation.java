package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.RegionOperationException;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * //expand 的直接 API 实现。
 */
public final class ExpandOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        WeOperationHelper.Context ctx = WeOperationHelper.requireRegionContext(mc);
        if (ctx == null) {
            error(mc, "无法获取选区");
            return false;
        }

        if (values.isEmpty() || values.get(0).isBlank()) {
            error(mc, "请指定扩展数量");
            return false;
        }

        int amount;
        try {
            amount = Integer.parseInt(values.get(0).trim());
        } catch (NumberFormatException e) {
            error(mc, "数量必须是整数");
            return false;
        }

        boolean reverse = "expand_rev".equals(usage.id());
        int reverseAmount = 0;
        if (reverse) {
            if (values.size() < 2 || values.get(1).isBlank()) {
                error(mc, "请指定反向数量");
                return false;
            }
            try {
                reverseAmount = Integer.parseInt(values.get(1).trim());
            } catch (NumberFormatException e) {
                error(mc, "反向数量必须是整数");
                return false;
            }
        }

        int dirIndex = reverse ? 2 : 1;
        String dirInput = values.size() > dirIndex ? values.get(dirIndex) : "";
        BlockVector3 dir = WeOperationHelper.parseDirection(dirInput, ctx.player);

        try {
            if (reverseAmount == 0) {
                ctx.region.expand(dir.multiply(amount));
            } else {
                ctx.region.expand(dir.multiply(amount), dir.multiply(-reverseAmount));
            }
            ctx.session.getRegionSelector(ctx.world).learnChanges();
            feedback(mc, "已扩展选区");
            return true;
        } catch (RegionOperationException e) {
            error(mc, "扩展失败: " + e.getMessage());
            return false;
        }
    }
}
