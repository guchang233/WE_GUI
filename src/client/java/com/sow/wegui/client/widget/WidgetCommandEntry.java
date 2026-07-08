package com.sow.wegui.client.widget;

import com.sow.wegui.WeCommand;
import com.sow.wegui.WeCommandType;
import com.sow.wegui.client.ButtonNoScroll;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.render.GuiContext;

import java.util.function.Consumer;

/**
 * 命令列表条目 —— 显示命令名、类型标记与悬停说明。
 */
public final class WidgetCommandEntry extends WidgetListEntryBase<WeCommand> {
    private static final int ROW_H = 22;

    public WidgetCommandEntry(int x, int y, int width, WeCommand command, int listIndex,
                              Consumer<WeCommand> onClick) {
        super(x, y, width, ROW_H, command, listIndex);

        String mark = typeMark(command);
        String label = command.displayName() + (mark.isEmpty() ? "" : " " + mark);

        ButtonGeneric btn = new ButtonNoScroll(x, y, width, ROW_H, label);
        if (!command.description().isEmpty()) {
            btn.setHoverStrings(command.description());
        }
        this.addButton(btn, new IButtonActionListener() {
            @Override
            public void actionPerformedWithButton(ButtonBase button, int mouseButton) {
                onClick.accept(command);
            }
        });
    }

    private static String typeMark(WeCommand cmd) {
        if (cmd.usages().size() > 1) return "§7[多]";
        WeCommandType t = cmd.usages().get(0).type();
        return switch (t) {
            case INSTANT -> "§7[即]";
            case PARAMETRIC -> "§7[参]";
            case BIND -> "§7[绑]";
        };
    }

    @Override
    public void render(GuiContext ctx, int mouseX, int mouseY, boolean selected) {
        // 奇偶行底色
        int bg = (this.getListIndex() % 2 == 0) ? 0x20FFFFFF : 0x30FFFFFF;
        ctx.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + ROW_H, bg);
        super.render(ctx, mouseX, mouseY, selected);
    }
}
