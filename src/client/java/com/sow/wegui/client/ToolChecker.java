package com.sow.wegui.client;

import com.sow.wegui.config.WeGuiConfigs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 检测玩家是否手持触发工具。
 */
public final class ToolChecker {
    private static final Identifier DEFAULT_ID = Identifier.parse("minecraft:wooden_axe");
    private static String cachedKey = null;
    private static Identifier cachedId = DEFAULT_ID;

    private ToolChecker() {}

    public static boolean isHoldingTriggerTool(Player player) {
        if (player == null) return false;
        Identifier id = resolveId();
        ItemStack main = player.getMainHandItem();
        if (!main.isEmpty() && BuiltInRegistries.ITEM.getKey(main.getItem()).equals(id)) return true;
        ItemStack off = player.getOffhandItem();
        return !off.isEmpty() && BuiltInRegistries.ITEM.getKey(off.getItem()).equals(id);
    }

    private static Identifier resolveId() {
        String key = WeGuiConfigs.getTriggerItem();
        if (key == null || key.isBlank()) return DEFAULT_ID;
        if (key.equals(cachedKey) && cachedId != null) return cachedId;
        try {
            cachedId = Identifier.parse(key);
        } catch (Exception e) {
            cachedId = DEFAULT_ID;
        }
        cachedKey = key;
        return cachedId;
    }
}