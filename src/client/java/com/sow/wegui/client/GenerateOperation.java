package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.util.TransformUtil;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * //generate 的直接 API 实现。
 */
public final class GenerateOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        WeOperationHelper.Context ctx = WeOperationHelper.requireRegionContext(mc);
        if (ctx == null) {
            error(mc, "无法获取选区或 WorldEdit 玩家对象");
            return false;
        }

        String flags = "-h";
        String patternInput;
        String expression;
        if (values.size() >= 3) {
            if (!values.get(0).isBlank()) {
                flags = values.get(0).trim();
            }
            patternInput = values.get(1);
            expression = values.get(2);
        } else {
            patternInput = values.size() >= 1 ? values.get(0) : "";
            expression = values.size() >= 2 ? values.get(1) : "";
        }

        if (patternInput.isBlank()) {
            error(mc, "请指定图案");
            return false;
        }
        if (expression.isBlank()) {
            error(mc, "请指定表达式");
            return false;
        }

        boolean hollow = false;
        boolean useRawCoords = false;
        boolean offsetPlacement = false;
        boolean offsetCenter = false;
        if (!flags.isBlank()) {
            String s = flags.startsWith("-") ? flags.substring(1) : flags;
            for (char c : s.toCharArray()) {
                switch (c) {
                    case 'h' -> hollow = true;
                    case 'r' -> useRawCoords = true;
                    case 'o' -> offsetPlacement = true;
                    case 'c' -> offsetCenter = true;
                    default -> {
                        // 忽略未知标志
                    }
                }
            }
        }

        Player player = ctx.player;
        World world = ctx.world;
        Region region = ctx.region;
        LocalSession session = ctx.session;

        try {
            Pattern pattern = WeOperationHelper.parsePattern(patternInput, player, world);
            Transform transform = TransformUtil.createTransformForExpressionCommand(
                    player, session, region, useRawCoords, offsetPlacement, offsetCenter);
            boolean finalHollow = hollow;
            String finalExpression = expression;
            return WeOperationHelper.edit(mc, player, world,
                    es -> es.makeShape(region, transform, pattern, finalExpression, finalHollow, session.getTimeout()));
        } catch (Exception e) {
            error(mc, "执行失败: " + e.getMessage());
            return false;
        }
    }
}
