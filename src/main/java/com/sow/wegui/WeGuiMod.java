package com.sow.wegui;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WE GUI 模组入口。
 */
public final class WeGuiMod implements ModInitializer {
    public static final String MOD_ID = "wegui";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        WeCommandRegistry.init();
        LOGGER.info("[WE GUI] 已就绪，共 {} 条命令", WeCommandRegistry.getAll().size());
    }
}