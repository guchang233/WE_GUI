package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.block.BlockDistributionCounter;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.visitor.RegionVisitor;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.Countable;
import com.sk89q.worldedit.world.block.BlockState;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * //distr 的直接 API 实现。
 */
public final class DistrOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        WeOperationHelper.Context ctx = WeOperationHelper.requireSessionContext(mc);
        if (ctx == null) {
            error(mc, "无法获取 WorldEdit 会话");
            return false;
        }

        boolean clipboard = "distr_c".equals(usage.id());
        boolean separate = values.stream().anyMatch(v -> "-d".equalsIgnoreCase(v.trim()));

        try {
            List<Countable<BlockState>> distribution;
            int total;
            if (clipboard) {
                ClipboardHolder holder = ctx.session.getClipboard();
                Clipboard cb = holder.getClipboard();
                BlockDistributionCounter counter = new BlockDistributionCounter(cb, Masks.alwaysTrue(), separate);
                RegionVisitor visitor = new RegionVisitor(cb.getRegion(), counter);
                Operations.completeBlindly(visitor);
                distribution = counter.getDistribution();
                total = distribution.stream().mapToInt(Countable::getAmount).sum();
                feedback(mc, "剪贴板方块分布 (" + total + " 方块):");
            } else {
                if (ctx.region == null) {
                    WeOperationHelper.Context regionCtx = WeOperationHelper.requireRegionContext(mc);
                    if (regionCtx == null) {
                        error(mc, "无法获取选区");
                        return false;
                    }
                    ctx = regionCtx;
                }
                try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                        .world(ctx.world)
                        .actor(ctx.player)
                        .build()) {
                    distribution = editSession.getBlockDistribution(ctx.region, Masks.alwaysTrue(), separate);
                }
                total = distribution.stream().mapToInt(Countable::getAmount).sum();
                feedback(mc, "选区方块分布 (" + total + " 方块):");
            }

            for (Countable<BlockState> c : distribution) {
                BlockState state = c.getID();
                int count = c.getAmount();
                double perc = total == 0 ? 0 : count * 100.0 / total;
                String name = separate ? state.getAsString() : state.getBlockType().id();
                feedback(mc, String.format("  %.2f%%  %d  %s", perc, count, name));
            }
            return true;
        } catch (Exception e) {
            error(mc, "分布统计失败: " + e.getMessage());
            return false;
        }
    }
}
