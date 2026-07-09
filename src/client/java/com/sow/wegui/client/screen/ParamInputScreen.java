package com.sow.wegui.client.screen;

import com.sow.wegui.commands.WeCommands.Command;
import com.sow.wegui.commands.WeCommands.Option;
import com.sow.wegui.commands.WeCommands.Param;
import com.sow.wegui.commands.WeCommands.ParamType;
import com.sow.wegui.commands.WeCommands.Usage;
import com.sow.wegui.client.CommandSender;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * 参数输入界面：为具体命令用法填写参数，并以中文释义和下拉框辅助输入。
 */
public class ParamInputScreen extends BaseScreen {
    private static final int MARGIN_X = 20;
    private static final int CONTENT_TOP = 48;
    private static final int BOTTOM_AREA = 42;
    private static final int ROW_HEIGHT = 50;

    private final Screen parent;
    private final Command command;
    private final Usage usage;
    private final List<ParamRow> rows = new ArrayList<>();
    private final List<FormattedCharSequence> headerLines = new ArrayList<>();
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private int contentBottom;

    public ParamInputScreen(Screen parent, Command command, Usage usage) {
        super(Component.literal(command.displayName()));
        this.parent = parent;
        this.command = command;
        this.usage = usage;
    }

    @Override
    protected void init() {
        super.init();
        contentBottom = height - BOTTOM_AREA;

        headerLines.clear();
        int maxW = width - MARGIN_X * 2;
        headerLines.addAll(font.split(Component.literal("用法: " + usage.displayTemplate()), maxW));
        if (!usage.description().isBlank()) {
            headerLines.addAll(font.split(Component.literal(usage.description()), maxW));
        }
        if (!command.description().isBlank()) {
            headerLines.addAll(font.split(Component.literal(command.description()), maxW));
        }
        int headerH = headerLines.size() * (font.lineHeight + 1) + 8;

        rows.clear();
        List<Param> params = usage.params();
        int contentH = params.size() * ROW_HEIGHT + 10;
        int viewH = contentBottom - CONTENT_TOP - headerH;
        maxScroll = Math.max(0, contentH - viewH);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        int y = CONTENT_TOP + headerH;
        for (Param param : params) {
            AbstractWidget widget = createWidget(param, y);
            rows.add(new ParamRow(param, widget, y));
            addRenderableWidget(widget);
            y += ROW_HEIGHT;
        }
        repositionRows();

        int bw = 70;
        addRenderableWidget(Button.builder(
                        Component.literal("执行"),
                        b -> execute())
                .bounds(width / 2 - bw - 5, height - 28, bw, 20)
                .build());
        addRenderableWidget(Button.builder(
                        Component.literal("返回"),
                        b -> minecraft.setScreen(parent))
                .bounds(width / 2 + 5, height - 28, bw, 20)
                .build());
    }

    private AbstractWidget createWidget(Param param, int y) {
        int w = width - MARGIN_X * 2;
        return switch (param.paramType()) {
            case FLAG -> Checkbox.builder(Component.literal(param.name()), font)
                    .pos(MARGIN_X, y + 12)
                    .selected(false)
                    .build();
            case ENUM -> {
                List<Option> opts = param.options();
                Option initial = opts.isEmpty() ? null : opts.get(0);
                if (param.defaultValue() != null) {
                    for (Option o : opts) {
                        if (o.value().equals(param.defaultValue())) {
                            initial = o;
                            break;
                        }
                    }
                }
                yield CycleButton.<Option>builder(
                                opt -> Component.literal(opt.label()),
                                initial)
                        .withValues(opts)
                        .create(MARGIN_X, y + 12, w, 20, Component.empty(), (btn, val) -> {});
            }
            default -> {
                EditBox box = new EditBox(font, MARGIN_X, y + 12, w, 20, Component.empty());
                String def = param.defaultValue() == null ? "" : param.defaultValue();
                box.setValue(def);
                if (param.hint() != null && !param.hint().isBlank()) {
                    box.setHint(Component.literal(param.hint()));
                }
                box.setMaxLength(256);
                yield box;
            }
        };
    }

    private void repositionRows() {
        for (ParamRow row : rows) {
            int y = row.baseY - scrollOffset;
            boolean visible = y + ROW_HEIGHT > CONTENT_TOP && y < contentBottom;
            AbstractWidget w = row.widget;
            if (w instanceof EditBox eb) {
                eb.setY(y + 12);
                eb.setVisible(visible);
                eb.setEditable(visible);
            } else if (w instanceof Checkbox cb) {
                cb.setY(y + 12);
                cb.visible = visible;
                cb.active = visible;
            } else if (w instanceof CycleButton<?> cb) {
                cb.setY(y + 12);
                cb.visible = visible;
                cb.active = visible;
            }
        }
    }

    private String collectValue(ParamRow row) {
        Param param = row.param;
        return switch (param.paramType()) {
            case FLAG -> {
                Checkbox cb = (Checkbox) row.widget;
                yield cb.selected() ? (param.defaultValue() == null ? "" : param.defaultValue()) : "";
            }
            case ENUM -> {
                CycleButton<Option> cb = (CycleButton<Option>) row.widget;
                Option opt = cb.getValue();
                yield opt == null ? "" : opt.value();
            }
            default -> {
                EditBox eb = (EditBox) row.widget;
                yield eb.getValue();
            }
        };
    }

    private void execute() {
        List<String> values = new ArrayList<>();
        for (ParamRow row : rows) {
            values.add(collectValue(row));
        }
        String commandLine = usage.buildCommand(values);
        CommandSender.send(commandLine);
        minecraft.setScreen(null);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int old = scrollOffset;
        scrollOffset = (int) Math.clamp(scrollOffset - scrollY * ROW_HEIGHT / 2, 0, maxScroll);
        if (old != scrollOffset) {
            repositionRows();
        }
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        int y = CONTENT_TOP;
        for (FormattedCharSequence line : headerLines) {
            g.drawString(font, line, MARGIN_X, y, 0xFFFFFFFF);
            y += font.lineHeight + 1;
        }

        for (ParamRow row : rows) {
            int rowY = row.baseY - scrollOffset;
            if (rowY + ROW_HEIGHT < CONTENT_TOP || rowY > contentBottom) continue;

            g.drawString(font, Component.literal(row.param.name()), MARGIN_X, rowY, 0xFFFFFF00);
            String desc = row.description();
            if (!desc.isBlank()) {
                g.drawString(font, Component.literal(desc), MARGIN_X, rowY + 34, 0xFFAAAAAA);
            }
        }

        List<String> values = new ArrayList<>();
        for (ParamRow row : rows) values.add(collectValue(row));
        String preview = usage.buildCommand(values);
        g.drawString(font, Component.literal("§7" + preview), MARGIN_X, height - 40, 0xFFAAAAAA);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private record ParamRow(Param param, AbstractWidget widget, int baseY) {
        String description() {
            String d = param.description();
            if (d != null && !d.isBlank()) return d;
            String h = param.hint();
            return h == null ? "" : h;
        }
    }
}
