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

    /**
     * WorldEdit 是否已加载。模组在启动阶段加载，运行时不会变化，因此只需检测一次。
     */
    private static volatile boolean loadedChecked = false;
    private static volatile boolean loaded = false;

    public static boolean isLoaded() {
        if (loadedChecked) return loaded;
        try {
            Class.forName("com.sk89q.worldedit.WorldEdit");
            loaded = true;
        } catch (ClassNotFoundException e) {
            loaded = false;
        }
        loadedChecked = true;
        return loaded;
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
