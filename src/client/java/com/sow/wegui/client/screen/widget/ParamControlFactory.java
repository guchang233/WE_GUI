package com.sow.wegui.client.screen.widget;

import com.sow.wegui.commands.WeCommands;
import com.sow.wegui.commands.WeCommands.Option;
import com.sow.wegui.commands.WeCommands.Param;
import com.sow.wegui.commands.WeCommands.ParamControlType;
import com.sow.wegui.commands.WeCommands.ParamType;
import com.sow.wegui.client.util.PlayerListHelper;
import com.sow.wegui.client.util.SchematicListHelper;
import fi.dy.masa.malilib.gui.GuiBase;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 根据参数类型创建合适的输入控件。
 */
public final class ParamControlFactory {
    private ParamControlFactory() {}

    public static IParamControl create(GuiBase screen, int x, int y, int w, Param param) {
        ParamType type = param.paramType();
        ParamControlType controlType = param.meta().controlType();

        return switch (type) {
            case FLAG -> new ToggleButton(screen, x, y, w, param);
            case INTEGER, DECIMAL -> new NumberInput(screen, x, y, w, param);
            case ENUM -> createEnumControl(screen, x, y, w, param, controlType);
            case PATTERN, MASK -> new PickerControl(screen, x, y, w, param, WeCommands.blockOptions());
            case PLAYER -> new PickerControl(screen, x, y, w, param,
                    PlayerListHelper.getPlayerNames().stream().map(name -> new Option(name, name)).collect(Collectors.toList()));
            case FILENAME -> new PickerControl(screen, x, y, w, param,
                    SchematicListHelper.getSchematicNames().stream().map(name -> new Option(name, name)).collect(Collectors.toList()));
            case BOOLEAN -> new ToggleButton(screen, x, y, w, param);
            case STRING, AXIS -> new TextInputControl(screen, x, y, w, param);
        };
    }

    private static IParamControl createEnumControl(GuiBase screen, int x, int y, int w, Param param, ParamControlType controlType) {
        int count = param.options().size();
        if (controlType == ParamControlType.SEARCHABLE_DROPDOWN) {
            return new PickerControl(screen, x, y, w, param, param.options());
        }
        if (controlType == ParamControlType.BUTTON_ROW || count <= 6) {
            return new EnumButtonRow(screen, x, y, w, param);
        }
        return new EnumDropdown(screen, x, y, w, param);
    }
}
