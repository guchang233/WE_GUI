package com.sow.wegui.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.sow.wegui.client.CommandSender;
import com.sow.wegui.client.WeGuiClient;
import com.sow.wegui.commands.WeCommands;
import com.sow.wegui.config.Config;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * WE GUI 设置面板：标签页 + 左侧分类 + 右侧滚动内容。
 */
public class SettingsScreen extends BaseScreen {
    private final Screen parent;
    private Tab currentTab = Tab.CONFIG;
    private ConfigCategory currentCategory = ConfigCategory.GENERAL;
    private WeCommands.Category selectedFunctionCategory = WeCommands.Category.GENERAL;

    private static final int CMD_BUTTON_W = 120;
    private static final int CMD_BUTTON_H = 18;
    private static final int CMD_GAP = 4;

    private ScrollPanel categoryPanel;
    private ScrollPanel contentPanel;
    private final List<Button> categoryButtons = new ArrayList<>();
    private KeyButton openPanelKeyButton;

    // 单次 GUI 会话内的滚动位置缓存
    private int configCategoryScrollY = 0;
    private int configContentScrollY = 0;
    private int functionCategoryScrollY = 0;
    private int functionContentScrollY = 0;

    // 配置组件引用
    private Checkbox statusBarCheck;
    private Checkbox pastePreviewCheck;
    private Checkbox selectionBoundsCheck;
    private CycleButton<Config.Anchor> anchorButton;
    private EditBox offsetXBox;
    private EditBox offsetYBox;
    private EditBox line1Box;
    private EditBox line2Box;

    // 颜色与方块描边
    private Checkbox blockOutlineCheck;
    private EditBox selectionBoxColorBox;
    private EditBox selectionPos1ColorBox;
    private EditBox selectionPos2ColorBox;
    private EditBox blockOutlineColorBox;

    // 按键绑定
    private KeyButton toggleAxeModeKeyButton;

    public SettingsScreen(Screen parent) {
        super(Component.literal("WorldEdit GUI"));
        this.parent = parent;
        Config cfg = Config.get();
        try {
            this.currentTab = Tab.valueOf(cfg.getLastSettingsTab());
        } catch (IllegalArgumentException ignored) {}
        try {
            this.currentCategory = ConfigCategory.valueOf(cfg.getLastConfigCategory());
        } catch (IllegalArgumentException ignored) {}
        try {
            this.selectedFunctionCategory = WeCommands.Category.valueOf(cfg.getLastFunctionCategory());
        } catch (IllegalArgumentException ignored) {}
    }

    @Override
    protected void init() {
        super.init();
        rebuild();
    }

    private void rebuild() {
        // 保存当前滚动位置
        if (currentTab == Tab.CONFIG) {
            if (categoryPanel != null) configCategoryScrollY = (int) categoryPanel.getScrollY();
            if (contentPanel != null) configContentScrollY = (int) contentPanel.getScrollY();
        } else {
            if (categoryPanel != null) functionCategoryScrollY = (int) categoryPanel.getScrollY();
            if (contentPanel != null) functionContentScrollY = (int) contentPanel.getScrollY();
        }

        clearWidgets();
        categoryButtons.clear();
        categoryPanel = null;
        contentPanel = null;
        openPanelKeyButton = null;
        toggleAxeModeKeyButton = null;
        statusBarCheck = null;
        pastePreviewCheck = null;
        selectionBoundsCheck = null;
        anchorButton = null;
        offsetXBox = null;
        offsetYBox = null;
        line1Box = null;
        line2Box = null;
        blockOutlineCheck = null;
        selectionBoxColorBox = null;
        selectionPos1ColorBox = null;
        selectionPos2ColorBox = null;
        blockOutlineColorBox = null;

        buildTabs();

        if (currentTab == Tab.CONFIG) {
            buildConfigLayout();
            if (categoryPanel != null) categoryPanel.setScrollY(configCategoryScrollY);
            if (contentPanel != null) contentPanel.setScrollY(configContentScrollY);
        } else {
            buildFeatureLayout();
            if (categoryPanel != null) categoryPanel.setScrollY(functionCategoryScrollY);
            if (contentPanel != null) contentPanel.setScrollY(functionContentScrollY);
        }

        if (currentTab == Tab.CONFIG) {
            addRenderableWidget(Button.builder(Component.literal("保存"), b -> save())
                    .bounds(width / 2 - 75 - 5, height - 28, 70, 20).build());
            addRenderableWidget(Button.builder(Component.literal("返回"), b -> onClose())
                    .bounds(width / 2 + 5, height - 28, 70, 20).build());
        } else {
            addRenderableWidget(Button.builder(Component.literal("返回"), b -> onClose())
                    .bounds(width / 2 - 35, height - 28, 70, 20).build());
        }
    }

    private void buildTabs() {
        String[] names = {"配置", "功能"};
        int tabW = 60;
        int startX = width / 2 - names.length * tabW / 2 - (names.length - 1) * 2;
        for (int i = 0; i < names.length; i++) {
            final Tab tab = Tab.values()[i];
            boolean selected = currentTab == tab;
            Button btn = Button.builder(Component.literal(names[i]), b -> switchTab(tab))
                    .bounds(startX + i * (tabW + 4), 5, tabW, 20)
                    .build();
            btn.active = !selected;
            addRenderableWidget(btn);
        }
    }

    private void switchTab(Tab tab) {
        this.currentTab = tab;
        rebuild();
    }

    private void buildConfigLayout() {
        int margin = 10;
        int leftW = 100;
        int top = 35;
        int bottom = 38;
        int gap = 4;

        int catX = margin;
        int catY = top;
        int catW = leftW;
        int catH = Math.max(60, height - top - bottom);

        int contentX = catX + catW + margin;
        int contentY = top;
        int contentW = Math.max(120, width - contentX - margin);
        int contentH = Math.max(60, height - top - bottom);

        categoryPanel = new ScrollPanel(catX, catY, catW, catH, Component.empty());
        addRenderableWidget(categoryPanel);

        ConfigCategory[] categories = ConfigCategory.values();
        int catBtnH = 22;
        for (int i = 0; i < categories.length; i++) {
            final ConfigCategory cat = categories[i];
            Button btn = Button.builder(Component.literal(cat.displayName), b -> selectCategory(cat))
                    .bounds(0, 0, catW, catBtnH)
                    .build();
            btn.active = currentCategory != cat;
            categoryPanel.addWidget(btn, 0, i * (catBtnH + gap));
            categoryButtons.add(btn);
        }
        categoryPanel.refreshContentHeight();

        contentPanel = new ScrollPanel(contentX, contentY, contentW, contentH, Component.empty());
        addRenderableWidget(contentPanel);

        switch (currentCategory) {
            case GENERAL -> buildGeneralPanel();
            case STATUS_BAR -> buildStatusBarPanel();
            case PASTE_PREVIEW -> buildPastePreviewPanel();
            case KEYBINDS -> buildKeybindsPanel();
        }
    }

    private void selectCategory(ConfigCategory cat) {
        this.currentCategory = cat;
        this.configContentScrollY = 0;
        rebuild();
    }

    private void selectFunctionCategory(WeCommands.Category cat) {
        this.selectedFunctionCategory = cat;
        this.functionContentScrollY = 0;
        rebuild();
    }

    private void buildCommandButtons() {
        List<WeCommands.Command> commands = WeCommands.byCategory(selectedFunctionCategory);
        int cols = Math.max(1, contentPanel.getWidth() / (CMD_BUTTON_W + CMD_GAP));
        int y = 0;
        for (int i = 0; i < commands.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int x = col * (CMD_BUTTON_W + CMD_GAP);
            int yPos = y + row * (CMD_BUTTON_H + CMD_GAP);
            WeCommands.Command cmd = commands.get(i);
            Button btn = Button.builder(Component.literal(cmd.displayName()), b -> onCommand(cmd))
                    .bounds(0, 0, CMD_BUTTON_W, CMD_BUTTON_H)
                    .build();
            btn.setTooltip(Tooltip.create(Component.literal(cmd.description())));
            contentPanel.addWidget(btn, x, yPos);
        }
        contentPanel.refreshContentHeight();
    }

    private void onCommand(WeCommands.Command cmd) {
        if (cmd.usages().size() == 1) {
            WeCommands.Usage usage = cmd.usages().get(0);
            if (usage.params().isEmpty()) {
                CommandSender.send(usage.buildCommand(List.of()));
                onClose();
            } else {
                minecraft.setScreen(new ParamInputScreen(this, cmd, usage));
            }
        } else {
            minecraft.setScreen(new UsageSelectScreen(this, cmd));
        }
    }

    private void buildFeatureLayout() {
        int margin = 10;
        int leftW = 100;
        int top = 35;
        int bottom = 38;
        int gap = 4;

        int catX = margin;
        int catY = top;
        int catW = leftW;
        int catH = Math.max(60, height - top - bottom);

        int contentX = catX + catW + margin;
        int contentY = top;
        int contentW = Math.max(120, width - contentX - margin);
        int contentH = Math.max(60, height - top - bottom);

        categoryPanel = new ScrollPanel(catX, catY, catW, catH, Component.empty());
        addRenderableWidget(categoryPanel);

        // 左侧命令分类
        WeCommands.Category[] categories = WeCommands.Category.values();
        int catBtnH = 22;
        for (int i = 0; i < categories.length; i++) {
            final WeCommands.Category cat = categories[i];
            Button btn = Button.builder(Component.literal(cat.getDisplayName()), b -> selectFunctionCategory(cat))
                    .bounds(0, 0, catW, catBtnH)
                    .build();
            btn.active = selectedFunctionCategory != cat;
            categoryPanel.addWidget(btn, 0, i * (catBtnH + gap));
            categoryButtons.add(btn);
        }
        categoryPanel.refreshContentHeight();

        // 右侧命令按钮网格（可滚动）
        contentPanel = new ScrollPanel(contentX, contentY, contentW, contentH, Component.empty());
        addRenderableWidget(contentPanel);
        buildCommandButtons();
    }

    private void buildGeneralPanel() {
        Config cfg = Config.get();
        int y = 0;
        contentPanel.addLabel("通用设置", 0, y);
        y += 22;
        statusBarCheck = contentPanel.addCheckbox("启用状态栏", cfg.isStatusBarEnabled(), 0, y);
        y += 26;
        pastePreviewCheck = contentPanel.addCheckbox("启用粘贴预览", cfg.isPastePreviewEnabled(), 0, y);
        y += 26;
        selectionBoundsCheck = contentPanel.addCheckbox("显示 copy 选区框", cfg.isSelectionBoundsEnabled(), 0, y);
    }

    private void buildStatusBarPanel() {
        Config cfg = Config.get();
        int y = 0;
        contentPanel.addLabel("状态栏设置", 0, y);
        y += 26;

        anchorButton = CycleButton.<Config.Anchor>builder(a -> Component.literal(switch (a) {
                    case TOP_LEFT -> "左上";
                    case TOP_RIGHT -> "右上";
                    case BOTTOM_LEFT -> "左下";
                    case BOTTOM_RIGHT -> "右下";
                }), cfg.getStatusBarAnchor())
                .withValues(Config.Anchor.values())
                .create(0, y, contentPanel.getWidth(), 20, Component.literal("状态栏位置"), (btn, val) -> {});
        contentPanel.addWidget(anchorButton, 0, y);
        y += 28;

        int half = (contentPanel.getWidth() - 10) / 2;
        contentPanel.addLabel("X 偏移", 0, y);
        offsetXBox = new EditBox(font, 0, y + 12, half, 20, Component.literal("X 偏移"));
        offsetXBox.setValue(String.valueOf(cfg.getStatusBarOffsetX()));
        contentPanel.addWidget(offsetXBox, 0, y + 12);

        contentPanel.addLabel("Y 偏移", half + 10, y);
        offsetYBox = new EditBox(font, half + 10, y + 12, half, 20, Component.literal("Y 偏移"));
        offsetYBox.setValue(String.valueOf(cfg.getStatusBarOffsetY()));
        contentPanel.addWidget(offsetYBox, half + 10, y + 12);
        y += 42;

        contentPanel.addLabel("第一行格式", 0, y);
        y += 12;
        line1Box = new EditBox(font, 0, y, contentPanel.getWidth(), 20, Component.literal("第一行格式"));
        line1Box.setValue(cfg.getStatusBarLine1());
        line1Box.setHint(Component.literal("可用变量: {size} {count} {clipboard}"));
        contentPanel.addWidget(line1Box, 0, y);
        y += 28;

        contentPanel.addLabel("第二行格式", 0, y);
        y += 12;
        line2Box = new EditBox(font, 0, y, contentPanel.getWidth(), 20, Component.literal("第二行格式"));
        line2Box.setValue(cfg.getStatusBarLine2());
        line2Box.setHint(Component.literal("留空则隐藏"));
        contentPanel.addWidget(line2Box, 0, y);
        y += 28;

        contentPanel.addLabel("§7提示：变量说明", 0, y);
        y += 12;
        contentPanel.addLabel("{size} 选区尺寸  {count} 选区方块数", 0, y);
        y += 12;
        contentPanel.addLabel("{clipboard} 剪贴板状态", 0, y);
    }

    private void buildPastePreviewPanel() {
        Config cfg = Config.get();
        int y = 0;
        contentPanel.addLabel("粘贴预览设置", 0, y);
        y += 22;
        pastePreviewCheck = contentPanel.addCheckbox("启用粘贴预览", cfg.isPastePreviewEnabled(), 0, y);
        y += 26;
        selectionBoundsCheck = contentPanel.addCheckbox("显示 copy 选区框", cfg.isSelectionBoundsEnabled(), 0, y);
        y += 26;
        blockOutlineCheck = contentPanel.addCheckbox("为非空气方块添加单独边框", cfg.isBlockOutlineEnabled(), 0, y);
        y += 32;

        contentPanel.addLabel("选区框颜色", 0, y);
        selectionBoxColorBox = addColorRow(y, cfg.getSelectionBoxColor(), Config.DEFAULT_SELECTION_BOX_COLOR);
        y += 42;

        contentPanel.addLabel("第一选点 (pos1) 颜色", 0, y);
        selectionPos1ColorBox = addColorRow(y, cfg.getSelectionPos1Color(), Config.DEFAULT_SELECTION_POS1_COLOR);
        y += 42;

        contentPanel.addLabel("第二选点 (pos2) 颜色", 0, y);
        selectionPos2ColorBox = addColorRow(y, cfg.getSelectionPos2Color(), Config.DEFAULT_SELECTION_POS2_COLOR);
        y += 42;

        contentPanel.addLabel("方块边框颜色", 0, y);
        blockOutlineColorBox = addColorRow(y, cfg.getBlockOutlineColor(), Config.DEFAULT_BLOCK_OUTLINE_COLOR);
        y += 42;

        contentPanel.addLabel("§7颜色格式为 8 位 ARGB 十六进制，例如 FFFFD200。", 0, y);
        y += 12;
        contentPanel.addLabel("§7执行 //flip、//rotate 后会自动刷新预览。", 0, y);
    }

    private EditBox addColorRow(int y, int currentColor, int defaultColor) {
        int boxW = 90;
        int btnW = 44;
        int gap = 6;
        int totalW = boxW + gap + btnW;
        int startX = Math.max(0, contentPanel.getWidth() - totalW);

        EditBox box = new EditBox(font, 0, 0, boxW, 18, Component.literal("颜色"));
        box.setValue(String.format("%08X", currentColor));
        box.setMaxLength(8);
        contentPanel.addWidget(box, startX, y);

        Button reset = Button.builder(Component.literal("重置"), b -> box.setValue(String.format("%08X", defaultColor)))
                .bounds(0, 0, btnW, 18)
                .build();
        contentPanel.addWidget(reset, startX + boxW + gap, y);
        return box;
    }

    private void buildKeybindsPanel() {
        Config cfg = Config.get();
        int y = 0;
        contentPanel.addLabel("按键绑定", 0, y);
        y += 26;

        contentPanel.addLabel("打开 WE GUI 面板", 0, y);
        openPanelKeyButton = new KeyButton(0, y + 12, contentPanel.getWidth(), 20,
                cfg.getKeyOpenPanel(), k -> {});
        contentPanel.addWidget(openPanelKeyButton, 0, y + 12);
        y += 42;

        contentPanel.addLabel("切换小木斧模式（正常 / 编辑选区）", 0, y);
        toggleAxeModeKeyButton = new KeyButton(0, y + 12, contentPanel.getWidth(), 20,
                cfg.getKeyToggleAxeMode(), k -> {});
        contentPanel.addWidget(toggleAxeModeKeyButton, 0, y + 12);
        y += 42;

        contentPanel.addLabel("§7点击按钮后按下任意键即可修改绑定，按 Esc 取消。", 0, y);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            return super.keyPressed(event);
        }
        if (openPanelKeyButton != null && openPanelKeyButton.isWaiting()) {
            openPanelKeyButton.setKeyCode(event.key());
            openPanelKeyButton.setWaiting(false);
            return true;
        }
        if (toggleAxeModeKeyButton != null && toggleAxeModeKeyButton.isWaiting()) {
            toggleAxeModeKeyButton.setKeyCode(event.key());
            toggleAxeModeKeyButton.setWaiting(false);
            return true;
        }
        return super.keyPressed(event);
    }

    private void save() {
        Config cfg = Config.get();
        if (statusBarCheck != null) cfg.setStatusBarEnabled(statusBarCheck.selected());
        if (pastePreviewCheck != null) cfg.setPastePreviewEnabled(pastePreviewCheck.selected());
        if (selectionBoundsCheck != null) cfg.setSelectionBoundsEnabled(selectionBoundsCheck.selected());
        if (blockOutlineCheck != null) cfg.setBlockOutlineEnabled(blockOutlineCheck.selected());
        if (anchorButton != null) cfg.setStatusBarAnchor(anchorButton.getValue());
        if (offsetXBox != null) cfg.setStatusBarOffsetX(parseInt(offsetXBox.getValue(), cfg.getStatusBarOffsetX()));
        if (offsetYBox != null) cfg.setStatusBarOffsetY(parseInt(offsetYBox.getValue(), cfg.getStatusBarOffsetY()));
        if (line1Box != null) cfg.setStatusBarLine1(line1Box.getValue());
        if (line2Box != null) cfg.setStatusBarLine2(line2Box.getValue());
        if (selectionBoxColorBox != null) cfg.setSelectionBoxColor(parseHexColor(selectionBoxColorBox.getValue(), cfg.getSelectionBoxColor()));
        if (selectionPos1ColorBox != null) cfg.setSelectionPos1Color(parseHexColor(selectionPos1ColorBox.getValue(), cfg.getSelectionPos1Color()));
        if (selectionPos2ColorBox != null) cfg.setSelectionPos2Color(parseHexColor(selectionPos2ColorBox.getValue(), cfg.getSelectionPos2Color()));
        if (blockOutlineColorBox != null) cfg.setBlockOutlineColor(parseHexColor(blockOutlineColorBox.getValue(), cfg.getBlockOutlineColor()));
        if (openPanelKeyButton != null) WeGuiClient.updateKeyOpenPanel(openPanelKeyButton.getKeyCode());
        if (toggleAxeModeKeyButton != null) WeGuiClient.updateKeyToggleAxeMode(toggleAxeModeKeyButton.getKeyCode());
        Config.save();
        onClose();
    }

    private int parseInt(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private int parseHexColor(String s, int fallback) {
        try {
            String hex = s.trim().replace("#", "");
            if (hex.length() == 6) hex = "FF" + hex;
            return (int) Long.parseLong(hex, 16);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public void onClose() {
        Config cfg = Config.get();
        cfg.setLastSettingsTab(currentTab.name());
        cfg.setLastConfigCategory(currentCategory.name());
        cfg.setLastFunctionCategory(selectedFunctionCategory.name());
        Config.save();
        minecraft.setScreen(parent);
    }

    private void returnToParent() {
        if (parent != null) {
            minecraft.setScreen(parent);
        } else {
            minecraft.setScreen(null);
        }
    }

    private enum Tab {
        CONFIG, FEATURE
    }

    private enum ConfigCategory {
        GENERAL("通用"),
        STATUS_BAR("状态栏"),
        PASTE_PREVIEW("粘贴预览"),
        KEYBINDS("按键");

        final String displayName;

        ConfigCategory(String displayName) {
            this.displayName = displayName;
        }
    }

    /**
     * 可滚动面板，支持放置任意 AbstractWidget 子组件。
     */
    private final class ScrollPanel extends AbstractWidget {
        private static final int SCROLLBAR_WIDTH = 6;
        private final List<WidgetEntry> children = new ArrayList<>();
        private double scrollY;
        private int contentHeight;
        private boolean scrolling;

        ScrollPanel(int x, int y, int width, int height, Component title) {
            super(x, y, width, height, title);
        }

        void addWidget(AbstractWidget widget, int relativeX, int relativeY) {
            widget.setX(getX() + relativeX);
            widget.setY(getY() + relativeY);
            children.add(new WidgetEntry(widget, relativeX, relativeY));
            contentHeight = Math.max(contentHeight, relativeY + widget.getHeight() + 4);
        }

        Checkbox addCheckbox(String label, boolean selected, int x, int y) {
            Checkbox cb = Checkbox.builder(Component.literal(label), font).pos(getX() + x, getY() + y).selected(selected).build();
            addWidget(cb, x, y);
            return cb;
        }

        void addLabel(String text, int x, int y) {
            // Label 不需要交互，这里用不可交互的占位 widget 来参与布局与滚动
            AbstractWidget label = new AbstractWidget(getX() + x, getY() + y, font.width(text), 10, Component.literal(text)) {
                @Override
                protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                    graphics.drawString(font, getMessage(), getX(), getY(), 0xFFFFFFFF);
                }

                @Override
                protected void updateWidgetNarration(NarrationElementOutput output) {
                }
            };
            addWidget(label, x, y);
        }

        void refreshContentHeight() {
            contentHeight = 0;
            for (WidgetEntry entry : children) {
                contentHeight = Math.max(contentHeight, entry.relY + entry.widget.getHeight() + 4);
            }
            setScrollY(scrollY);
        }

        private int getMaxScrollY() {
            return Math.max(0, contentHeight - height);
        }

        double getScrollY() {
            return scrollY;
        }

        void setScrollY(double value) {
            this.scrollY = Mth.clamp(value, 0, getMaxScrollY());
            for (WidgetEntry entry : children) {
                entry.widget.setY(getY() + entry.relY - (int) scrollY);
            }
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            if (isMouseOver(mouseX, mouseY)) {
                setScrollY(this.scrollY - scrollY * 16);
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            double mouseX = event.x();
            double mouseY = event.y();
            if (!isMouseOver(mouseX, mouseY)) return false;
            if (scrollbarVisible() && isOverScrollbar(mouseX, mouseY)) {
                scrolling = true;
                return true;
            }
            for (WidgetEntry entry : children) {
                if (entry.widget.visible && isInsidePanel(entry.widget) && entry.widget.mouseClicked(event, doubleClick)) {
                    return true;
                }
            }
            return true;
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
            scrolling = false;
            boolean handled = false;
            for (WidgetEntry entry : children) {
                if (entry.widget.visible && isInsidePanel(entry.widget)) {
                    if (entry.widget.mouseReleased(event)) handled = true;
                }
            }
            return handled;
        }

        @Override
        public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
            if (scrolling && scrollbarVisible()) {
                int trackHeight = height - getScrollbarThumbHeight();
                double ratio = dragY / trackHeight;
                setScrollY(scrollY + ratio * getMaxScrollY());
                return true;
            }
            for (WidgetEntry entry : children) {
                if (entry.widget.visible && isInsidePanel(entry.widget)) {
                    if (entry.widget.mouseDragged(event, dragX, dragY)) return true;
                }
            }
            return false;
        }

        @Override
        public boolean charTyped(CharacterEvent event) {
            for (WidgetEntry entry : children) {
                if (entry.widget.charTyped(event)) return true;
            }
            return false;
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            for (WidgetEntry entry : children) {
                if (entry.widget.keyPressed(event)) return true;
            }
            return false;
        }

        @Override
        public boolean keyReleased(KeyEvent event) {
            for (WidgetEntry entry : children) {
                if (entry.widget.keyReleased(event)) return true;
            }
            return false;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            // 背景
            graphics.fill(getX(), getY(), getX() + width, getY() + height, 0x66000000);

            // 裁剪到面板区域
            graphics.enableScissor(getX(), getY(), getX() + width, getY() + height);
            for (WidgetEntry entry : children) {
                if (isInsidePanel(entry.widget)) {
                    entry.widget.render(graphics, mouseX, mouseY, partialTick);
                }
            }
            graphics.disableScissor();

            // 滚动条
            if (scrollbarVisible()) {
                renderScrollbar(graphics);
            }
        }

        private void renderScrollbar(GuiGraphics graphics) {
            int x = getX() + width - SCROLLBAR_WIDTH - 1;
            int y = getY();
            int h = height;
            graphics.fill(x, y, x + SCROLLBAR_WIDTH, y + h, 0x33000000);
            int thumbH = getScrollbarThumbHeight();
            int thumbY = getY() + (int) (scrollY / Math.max(1, getMaxScrollY()) * (h - thumbH));
            graphics.fill(x, thumbY, x + SCROLLBAR_WIDTH, thumbY + thumbH, 0xFFAAAAAA);
        }

        private int getScrollbarThumbHeight() {
            return Math.max(20, height * height / Math.max(height, contentHeight));
        }

        private boolean scrollbarVisible() {
            return contentHeight > height;
        }

        private boolean isOverScrollbar(double mouseX, double mouseY) {
            int x = getX() + width - SCROLLBAR_WIDTH - 1;
            return mouseX >= x && mouseX <= x + SCROLLBAR_WIDTH && mouseY >= getY() && mouseY <= getY() + height;
        }

        private boolean isInsidePanel(AbstractWidget widget) {
            return widget.getY() + widget.getHeight() > getY() && widget.getY() < getY() + height;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
        }

        private record WidgetEntry(AbstractWidget widget, int relX, int relY) {
        }
    }

    /**
     * 按键绑定按钮，点击后等待用户按下新按键。
     */
    private final class KeyButton extends AbstractWidget {
        private int keyCode;
        private boolean waiting;
        private final Consumer<Integer> onChange;

        KeyButton(int x, int y, int width, int height, int keyCode, Consumer<Integer> onChange) {
            super(x, y, width, height, Component.literal(InputConstants.Type.KEYSYM.getOrCreate(keyCode).getDisplayName().getString()));
            this.keyCode = keyCode;
            this.onChange = onChange;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int color = waiting ? 0xFF55AA55 : (isHovered() ? 0xFFAAAAAA : 0xFF777777);
            graphics.fill(getX(), getY(), getX() + width, getY() + height, color);
            graphics.renderOutline(getX(), getY(), width, height, 0xFFFFFFFF);
            String text = waiting ? "按下按键..." : getMessage().getString();
            int tw = font.width(text);
            graphics.drawString(font, text, getX() + (width - tw) / 2, getY() + (height - 8) / 2, 0xFFFFFFFF);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (isMouseOver(event.x(), event.y())) {
                waiting = true;
                return true;
            }
            return false;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
        }

        int getKeyCode() {
            return keyCode;
        }

        void setKeyCode(int keyCode) {
            this.keyCode = keyCode;
            setMessage(Component.literal(InputConstants.Type.KEYSYM.getOrCreate(keyCode).getDisplayName().getString()));
            onChange.accept(keyCode);
        }

        boolean isWaiting() {
            return waiting;
        }

        void setWaiting(boolean waiting) {
            this.waiting = waiting;
        }
    }
}
