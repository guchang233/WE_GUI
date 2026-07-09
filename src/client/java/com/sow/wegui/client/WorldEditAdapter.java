package com.sow.wegui.client;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.regions.Region;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * 在客户端获取 WorldEdit 会话与选区。
 * 通过玩家名字查找 LocalSession，避免依赖服务端玩家对象。
 */
public final class WorldEditAdapter {
    private WorldEditAdapter() {}

    public static boolean isLoaded() {
        try {
            Class.forName("com.sk89q.worldedit.WorldEdit");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Nullable
    public static LocalSession session(LocalPlayer player) {
        if (player == null || !isLoaded()) return null;
        try {
            return WorldEdit.getInstance().getSessionManager().findByName(player.getName().getString());
        } catch (Throwable t) {
            return null;
        }
    }

    @Nullable
    public static Region selection(LocalPlayer player) {
        LocalSession session = session(player);
        if (session == null) return null;
        try {
            return session.getSelection(session.getSelectionWorld());
        } catch (Throwable t) {
            return null;
        }
    }
}
