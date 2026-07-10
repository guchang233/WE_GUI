package com.sow.wegui.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 获取当前客户端已知玩家名称列表，用于 PLAYER 参数建议。
 */
public final class PlayerListHelper {
    private PlayerListHelper() {}

    public static List<String> getPlayerNames() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return Collections.emptyList();

        List<String> names = new ArrayList<>();
        for (AbstractClientPlayer player : mc.level.players()) {
            String name = player.getName().getString();
            if (name != null && !name.isBlank()) {
                names.add(name);
            }
        }
        names.sort(String::compareToIgnoreCase);
        return names;
    }
}
