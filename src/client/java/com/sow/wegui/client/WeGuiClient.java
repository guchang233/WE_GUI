package com.sow.wegui.client;

import com.sow.wegui.ModConfig;
import com.sow.wegui.WeGuiMod;
import com.sow.wegui.config.WeGuiConfigs;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * 客户端入口。
 *
 * 已摈弃轮盘 HUD，全面使用列表式命令面板（MainPanelScreen）。
 * 按键与配置迁移到 MaLiLib 管理。
 */
public final class WeGuiClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModConfig.load();
        WeGuiConfigs.init();
        WeGuiInputHandler.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            StatusBar.tick(client);
        });

        // 仅保留状态栏 HUD（轮盘已移除）
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(WeGuiMod.MOD_ID, "status_bar"),
                (graphics, tickCounter) -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player == null || !WeGuiConfigs.isStatusBarEnabled()) return;
                    StatusBar.render(graphics, mc.font,
                            mc.getWindow().getGuiScaledWidth(),
                            mc.getWindow().getGuiScaledHeight());
                });

        WeGuiMod.LOGGER.info("[WE GUI] 客户端初始化完成（MaLiLib 模式）");
    }
}
