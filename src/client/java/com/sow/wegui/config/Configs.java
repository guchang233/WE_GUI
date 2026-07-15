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
 *
 * <p>渲染相关配置全部归入 {@link RenderStyles} 单一分类，默认值严格对齐 Litematica 26.2：
 * <ul>
 *   <li>开关类（enableRendering/enableGhostBlocks/enableOverlay/...）对应 Litematica Visuals</li>
 *   <li>Overlay 参数（overlayOutlineWidth/overlayRenderThrough/...）对应 Litematica SCHEMATIC_OVERLAY_*</li>
 *   <li>mismatch 颜色（verifyMissingColor=#00FFFF 等）对应 Litematica MismatchType 颜色常量</li>
 *   <li>选区角点颜色（selectionPos1Color=#FF1010）对应 Litematica OverlayRenderer.colorPos1</li>
 * </ul>
 * 颜色硬编码为 Litematica 同款数值，可通过配置覆盖以保证像素级视觉一致。</p>
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
    private static final String RENDER_STYLES_KEY = WeGuiMod.MOD_ID + ".config.render_styles";

    public static class Generic {
        public static final ConfigBooleanHotkeyed STATUS_BAR_ENABLED = new ConfigBooleanHotkeyed("statusBarEnabled", false, "").apply(GENERIC_KEY);
        public static final ConfigBooleanHotkeyed PASTE_PREVIEW_ENABLED = new ConfigBooleanHotkeyed("pastePreviewEnabled", true, "").apply(GENERIC_KEY);
        public static final ConfigBooleanHotkeyed SELECTION_BOUNDS_ENABLED = new ConfigBooleanHotkeyed("selectionBoundsEnabled", true, "").apply(GENERIC_KEY);
        public static final ConfigBooleanHotkeyed SELECTION_MESSAGE_ENABLED = new ConfigBooleanHotkeyed("selectionMessageEnabled", false, "").apply(GENERIC_KEY);
        public static final ConfigBooleanHotkeyed BLOCK_OUTLINE_ENABLED = new ConfigBooleanHotkeyed("blockOutlineEnabled", false, "").apply(GENERIC_KEY);
        public static final ConfigBooleanHotkeyed BLOCK_VERIFICATION_ENABLED = new ConfigBooleanHotkeyed("blockVerificationEnabled", true, "").apply(GENERIC_KEY);
        public static final ConfigString WAND_ITEM = new ConfigString("wandItem", "minecraft:wooden_axe").apply(GENERIC_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                STATUS_BAR_ENABLED,
                PASTE_PREVIEW_ENABLED,
                SELECTION_BOUNDS_ENABLED,
                SELECTION_MESSAGE_ENABLED,
                BLOCK_OUTLINE_ENABLED,
                BLOCK_VERIFICATION_ENABLED,
                WAND_ITEM
        );

        public static final ImmutableList<IHotkey> TOGGLE_HOTKEYS = ImmutableList.of(
                STATUS_BAR_ENABLED,
                PASTE_PREVIEW_ENABLED,
                SELECTION_BOUNDS_ENABLED,
                SELECTION_MESSAGE_ENABLED,
                BLOCK_OUTLINE_ENABLED,
                BLOCK_VERIFICATION_ENABLED
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
        public static final ConfigOptionList RENDER_STYLE_PRESET = new ConfigOptionList("renderStylePreset", RenderStylePreset.DEFAULT).apply(PASTE_PREVIEW_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                PASTE_PLACEMENT_MODE,
                RENDER_STYLE_PRESET
        );
    }

    /**
     * 渲染样式统一分类：所有渲染相关开关、参数、颜色集中此处。
     *
     * <p>默认值严格对齐 Litematica 26.2 源码（fi.dy.masa.litematica.config.Configs）：
     * <ul>
     *   <li>开关默认值：与 Litematica Visuals 一致</li>
     *   <li>数值默认值：与 Litematica Visuals 一致（如 outlineWidth=1.0、alpha=0.2、distance=64）</li>
     *   <li>颜色默认值：与 Litematica MismatchType / OverlayRenderer 一致
     *     <ul>
     *       <li>MISSING=#00FFFF（Litematica MismatchType.MISSING=0x00FFFF）</li>
     *       <li>WRONG_BLOCK=#FF0000（Litematica MismatchType.WRONG_BLOCK=0xFF0000）</li>
     *       <li>WRONG_STATE=#FFAF00（Litematica MismatchType.WRONG_STATE=0xFFAF00）</li>
     *       <li>EXTRA=#FF00CF（Litematica MismatchType.EXTRA=0xFF00CF）</li>
     *       <li>DIFF_BLOCK=#FAF000（Litematica MismatchType.DIFF_BLOCK=0xFAF000）</li>
     *       <li>POS1=#FF1010（Litematica OverlayRenderer.colorPos1=(1, 0.0625, 0.0625)）</li>
     *       <li>POS2=#1010FF（Litematica OverlayRenderer.colorPos2=(0.0625, 0.0625, 1)）</li>
     *     </ul>
     *   </li>
     * </ul></p>
     */
    public static class RenderStyles {
        // ---- 总开关 ----
        /** 渲染总开关（Litematica Visuals.ENABLE_RENDERING） */
        public static final ConfigBooleanHotkeyed ENABLE_RENDERING = new ConfigBooleanHotkeyed("enableRendering", true, "").apply(RENDER_STYLES_KEY);
        /** 启用幽灵方块（Litematica Visuals.ENABLE_SCHEMATIC_BLOCKS） */
        public static final ConfigBooleanHotkeyed ENABLE_GHOST_BLOCKS = new ConfigBooleanHotkeyed("enableGhostBlocks", true, "").apply(RENDER_STYLES_KEY);
        /** 启用选区渲染（Litematica Visuals.ENABLE_AREA_SELECTION_RENDERING） */
        public static final ConfigBooleanHotkeyed ENABLE_SELECTION_RENDERING = new ConfigBooleanHotkeyed("enableSelectionRendering", true, "").apply(RENDER_STYLES_KEY);
        /** 启用覆盖层（Litematica Visuals.ENABLE_SCHEMATIC_OVERLAY） */
        public static final ConfigBooleanHotkeyed ENABLE_OVERLAY = new ConfigBooleanHotkeyed("enableOverlay", true, "").apply(RENDER_STYLES_KEY);
        /** 启用 overlay 剔除（Litematica Visuals.ENABLE_SCHEMATIC_OVERLAY_CULLING） */
        public static final ConfigBooleanHotkeyed ENABLE_OVERLAY_CULLING = new ConfigBooleanHotkeyed("enableOverlayCulling", true, "").apply(RENDER_STYLES_KEY);

        // ---- 选区框 ----
        /** 选区框半透明面（Litematica Visuals.RENDER_AREA_SELECTION_BOX_SIDES，默认 true） */
        public static final ConfigBoolean RENDER_AREA_SELECTION_BOX_SIDES = new ConfigBoolean("renderAreaSelectionBoxSides", true).apply(RENDER_STYLES_KEY);
        /** 粘贴框半透明面（Litematica Visuals.RENDER_PLACEMENT_BOX_SIDES，默认 false） */
        public static final ConfigBoolean RENDER_PASTE_BOX_SIDES = new ConfigBoolean("renderPasteBoxSides", false).apply(RENDER_STYLES_KEY);
        /** 粘贴框面透明度（Litematica Visuals.PLACEMENT_BOX_SIDE_ALPHA=0.2） */
        public static final ConfigDouble PASTE_BOX_SIDE_ALPHA = new ConfigDouble("pasteBoxSideAlpha", 0.2, 0.0, 1.0).apply(RENDER_STYLES_KEY);
        /** 粘贴框线框 + 面色（Litematica colorArea=(1,1,1) 白色，粘贴外框线与半透明面共用此色） */
        public static final ConfigColor PASTE_BOX_COLOR = new ConfigColor("pasteBoxColor", "#FFFFFFFF").apply(RENDER_STYLES_KEY);
        /** 选区框面色（Litematica Colors.AREA_SELECTION_BOX_SIDE_COLOR，默认 #30FFFFFF） */
        public static final ConfigColor AREA_SELECTION_BOX_SIDE_COLOR = new ConfigColor("areaSelectionBoxSideColor", "#30FFFFFF").apply(RENDER_STYLES_KEY);
        /** 非验证模式下每方块覆盖层颜色（默认青色 #00FFFFFF，与 Litematica schematic overlay 单色风格一致） */
        public static final ConfigColor BLOCK_OVERLAY_COLOR = new ConfigColor("blockOverlayColor", "#00FFFFFF").apply(RENDER_STYLES_KEY);
        /** pos1 角点框颜色（Litematica OverlayRenderer.colorPos1=(1, 0.0625, 0.0625) ≈ #FF1010） */
        public static final ConfigColor SELECTION_POS1_COLOR = new ConfigColor("selectionPos1Color", new Color4f(1.0f, 0.0625f, 0.0625f, 1.0f)).apply(RENDER_STYLES_KEY);
        /** pos2 角点框颜色（Litematica OverlayRenderer.colorPos2=(0.0625, 0.0625, 1) ≈ #1010FF） */
        public static final ConfigColor SELECTION_POS2_COLOR = new ConfigColor("selectionPos2Color", new Color4f(0.0625f, 0.0625f, 1.0f, 1.0f)).apply(RENDER_STYLES_KEY);

        // ---- Ghost block 渲染风格 ----
        /** 半透明方块（Litematica Visuals.RENDER_BLOCKS_AS_TRANSLUCENT，WeGui 默认 true 以避免深度遮挡） */
        public static final ConfigBooleanHotkeyed RENDER_BLOCKS_AS_TRANSLUCENT = new ConfigBooleanHotkeyed("renderBlocksAsTranslucent", true, "").apply(RENDER_STYLES_KEY);
        /** 假光照（Litematica Visuals.ENABLE_FAKE_LIGHTING） */
        public static final ConfigBooleanHotkeyed ENABLE_FAKE_LIGHTING = new ConfigBooleanHotkeyed("enableFakeLighting", false, "").apply(RENDER_STYLES_KEY);
        /** 幽灵方块透明度（Litematica Visuals.GHOST_BLOCK_ALPHA=0.5） */
        public static final ConfigDouble GHOST_BLOCK_ALPHA = new ConfigDouble("ghostBlockAlpha", 0.5, 0.0, 1.0).apply(RENDER_STYLES_KEY);
        /** 假光照等级（Litematica Visuals.RENDER_FAKE_LIGHTING_LEVEL=15） */
        public static final ConfigInteger RENDER_FAKE_LIGHTING_LEVEL = new ConfigInteger("renderFakeLightingLevel", 15, 0, 15).apply(RENDER_STYLES_KEY);
        /** 幽灵渲染距离（按 chunk 分组剔除，无对应 Litematica 配置） */
        public static final ConfigInteger GHOST_RENDER_DISTANCE = new ConfigInteger("ghostRenderDistance", 64, 16, 256).apply(RENDER_STYLES_KEY);

        // ---- Overlay 参数（Litematica SCHEMATIC_OVERLAY_*） ----
        /** 减少内部面（Litematica Visuals.OVERLAY_REDUCED_INNER_SIDES=false） */
        public static final ConfigBooleanHotkeyed OVERLAY_REDUCED_INNER_SIDES = new ConfigBooleanHotkeyed("overlayReducedInnerSides", false, "").apply(RENDER_STYLES_KEY);
        /** 覆盖层边框线（Litematica Visuals.SCHEMATIC_OVERLAY_ENABLE_OUTLINES=true） */
        public static final ConfigBooleanHotkeyed OVERLAY_ENABLE_OUTLINES = new ConfigBooleanHotkeyed("overlayEnableOutlines", true, "").apply(RENDER_STYLES_KEY);
        /** 覆盖层填充面（Litematica Visuals.SCHEMATIC_OVERLAY_ENABLE_SIDES=true） */
        public static final ConfigBooleanHotkeyed OVERLAY_ENABLE_SIDES = new ConfigBooleanHotkeyed("overlayEnableSides", true, "").apply(RENDER_STYLES_KEY);
        /** 模型边框（Litematica Visuals.SCHEMATIC_OVERLAY_MODEL_OUTLINE=true） */
        public static final ConfigBooleanHotkeyed OVERLAY_MODEL_OUTLINE = new ConfigBooleanHotkeyed("overlayModelOutline", true, "").apply(RENDER_STYLES_KEY);
        /** 模型填充面（Litematica Visuals.SCHEMATIC_OVERLAY_MODEL_SIDES=true） */
        public static final ConfigBooleanHotkeyed OVERLAY_MODEL_SIDES = new ConfigBooleanHotkeyed("overlayModelSides", true, "").apply(RENDER_STYLES_KEY);
        /** 透墙渲染（Litematica Visuals.SCHEMATIC_OVERLAY_RENDER_THROUGH=false） */
        public static final ConfigBooleanHotkeyed OVERLAY_RENDER_THROUGH = new ConfigBooleanHotkeyed("overlayRenderThrough", false, "").apply(RENDER_STYLES_KEY);
        /** 覆盖层线宽（Litematica Visuals.SCHEMATIC_OVERLAY_OUTLINE_WIDTH=1.0） */
        public static final ConfigDouble OVERLAY_OUTLINE_WIDTH = new ConfigDouble("overlayOutlineWidth", 1.0, 0.0, 64.0).apply(RENDER_STYLES_KEY);
        /** 透墙线宽（Litematica Visuals.SCHEMATIC_OVERLAY_OUTLINE_WIDTH_THROUGH=3.0） */
        public static final ConfigDouble OVERLAY_OUTLINE_WIDTH_THROUGH = new ConfigDouble("overlayOutlineWidthThrough", 3.0, 0.0, 64.0).apply(RENDER_STYLES_KEY);

        // ---- Mismatch 类型开关（Litematica SCHEMATIC_OVERLAY_TYPE_*） ----
        public static final ConfigBooleanHotkeyed OVERLAY_TYPE_DIFF_BLOCK = new ConfigBooleanHotkeyed("overlayTypeDiffBlock", true, "").apply(RENDER_STYLES_KEY);
        public static final ConfigBooleanHotkeyed OVERLAY_TYPE_EXTRA = new ConfigBooleanHotkeyed("overlayTypeExtra", true, "").apply(RENDER_STYLES_KEY);
        public static final ConfigBooleanHotkeyed OVERLAY_TYPE_MISSING = new ConfigBooleanHotkeyed("overlayTypeMissing", true, "").apply(RENDER_STYLES_KEY);
        public static final ConfigBooleanHotkeyed OVERLAY_TYPE_WRONG_BLOCK = new ConfigBooleanHotkeyed("overlayTypeWrongBlock", true, "").apply(RENDER_STYLES_KEY);
        public static final ConfigBooleanHotkeyed OVERLAY_TYPE_WRONG_STATE = new ConfigBooleanHotkeyed("overlayTypeWrongState", true, "").apply(RENDER_STYLES_KEY);

        // ---- Mismatch 渲染参数（Litematica Visuals.RENDER_ERROR_MARKER_*） ----
        /** 错误连接线（Litematica Visuals.RENDER_ERROR_MARKER_CONNECTIONS=false） */
        public static final ConfigBoolean RENDER_ERROR_MARKER_CONNECTIONS = new ConfigBoolean("renderErrorMarkerConnections", false).apply(RENDER_STYLES_KEY);
        /** 渲染错误标记填充面（Litematica Visuals.RENDER_ERROR_MARKER_SIDES=true，始终穿墙） */
        public static final ConfigBoolean RENDER_ERROR_MARKER_SIDES = new ConfigBoolean("renderErrorMarkerSides", true).apply(RENDER_STYLES_KEY);
        /** 验证填充面透明度（Litematica InfoOverlays.VERIFIER_ERROR_HILIGHT_ALPHA=0.2） */
        public static final ConfigDouble VERIFY_HILIGHT_ALPHA = new ConfigDouble("verifyHilightAlpha", 0.2, 0.0, 1.0).apply(RENDER_STYLES_KEY);
        /** 验证最大位置数（Litematica InfoOverlays.VERIFIER_ERROR_HILIGHT_MAX_POSITIONS=1000） */
        public static final ConfigInteger VERIFIER_ERROR_HILIGHT_MAX_POSITIONS = new ConfigInteger("verifierErrorHilightMaxPositions", 1000, 1, 1000000).apply(RENDER_STYLES_KEY);

        // ---- 验证颜色（严格对齐 Litematica Configs.Colors.SCHEMATIC_OVERLAY_COLOR_*，#AARRGGBB 格式） ----
        /** 缺失颜色（Litematica SCHEMATIC_OVERLAY_COLOR_MISSING=#2C33B3E6，青色半透明） */
        public static final ConfigColor VERIFY_MISSING_COLOR = new ConfigColor("verifyMissingColor", "#2C33B3E6").apply(RENDER_STYLES_KEY);
        /** 错方块颜色（Litematica SCHEMATIC_OVERLAY_COLOR_WRONG_BLOCK=#4CFF3333，红色半透明） */
        public static final ConfigColor VERIFY_WRONG_BLOCK_COLOR = new ConfigColor("verifyWrongBlockColor", "#4CFF3333").apply(RENDER_STYLES_KEY);
        /** 错状态颜色（Litematica SCHEMATIC_OVERLAY_COLOR_WRONG_STATE=#4CFF9010，橙色半透明） */
        public static final ConfigColor VERIFY_WRONG_STATE_COLOR = new ConfigColor("verifyWrongStateColor", "#4CFF9010").apply(RENDER_STYLES_KEY);
        /** 多余颜色（Litematica SCHEMATIC_OVERLAY_COLOR_EXTRA=#4CFF4CE6，品红半透明） */
        public static final ConfigColor VERIFY_EXTRA_COLOR = new ConfigColor("verifyExtraColor", "#4CFF4CE6").apply(RENDER_STYLES_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                // 总开关
                ENABLE_RENDERING, ENABLE_GHOST_BLOCKS, ENABLE_SELECTION_RENDERING,
                ENABLE_OVERLAY, ENABLE_OVERLAY_CULLING,
                // 选区框
                RENDER_AREA_SELECTION_BOX_SIDES, RENDER_PASTE_BOX_SIDES, PASTE_BOX_SIDE_ALPHA,
                PASTE_BOX_COLOR, AREA_SELECTION_BOX_SIDE_COLOR, BLOCK_OVERLAY_COLOR,
                SELECTION_POS1_COLOR, SELECTION_POS2_COLOR,
                // Ghost block 渲染风格
                RENDER_BLOCKS_AS_TRANSLUCENT, ENABLE_FAKE_LIGHTING,
                GHOST_BLOCK_ALPHA, RENDER_FAKE_LIGHTING_LEVEL, GHOST_RENDER_DISTANCE,
                // Overlay 参数
                OVERLAY_REDUCED_INNER_SIDES, OVERLAY_ENABLE_OUTLINES, OVERLAY_ENABLE_SIDES,
                OVERLAY_MODEL_OUTLINE, OVERLAY_MODEL_SIDES, OVERLAY_RENDER_THROUGH,
                OVERLAY_OUTLINE_WIDTH, OVERLAY_OUTLINE_WIDTH_THROUGH,
                // Mismatch 类型开关
                OVERLAY_TYPE_DIFF_BLOCK, OVERLAY_TYPE_EXTRA, OVERLAY_TYPE_MISSING,
                OVERLAY_TYPE_WRONG_BLOCK, OVERLAY_TYPE_WRONG_STATE,
                // Mismatch 渲染参数
                RENDER_ERROR_MARKER_CONNECTIONS, RENDER_ERROR_MARKER_SIDES,
                VERIFY_HILIGHT_ALPHA, VERIFIER_ERROR_HILIGHT_MAX_POSITIONS,
                // 验证颜色
                VERIFY_MISSING_COLOR, VERIFY_WRONG_BLOCK_COLOR,
                VERIFY_WRONG_STATE_COLOR, VERIFY_EXTRA_COLOR
        );

        public static final ImmutableList<IHotkey> HOTKEY_LIST = ImmutableList.of(
                ENABLE_RENDERING, ENABLE_GHOST_BLOCKS, ENABLE_SELECTION_RENDERING,
                ENABLE_OVERLAY, ENABLE_OVERLAY_CULLING,
                OVERLAY_REDUCED_INNER_SIDES, OVERLAY_ENABLE_OUTLINES, OVERLAY_ENABLE_SIDES,
                OVERLAY_MODEL_OUTLINE, OVERLAY_MODEL_SIDES, OVERLAY_RENDER_THROUGH,
                OVERLAY_TYPE_DIFF_BLOCK, OVERLAY_TYPE_EXTRA, OVERLAY_TYPE_MISSING,
                OVERLAY_TYPE_WRONG_BLOCK, OVERLAY_TYPE_WRONG_STATE,
                RENDER_BLOCKS_AS_TRANSLUCENT, ENABLE_FAKE_LIGHTING
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
                ConfigUtils.readConfigBase(root, "RenderStyles", RenderStyles.OPTIONS);
                // 旧配置迁移：若 RenderStyles 不存在但 Visuals/Colors/旧 RenderStyles 存在，则迁移
                migrateFromOldCategories(root);
            } else {
                WeGuiMod.LOGGER.error("无法解析配置文件 '{}'", configFile.toAbsolutePath());
            }
        }
    }

    /**
     * 旧配置迁移：从旧 Visuals/Colors/InfoOverlays/旧 RenderStyles 分类迁移到新统一 RenderStyles 分类。
     * 仅在新 RenderStyles 分类不存在时执行（首次升级）。
     */
    private static void migrateFromOldCategories(JsonObject root) {
        boolean hasNewRenderStyles = root.has("RenderStyles") && root.get("RenderStyles").isJsonObject();
        if (hasNewRenderStyles) {
            // 新分类已存在但仍可能缺失新增字段，malilib 会自动用默认值填充，无需迁移
            return;
        }

        // 从旧 Visuals 迁移
        if (root.has("Visuals") && root.get("Visuals").isJsonObject()) {
            JsonObject oldObj = root.getAsJsonObject("Visuals");
            migrateOption(oldObj, "enableRendering", RenderStyles.ENABLE_RENDERING);
            migrateOption(oldObj, "enableGhostBlocks", RenderStyles.ENABLE_GHOST_BLOCKS);
            migrateOption(oldObj, "enableSelectionRendering", RenderStyles.ENABLE_SELECTION_RENDERING);
            migrateOption(oldObj, "enableOverlay", RenderStyles.ENABLE_OVERLAY);
            migrateOption(oldObj, "renderAreaSelectionBoxSides", RenderStyles.RENDER_AREA_SELECTION_BOX_SIDES);
            migrateOption(oldObj, "renderPasteBoxSides", RenderStyles.RENDER_PASTE_BOX_SIDES);
            migrateOption(oldObj, "pasteBoxSideAlpha", RenderStyles.PASTE_BOX_SIDE_ALPHA);
            migrateOption(oldObj, "ghostRenderDistance", RenderStyles.GHOST_RENDER_DISTANCE);
            migrateOption(oldObj, "enableOverlayCulling", RenderStyles.ENABLE_OVERLAY_CULLING);
            migrateOption(oldObj, "renderBlocksAsTranslucent", RenderStyles.RENDER_BLOCKS_AS_TRANSLUCENT);
            migrateOption(oldObj, "overlayReducedInnerSides", RenderStyles.OVERLAY_REDUCED_INNER_SIDES);
            migrateOption(oldObj, "overlayEnableOutlines", RenderStyles.OVERLAY_ENABLE_OUTLINES);
            migrateOption(oldObj, "overlayEnableSides", RenderStyles.OVERLAY_ENABLE_SIDES);
            migrateOption(oldObj, "overlayModelOutline", RenderStyles.OVERLAY_MODEL_OUTLINE);
            migrateOption(oldObj, "overlayModelSides", RenderStyles.OVERLAY_MODEL_SIDES);
            migrateOption(oldObj, "overlayRenderThrough", RenderStyles.OVERLAY_RENDER_THROUGH);
            migrateOption(oldObj, "renderErrorMarkerConnections", RenderStyles.RENDER_ERROR_MARKER_CONNECTIONS);
            migrateOption(oldObj, "renderErrorMarkerSides", RenderStyles.RENDER_ERROR_MARKER_SIDES);
            migrateOption(oldObj, "enableFakeLighting", RenderStyles.ENABLE_FAKE_LIGHTING);
            migrateOption(oldObj, "overlayTypeDiffBlock", RenderStyles.OVERLAY_TYPE_DIFF_BLOCK);
            migrateOption(oldObj, "overlayTypeExtra", RenderStyles.OVERLAY_TYPE_EXTRA);
            migrateOption(oldObj, "overlayTypeMissing", RenderStyles.OVERLAY_TYPE_MISSING);
            migrateOption(oldObj, "overlayTypeWrongBlock", RenderStyles.OVERLAY_TYPE_WRONG_BLOCK);
            migrateOption(oldObj, "overlayTypeWrongState", RenderStyles.OVERLAY_TYPE_WRONG_STATE);
            migrateOption(oldObj, "ghostBlockAlpha", RenderStyles.GHOST_BLOCK_ALPHA);
            migrateOption(oldObj, "overlayOutlineWidth", RenderStyles.OVERLAY_OUTLINE_WIDTH);
            migrateOption(oldObj, "overlayOutlineWidthThrough", RenderStyles.OVERLAY_OUTLINE_WIDTH_THROUGH);
            migrateOption(oldObj, "renderFakeLightingLevel", RenderStyles.RENDER_FAKE_LIGHTING_LEVEL);
        }

        // 从旧 Colors 迁移
        if (root.has("Colors") && root.get("Colors").isJsonObject()) {
            JsonObject oldObj = root.getAsJsonObject("Colors");
            migrateOption(oldObj, "selectionPos1Color", RenderStyles.SELECTION_POS1_COLOR);
            migrateOption(oldObj, "selectionPos2Color", RenderStyles.SELECTION_POS2_COLOR);
            migrateOption(oldObj, "areaSelectionBoxSideColor", RenderStyles.AREA_SELECTION_BOX_SIDE_COLOR);
            migrateOption(oldObj, "boxSideColor", RenderStyles.PASTE_BOX_COLOR);
            migrateOption(oldObj, "blockOutlineColor", RenderStyles.BLOCK_OVERLAY_COLOR);
            migrateOption(oldObj, "verifyMissingColor", RenderStyles.VERIFY_MISSING_COLOR);
            migrateOption(oldObj, "verifyWrongBlockColor", RenderStyles.VERIFY_WRONG_BLOCK_COLOR);
            migrateOption(oldObj, "verifyWrongStateColor", RenderStyles.VERIFY_WRONG_STATE_COLOR);
            migrateOption(oldObj, "verifyExtraColor", RenderStyles.VERIFY_EXTRA_COLOR);
        }

        // 从旧 InfoOverlays 迁移
        if (root.has("InfoOverlays") && root.get("InfoOverlays").isJsonObject()) {
            JsonObject oldObj = root.getAsJsonObject("InfoOverlays");
            migrateOption(oldObj, "verifyHilightAlpha", RenderStyles.VERIFY_HILIGHT_ALPHA);
            migrateOption(oldObj, "verifierErrorHilightMaxPositions", RenderStyles.VERIFIER_ERROR_HILIGHT_MAX_POSITIONS);
        }

        // 从旧 PastePreview 迁移
        if (root.has("PastePreview") && root.get("PastePreview").isJsonObject()) {
            JsonObject oldObj = root.getAsJsonObject("PastePreview");
            migrateOption(oldObj, "ghostBlockAlpha", RenderStyles.GHOST_BLOCK_ALPHA);
            migrateOption(oldObj, "ghostBlockSolid", RenderStyles.RENDER_BLOCKS_AS_TRANSLUCENT, true);
            migrateOption(oldObj, "verifyMissingColor", RenderStyles.VERIFY_MISSING_COLOR);
            migrateOption(oldObj, "verifyWrongBlockColor", RenderStyles.VERIFY_WRONG_BLOCK_COLOR);
            migrateOption(oldObj, "verifyWrongStateColor", RenderStyles.VERIFY_WRONG_STATE_COLOR);
            migrateOption(oldObj, "verifyExtraColor", RenderStyles.VERIFY_EXTRA_COLOR);
            migrateOption(oldObj, "verifyRenderSides", RenderStyles.RENDER_ERROR_MARKER_SIDES);
            migrateOption(oldObj, "verifyHilightAlpha", RenderStyles.VERIFY_HILIGHT_ALPHA);
        }
    }

    /**
     * 从旧 JsonObject 中按名称读取一个配置项的字符串值，写入新配置项。
     * 若 invert=true，则反转布尔语义（用于 GHOST_BLOCK_SOLID → RENDER_BLOCKS_AS_TRANSLUCENT 迁移）。
     */
    private static void migrateOption(JsonObject oldObj, String oldName, IConfigValue newOption) {
        migrateOption(oldObj, oldName, newOption, false);
    }

    private static void migrateOption(JsonObject oldObj, String oldName, IConfigValue newOption, boolean invert) {
        if (!oldObj.has(oldName)) return;
        JsonElement val = oldObj.get(oldName);
        if (val == null || val.isJsonNull()) return;
        String str = val.isJsonPrimitive() ? val.getAsString() : null;
        if (str == null || str.isEmpty()) return;
        if (invert) {
            // 旧 GHOST_BLOCK_SOLID=true → 新 RENDER_BLOCKS_AS_TRANSLUCENT=false，反之亦然
            if ("true".equalsIgnoreCase(str)) {
                newOption.setValueFromString("false");
            } else if ("false".equalsIgnoreCase(str)) {
                newOption.setValueFromString("true");
            }
        } else {
            newOption.setValueFromString(str);
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
            ConfigUtils.writeConfigBase(root, "RenderStyles", RenderStyles.OPTIONS);
            JsonUtils.writeJsonToFileAsPath(root, dir.resolve(CONFIG_FILE_NAME));
        }
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
