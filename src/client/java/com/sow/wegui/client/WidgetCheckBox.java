package com.sow.wegui.client;

import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.render.GuiContext;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.function.Consumer;

/**
 * 简单勾选框组件 —— 显示一个可勾选的方框和右侧标签。
 * 用于可选参数：未勾选时该参数不参与执行。
 */
public final class WidgetCheckBox extends WidgetBase {
    private static final int BOX_SIZE = 11;

    private final String label;
    private boolean checked;
    private Consumer<WidgetCheckBox> listener;

    public WidgetCheckBox(int x, int y, int width, int height, String label, boolean checked) {
        super(x, y, width, height);
        this.label = label;
        this.checked = checked;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        setChecked(checked, true);
    }

    public void setChecked(boolean checked, boolean notify) {
        this.checked = checked;
        if (notify && listener != null) {
            listener.accept(this);
        }
    }

    public void setListener(Consumer<WidgetCheckBox> listener) {
        this.listener = listener;
    }

    @Override
    public boolean onMouseClicked(MouseButtonEvent event, boolean isLeft) {
        if (event.button() == 0 && this.isMouseOver((int) event.x(), (int) event.y())) {
            setChecked(!checked);
            return true;
        }
        return super.onMouseClicked(event, isLeft);
    }

    @Override
    public void render(GuiContext ctx, int mouseX, int mouseY, boolean selected) {
        int x = this.getX();
        int y = this.getY();

        // 方框背景
        ctx.fill(x, y, x + BOX_SIZE, y + BOX_SIZE, 0xFF111111);
        ctx.fill(x + 1, y + 1, x + BOX_SIZE - 1, y + BOX_SIZE - 1, 0xFF333333);

        // 勾选标记
        if (checked) {
            ctx.fill(x + 2, y + 2, x + BOX_SIZE - 2, y + BOX_SIZE - 2, 0xFF4A90D9);
            ctx.fill(x + 3, y + 3, x + BOX_SIZE - 3, y + BOX_SIZE - 3, 0xFF6AB0F9);
        }

        // 标签
        this.drawString(ctx, x + BOX_SIZE + 4, y + 1, 0xFFFFFFFF, label);
    }
}
