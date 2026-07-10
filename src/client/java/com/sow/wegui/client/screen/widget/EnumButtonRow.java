package com.sow.wegui.client.screen.widget;

import com.sow.wegui.commands.WeCommands;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;

import java.util.ArrayList;
import java.util.List;

/**
 * 选项较少的枚举参数横向按钮组。
 */
public class EnumButtonRow implements IParamControl {
    private final List<ButtonGeneric> buttons = new ArrayList<>();
    private final List<WeCommands.Option> options;
    private WeCommands.Option selected;

    public EnumButtonRow(GuiBase screen, int x, int y, int w, WeCommands.Param param) {
        this.options = new ArrayList<>(param.options());
        int gap = 2;
        int count = options.size();
        int btnW = count == 0 ? w : (w - gap * (count - 1)) / count;
        int cx = x;
        for (WeCommands.Option opt : options) {
            int width = cx + btnW > x + w ? x + w - cx : btnW;
            ButtonGeneric btn = new ButtonGeneric(cx, y, width, 20, opt.label());
            if (!opt.tooltip().isBlank()) {
                btn.setHoverStrings(opt.tooltip());
            }
            screen.addButton(btn, (b, mouse) -> select(opt));
            buttons.add(btn);
            cx += width + gap;
        }
        selectByValue(param.defaultValue());
    }

    private void select(WeCommands.Option option) {
        this.selected = option;
        updateButtons();
    }

    private void selectByValue(String value) {
        for (WeCommands.Option opt : options) {
            if (opt.value().equals(value)) {
                selected = opt;
                break;
            }
        }
        if (selected == null && !options.isEmpty()) {
            selected = options.get(0);
        }
        updateButtons();
    }

    private void updateButtons() {
        for (int i = 0; i < buttons.size(); i++) {
            WeCommands.Option opt = options.get(i);
            ButtonGeneric btn = buttons.get(i);
            boolean active = opt.equals(selected);
            btn.setDisplayString((active ? "● " : "○ ") + opt.label());
        }
    }

    @Override
    public String getValue() {
        return selected == null ? "" : selected.value();
    }

    @Override
    public void setValue(String value) {
        selectByValue(value);
    }

    @Override
    public boolean isValid(WeCommands.Param param) {
        return param.optional() || selected != null;
    }
}
