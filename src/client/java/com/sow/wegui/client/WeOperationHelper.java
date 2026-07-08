package com.sow.wegui.client;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.World;
import net.minecraft.client.Minecraft;

import javax.annotation.Nullable;

/**
 * WorldEdit 直接 API 调用的公共辅助方法。
 * 集中处理玩家/会话/选区获取、图案/掩码/方向解析以及 EditSession 执行流程。
 */
public final class WeOperationHelper {
    private WeOperationHelper() {}

    public static class Context {
        public final Minecraft mc;
        public final Player player;
        public final LocalSession session;
        public final World world;
        public final Region region;

        public Context(Minecraft mc, Player player, LocalSession session, World world, Region region) {
            this.mc = mc;
            this.player = player;
            this.session = session;
            this.world = world;
            this.region = region;
        }
    }

    @Nullable
    public static Player requirePlayer(Minecraft mc) {
        return WorldEditAdapter.adaptPlayer(mc);
    }

    @Nullable
    public static LocalSession requireSession(Player player) {
        return WorldEdit.getInstance().getSessionManager().get(player);
    }

    @Nullable
    public static Region requireSelection(LocalSession session, World world, Player player) {
        try {
            return session.getSelection(world);
        } catch (IncompleteRegionException e) {
            return null;
        }
    }

    @Nullable
    public static Context requireRegionContext(Minecraft mc) {
        Player player = requirePlayer(mc);
        if (player == null) return null;
        LocalSession session = requireSession(player);
        if (session == null) return null;
        World world = player.getWorld();
        Region region = requireSelection(session, world, player);
        if (region == null) return null;
        return new Context(mc, player, session, world, region);
    }

    /**
     * 不需要完整选区的上下文（用于 pos/expand 等选区操作）。
     */
    @Nullable
    public static Context requireSessionContext(Minecraft mc) {
        Player player = requirePlayer(mc);
        if (player == null) return null;
        LocalSession session = requireSession(player);
        if (session == null) return null;
        World world = player.getWorld();
        return new Context(mc, player, session, world, null);
    }

    @Nullable
    public static RegionSelector getRegionSelector(LocalSession session, World world) {
        return session.getRegionSelector(world);
    }

    @Nullable
    public static BlockVector3 getPlacementPosition(Player player, LocalSession session) {
        try {
            return session.getPlacementPosition(player);
        } catch (IncompleteRegionException e) {
            return null;
        }
    }

    public static ParserContext parserContext(Player player, World world) {
        ParserContext ctx = new ParserContext();
        ctx.setActor(player);
        ctx.setWorld(world);
        ctx.setExtent(world);
        return ctx;
    }

    public static Pattern parsePattern(String input, Player player, World world) throws Exception {
        return WorldEdit.getInstance().getPatternFactory().parseFromInput(input, parserContext(player, world));
    }

    public static Mask parseMask(String input, Player player, World world) throws Exception {
        return WorldEdit.getInstance().getMaskFactory().parseFromInput(input, parserContext(player, world));
    }

    public static BlockVector3 parseDirection(String input, Player player) {
        String s = input.trim().toLowerCase();
        if (s.isEmpty() || s.equals("me")) {
            return player.getLocation().getDirection().toBlockPoint();
        }
        return switch (s) {
            case "north", "n" -> BlockVector3.at(0, 0, -1);
            case "south", "s" -> BlockVector3.at(0, 0, 1);
            case "east", "e" -> BlockVector3.at(1, 0, 0);
            case "west", "w" -> BlockVector3.at(-1, 0, 0);
            case "up", "u" -> BlockVector3.at(0, 1, 0);
            case "down", "d" -> BlockVector3.at(0, -1, 0);
            default -> player.getLocation().getDirection().toBlockPoint();
        };
    }

    public static BlockVector3 parseRelativeDirection(String input, Player player) {
        String s = input.trim().toLowerCase();
        Vector3 forward = player.getLocation().getDirection().withY(0).normalize();
        if (forward.lengthSq() < 0.0001) {
            forward = Vector3.at(0, 0, 1);
        }
        BlockVector3 fb = forward.toBlockPoint();
        BlockVector3 lr = BlockVector3.at(-fb.z(), 0, fb.x());
        return switch (s) {
            case "back", "b" -> fb.multiply(-1);
            case "left", "l" -> lr;
            case "right", "r" -> lr.multiply(-1);
            default -> fb;
        };
    }

    public static BlockVector3 parseAxis(String input, Player player) {
        String s = input.trim().toLowerCase();
        if (s.isEmpty()) {
            return BlockVector3.at(0, 0, -1); // 默认 north
        }
        return switch (s) {
            case "north", "n" -> BlockVector3.at(0, 0, -1);
            case "south", "s" -> BlockVector3.at(0, 0, 1);
            case "east", "e" -> BlockVector3.at(1, 0, 0);
            case "west", "w" -> BlockVector3.at(-1, 0, 0);
            case "up", "u", "top", "y" -> BlockVector3.at(0, 1, 0);
            case "down", "d", "bottom" -> BlockVector3.at(0, -1, 0);
            case "forward", "f" -> player.getLocation().getDirection().withY(0).normalize().toBlockPoint();
            case "back", "b" -> player.getLocation().getDirection().withY(0).normalize().multiply(-1).toBlockPoint();
            default -> {
                if (s.contains(",")) {
                    String[] parts = s.split(",");
                    if (parts.length >= 3) {
                        try {
                            yield BlockVector3.at(
                                    Integer.parseInt(parts[0].trim()),
                                    Integer.parseInt(parts[1].trim()),
                                    Integer.parseInt(parts[2].trim()));
                        } catch (NumberFormatException ignored) {}
                    }
                }
                yield BlockVector3.at(0, 0, -1);
            }
        };
    }

    /**
     * 创建并执行 EditSession，操作完成后自动 remember。
     */
    public static boolean edit(Minecraft mc, Player player, World world, EditSessionConsumer consumer) {
        try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                .world(world)
                .actor(player)
                .build()) {
            consumer.accept(editSession);
            LocalSession session = requireSession(player);
            if (session != null) {
                session.remember(editSession);
            }
            return true;
        } catch (Exception e) {
            if (mc.player != null) {
                mc.player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§c[WE GUI] §f" + e.getMessage()), false);
            }
            return false;
        }
    }

    public interface EditSessionConsumer {
        void accept(EditSession editSession) throws Exception;
    }
}
