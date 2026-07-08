package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * //revolve <角度> [轴] [复制份数] 的直接 API 实现。
 * 绕指定轴旋转复制选区。
 */
public final class RevolveOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        WeOperationHelper.Context ctx = WeOperationHelper.requireRegionContext(mc);
        if (ctx == null) {
            error(mc, "无法获取选区或 WorldEdit 玩家对象");
            return false;
        }

        if (values.isEmpty() || values.get(0) == null || values.get(0).isBlank()) {
            error(mc, "请指定旋转角度");
            return false;
        }

        int angle;
        try {
            angle = Integer.parseInt(values.get(0).trim());
        } catch (NumberFormatException e) {
            error(mc, "角度必须是整数");
            return false;
        }

        Axis axis = parseAxis(values.size() >= 2 ? values.get(1) : "", ctx.player);

        int copies = 1;
        if (values.size() >= 3 && values.get(2) != null && !values.get(2).isBlank()) {
            try {
                copies = Integer.parseInt(values.get(2).trim());
                if (copies < 1) copies = 1;
            } catch (NumberFormatException e) {
                error(mc, "复制份数必须是整数");
                return false;
            }
        }

        Player player = ctx.player;
        World world = ctx.world;
        Region region = ctx.region;
        Vector3 centerVec = region.getCenter();
        BlockVector3 center = BlockVector3.at(
                (int) Math.floor(centerVec.x()),
                (int) Math.floor(centerVec.y()),
                (int) Math.floor(centerVec.z()));

        try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                .world(world)
                .actor(player)
                .build()) {
            BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
            clipboard.setOrigin(center);
            ForwardExtentCopy copy = new ForwardExtentCopy(editSession, region, clipboard, region.getMinimumPoint());
            Operations.completeLegacy(copy);

            for (int i = 1; i <= copies; i++) {
                double stepAngle = angle * i / (double) copies;
                ClipboardHolder holder = new ClipboardHolder(clipboard);
                AffineTransform transform = new AffineTransform();
                transform = switch (axis) {
                    case X -> transform.rotateX(stepAngle);
                    case Y -> transform.rotateY(stepAngle);
                    case Z -> transform.rotateZ(stepAngle);
                };
                holder.setTransform(transform);
                Operation paste = holder.createPaste(editSession)
                        .to(center)
                        .ignoreAirBlocks(true)
                        .copyEntities(false)
                        .copyBiomes(false)
                        .build();
                Operations.completeLegacy(paste);
            }

            ctx.session.remember(editSession);
            feedback(mc, "已旋转复制 " + copies + " 份 (轴: " + axis.name().toLowerCase() + ")");
            return true;
        } catch (Exception e) {
            error(mc, "旋转复制失败: " + e.getMessage());
            return false;
        }
    }

    private Axis parseAxis(String input, Player player) {
        String s = input == null ? "" : input.trim().toLowerCase();
        if (s.isEmpty()) {
            return Axis.Y;
        }
        return switch (s) {
            case "x", "east", "west", "e", "w", "left", "right", "l", "r" -> Axis.X;
            case "z", "north", "south", "n", "s", "forward", "back", "f", "b" -> Axis.Z;
            default -> Axis.Y;
        };
    }

    private enum Axis {
        X, Y, Z
    }
}
