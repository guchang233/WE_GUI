package com.sow.wegui.client;

import com.sow.wegui.InitHandler;
import com.sow.wegui.config.Configs;
import fi.dy.masa.malilib.event.InitializationHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * 客户端入口：加载配置、注册 HUD、剪贴板同步与输入处理。
 * 渲染由 Litematica 通过 LitematicaBridge 接管；WAND_ITEM 与 WorldEdit 双向同步。
 */
public class WeGuiClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Configs.loadFromFile();
        InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());

        // 初始化时以本模组配置为准同步到 WorldEdit
        String cfgWand = Configs.Generic.WAND_ITEM.getStringValue();
        WorldEditBridge.setWandItem(cfgWand);
        WorldEditBridge.bindWandItem(cfgWand);

        // 配置改变 -> 同步到 WorldEdit
        Configs.Generic.WAND_ITEM.setValueChangeCallback(cfg -> {
            String itemId = cfg.getStringValue();
            WorldEditBridge.setWandItem(itemId);
            WorldEditBridge.bindWandItem(itemId);
        });

        // 检查 WorldEdit 默认 wandItem 是否被外部命令修改，回写到配置
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null) return;
            String current = WorldEditBridge.getWandItem();
            String cfg = Configs.Generic.WAND_ITEM.getStringValue();
            if (!current.isEmpty() && !current.equals(cfg)) {
                Configs.Generic.WAND_ITEM.setValueFromString(current);
            }
        });

        StatusBar.register();
        LitematicaBridge.register();
        AxeModeHandler.register();

        CommandSender.init();
    }
}
