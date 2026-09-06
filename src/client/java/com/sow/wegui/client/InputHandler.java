package com.sow.wegui.client;

import com.sow.wegui.client.screen.RadialMenuScreen;
import com.sow.wegui.client.screen.WeCommandScreen;
import com.sow.wegui.commands.WeCommands;
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
            // 轮盘菜单打开时再按一次轮盘键关闭（切换行为），此时仍需拦截
            if (mc.gui.screen() instanceof RadialMenuScreen
                    && Configs.Hotkeys.OPEN_RADIAL.getKeybind().matches(input.key())) {
                mc.setScreenAndShow(null);
                return true;
            }
            // 其他屏幕打开时（如原版按键设置界面、聊天栏）不拦截按键事件，
            // 否则会阻止将原版按键绑定系统的条目修改为 R/G 等本模组占用的按键
            if (GuiUtils.getCurrentScreen() != null) {
                return false;
            }
            if (Configs.Hotkeys.OPEN_GUI.getKeybind().matches(input.key())) {
                openMainPanel(mc);
                return true;
            }
            if (Configs.Hotkeys.OPEN_RADIAL.getKeybind().matches(input.key())) {
                openRadialMenu(mc);
                return true;
            }
        }
        return false;
    }

    private static void openMainPanel(Minecraft mc) {
        if (mc.player == null) return;
        mc.setScreenAndShow(new WeCommandScreen());
    }

    private static void openRadialMenu(Minecraft mc) {
        if (mc.player == null) return;
        WeCommands.init();
        mc.setScreenAndShow(new RadialMenuScreen());
    }
}
