package com.sow.wegui.client;

import com.sow.wegui.WeGuiMod;
import com.sow.wegui.config.WeGuiConfigs;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * MaLiLib 按键提供器。
 * 注册 openPanelHotkey，仅在玩家手持触发物品时打开主面板。
 */
public final class WeGuiInputHandler implements IKeybindProvider {
    private static final String KEYBIND_CATEGORY = "keybinds.category.wegui";
    private static final WeGuiInputHandler INSTANCE = new WeGuiInputHandler();

    private WeGuiInputHandler() {}

    public static void register() {
        fi.dy.masa.malilib.event.InputEventHandler.getKeybindManager()
                .registerKeybindProvider(INSTANCE);
    }

    public static WeGuiInputHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public void addKeysToMap(IKeybindManager manager) {
        manager.addKeybindToMap(WeGuiConfigs.OPEN_PANEL_HOTKEY.getKeybind());
    }

    @Override
    public void addHotkeys(IKeybindManager manager) {
        manager.addHotkeysForCategory(WeGuiMod.MOD_ID, KEYBIND_CATEGORY,
                List.of(WeGuiConfigs.OPEN_PANEL_HOTKEY));
    }

    static {
        WeGuiConfigs.OPEN_PANEL_HOTKEY.getKeybind().setCallback(new IHotkeyCallback() {
            @Override
            public boolean onKeyAction(KeyAction action, fi.dy.masa.malilib.hotkeys.IKeybind keybind) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null || mc.screen != null) {
                    return false;
                }
                if (!ToolChecker.isHoldingTriggerTool(mc.player)) {
                    return false;
                }
                mc.setScreen(new MainPanelScreen());
                return true;
            }
        });
    }
}
