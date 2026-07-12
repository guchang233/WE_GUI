package com.sow.wegui.client;

import com.sow.wegui.InitHandler;
import com.sow.wegui.config.Configs;
import fi.dy.masa.malilib.event.InitializationHandler;
import fi.dy.masa.malilib.event.RenderEventHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * 客户端入口：加载配置、注册 HUD、粘贴预览与输入处理。
 * WAND_ITEM 与 WorldEdit 的 wandItem 保持双向同步。
 */
public class WeGuiClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Configs.loadFromFile();
        InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());

        // 初始化时以本模组配置为准同步到 WorldEdit，保证两边一致
        String cfgWand = Configs.Generic.WAND_ITEM.getStringValue();
        WorldEditBridge.setWandItem(cfgWand);
        WorldEditBridge.bindWandItem(cfgWand);

        // 配置改变 -> 同步到 WorldEdit（默认 wand 配置 + 当前玩家 session 的工具绑定）
        Configs.Generic.WAND_ITEM.setValueChangeCallback(cfg -> {
            String itemId = cfg.getStringValue();
            WorldEditBridge.setWandItem(itemId);
            WorldEditBridge.bindWandItem(itemId);
        });

        // 每 tick 检查 WorldEdit 默认 wandItem 是否被外部命令修改，回写到配置
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null) return;
            String current = WorldEditBridge.getWandItem();
            String cfg = Configs.Generic.WAND_ITEM.getStringValue();
            if (!current.isEmpty() && !current.equals(cfg)) {
                Configs.Generic.WAND_ITEM.setValueFromString(current);
            }
        });

        StatusBar.register();
        // ghost blocks（层③）注册到 BEFORE_TRANSLUCENT
        PastePreviewRenderer.register();
        // overlays（层①②④）注册到 malilib IRenderer
        RenderEventHandler.getInstance().registerWorldLastRenderer(PastePreviewRenderer.getInstance());
        AxeModeHandler.register();

        // 触发 CommandSender 类初始化，注册聊天命令拦截器
        CommandSender.init();
    }
}
