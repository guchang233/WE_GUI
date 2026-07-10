package com.sow.wegui.client.screen.widget;

import com.sow.wegui.commands.WeCommands;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.widgets.WidgetDropDownList;
import fi.dy.masa.malilib.interfaces.IStringRetriever;

import java.util.ArrayList;
import java.util.List;

/**
 * 选项较多的枚举参数下拉框。
 */
public class EnumDropdown implements IParamControl {
    private final WidgetDropDownList<WeCommands.Option> dropdown;
    private final List<WeCommands.Option> options;

    public EnumDropdown(GuiBase screen, int x, int y, int w, WeCommands.Param param) {
        this.options = new ArrayList<>(param.options());
        WeCommands.Option initial = options.isEmpty() ? null : options.get(0);
        if (param.defaultValue() != null) {
            for (WeCommands.Option o : options) {
                if (o.value().equals(param.defaultValue())) {
                    initial = o;
                    break;
                }
            }
        }
        IStringRetriever<WeCommands.Option> retriever = opt -> opt == null ? "" : opt.label();
        dropdown = new WidgetDropDownList<>(x, y, w, 18, 160, 12, options, retriever);
        if (initial != null) {
            dropdown.setSelectedEntry(initial);
        }
        screen.addWidget(dropdown);
    }

    @Override
    public String getValue() {
        WeCommands.Option opt = dropdown.getSelectedEntry();
        return opt == null ? "" : opt.value();
    }

    @Override
    public void setValue(String value) {
        for (WeCommands.Option opt : options) {
            if (opt.value().equals(value)) {
                dropdown.setSelectedEntry(opt);
                break;
            }
        }
    }

    @Override
    public boolean isValid(WeCommands.Param param) {
        return param.optional() || dropdown.getSelectedEntry() != null;
    }
}
