package com.sow.wegui.client.screen.widget;

import com.sow.wegui.commands.WeCommands;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.wrappers.TextFieldWrapper;
import net.minecraft.network.chat.Component;

/**
 * 数字输入控件：文本框 + 加减按钮。
 */
public class NumberInput implements IParamControl {
    private final TextFieldWrapper<GuiTextFieldGeneric> wrapper;
    private final boolean integer;
    private final double step;

    public NumberInput(GuiBase screen, int x, int y, int w, WeCommands.Param param) {
        this.integer = param.paramType() == WeCommands.ParamType.INTEGER;
        this.step = computeStep(param);
        int btnW = 16;
        int gap = 2;
        int fieldW = w - btnW * 2 - gap * 2;

        GuiTextFieldGeneric field = new GuiTextFieldGeneric(x + btnW + gap, y, fieldW, 18, screen.textRenderer);
        String def = param.defaultValue() == null ? "" : param.defaultValue();
        field.setValue(def);
        if (param.hint() != null && !param.hint().isBlank()) {
            field.setHint(Component.literal(param.hint()));
        }
        field.setMaxLength(32);
        this.wrapper = screen.addTextField(field, (textField) -> true);

        ButtonGeneric minus = new ButtonGeneric(x, y, btnW, 18, "-");
        screen.addButton(minus, (btn, mouseButton) -> adjust(-1));

        ButtonGeneric plus = new ButtonGeneric(x + fieldW + btnW + gap * 2, y, btnW, 18, "+");
        screen.addButton(plus, (btn, mouseButton) -> adjust(1));
    }

    private double computeStep(WeCommands.Param param) {
        WeCommands.ParamMeta meta = param.meta();
        long range = (long) meta.max() - (long) meta.min();
        if (integer) {
            return range > 1000 ? 10 : 1;
        }
        return range > 100 ? 1.0 : 0.1;
    }

    private void adjust(int delta) {
        try {
            double val = Double.parseDouble(getValue().trim());
            double next = integer ? (int) val + delta * step : val + delta * step;
            wrapper.getTextField().setValue(format(next));
        } catch (NumberFormatException e) {
            wrapper.getTextField().setValue(integer ? "0" : "0.0");
        }
    }

    private String format(double value) {
        if (integer) return String.valueOf((int) value);
        if (value == (int) value) return String.valueOf((int) value);
        return String.valueOf(value);
    }

    @Override
    public String getValue() {
        return wrapper.getTextField().getValue();
    }

    @Override
    public void setValue(String value) {
        wrapper.getTextField().setValue(value == null ? "" : value);
    }

    @Override
    public boolean isValid(WeCommands.Param param) {
        String v = getValue().trim();
        if (!param.optional() && v.isEmpty()) return false;
        if (v.isEmpty()) return true;
        if (integer && v.contains(".")) return false;
        try {
            double d = Double.parseDouble(v);
            WeCommands.ParamMeta meta = param.meta();
            return !(d < meta.min()) && !(d > meta.max());
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public void setFocused(boolean focused) {
        wrapper.getTextField().setFocused(focused);
    }
}
