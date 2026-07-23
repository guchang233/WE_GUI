package com.sow.wegui.client.screen;

import com.sow.wegui.client.CommandHistory;
import com.sow.wegui.client.CommandSender;
import com.sow.wegui.client.util.CommandMatcher;
import com.sow.wegui.commands.WeCommands;
import com.sow.wegui.commands.WeCommands.Category;
import com.sow.wegui.commands.WeCommands.Command;
import com.sow.wegui.config.Configs;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 基于 malilib 的 WorldEdit 命令主面板。
 * 左侧分类/收藏/最近使用栏，顶部搜索框，主区域命令卡片列表。
 */
public class WeCommandScreen extends GuiListBase<CommandRow, WidgetCommandEntry, WidgetListCommands> implements ISelectionListener<CommandRow> {
    private static final int SIDE_BAR_WIDTH = 120;
    private static final int TOP_BAR_HEIGHT = 42;
    private static final int BOTTOM_BAR_HEIGHT = 32;
    private static final int PADDING = 10;
    private static final int CATEGORY_ENTRY_HEIGHT = 24;
    private static final int CATEGORY_SCROLLBAR_WIDTH = 6;

    private FilterEntry selectedFilter = new FilterEntry(FilterMode.ALL, null, StringUtils.translate("wegui.command.category.all"), 0);
    private GuiTextFieldGeneric searchField;
    private WidgetListCommands listWidget;
    private final List<NoScrollButton> categoryButtons = new ArrayList<>();
    private int categoryScrollOffset = 0;
    private int categoryMaxScroll = 0;
    private int categoryEntryCount = 0;
    private boolean scrollbarDragging = false;
    private double scrollbarDragOffset = 0;

    public WeCommandScreen() {
        super(PADDING + SIDE_BAR_WIDTH, TOP_BAR_HEIGHT);
        this.setTitle(StringUtils.translate("wegui.command.title"));
    }

    public FilterEntry getSelectedFilter() {
        return selectedFilter;
    }

    public FilterMode getSelectedFilterMode() {
        return selectedFilter.mode();
    }

    public Category getSelectedCategory() {
        return selectedFilter.mode() == FilterMode.CATEGORY ? selectedFilter.category() : null;
    }

    public String getSearchQuery() {
        return searchField == null ? "" : searchField.getValue().trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public void onSelectionChange(CommandRow entry) {
        // 命令通过卡片内部按钮触发
    }

    @Override
    protected ISelectionListener<CommandRow> getSelectionListener() {
        return this;
    }

    @Override
    public void initGui() {
        super.initGui();
        addCategorySidebar();
        addSearchBar();
        addBottomButtons();
        this.listWidget = this.getListWidget();
    }

    private void addSearchBar() {
        int x = PADDING + SIDE_BAR_WIDTH;
        int y = 12;
        int w = this.width - x - PADDING;
        searchField = new GuiTextFieldGeneric(x, y, w, 18, this.font);
        searchField.setMaxLength(128);
        this.addTextField(searchField, (textField) -> {
            refreshList();
            updateCategoryButtons();
            return true;
        });
    }

    private void addCategorySidebar() {
        int x = PADDING;
        int w = SIDE_BAR_WIDTH - 14 - CATEGORY_SCROLLBAR_WIDTH;

        categoryButtons.clear();
        List<FilterEntry> entries = buildFilterEntries();
        this.categoryEntryCount = entries.size();
        for (int i = 0; i < entries.size(); i++) {
            FilterEntry entry = entries.get(i);
            NoScrollButton btn = new NoScrollButton(x, TOP_BAR_HEIGHT + i * CATEGORY_ENTRY_HEIGHT, w, 20, entry.displayName() + " (" + entry.count() + ")");
            btn.setEnabled(!entry.equals(selectedFilter));
            categoryButtons.add(btn);
            this.addButton(btn, (b, mouseButton) -> {
                selectedFilter = entry;
                updateCategoryButtons();
                refreshList();
            });
        }
        updateCategoryButtonPositions();
    }

    private void updateCategoryButtons() {
        List<FilterEntry> entries = buildFilterEntries();
        this.categoryEntryCount = entries.size();
        int selectedIndex = -1;
        for (int i = 0; i < categoryButtons.size(); i++) {
            NoScrollButton btn = categoryButtons.get(i);
            if (i >= entries.size()) {
                btn.setVisiblePublic(false);
                continue;
            }
            FilterEntry entry = entries.get(i);
            btn.setDisplayString(entry.displayName() + " (" + entry.count() + ")");
            btn.setEnabled(!entry.equals(selectedFilter));
            if (entry.equals(selectedFilter)) {
                selectedIndex = i;
            }
        }
        updateCategoryButtonPositions();
        if (selectedIndex >= 0) {
            ensureCategoryVisible(selectedIndex);
        }
    }

    private void updateCategoryButtonPositions() {
        int visibleTop = TOP_BAR_HEIGHT;
        int visibleBottom = this.height - BOTTOM_BAR_HEIGHT;
        int availableHeight = visibleBottom - visibleTop;
        int totalHeight = this.categoryEntryCount * CATEGORY_ENTRY_HEIGHT;
        int rawMaxScroll = Math.max(0, totalHeight - availableHeight);

        categoryMaxScroll = roundUpToMultiple(rawMaxScroll, CATEGORY_ENTRY_HEIGHT);
        categoryScrollOffset = roundDownToMultiple(
                Math.max(0, Math.min(categoryScrollOffset, categoryMaxScroll)),
                CATEGORY_ENTRY_HEIGHT);

        for (int i = 0; i < categoryButtons.size(); i++) {
            NoScrollButton btn = categoryButtons.get(i);
            int y = TOP_BAR_HEIGHT + i * CATEGORY_ENTRY_HEIGHT - categoryScrollOffset;
            btn.setY(y);
            btn.setVisiblePublic(y >= visibleTop && y + btn.getHeight() <= visibleBottom);
        }
    }

    private static int roundUpToMultiple(int value, int multiple) {
        if (value <= 0) return 0;
        return ((value + multiple - 1) / multiple) * multiple;
    }

    private static int roundDownToMultiple(int value, int multiple) {
        if (value <= 0) return 0;
        return (value / multiple) * multiple;
    }

    private void ensureCategoryVisible(int index) {
        updateCategoryButtonPositions();
        if (categoryMaxScroll <= 0) {
            categoryScrollOffset = 0;
            updateCategoryButtonPositions();
            return;
        }

        int visibleTop = TOP_BAR_HEIGHT;
        int visibleBottom = this.height - BOTTOM_BAR_HEIGHT;
        int availableHeight = visibleBottom - visibleTop;

        int entryTop = index * CATEGORY_ENTRY_HEIGHT;
        int entryBottom = entryTop + CATEGORY_ENTRY_HEIGHT;

        if (entryTop < categoryScrollOffset) {
            categoryScrollOffset = roundDownToMultiple(entryTop, CATEGORY_ENTRY_HEIGHT);
        } else if (entryBottom > categoryScrollOffset + availableHeight) {
            int needed = entryBottom - availableHeight;
            categoryScrollOffset = roundUpToMultiple(needed, CATEGORY_ENTRY_HEIGHT);
        }
        categoryScrollOffset = Math.max(0, Math.min(categoryScrollOffset, categoryMaxScroll));
        updateCategoryButtonPositions();
    }

    private List<FilterEntry> buildFilterEntries() {
        List<FilterEntry> entries = new ArrayList<>();
        WeCommands.init();

        String query = getSearchQuery();

        // 将收藏和最近使用放到顶部
        entries.add(new FilterEntry(FilterMode.FAVORITES, null,
                StringUtils.translate("wegui.command.favorites"), CommandHistory.getFavorites().size()));
        entries.add(new FilterEntry(FilterMode.RECENT, null,
                StringUtils.translate("wegui.command.recent"), CommandHistory.getRecent().size()));

        // 单次遍历同时统计 ALL 与各 CATEGORY 的匹配数，避免 N + N 次匹配
        int allCount = 0;
        java.util.Map<Category, int[]> categoryCounts = new java.util.EnumMap<>(Category.class);
        for (Command cmd : WeCommands.all()) {
            if (CommandMatcher.matches(cmd, query)) {
                allCount++;
                categoryCounts.computeIfAbsent(cmd.category(), c -> new int[1])[0]++;
            }
        }
        entries.add(new FilterEntry(FilterMode.ALL, null, StringUtils.translate("wegui.command.category.all"), allCount));

        for (Category category : Category.values()) {
            int count = categoryCounts.containsKey(category) ? categoryCounts.get(category)[0] : 0;
            if (WeCommands.byCategory(category).isEmpty()) continue;
            entries.add(new FilterEntry(FilterMode.CATEGORY, category,
                    Component.translatable(category.getTranslationKey()).getString(), count));
        }

        return entries;
    }

    private void addBottomButtons() {
        ButtonGeneric back = new ButtonGeneric(PADDING, this.height - 26, 70, 20, StringUtils.translate("wegui.command.back"));
        this.addButton(back, (btn, mouseButton) -> this.mc.setScreen(null));

        ButtonGeneric bindWand = new ButtonGeneric(this.width - 180, this.height - 26, 90, 20, StringUtils.translate("wegui.command.bind_wand"));
        this.addButton(bindWand, (btn, mouseButton) -> bindHeldItemAsWand());

        ButtonGeneric settings = new ButtonGeneric(this.width - 80, this.height - 26, 70, 20, StringUtils.translate("wegui.command.settings"));
        this.addButton(settings, (btn, mouseButton) -> this.mc.setScreen(new WeGuiConfigs().setParent(this)));
    }

    private void bindHeldItemAsWand() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack held = mc.player.getMainHandItem();
        String itemId = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
        Configs.Generic.WAND_ITEM.setValueFromString(itemId);
        CommandSender.send("/tool selwand");
    }

    public void refreshList() {
        if (listWidget != null) {
            listWidget.refreshEntries();
            listWidget.resetScrollbarPosition();
        }
    }

    /**
     * 刷新列表内容但不重置滚动位置（用于收藏切换等不需要跳转的场景）。
     */
    public void refreshListKeepScroll() {
        if (listWidget != null) {
            listWidget.refreshEntries();
        }
    }

    public void onFavoriteChanged() {
        updateCategoryButtons();
        refreshListKeepScroll();
    }

    @Override
    public void drawContents(GuiGraphics ctx, int mouseX, int mouseY, float partialTick) {
        this.drawSearchBackground(ctx);
        this.drawSearchHint(ctx);
        if (this.getListWidget() != null) {
            super.drawContents(ctx, mouseX, mouseY, partialTick);
        }
        this.drawCategoryScrollbar(ctx);
    }

    @Override
    public void drawScreenBackground(GuiGraphics ctx, int mouseX, int mouseY) {
        super.drawScreenBackground(ctx, mouseX, mouseY);
        this.drawCategoryBackground(ctx);
    }

    private void drawSearchBackground(GuiGraphics ctx) {
        if (searchField == null) return;
        int x = PADDING + SIDE_BAR_WIDTH;
        int y = 12;
        int w = this.width - x - PADDING;
        ctx.fill(x - 1, y - 1, x + w + 1, y + 19, 0xFF000000);
    }

    private void drawSearchHint(GuiGraphics ctx) {
        if (searchField == null) return;
        if (!searchField.getValue().isEmpty() || searchField.isFocused()) return;
        int x = PADDING + SIDE_BAR_WIDTH + 4;
        int y = 12 + (18 - this.font.lineHeight) / 2 + 1;
        ctx.drawString(this.font, StringUtils.translate("wegui.command.search_hint"), x, y, 0xFF888888, false);
    }

    private void drawCategoryBackground(GuiGraphics ctx) {
        int x = PADDING;
        int y = TOP_BAR_HEIGHT;
        int width = SIDE_BAR_WIDTH - 10;
        int height = this.height - TOP_BAR_HEIGHT - BOTTOM_BAR_HEIGHT;
        ctx.fill(x, y, x + width, y + height, 0x90000000);
    }

    private void drawCategoryScrollbar(GuiGraphics ctx) {
        if (categoryMaxScroll <= 0) {
            return;
        }

        int visibleTop = TOP_BAR_HEIGHT;
        int visibleBottom = this.height - BOTTOM_BAR_HEIGHT;
        int availableHeight = visibleBottom - visibleTop;
        int totalHeight = categoryButtons.size() * CATEGORY_ENTRY_HEIGHT;

        int trackX = PADDING + SIDE_BAR_WIDTH - CATEGORY_SCROLLBAR_WIDTH - 2;
        int trackY = visibleTop;

        ctx.fill(trackX, trackY, trackX + CATEGORY_SCROLLBAR_WIDTH, trackY + availableHeight, 0xFF333333);

        int thumbH = Math.max(20, availableHeight * availableHeight / totalHeight);
        int thumbY = trackY + categoryScrollOffset * (availableHeight - thumbH) / categoryMaxScroll;
        ctx.fill(trackX, thumbY, trackX + CATEGORY_SCROLLBAR_WIDTH, thumbY + thumbH, 0xFF888888);
    }

    /**
     * 返回当前 scrollbar thumb 的 Y 坐标与高度。
     * 若 categoryMaxScroll == 0 或不可见，返回 null。
     */
    private int[] getScrollbarThumbGeometry() {
        if (categoryMaxScroll <= 0) return null;
        int visibleTop = TOP_BAR_HEIGHT;
        int visibleBottom = this.height - BOTTOM_BAR_HEIGHT;
        int availableHeight = visibleBottom - visibleTop;
        int totalHeight = categoryButtons.size() * CATEGORY_ENTRY_HEIGHT;
        int thumbH = Math.max(20, availableHeight * availableHeight / totalHeight);
        int trackY = visibleTop;
        int thumbY = trackY + categoryScrollOffset * (availableHeight - thumbH) / categoryMaxScroll;
        return new int[]{thumbY, thumbH, trackY, availableHeight};
    }

    private boolean isInScrollbarTrack(double mouseX, double mouseY) {
        if (categoryMaxScroll <= 0) return false;
        int visibleTop = TOP_BAR_HEIGHT;
        int visibleBottom = this.height - BOTTOM_BAR_HEIGHT;
        int trackX = PADDING + SIDE_BAR_WIDTH - CATEGORY_SCROLLBAR_WIDTH - 2;
        return mouseX >= trackX && mouseX < trackX + CATEGORY_SCROLLBAR_WIDTH &&
               mouseY >= visibleTop && mouseY < visibleBottom;
    }

    @Override
    public boolean onMouseScrolled(int mouseX, int mouseY, double deltaX, double deltaY) {
        int visibleTop = TOP_BAR_HEIGHT;
        int visibleBottom = this.height - BOTTOM_BAR_HEIGHT;
        if (mouseX >= PADDING && mouseX < PADDING + SIDE_BAR_WIDTH &&
            mouseY >= visibleTop && mouseY < visibleBottom) {
            categoryScrollOffset -= (int) deltaY * CATEGORY_ENTRY_HEIGHT;
            updateCategoryButtonPositions();
            return true;
        }
        return super.onMouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean onMouseClicked(int mouseX, int mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && isInScrollbarTrack(mouseX, mouseY)) {
            int[] geo = getScrollbarThumbGeometry();
            if (geo == null) {
                return super.onMouseClicked(mouseX, mouseY, button);
            }
            int thumbY = geo[0];
            int thumbH = geo[1];
            int trackY = geo[2];
            int availableHeight = geo[3];
            if (mouseY >= thumbY && mouseY < thumbY + thumbH) {
                // 点击落在 thumb 上：记录偏移
                scrollbarDragOffset = mouseY - thumbY;
            } else {
                // 点击落在 track 上：跳转 thumb 使其中心对齐 mouseY
                scrollbarDragOffset = thumbH / 2.0;
                updateScrollFromThumbY(mouseY - scrollbarDragOffset, thumbH, trackY, availableHeight);
            }
            scrollbarDragging = true;
            return true;
        }
        return super.onMouseClicked(mouseX, mouseY, button);
    }

    private void updateScrollFromThumbY(double newThumbY, int thumbH, int trackY, int availableHeight) {
        if (categoryMaxScroll <= 0) return;
        int movable = availableHeight - thumbH;
        if (movable <= 0) return;
        double relativeY = newThumbY - trackY;
        relativeY = Math.max(0, Math.min(relativeY, movable));
        int newOffset = (int) Math.round(relativeY * categoryMaxScroll / movable);
        newOffset = roundDownToMultiple(Math.max(0, Math.min(newOffset, categoryMaxScroll)), CATEGORY_ENTRY_HEIGHT);
        categoryScrollOffset = newOffset;
        updateCategoryButtonPositions();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrollbarDragging) {
            int[] geo = getScrollbarThumbGeometry();
            if (geo == null) return true;
            int thumbH = geo[1];
            int trackY = geo[2];
            int availableHeight = geo[3];
            updateScrollFromThumbY(mouseY - scrollbarDragOffset, thumbH, trackY, availableHeight);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean onMouseReleased(int mouseX, int mouseY, int button) {
        if (scrollbarDragging) {
            scrollbarDragging = false;
            return true;
        }
        return super.onMouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected WidgetListCommands createListWidget(int x, int y) {
        return new WidgetListCommands(x, y, getBrowserWidth(), getBrowserHeight(), this);
    }

    @Override
    protected int getBrowserWidth() {
        return this.width - PADDING - SIDE_BAR_WIDTH - 14;
    }

    @Override
    protected int getBrowserHeight() {
        return this.height - TOP_BAR_HEIGHT - BOTTOM_BAR_HEIGHT - 10;
    }

    @Override
    protected int getListX() {
        return PADDING + SIDE_BAR_WIDTH;
    }

    @Override
    protected int getListY() {
        return TOP_BAR_HEIGHT;
    }

    public record FilterEntry(FilterMode mode, Category category, String displayName, int count) {
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof FilterEntry other)) return false;
            return mode == other.mode && category == other.category;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(mode, category);
        }
    }

    public enum FilterMode {
        ALL,
        CATEGORY,
        FAVORITES,
        RECENT
    }
}
