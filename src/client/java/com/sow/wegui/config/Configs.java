package com.sow.wegui.config;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sow.wegui.WeGuiMod;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.IConfigValue;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.config.options.ConfigColor;
import fi.dy.masa.malilib.config.options.ConfigDouble;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import fi.dy.masa.malilib.config.options.ConfigString;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.data.Color4f;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * MaLiLib 风格的统一配置。
 */
public class Configs implements IConfigHandler {
    public static final Configs INSTANCE = new Configs();

    private static final String CONFIG_FILE_NAME = WeGuiMod.MOD_ID + ".json";

    private static final String GENERIC_KEY = WeGuiMod.MOD_ID + ".config.generic";
    private static final String STATUS_BAR_KEY = WeGuiMod.MOD_ID + ".config.status_bar";
    private static final String PASTE_PREVIEW_KEY = WeGuiMod.MOD_ID + ".config.paste_preview";
    private static final String MODE_INDICATOR_KEY = WeGuiMod.MOD_ID + ".config.mode_indicator";
    private static final String COMMAND_PANEL_KEY = WeGuiMod.MOD_ID + ".config.command_panel";
    private static final String HOTKEYS_KEY = WeGuiMod.MOD_ID + ".config.hotkeys";
    private static final String INTERNAL_KEY = WeGuiMod.MOD_ID + ".config.internal";

    public static class Generic {
        public static final ConfigBooleanHotkeyed STATUS_BAR_ENABLED = new ConfigBooleanHotkeyed("statusBarEnabled", false, "").apply(GENERIC_KEY);
        public static final ConfigBooleanHotkeyed PASTE_PREVIEW_ENABLED = new ConfigBooleanHotkeyed("pastePreviewEnabled", true, "").apply(GENERIC_KEY);
        public static final ConfigBooleanHotkeyed SELECTION_BOUNDS_ENABLED = new ConfigBooleanHotkeyed("selectionBoundsEnabled", true, "").apply(GENERIC_KEY);
        public static final ConfigBooleanHotkeyed SELECTION_MESSAGE_ENABLED = new ConfigBooleanHotkeyed("selectionMessageEnabled", false, "").apply(GENERIC_KEY);
        public static final ConfigBooleanHotkeyed BLOCK_OUTLINE_ENABLED = new ConfigBooleanHotkeyed("blockOutlineEnabled", false, "").apply(GENERIC_KEY);
        public static final ConfigString WAND_ITEM = new ConfigString("wandItem", "minecraft:wooden_axe").apply(GENERIC_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                STATUS_BAR_ENABLED,
                PASTE_PREVIEW_ENABLED,
                SELECTION_BOUNDS_ENABLED,
                SELECTION_MESSAGE_ENABLED,
                BLOCK_OUTLINE_ENABLED,
                WAND_ITEM
        );

        public static final ImmutableList<IHotkey> TOGGLE_HOTKEYS = ImmutableList.of(
                STATUS_BAR_ENABLED,
                PASTE_PREVIEW_ENABLED,
                SELECTION_BOUNDS_ENABLED,
                SELECTION_MESSAGE_ENABLED,
                BLOCK_OUTLINE_ENABLED
        );
    }

    public static class StatusBar {
        public static final ConfigOptionList STATUS_BAR_ANCHOR = new ConfigOptionList("statusBarAnchor", Anchor.TOP_LEFT).apply(STATUS_BAR_KEY);
        public static final ConfigInteger STATUS_BAR_OFFSET_X = new ConfigInteger("statusBarOffsetX", 4, -4096, 4096).apply(STATUS_BAR_KEY);
        public static final ConfigInteger STATUS_BAR_OFFSET_Y = new ConfigInteger("statusBarOffsetY", 4, -4096, 4096).apply(STATUS_BAR_KEY);
        public static final ConfigString STATUS_BAR_LINE1 = new ConfigString("statusBarLine1", "{size} §8| §7{count}").apply(STATUS_BAR_KEY);
        public static final ConfigString STATUS_BAR_LINE2 = new ConfigString("statusBarLine2", "§7Clipboard: {clipboard}").apply(STATUS_BAR_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                STATUS_BAR_ANCHOR,
                STATUS_BAR_OFFSET_X,
                STATUS_BAR_OFFSET_Y,
                STATUS_BAR_LINE1,
                STATUS_BAR_LINE2
        );
    }

    public static class PastePreview {
        public static final ConfigOptionList PASTE_PLACEMENT_MODE = new ConfigOptionList("pastePlacementMode", PastePlacementMode.FOLLOW_PLAYER).apply(PASTE_PREVIEW_KEY);
        public static final ConfigColor SELECTION_BOX_COLOR = new ConfigColor("selectionBoxColor", new Color4f(1.0f, 0.82f, 0.0f, 1.0f)).apply(PASTE_PREVIEW_KEY);
        public static final ConfigColor SELECTION_POS1_COLOR = new ConfigColor("selectionPos1Color", new Color4f(1.0f, 0.06f, 0.06f, 1.0f)).apply(PASTE_PREVIEW_KEY);
        public static final ConfigColor SELECTION_POS2_COLOR = new ConfigColor("selectionPos2Color", new Color4f(0.06f, 0.06f, 1.0f, 1.0f)).apply(PASTE_PREVIEW_KEY);
        public static final ConfigColor BLOCK_OUTLINE_COLOR = new ConfigColor("blockOutlineColor", new Color4f(0.0f, 1.0f, 1.0f, 1.0f)).apply(PASTE_PREVIEW_KEY);
        public static final ConfigColor BOX_SIDE_COLOR = new ConfigColor("boxSideColor", new Color4f(0.0f, 1.0f, 0.5f, 0.2f)).apply(PASTE_PREVIEW_KEY);
        public static final ConfigDouble GHOST_BLOCK_ALPHA = new ConfigDouble("ghostBlockAlpha", 0.5, 0.0, 1.0).apply(PASTE_PREVIEW_KEY);
        public static final ConfigDouble BOX_SIDE_ALPHA = new ConfigDouble("boxSideAlpha", 0.2, 0.0, 1.0).apply(PASTE_PREVIEW_KEY);
        public static final ConfigInteger GHOST_RENDER_DISTANCE = new ConfigInteger("ghostRenderDistance", 64, 16, 256).apply(PASTE_PREVIEW_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                PASTE_PLACEMENT_MODE,
                SELECTION_BOX_COLOR,
                SELECTION_POS1_COLOR,
                SELECTION_POS2_COLOR,
                BLOCK_OUTLINE_COLOR,
                BOX_SIDE_COLOR,
                GHOST_BLOCK_ALPHA,
                BOX_SIDE_ALPHA,
                GHOST_RENDER_DISTANCE
        );
    }

    public static class ModeIndicator {
        public static final ConfigBoolean ENABLED = new ConfigBoolean("modeIndicatorEnabled", true).apply(MODE_INDICATOR_KEY);
        public static final ConfigInteger OFFSET_X = new ConfigInteger("modeIndicatorOffsetX", 6, -4096, 4096).apply(MODE_INDICATOR_KEY);
        public static final ConfigInteger OFFSET_Y = new ConfigInteger("modeIndicatorOffsetY", 6, -4096, 4096).apply(MODE_INDICATOR_KEY);
        public static final ConfigInteger SCALE = new ConfigInteger("modeIndicatorScale", 100, 50, 200).apply(MODE_INDICATOR_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                ENABLED,
                OFFSET_X,
                OFFSET_Y,
                SCALE
        );
    }

    public static class CommandPanel {
        public static final ConfigBoolean SHOW_DESCRIPTION = new ConfigBoolean("commandPanelShowDescription", true).apply(COMMAND_PANEL_KEY);
        public static final ConfigBoolean COMPACT_MODE = new ConfigBoolean("commandPanelCompactMode", false).apply(COMMAND_PANEL_KEY);
        public static final ConfigString FAVORITES = new ConfigString("commandPanelFavorites", "").apply(COMMAND_PANEL_KEY);
        public static final ConfigString RECENT_COMMANDS = new ConfigString("commandPanelRecentCommands", "").apply(COMMAND_PANEL_KEY);
        public static final ConfigInteger MAX_RECENT = new ConfigInteger("commandPanelMaxRecent", 10, 0, 50).apply(COMMAND_PANEL_KEY);
        public static final ConfigInteger RADIAL_RADIUS = new ConfigInteger("radialRadius", 70, 40, 200).apply(COMMAND_PANEL_KEY);
        public static final ConfigInteger RADIAL_INNER_RADIUS = new ConfigInteger("radialInnerRadius", 20, 5, 80).apply(COMMAND_PANEL_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                SHOW_DESCRIPTION,
                COMPACT_MODE,
                FAVORITES,
                RECENT_COMMANDS,
                MAX_RECENT,
                RADIAL_RADIUS,
                RADIAL_INNER_RADIUS
        );
    }

    public static class Hotkeys {
        public static final ConfigHotkey OPEN_GUI = new ConfigHotkey("openGui", "G").apply(HOTKEYS_KEY);
        public static final ConfigHotkey OPEN_RADIAL = new ConfigHotkey("openRadialMenu", "R").apply(HOTKEYS_KEY);

        public static final ImmutableList<IConfigValue> OPTIONS = ImmutableList.of(OPEN_GUI, OPEN_RADIAL);
        public static final List<IHotkey> HOTKEY_LIST = ImmutableList.of(OPEN_GUI, OPEN_RADIAL);
    }

    public static class Internal {
        public static final ConfigString LAST_CONFIG_CATEGORY = new ConfigString("lastConfigCategory", "GENERAL").apply(INTERNAL_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(LAST_CONFIG_CATEGORY);
    }

    public static void loadFromFile() {
        Path configFile = FileUtils.getConfigDirectoryAsPath().resolve(CONFIG_FILE_NAME);
        if (Files.exists(configFile) && Files.isReadable(configFile)) {
            JsonElement element = JsonUtils.parseJsonFileAsPath(configFile);
            if (element != null && element.isJsonObject()) {
                JsonObject root = element.getAsJsonObject();
                ConfigUtils.readConfigBase(root, "Generic", Generic.OPTIONS);
                ConfigUtils.readConfigBase(root, "StatusBar", StatusBar.OPTIONS);
                ConfigUtils.readConfigBase(root, "PastePreview", PastePreview.OPTIONS);
                ConfigUtils.readConfigBase(root, "ModeIndicator", ModeIndicator.OPTIONS);
                ConfigUtils.readConfigBase(root, "CommandPanel", CommandPanel.OPTIONS);
                ConfigUtils.readConfigBase(root, "Hotkeys", Hotkeys.OPTIONS);
                ConfigUtils.readConfigBase(root, "Internal", Internal.OPTIONS);
            } else {
                WeGuiMod.LOGGER.error("无法解析配置文件 '{}'", configFile.toAbsolutePath());
            }
        }
    }

    public static void saveToFile() {
        Path dir = FileUtils.getConfigDirectoryAsPath();
        if (!Files.exists(dir)) {
            FileUtils.createDirectoriesIfMissing(dir);
        }
        if (Files.isDirectory(dir)) {
            JsonObject root = new JsonObject();
            ConfigUtils.writeConfigBase(root, "Generic", Generic.OPTIONS);
            ConfigUtils.writeConfigBase(root, "StatusBar", StatusBar.OPTIONS);
            ConfigUtils.writeConfigBase(root, "PastePreview", PastePreview.OPTIONS);
            ConfigUtils.writeConfigBase(root, "ModeIndicator", ModeIndicator.OPTIONS);
            ConfigUtils.writeConfigBase(root, "CommandPanel", CommandPanel.OPTIONS);
            ConfigUtils.writeConfigBase(root, "Hotkeys", Hotkeys.OPTIONS);
            ConfigUtils.writeConfigBase(root, "Internal", Internal.OPTIONS);
            JsonUtils.writeJsonToFileAsPath(root, dir.resolve(CONFIG_FILE_NAME));
        }
    }

    @Override
    public void onConfigsChanged() {
        saveToFile();
        loadFromFile();
    }

    @Override
    public void load() {
        loadFromFile();
    }

    @Override
    public void save() {
        saveToFile();
    }
}
