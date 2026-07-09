package com.sow.wegui;

import com.sow.wegui.commands.WeCommands;
import com.sow.wegui.config.Config;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WorldEdit GUI 模组主入口。
 */
public class WeGuiMod implements ModInitializer {
    public static final String MOD_ID = "wegui";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        Config.load();
        WeCommands.init();
        LOGGER.info("WorldEdit GUI loaded.");
    }
}
