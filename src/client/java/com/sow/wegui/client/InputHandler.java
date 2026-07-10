package com.sow.wegui.client;

import com.sow.wegui.client.screen.WeCommandScreen;
import com.sow.wegui.config.Configs;
import fi.dy.masa.malilib.hotkeys.*;
import fi.dy.masa.malilib.util.GuiUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;

public class InputHandler implements IKeybindProvider, IKeyboardInputHandler {
    private static final InputHandler INSTANCE = new InputHandler();

    private InputHandler() {}

    public static InputHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public void addKeysToMap(IKeybindManager manager) {
        for (IHotkey hotkey : Configs.Hotkeys.HOTKEY_LIST) {
            manager.addKeybindToMap(hotkey.getKeybind());
        }
        for (IHotkey hotkey : Configs.Generic.TOGGLE_HOTKEYS) {
            manager.addKeybindToMap(hotkey.getKeybind());
        }
    }

    @Override
    public void addHotkeys(IKeybindManager manager) {
        manager.addHotkeysForCategory(
                com.sow.wegui.WeGuiMod.MOD_NAME,
                "wegui.hotkeys.category.generic",
                Configs.Hotkeys.HOTKEY_LIST
        );
        manager.addHotkeysForCategory(
                com.sow.wegui.WeGuiMod.MOD_NAME,
                "wegui.hotkeys.category.toggles",
                Configs.Generic.TOGGLE_HOTKEYS
        );
    }

    @Override
    public boolean onKeyInput(KeyEvent input, boolean eventKeyState) {
        if (eventKeyState) {
            Minecraft mc = Minecraft.getInstance();
            if (Configs.Hotkeys.OPEN_GUI.getKeybind().matches(input.key())) {
                openMainPanel(mc);
                return true;
            }
        }
        return false;
    }

    private static void openMainPanel(Minecraft mc) {
        if (mc.player == null || GuiUtils.getCurrentScreen() != null) return;
        mc.setScreen(new WeCommandScreen());
    }
}
