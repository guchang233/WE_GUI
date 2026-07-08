package com.sow.wegui.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sow.wegui.WeGuiMod;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.ConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigString;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * MaLiLib 配置处理器。
 * 保存于 config/wegui-malilib.json，并在 ConfigManager 注册。
 */
public final class WeGuiConfigs implements IConfigHandler {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir()
            .resolve("wegui-malilib.json");

    public static final ConfigHotkey OPEN_PANEL_HOTKEY = new ConfigHotkey(
            "openPanelHotkey", "G",
            "打开 WorldEdit GUI 面板的快捷键（默认 G）");

    public static final ConfigString TRIGGER_ITEM = new ConfigString(
            "triggerItem", "minecraft:wooden_axe",
            "触发 GUI 的物品 ID，手持该物品时按热键才会打开面板");

    public static final ConfigBoolean STATUS_BAR_ENABLED = new ConfigBoolean(
            "statusBarEnabled", true,
            "是否显示 WorldEdit 选区状态栏 HUD");

    public static final ConfigInteger STATUS_BAR_OFFSET_X = new ConfigInteger(
            "statusBarOffsetX", 0, -8192, 8192,
            "状态栏水平偏移");

    public static final ConfigInteger STATUS_BAR_OFFSET_Y = new ConfigInteger(
            "statusBarOffsetY", 0, -8192, 8192,
            "状态栏垂直偏移");

    private static final WeGuiConfigs INSTANCE = new WeGuiConfigs();
    private static final List<ConfigBase<?>> CONFIGS = new ArrayList<>();

    static {
        CONFIGS.add(OPEN_PANEL_HOTKEY);
        CONFIGS.add(TRIGGER_ITEM);
        CONFIGS.add(STATUS_BAR_ENABLED);
        CONFIGS.add(STATUS_BAR_OFFSET_X);
        CONFIGS.add(STATUS_BAR_OFFSET_Y);

        ConfigManager.getInstance().registerConfigHandler(WeGuiMod.MOD_ID, INSTANCE);
        INSTANCE.load();
    }

    private WeGuiConfigs() {}

    /**
     * 触发静态初始化并在游戏启动时加载配置。
     */
    public static void init() {
        // 静态块已完成加载
    }

    public static WeGuiConfigs getInstance() {
        return INSTANCE;
    }

    @Override
    public void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }
        try {
            String json = Files.readString(CONFIG_PATH);
            JsonObject root = GSON.fromJson(json, JsonObject.class);
            if (root == null) {
                save();
                return;
            }
            for (ConfigBase<?> config : CONFIGS) {
                JsonElement element = root.get(config.getName());
                if (element != null) {
                    config.setValueFromJsonElement(element);
                }
            }
        } catch (Exception e) {
            WeGuiMod.LOGGER.warn("[WE GUI] 读取 MaLiLib 配置失败，使用默认值", e);
        }
    }

    @Override
    public void save() {
        JsonObject root = new JsonObject();
        for (ConfigBase<?> config : CONFIGS) {
            root.add(config.getName(), config.getAsJsonElement());
        }
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(root));
        } catch (IOException e) {
            WeGuiMod.LOGGER.warn("[WE GUI] 保存 MaLiLib 配置失败", e);
        }
    }

    /**
     * 获取触发物品 ID（优先使用 MaLiLib 配置）。
     */
    public static String getTriggerItem() {
        return TRIGGER_ITEM.getStringValue();
    }

    /**
     * 是否启用状态栏 HUD。
     */
    public static boolean isStatusBarEnabled() {
        return STATUS_BAR_ENABLED.getBooleanValue();
    }
}
