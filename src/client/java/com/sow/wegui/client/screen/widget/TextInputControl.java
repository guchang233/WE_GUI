package com.sow.wegui.client.screen.widget;

import com.sow.wegui.commands.WeCommands;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.wrappers.TextFieldWrapper;
import net.minecraft.network.chat.Component;

/**
 * 通用文本输入控件，支持 hint。
 */
public class TextInputControl implements IParamControl {
    private final TextFieldWrapper<GuiTextFieldGeneric> wrapper;

    public TextInputControl(GuiBase screen, int x, int y, int w, WeCommands.Param param) {
        GuiTextFieldGeneric field = new GuiTextFieldGeneric(x, y, w, 18, screen.font);
        String def = param.defaultValue() == null ? "" : param.defaultValue();
        field.setValue(def);
        if (param.hint() != null && !param.hint().isBlank()) {
            field.setHint(Component.literal(param.hint()));
        }
        field.setMaxLength(256);
        this.wrapper = screen.addTextField(field, (textField) -> true);
    }

    @Override
    public String getValue() {
        return wrapper.textField().getValue();
    }

    @Override
    public void setValue(String value) {
        wrapper.textField().setValue(value == null ? "" : value);
    }

    @Override
    public boolean isValid(WeCommands.Param param) {
        String v = getValue().trim();
        if (!param.optional() && v.isEmpty()) return false;
        WeCommands.ParamMeta meta = param.meta();
        if (meta.validatorRegex() != null && !v.isEmpty() && !v.matches(meta.validatorRegex())) return false;
        return true;
    }

    @Override
    public void setFocused(boolean focused) {
        wrapper.textField().setFocused(focused);
    }
}
