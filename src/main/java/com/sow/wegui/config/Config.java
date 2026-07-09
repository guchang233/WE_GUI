package com.sow.wegui.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sow.wegui.WeGuiMod;
import com.sow.wegui.wheel.WheelPresets;
import com.sow.wegui.wheel.WheelProfile;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 统一配置：状态栏、粘贴预览开关、状态栏布局格式、轮盘预设。
 */
public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("wegui.json");

    public enum Anchor {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    private boolean statusBarEnabled = true;
    private boolean pastePreviewEnabled = true;
    private boolean selectionBoundsEnabled = true;
    private int keyOpenPanel = GLFW.GLFW_KEY_G;
    private int keyToggleAxeMode = GLFW.GLFW_KEY_M;
    private Anchor statusBarAnchor = Anchor.TOP_LEFT;
    private int statusBarOffsetX = 4;
    private int statusBarOffsetY = 4;
    private String statusBarLine1 = "{size} §8| §7{count}";
    private String statusBarLine2 = "§7剪贴板: {clipboard}";
    private String lastSettingsTab = "CONFIG";
    private String lastConfigCategory = "GENERAL";
    private String lastFunctionCategory = "GENERAL";

    // 选区/预览渲染颜色（ARGB）
    public static final int DEFAULT_SELECTION_BOX_COLOR = 0xFFFFD200;
    public static final int DEFAULT_SELECTION_POS1_COLOR = 0xFFFF1010;
    public static final int DEFAULT_SELECTION_POS2_COLOR = 0xFF1010FF;
    public static final int DEFAULT_BLOCK_OUTLINE_COLOR = 0xFF00FFFF;

    private int selectionBoxColor = DEFAULT_SELECTION_BOX_COLOR;
    private int selectionPos1Color = DEFAULT_SELECTION_POS1_COLOR;
    private int selectionPos2Color = DEFAULT_SELECTION_POS2_COLOR;
    private int blockOutlineColor = DEFAULT_BLOCK_OUTLINE_COLOR;
    private boolean blockOutlineEnabled = false;

    private final List<WheelProfile> wheels = new ArrayList<>();

    private static Config INSTANCE;

    public static Config get() {
        if (INSTANCE == null) load();
        return INSTANCE;
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                INSTANCE = GSON.fromJson(json, Config.class);
                if (INSTANCE == null) INSTANCE = new Config();
            } catch (IOException e) {
                WeGuiMod.LOGGER.error("无法读取配置", e);
                INSTANCE = new Config();
            }
        } else {
            INSTANCE = new Config();
        }
        INSTANCE.ensureDefaults();
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(INSTANCE));
        } catch (IOException e) {
            WeGuiMod.LOGGER.error("无法保存配置", e);
        }
    }

    private void ensureDefaults() {
        if (wheels.isEmpty()) {
            wheels.addAll(WheelPresets.createDefaults());
        }
    }

    public boolean isStatusBarEnabled() {
        return statusBarEnabled;
    }

    public void setStatusBarEnabled(boolean enabled) {
        statusBarEnabled = enabled;
    }

    public boolean isPastePreviewEnabled() {
        return pastePreviewEnabled;
    }

    public void setPastePreviewEnabled(boolean enabled) {
        pastePreviewEnabled = enabled;
    }

    public boolean isSelectionBoundsEnabled() {
        return selectionBoundsEnabled;
    }

    public void setSelectionBoundsEnabled(boolean enabled) {
        selectionBoundsEnabled = enabled;
    }

    public int getKeyOpenPanel() {
        return keyOpenPanel;
    }

    public void setKeyOpenPanel(int key) {
        keyOpenPanel = key;
    }

    public int getKeyToggleAxeMode() {
        return keyToggleAxeMode;
    }

    public void setKeyToggleAxeMode(int key) {
        keyToggleAxeMode = key;
    }

    public int getSelectionBoxColor() {
        return selectionBoxColor;
    }

    public void setSelectionBoxColor(int color) {
        this.selectionBoxColor = color;
    }

    public int getSelectionPos1Color() {
        return selectionPos1Color;
    }

    public void setSelectionPos1Color(int color) {
        this.selectionPos1Color = color;
    }

    public int getSelectionPos2Color() {
        return selectionPos2Color;
    }

    public void setSelectionPos2Color(int color) {
        this.selectionPos2Color = color;
    }

    public int getBlockOutlineColor() {
        return blockOutlineColor;
    }

    public void setBlockOutlineColor(int color) {
        this.blockOutlineColor = color;
    }

    public boolean isBlockOutlineEnabled() {
        return blockOutlineEnabled;
    }

    public void setBlockOutlineEnabled(boolean enabled) {
        this.blockOutlineEnabled = enabled;
    }

    public Anchor getStatusBarAnchor() {
        return statusBarAnchor;
    }

    public void setStatusBarAnchor(Anchor anchor) {
        this.statusBarAnchor = anchor;
    }

    public int getStatusBarOffsetX() {
        return statusBarOffsetX;
    }

    public void setStatusBarOffsetX(int offsetX) {
        this.statusBarOffsetX = offsetX;
    }

    public int getStatusBarOffsetY() {
        return statusBarOffsetY;
    }

    public void setStatusBarOffsetY(int offsetY) {
        this.statusBarOffsetY = offsetY;
    }

    public String getStatusBarLine1() {
        return statusBarLine1;
    }

    public void setStatusBarLine1(String line) {
        this.statusBarLine1 = line;
    }

    public String getStatusBarLine2() {
        return statusBarLine2;
    }

    public void setStatusBarLine2(String line) {
        this.statusBarLine2 = line;
    }

    public List<WheelProfile> getWheels() {
        return wheels;
    }

    public String getLastSettingsTab() {
        return lastSettingsTab;
    }

    public void setLastSettingsTab(String lastSettingsTab) {
        this.lastSettingsTab = lastSettingsTab;
    }

    public String getLastConfigCategory() {
        return lastConfigCategory;
    }

    public void setLastConfigCategory(String lastConfigCategory) {
        this.lastConfigCategory = lastConfigCategory;
    }

    public String getLastFunctionCategory() {
        return lastFunctionCategory;
    }

    public void setLastFunctionCategory(String lastFunctionCategory) {
        this.lastFunctionCategory = lastFunctionCategory;
    }
}
