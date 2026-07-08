package com.sow.wegui.client.widget;

import com.sow.wegui.WeCommandUsage;
import com.sow.wegui.client.ButtonNoScroll;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.render.GuiContext;

import java.util.function.Consumer;

/**
 * 用法变体列表条目 —— 显示模板与说明。
 */
public final class WidgetUsageEntry extends WidgetListEntryBase<WeCommandUsage> {
    private static final int ROW_H = 22;

    public WidgetUsageEntry(int x, int y, int width, WeCommandUsage usage, int listIndex,
                            Consumer<WeCommandUsage> onClick) {
        super(x, y, width, ROW_H, usage, listIndex);

        ButtonGeneric btn = new ButtonNoScroll(x, y, width, ROW_H, usage.displayTemplate());
        if (!usage.description().isEmpty()) {
            btn.setHoverStrings(usage.description());
        }
        this.addButton(btn, new IButtonActionListener() {
            @Override
            public void actionPerformedWithButton(ButtonBase button, int mouseButton) {
                onClick.accept(usage);
            }
        });
    }

    @Override
    public void render(GuiContext ctx, int mouseX, int mouseY, boolean selected) {
        int bg = (this.getListIndex() % 2 == 0) ? 0x20FFFFFF : 0x30FFFFFF;
        ctx.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + ROW_H, bg);
        super.render(ctx, mouseX, mouseY, selected);
    }
}
