package com.sow.wegui.client;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionSelector;
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

    /**
     * LocalSession 缓存：玩家名不变时复用同一 session 引用，避免每帧多次 findByName 查找。
     */
    private static volatile LocalSession cachedSession;
    private static volatile String cachedSessionPlayerName;

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
        String name = player.getName().getString();
        LocalSession cached = cachedSession;
        if (cached != null && name.equals(cachedSessionPlayerName)) {
            return cached;
        }
        try {
            LocalSession session = WorldEdit.getInstance().getSessionManager().findByName(name);
            cachedSession = session;
            cachedSessionPlayerName = name;
            return session;
        } catch (Throwable t) {
            return null;
        }
    }

    @Nullable
    public static Region selection(LocalPlayer player) {
        LocalSession session = session(player);
        if (session == null) return null;
        try {
            com.sk89q.worldedit.world.World weWorld = session.getSelectionWorld();
            if (weWorld == null) return null;
            RegionSelector selector = session.getRegionSelector(weWorld);
            // 选区不完整（只设置了 pos1 或 pos2）时直接返回 null，
            // 避免 session.getSelection() 抛 IncompleteRegionException（异常构造开销大，每帧渲染都会调用）
            return selector.isDefined() ? selector.getRegion() : null;
        } catch (Throwable t) {
            return null;
        }
    }
}
