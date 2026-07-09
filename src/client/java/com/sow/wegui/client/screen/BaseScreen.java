package com.sow.wegui.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 所有 WE GUI 屏幕的基类，提供半透明背景与非暂停特性。
 */
public abstract class BaseScreen extends Screen {
    protected BaseScreen(Component title) {
        super(title);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xCC000000);
    }
}
