package com.sow.wegui.client.screen.widget;

import com.sow.wegui.commands.WeCommands;
import com.sow.wegui.commands.WeCommands.Option;
import com.sow.wegui.commands.WeCommands.ParamType;
import com.sow.wegui.client.screen.InventoryPickerScreen;
import com.sow.wegui.client.screen.OptionPickerScreen;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.wrappers.TextFieldWrapper;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 带选择按钮的文本输入控件：
 * PATTERN/MASK 打开背包物品选择器和方块建议列表；
 * 其他类型打开可搜索选项选择器。
 */
public class PickerControl implements IParamControl {
    private static final int BTN_WIDTH = 18;
    private static final int GAP = 2;

    private final TextFieldWrapper<GuiTextFieldGeneric> wrapper;
    private final WeCommands.Param param;
    private final List<Option> options;

    public PickerControl(GuiBase screen, int x, int y, int w, WeCommands.Param param, List<Option> options) {
        this.param = param;
        this.options = options;

        boolean isPatternOrMask = param.paramType() == ParamType.PATTERN || param.paramType() == ParamType.MASK;
        int btnCount = isPatternOrMask ? 2 : 1;
        int totalBtnWidth = BTN_WIDTH * btnCount + GAP * (btnCount - 1);
        int fieldW = w - totalBtnWidth - GAP;

        GuiTextFieldGeneric field = new GuiTextFieldGeneric(x, y, fieldW, 18, screen.textRenderer);
        String def = param.defaultValue() == null ? "" : param.defaultValue();
        field.setValue(def);
        if (param.hint() != null && !param.hint().isBlank()) {
            field.setHint(Component.literal(param.hint()));
        }
        field.setMaxLength(256);
        this.wrapper = screen.addTextField(field, (textField) -> true);

        int bx = x + fieldW + GAP;
        if (isPatternOrMask) {
            ButtonGeneric pick = new ButtonGeneric(bx, y, BTN_WIDTH, 18, StringUtils.translate("wegui.picker.button"));
            screen.addButton(pick, (btn, mouseButton) -> screen.mc.setScreen(new InventoryPickerScreen(screen, id -> {
                wrapper.getTextField().setValue(id);
                wrapper.getTextField().setCursorPosition(id.length());
            })));
            bx += BTN_WIDTH + GAP;
        }

        if (isPatternOrMask || !options.isEmpty()) {
            String suggestLabel = isPatternOrMask ? "常" : "▽";
            ButtonGeneric suggest = new ButtonGeneric(bx, y, BTN_WIDTH, 18, suggestLabel);
            List<Option> pickerOptions = isPatternOrMask ? options : this.options;
            screen.addButton(suggest, (btn, mouseButton) -> screen.mc.setScreen(new OptionPickerScreen(
                    screen, param.name(), pickerOptions, StringUtils.translate("wegui.picker.search"), opt -> {
                wrapper.getTextField().setValue(opt.value());
                wrapper.getTextField().setCursorPosition(opt.value().length());
            })));
        }
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
        return param.optional() || !v.isEmpty();
    }

    @Override
    public void setFocused(boolean focused) {
        wrapper.getTextField().setFocused(focused);
    }
}
