package com.sow.wegui.client.screen.widget;

import com.sow.wegui.commands.WeCommands;

/**
 * 参数输入控件的统一接口。
 */
public interface IParamControl {
    String getValue();

    default void setValue(String value) {}

    default boolean isValid(WeCommands.Param param) { return true; }

    default void setFocused(boolean focused) {}
}
