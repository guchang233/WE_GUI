package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.convolution.GaussianKernel;
import com.sk89q.worldedit.math.convolution.HeightMap;
import com.sk89q.worldedit.math.convolution.HeightMapFilter;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * //smooth 的直接 API 实现。
 */
public final class SmoothOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        WeOperationHelper.Context ctx = WeOperationHelper.requireRegionContext(mc);
        if (ctx == null) {
            error(mc, "无法获取选区或 WorldEdit 玩家对象");
            return false;
        }

        int iterations = 1;
        if (!values.isEmpty() && !values.get(0).isBlank()) {
            try {
                iterations = Integer.parseInt(values.get(0).trim());
                if (iterations < 1) iterations = 1;
            } catch (NumberFormatException e) {
                error(mc, "迭代次数必须是整数");
                return false;
            }
        }

        Mask mask = null;
        if (values.size() >= 2 && !values.get(1).isBlank()) {
            try {
                mask = WeOperationHelper.parseMask(values.get(1), ctx.player, ctx.world);
            } catch (Exception e) {
                error(mc, "掩码解析失败: " + e.getMessage());
                return false;
            }
        }

        try {
            Mask finalMask = mask;
            int finalIterations = iterations;
            return WeOperationHelper.edit(mc, ctx.player, ctx.world, es -> {
                HeightMap heightMap = new HeightMap(es, ctx.region, finalMask);
                HeightMapFilter filter = new HeightMapFilter(new GaussianKernel(5, 1.0));
                heightMap.applyFilter(filter, finalIterations);
            });
        } catch (Exception e) {
            error(mc, "执行失败: " + e.getMessage());
            return false;
        }
    }
}
