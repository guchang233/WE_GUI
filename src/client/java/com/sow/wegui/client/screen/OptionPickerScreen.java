package com.sow.wegui.client.screen;

import com.sow.wegui.commands.WeCommands;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 通用可搜索选项选择器，用于群系、实体类型、玩家、文件名等长列表。
 */
public class OptionPickerScreen extends GuiBase {
    private static final int MARGIN_X = 20;
    private static final int TOP = 48;
    private static final int ROW_HEIGHT = 22;

    private final Screen parent;
    private final List<WeCommands.Option> allOptions;
    private final Consumer<WeCommands.Option> onSelected;
    private final String searchHint;

    private GuiTextFieldGeneric searchField;
    private List<WeCommands.Option> filtered = new ArrayList<>();

    public OptionPickerScreen(Screen parent, String title, List<WeCommands.Option> options, String searchHint, Consumer<WeCommands.Option> onSelected) {
        this.setParent(parent);
        this.parent = parent;
        this.setTitle(title);
        this.allOptions = new ArrayList<>(options);
        this.filtered.addAll(allOptions);
        this.searchHint = searchHint;
        this.onSelected = onSelected;
    }

    @Override
    public void initGui() {
        super.initGui();

        ButtonGeneric back = new ButtonGeneric(MARGIN_X, 8, 60, 18, StringUtils.translate("wegui.command.back"));
        this.addButton(back, (btn, mouseButton) -> this.mc.setScreen(parent));

        int fieldY = TOP - 34;
        int fieldW = this.width - MARGIN_X * 2;
        searchField = new GuiTextFieldGeneric(MARGIN_X, fieldY, fieldW, 18, this.font);
        searchField.setHint(Component.literal(searchHint));
        searchField.setValue("");
        searchField.setMaxLength(128);
        this.addTextField(searchField, (textField) -> {
            updateFilter();
            return true;
        });

        rebuildOptionButtons();
    }

    private void updateFilter() {
        String query = searchField.getValue().trim().toLowerCase();
        filtered.clear();
        for (WeCommands.Option opt : allOptions) {
            if (query.isEmpty()
                    || opt.value().toLowerCase().contains(query)
                    || opt.label().toLowerCase().contains(query)
                    || opt.tooltip().toLowerCase().contains(query)) {
                filtered.add(opt);
            }
        }
        rebuildOptionButtons();
    }

    private void rebuildOptionButtons() {
        this.clearElements();

        // Re-add static controls
        ButtonGeneric back = new ButtonGeneric(MARGIN_X, 8, 60, 18, StringUtils.translate("wegui.command.back"));
        this.addButton(back, (btn, mouseButton) -> this.mc.setScreen(parent));

        int fieldY = TOP - 34;
        int fieldW = this.width - MARGIN_X * 2;
        GuiTextFieldGeneric field = new GuiTextFieldGeneric(MARGIN_X, fieldY, fieldW, 18, this.font);
        field.setHint(Component.literal(searchHint));
        field.setValue(searchField == null ? "" : searchField.getValue());
        field.setMaxLength(128);
        this.addTextField(field, (textField) -> {
            searchField = textField;
            updateFilter();
            return true;
        });
        searchField = field;

        int y = TOP;
        int btnW = this.width - MARGIN_X * 2;
        int maxVisible = Math.max(6, (this.height - TOP - 40) / ROW_HEIGHT);
        for (int i = 0; i < Math.min(filtered.size(), maxVisible); i++) {
            WeCommands.Option opt = filtered.get(i);
            ButtonGeneric btn = new ButtonGeneric(MARGIN_X, y, btnW, 20, opt.label());
            if (!opt.tooltip().isBlank()) {
                btn.setHoverStrings(opt.tooltip());
            }
            this.addButton(btn, new OptionButtonListener(this, opt));
            y += ROW_HEIGHT;
        }

        if (filtered.isEmpty()) {
            this.addLabel(MARGIN_X, y, btnW, 20, 0xFFAAAAAA, StringUtils.translate("wegui.picker.no_results"));
        } else if (filtered.size() > maxVisible) {
            this.addLabel(MARGIN_X, y, btnW, 20, 0xFFAAAAAA,
                    StringUtils.translate("wegui.picker.more_results", filtered.size() - maxVisible));
        }
    }

    @Override
    public void drawContents(GuiContext ctx, int mouseX, int mouseY, float partialTick) {
        // 控件自行渲染
    }

    private void onSelect(WeCommands.Option option) {
        onSelected.accept(option);
        this.mc.setScreen(parent);
    }

    private record OptionButtonListener(OptionPickerScreen screen, WeCommands.Option option) implements IButtonActionListener {
        @Override
        public void actionPerformedWithButton(fi.dy.masa.malilib.gui.button.ButtonBase button, int mouseButton) {
            screen.onSelect(option);
        }
    }
}
