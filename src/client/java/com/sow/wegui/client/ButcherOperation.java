package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.command.util.CreatureButcher;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.function.EntityFunction;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.visitor.EntityVisitor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CylinderRegion;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.function.Supplier;

/**
 * /butcher 的直接 API 实现。
 */
public final class ButcherOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        WeOperationHelper.Context ctx = WeOperationHelper.requireSessionContext(mc);
        if (ctx == null) {
            error(mc, "无法获取 WorldEdit 玩家对象");
            return false;
        }

        String flags = values.isEmpty() ? "" : values.get(0).toLowerCase();
        int radius = 5;
        if (values.size() >= 2 && !values.get(1).isBlank()) {
            try {
                radius = Integer.parseInt(values.get(1).trim());
            } catch (NumberFormatException e) {
                error(mc, "半径必须是整数");
                return false;
            }
        }

        boolean killPets = flags.contains("p");
        boolean killNpcs = flags.contains("n");
        boolean killGolems = flags.contains("g");
        boolean killAnimals = flags.contains("a");
        boolean killAmbient = flags.contains("b");
        boolean killWithName = flags.contains("t");
        boolean killFriendly = flags.contains("f");
        boolean killArmorStands = flags.contains("r");
        boolean killWater = flags.contains("w");

        CreatureButcher butcher = new CreatureButcher(ctx.player);
        butcher.or(CreatureButcher.Flags.FRIENDLY, killFriendly);
        butcher.or(CreatureButcher.Flags.PETS, killPets);
        butcher.or(CreatureButcher.Flags.NPCS, killNpcs);
        butcher.or(CreatureButcher.Flags.GOLEMS, killGolems);
        butcher.or(CreatureButcher.Flags.ANIMALS, killAnimals);
        butcher.or(CreatureButcher.Flags.AMBIENT, killAmbient);
        butcher.or(CreatureButcher.Flags.TAGGED, killWithName);
        butcher.or(CreatureButcher.Flags.ARMOR_STAND, killArmorStands);
        butcher.or(CreatureButcher.Flags.WATER, killWater);

        Supplier<EntityFunction> func = butcher::createFunction;
        int finalRadius = radius;
        try {
            return WeOperationHelper.edit(mc, ctx.player, ctx.world, es -> {
                BlockVector3 center = ctx.session.getPlacementPosition(ctx.player);
                List<? extends Entity> entities;
                if (finalRadius >= 0) {
                    CylinderRegion region = CylinderRegion.createRadius(es, center, finalRadius);
                    entities = es.getEntities(region);
                } else {
                    entities = es.getEntities();
                }
                EntityVisitor visitor = new EntityVisitor(entities.iterator(), func.get());
                Operations.completeLegacy(visitor);
                feedback(mc, "已击杀 " + visitor.getAffected() + " 个生物");
            });
        } catch (Exception e) {
            error(mc, "执行失败: " + e.getMessage());
            return false;
        }
    }
}
