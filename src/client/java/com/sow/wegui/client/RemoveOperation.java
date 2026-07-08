package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.command.util.EntityRemover;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.function.EntityFunction;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.visitor.EntityVisitor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CylinderRegion;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * /remove 的直接 API 实现。
 */
public final class RemoveOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        if (values.isEmpty() || values.get(0).isBlank()) {
            error(mc, "请指定实体类型");
            return false;
        }
        if (values.size() < 2 || values.get(1).isBlank()) {
            error(mc, "请指定半径");
            return false;
        }

        WeOperationHelper.Context ctx = WeOperationHelper.requireSessionContext(mc);
        if (ctx == null) {
            error(mc, "无法获取 WorldEdit 玩家对象");
            return false;
        }

        int radius;
        try {
            radius = Integer.parseInt(values.get(1).trim());
        } catch (NumberFormatException e) {
            error(mc, "半径必须是整数");
            return false;
        }

        EntityRemover remover;
        try {
            remover = EntityRemover.fromString(values.get(0).trim());
        } catch (Exception e) {
            error(mc, "实体类型解析失败: " + e.getMessage());
            return false;
        }

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
                EntityVisitor visitor = new EntityVisitor(entities.iterator(), remover.createFunction());
                Operations.completeLegacy(visitor);
                feedback(mc, "已移除 " + visitor.getAffected() + " 个实体");
            });
        } catch (Exception e) {
            error(mc, "执行失败: " + e.getMessage());
            return false;
        }
    }
}
