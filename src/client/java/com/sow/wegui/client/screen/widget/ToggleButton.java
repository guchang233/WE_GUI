package com.sow.wegui.client.screen.widget;

import com.sow.wegui.commands.WeCommands;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;

/**
 * 标志（FLAG）切换按钮：显示 [√] / [ ] 状态。
 */
public class ToggleButton implements IParamControl {
    private final ButtonGeneric button;
    private final String flagValue;
    private boolean checked;

    public ToggleButton(GuiBase screen, int x, int y, int w, WeCommands.Param param) {
        this.flagValue = param.defaultValue() == null ? "" : param.defaultValue();
        this.button = new ButtonGeneric(x, y, w, 20, "");
        updateLabel(param.name());
        screen.addButton(button, (btn, mouseButton) -> {
            checked = !checked;
            updateLabel(param.name());
        });
    }

    private void updateLabel(String name) {
        button.setDisplayString((checked ? "[√] " : "[ ] ") + name);
        button.setHoverStrings(name);
    }

    @Override
    public String getValue() {
        return checked ? flagValue : "";
    }

    @Override
    public void setValue(String value) {
        checked = flagValue.equals(value);
        updateLabel(button.getHoverStrings().isEmpty() ? "" : button.getHoverStrings().get(0));
    }

    @Override
    public boolean isValid(WeCommands.Param param) {
        return true;
    }
}
