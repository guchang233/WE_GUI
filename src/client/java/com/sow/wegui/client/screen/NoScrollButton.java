package com.sow.wegui.client.screen;

import fi.dy.masa.malilib.gui.button.ButtonGeneric;

/**
 * 禁用滚轮触发点击的 malilib 按钮。
 * malilib 的 ButtonBase 默认会把滚轮事件转换成鼠标点击，导致在命令网格中滚动时误触命令。
 */
public class NoScrollButton extends ButtonGeneric {
    public NoScrollButton(int x, int y, int width, int height, String translationKey, String... hoverStrings) {
        super(x, y, width, height, translationKey, hoverStrings);
    }

    public NoScrollButton(int x, int y, int width, int height, String displayString) {
        super(x, y, width, height, displayString);
    }

    @Override
    public boolean onMouseScrolledImpl(int mouseX, int mouseY, double scrollX, double scrollY) {
        return false;
    }

    public void setVisiblePublic(boolean visible) {
        this.visible = visible;
    }
}
