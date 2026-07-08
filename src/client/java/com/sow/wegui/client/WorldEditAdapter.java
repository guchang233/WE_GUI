package com.sow.wegui.client;

import com.sow.wegui.WeGuiMod;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Minecraft 客户端对象与 WorldEdit API 之间的适配器。
 * 在单人游戏中通过反射获取 ServerPlayer 并转换为 WorldEdit Player。
 */
public final class WorldEditAdapter {
    private static final String FABRIC_ADAPTER = "com.sk89q.worldedit.fabric.FabricAdapter";
    private static Method adaptPlayerMethod;
    private static Method adaptWorldMethod;

    private WorldEditAdapter() {}

    @Nullable
    public static Player adaptPlayer(Minecraft mc) {
        LocalPlayer local = mc.player;
        if (local == null) return null;

        ServerPlayer serverPlayer = getServerPlayer(mc, local.getUUID());
        if (serverPlayer != null) {
            try {
                Method m = getAdaptPlayerMethod();
                Object result = m.invoke(null, serverPlayer);
                return result instanceof Player p ? p : null;
            } catch (Exception e) {
                WeGuiMod.LOGGER.warn("[WE GUI] 无法通过 FabricAdapter 适配玩家: {}", e.getMessage());
            }
        }
        return null;
    }

    @Nullable
    private static ServerPlayer getServerPlayer(Minecraft mc, UUID uuid) {
        if (mc.getSingleplayerServer() == null) return null;
        var playerList = mc.getSingleplayerServer().getPlayerList();
        return playerList == null ? null : playerList.getPlayer(uuid);
    }

    private static Method getAdaptPlayerMethod() throws ClassNotFoundException, NoSuchMethodException {
        if (adaptPlayerMethod == null) {
            Class<?> adapter = Class.forName(FABRIC_ADAPTER);
            adaptPlayerMethod = adapter.getMethod("adaptPlayer", ServerPlayer.class);
        }
        return adaptPlayerMethod;
    }

    @Nullable
    public static World adaptWorld(Minecraft mc) {
        Player player = adaptPlayer(mc);
        if (player != null) return player.getWorld();

        if (mc.level == null) return null;
        try {
            if (adaptWorldMethod == null) {
                Class<?> adapter = Class.forName(FABRIC_ADAPTER);
                adaptWorldMethod = adapter.getMethod("adapt", net.minecraft.world.level.Level.class);
            }
            Object result = adaptWorldMethod.invoke(null, mc.level);
            return result instanceof World w ? w : null;
        } catch (Exception e) {
            WeGuiMod.LOGGER.warn("[WE GUI] 无法通过 FabricAdapter 适配世界: {}", e.getMessage());
            return null;
        }
    }

    @Nullable
    public static LocalSession session(Minecraft mc) {
        Player player = adaptPlayer(mc);
        return player == null ? null : WorldEdit.getInstance().getSessionManager().get(player);
    }

    @Nullable
    public static Region selection(Minecraft mc) {
        Player player = adaptPlayer(mc);
        if (player == null) return null;
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(player);
        World world = player.getWorld();
        try {
            return session.getSelection(world);
        } catch (IncompleteRegionException e) {
            return null;
        }
    }

    @Nullable
    public static World world(Minecraft mc) {
        Player player = adaptPlayer(mc);
        if (player != null) return player.getWorld();
        return adaptWorld(mc);
    }

    @Nullable
    public static BlockVector3 playerPos(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null) return null;
        return BlockVector3.at(p.getX(), p.getY(), p.getZ());
    }

    @Nullable
    public static Location playerLocation(Minecraft mc) {
        Player player = adaptPlayer(mc);
        return player == null ? null : player.getLocation();
    }
}
