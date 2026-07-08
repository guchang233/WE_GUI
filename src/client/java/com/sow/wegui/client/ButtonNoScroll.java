package com.sow.wegui.client;

import fi.dy.masa.malilib.gui.button.ButtonGeneric;

/**
 * 不响应滚轮的普通按钮。
 *
 * MaLiLib 的 ButtonBase 在 onMouseScrolledImpl 中会把滚轮事件转成点击事件，
 * 导致在可滚动列表里对准按钮滚轮时会误触发按钮。此类禁用该行为，让滚轮
 * 事件继续向上/向列表传递。
 */
public final class ButtonNoScroll extends ButtonGeneric {
    public ButtonNoScroll(int x, int y, int width, int height, String text) {
        super(x, y, width, height, text);
    }

    @Override
    public boolean onMouseScrolledImpl(double mouseX, double mouseY, double horizontal, double vertical) {
        return false;
    }
}
