package com.sow.wegui.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sow.wegui.client.screen.SettingsScreen;
import com.sow.wegui.config.Config;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * 客户端入口：注册按键、HUD、粘贴预览。
 */
public class WeGuiClient implements ClientModInitializer {
    public static final String KEY_OPEN_PANEL = "key.wegui.open_panel";
    public static final String KEY_TOGGLE_AXE_MODE = "key.wegui.toggle_axe_mode";
    private static KeyMapping openPanelKey;
    private static KeyMapping toggleAxeModeKey;

    @Override
    public void onInitializeClient() {
        Config.load();
        openPanelKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                KEY_OPEN_PANEL,
                InputConstants.Type.KEYSYM,
                Config.get().getKeyOpenPanel(),
                KeyMapping.Category.MISC
        ));
        toggleAxeModeKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                KEY_TOGGLE_AXE_MODE,
                InputConstants.Type.KEYSYM,
                Config.get().getKeyToggleAxeMode(),
                KeyMapping.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openPanelKey.consumeClick()) {
                openMainPanel();
            }
            if (toggleAxeModeKey.consumeClick()) {
                AxeModeHandler.toggleMode();
            }
        });

        StatusBar.register();
        PastePreviewRenderer.register();
        AxeModeHandler.register();
    }

    private static void openMainPanel() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        mc.setScreen(new SettingsScreen(null));
    }

    /**
     * 更新打开主面板的按键绑定。
     */
    public static void updateKeyOpenPanel(int keyCode) {
        Config.get().setKeyOpenPanel(keyCode);
        if (openPanelKey != null) {
            openPanelKey.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
        }
    }

    /**
     * 更新切换小木斧模式的按键绑定。
     */
    public static void updateKeyToggleAxeMode(int keyCode) {
        Config.get().setKeyToggleAxeMode(keyCode);
        if (toggleAxeModeKey != null) {
            toggleAxeModeKey.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
        }
    }
}
