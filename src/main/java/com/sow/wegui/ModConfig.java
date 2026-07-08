package com.sow.wegui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 模组全局配置。
 * 使用 Gson 序列化，保存在 config/wegui.json。
 */
public final class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile ModConfig INSTANCE;
    private static Path configPath;

    // ── 字段 ──
    public String triggerItem = "minecraft:wooden_axe";
    public int extraKey = -1;                         // GLFW key code，-1 = 仅 Alt
    public int wheelSlotCount = 8;                     // 每个轮盘默认槽位数
    public int holdThresholdMs = 250;                  // 短按/长按阈值
    public int activeWheelIndex = 0;
    public List<WheelProfile> wheels = new ArrayList<>();

    public String statusBarPosition = "BOTTOM_LEFT";
    public int statusBarOffsetX = 0;
    public int statusBarOffsetY = 0;
    public String statusBarLine1 = "{size} §8| §7{count}";
    public String statusBarLine2 = "§7剪贴板: {clipboard}";

    public enum Anchor { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    public Anchor statusBarAnchor() {
        try {
            return Anchor.valueOf(statusBarPosition);
        } catch (Exception e) {
            return Anchor.BOTTOM_LEFT;
        }
    }

    public static ModConfig get() {
        if (INSTANCE == null) load();
        return INSTANCE;
    }

    public WheelProfile getActiveWheel() {
        if (wheels.isEmpty()) {
            wheels.addAll(WheelPresetsFactory.createDefaultWheels());
            save();
        }
        if (activeWheelIndex < 0 || activeWheelIndex >= wheels.size()) {
            activeWheelIndex = 0;
        }
        return wheels.get(activeWheelIndex);
    }

    public void setActiveWheel(int index) {
        if (index >= 0 && index < wheels.size()) {
            activeWheelIndex = index;
        }
    }

    public void setWheelSlotCount(int count) {
        wheelSlotCount = Math.max(4, Math.min(12, count));
        for (WheelProfile w : wheels) {
            w.ensureSlotCount(wheelSlotCount);
        }
        save();
    }

    public void rebuildSlots() {
        for (WheelProfile w : wheels) {
            w.ensureSlotCount(wheelSlotCount);
        }
    }

    // ── 加载/保存 ──
    public static void load() {
        configPath = FabricLoader.getInstance().getConfigDir().resolve(WeGuiMod.MOD_ID + ".json");
        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                ModConfig loaded = GSON.fromJson(json, ModConfig.class);
                if (loaded != null) INSTANCE = loaded;
            } catch (Exception e) {
                WeGuiMod.LOGGER.warn("[WE GUI] 配置解析失败，使用默认值", e);
                try {
                    Path bak = configPath.resolveSibling(WeGuiMod.MOD_ID + ".json.bak");
                    Files.move(configPath, bak);
                } catch (Exception ignored) {}
            }
        }
        if (INSTANCE == null) INSTANCE = new ModConfig();
        ensureDefaults();
        save();
    }

    public static void save() {
        if (configPath == null) return;
        try {
            Files.writeString(configPath, GSON.toJson(INSTANCE));
        } catch (IOException e) {
            WeGuiMod.LOGGER.warn("[WE GUI] 配置保存失败", e);
        }
    }

    private static void ensureDefaults() {
        INSTANCE.holdThresholdMs = Math.max(50, Math.min(1000, INSTANCE.holdThresholdMs));
        INSTANCE.wheelSlotCount = Math.max(4, Math.min(12, INSTANCE.wheelSlotCount));
        if (INSTANCE.wheels == null || INSTANCE.wheels.isEmpty()) {
            INSTANCE.wheels = WheelPresetsFactory.createDefaultWheels();
        }
        INSTANCE.rebuildSlots();
        if (INSTANCE.triggerItem == null || INSTANCE.triggerItem.isBlank()) {
            INSTANCE.triggerItem = "minecraft:wooden_axe";
        }
        if (INSTANCE.statusBarLine1 == null) INSTANCE.statusBarLine1 = "{size} §8| §7{count}";
        if (INSTANCE.statusBarLine2 == null) INSTANCE.statusBarLine2 = "§7剪贴板: {clipboard}";
    }
}