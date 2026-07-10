package com.sow.wegui.client.screen;

import com.sow.wegui.client.CommandSender;
import com.sow.wegui.commands.WeCommands;
import com.sow.wegui.commands.WeCommands.Category;
import com.sow.wegui.commands.WeCommands.Command;
import com.sow.wegui.config.Configs;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetDropDownList;
import fi.dy.masa.malilib.interfaces.IStringRetriever;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于 malilib 的 WorldEdit 命令主面板。
 * 左侧分类栏，右侧两列命令网格。
 */
public class WeCommandScreen extends GuiListBase<CommandRow, WidgetCommandEntry, WidgetListCommands> implements ISelectionListener<CommandRow> {
    private static final int SIDE_BAR_WIDTH = 120;
    private static final int TOP_BAR_HEIGHT = 38;
    private static final int BOTTOM_BAR_HEIGHT = 32;
    private static final int PADDING = 10;

    private CategoryEntry selectedCategory = new CategoryEntry(null, Component.translatable("wegui.command.category.all").getString());
    private WidgetDropDownList<CategoryEntry> categoryDropdown;

    public WeCommandScreen() {
        super(PADDING + SIDE_BAR_WIDTH, TOP_BAR_HEIGHT);
        this.setTitle(StringUtils.translate("wegui.command.title"));
    }

    public Category getSelectedCategory() {
        return selectedCategory.category();
    }

    @Override
    public void onSelectionChange(CommandRow entry) {
        // 命令通过内部按钮触发
    }

    @Override
    protected ISelectionListener<CommandRow> getSelectionListener() {
        return this;
    }

    @Override
    public void initGui() {
        super.initGui();
        addCategorySidebar();
        addBottomButtons();
    }

    private void addCategorySidebar() {
        int x = PADDING;
        int y = TOP_BAR_HEIGHT;
        int w = SIDE_BAR_WIDTH - 10;

        this.addLabel(x, y, w, 12, 0xFFFFFFFF, Component.translatable("wegui.command.category_label").getString());
        y += 14;

        List<CategoryEntry> entries = new ArrayList<>();
        entries.add(new CategoryEntry(null, Component.translatable("wegui.command.category.all").getString()));
        for (Category category : Category.values()) {
            if (WeCommands.byCategory(category).isEmpty()) continue;
            entries.add(new CategoryEntry(category, Component.translatable(category.getTranslationKey()).getString()));
        }

        IStringRetriever<CategoryEntry> retriever = CategoryEntry::displayName;
        categoryDropdown = new WidgetDropDownList<>(x, y, w, 18, 160, 12, entries, retriever);
        categoryDropdown.setSelectedEntry(selectedCategory);
        this.addWidget(categoryDropdown);
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

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        if (categoryDropdown != null) {
            CategoryEntry current = categoryDropdown.getSelectedEntry();
            if (current != null && !current.equals(selectedCategory)) {
                this.selectedCategory = current;
                this.getListWidget().refreshEntries();
                this.getListWidget().resetScrollbarPosition();
            }
        }
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
}
