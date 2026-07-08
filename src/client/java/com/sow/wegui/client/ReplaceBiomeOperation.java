package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.BiomeCommands;
import com.sk89q.worldedit.function.mask.BiomeMask;
import com.sk89q.worldedit.world.biome.BiomeType;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * //replacebiome 的直接 API 实现。
 */
public final class ReplaceBiomeOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        WeOperationHelper.Context ctx = WeOperationHelper.requireRegionContext(mc);
        if (ctx == null) {
            error(mc, "无法获取选区或 WorldEdit 玩家对象");
            return false;
        }

        if (values.size() < 2 || values.get(0).isBlank() || values.get(1).isBlank()) {
            error(mc, "请指定原群系和目标群系");
            return false;
        }

        BiomeType fromBiome = resolveBiome(values.get(0).trim());
        BiomeType toBiome = resolveBiome(values.get(1).trim());
        if (fromBiome == null || toBiome == null) {
            error(mc, "无法解析群系");
            return false;
        }

        try (var editSession = WorldEdit.getInstance().newEditSessionBuilder()
                .world(ctx.world)
                .actor(ctx.player)
                .build()) {
            BiomeMask mask = new BiomeMask(editSession, fromBiome);
            new BiomeCommands().replaceBiome(ctx.player, ctx.world, ctx.session, editSession, mask, toBiome, false);
            ctx.session.remember(editSession);
            feedback(mc, "群系已替换");
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
