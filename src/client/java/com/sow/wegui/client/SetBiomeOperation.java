package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.BiomeCommands;
import com.sk89q.worldedit.world.biome.BiomeType;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * //setbiome 的直接 API 实现。
 */
public final class SetBiomeOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        WeOperationHelper.Context ctx = WeOperationHelper.requireSessionContext(mc);
        if (ctx == null) {
            error(mc, "无法获取 WorldEdit 玩家对象");
            return false;
        }

        int biomeIndex = 0;
        if (!values.isEmpty() && values.get(0).startsWith("-")) {
            biomeIndex = 1;
        }

        if (values.size() <= biomeIndex || values.get(biomeIndex).isBlank()) {
            error(mc, "请指定群系");
            return false;
        }

        String flag = values.isEmpty() ? "" : values.get(0);
        boolean atPosition = flag.contains("-p");

        if (!atPosition && ctx.region == null) {
            error(mc, "无法获取选区");
            return false;
        }

        BiomeType biome = resolveBiome(values.get(biomeIndex).trim());
        if (biome == null) {
            error(mc, "无法解析群系: " + values.get(biomeIndex));
            return false;
        }

        try (var editSession = WorldEdit.getInstance().newEditSessionBuilder()
                .world(ctx.world)
                .actor(ctx.player)
                .build()) {
            new BiomeCommands().setBiome(ctx.player, ctx.world, ctx.session, editSession, biome, atPosition);
            ctx.session.remember(editSession);
            feedback(mc, "群系已设置");
            return true;
        } catch (Exception e) {
            error(mc, "执行失败: " + e.getMessage());
            return false;
        }
    }

    private BiomeType resolveBiome(String input) {
        BiomeType fromRegistry = BiomeType.REGISTRY.get(input);
        return fromRegistry != null ? fromRegistry : new BiomeType(input);
    }
}
