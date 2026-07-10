package com.sow.wegui;

import com.sow.wegui.client.InputHandler;
import com.sow.wegui.client.screen.WeGuiConfigs;
import com.sow.wegui.config.Configs;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ModInfo;

public class InitHandler implements IInitializationHandler {
    @Override
    public void registerModHandlers() {
        ConfigManager.getInstance().registerConfigHandler(WeGuiMod.MOD_ID, Configs.INSTANCE);
        Registry.CONFIG_SCREEN.registerConfigScreenFactory(
                new ModInfo(WeGuiMod.MOD_ID, WeGuiMod.MOD_NAME, WeGuiConfigs::new)
        );

        InputHandler inputHandler = InputHandler.getInstance();
        InputEventHandler.getKeybindManager().registerKeybindProvider(inputHandler);
        InputEventHandler.getInputManager().registerKeyboardInputHandler(inputHandler);
    }
}
