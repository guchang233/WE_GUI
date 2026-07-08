package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.ConvexPolyhedralRegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.regions.selector.CylinderRegionSelector;
import com.sk89q.worldedit.regions.selector.EllipsoidRegionSelector;
import com.sk89q.worldedit.regions.selector.ExtendingCuboidRegionSelector;
import com.sk89q.worldedit.regions.selector.Polygonal2DRegionSelector;
import com.sk89q.worldedit.regions.selector.SphereRegionSelector;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * //sel 的直接 API 实现。
 */
public final class SelOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        WeOperationHelper.Context ctx = WeOperationHelper.requireSessionContext(mc);
        if (ctx == null) {
            error(mc, "无法获取 WorldEdit 会话");
            return false;
        }

        if (values.isEmpty() || values.get(0).isBlank()) {
            error(mc, "请指定选区模式");
            return false;
        }

        String mode = values.get(0).trim().toLowerCase();
        RegionSelector oldSelector = ctx.session.getRegionSelector(ctx.world);
        RegionSelector newSelector = switch (mode) {
            case "cuboid" -> oldSelector instanceof CuboidRegionSelector
                    ? oldSelector : new CuboidRegionSelector(ctx.world);
            case "extend" -> oldSelector instanceof ExtendingCuboidRegionSelector
                    ? oldSelector : new ExtendingCuboidRegionSelector(ctx.world);
            case "poly" -> oldSelector instanceof Polygonal2DRegionSelector
                    ? oldSelector : new Polygonal2DRegionSelector(ctx.world);
            case "ellipsoid" -> oldSelector instanceof EllipsoidRegionSelector
                    ? oldSelector : new EllipsoidRegionSelector(ctx.world);
            case "sphere" -> oldSelector instanceof SphereRegionSelector
                    ? oldSelector : new SphereRegionSelector(ctx.world);
            case "cyl" -> oldSelector instanceof CylinderRegionSelector
                    ? oldSelector : new CylinderRegionSelector(ctx.world);
            case "convex" -> oldSelector instanceof ConvexPolyhedralRegionSelector
                    ? oldSelector : new ConvexPolyhedralRegionSelector(ctx.world);
            default -> null;
        };

        if (newSelector == null) {
            error(mc, "未知选区模式: " + mode);
            return false;
        }

        ctx.session.setRegionSelector(ctx.world, newSelector);
        feedback(mc, "已切换选区模式: " + mode);
        return true;
    }
}
