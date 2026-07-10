package com.sow.wegui.client.screen;

import com.sow.wegui.commands.WeCommands.Command;
import com.sow.wegui.commands.WeCommands.Option;
import com.sow.wegui.commands.WeCommands.Param;
import com.sow.wegui.commands.WeCommands.ParamType;
import com.sow.wegui.commands.WeCommands.Usage;
import com.sow.wegui.client.CommandSender;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.interfaces.IStringRetriever;
import fi.dy.masa.malilib.gui.widgets.WidgetDropDownList;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.gui.wrappers.TextFieldWrapper;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 参数输入界面：为具体命令用法填写参数，并以中文释义和下拉框辅助输入（malilib 风格）。
 */
public class ParamInputScreen extends GuiBase {
    private static final int MARGIN_X = 20;
    private static final int CONTENT_TOP = 48;
    private static final int BOTTOM_AREA = 42;
    private static final int ROW_HEIGHT = 50;

    private final Screen parent;
    private final Command command;
    private final Usage usage;
    private final List<ParamRow> rows = new ArrayList<>();
    private final List<String> headerLines = new ArrayList<>();

    public ParamInputScreen(Screen parent, Command command, Usage usage) {
        this.setParent(parent);
        this.parent = parent;
        this.command = command;
        this.usage = usage;
        this.setTitle(StringUtils.translate(command.displayName()));
    }

    @Override
    public void initGui() {
        super.initGui();

        int maxW = this.width - MARGIN_X * 2;
        headerLines.clear();
        headerLines.addAll(splitLines("用法: " + StringUtils.translate(usage.displayTemplate()), maxW));
        if (!usage.description().isBlank()) {
            headerLines.addAll(splitLines(StringUtils.translate(usage.description()), maxW));
        }
        if (!command.description().isBlank()) {
            headerLines.addAll(splitLines(StringUtils.translate(command.description()), maxW));
        }
        int headerH = headerLines.size() * (this.fontHeight + 1) + 8;

        rows.clear();
        List<Param> params = usage.params();
        int y = CONTENT_TOP + headerH;
        for (Param param : params) {
            addParamRow(param, MARGIN_X, y, maxW);
            y += ROW_HEIGHT;
        }

        int bw = 70;
        ButtonGeneric exec = new ButtonGeneric(this.width / 2 - bw - 5, this.height - 28, bw, 20, StringUtils.translate("wegui.command.execute"));
        this.addButton(exec, (btn, mouseButton) -> execute());

        ButtonGeneric back = new ButtonGeneric(this.width / 2 + 5, this.height - 28, bw, 20, StringUtils.translate("wegui.command.back"));
        this.addButton(back, (btn, mouseButton) -> this.mc.setScreen(parent));
    }

    private void addParamRow(Param param, int x, int y, int w) {
        this.addLabel(x, y, w, 12, 0xFFFFFF00, StringUtils.translate(param.name()));

        String desc = descriptionOf(param);
        if (!desc.isBlank()) {
            this.addLabel(x, y + 34, w, 12, 0xFFAAAAAA, desc);
        }

        InputControl control = createControl(param, x, y + 12, w);
        rows.add(new ParamRow(param, control));
    }

    private InputControl createControl(Param param, int x, int y, int w) {
        return switch (param.paramType()) {
            case FLAG -> {
                String flagValue = param.defaultValue() == null ? "" : param.defaultValue();
                FlagButton button = new FlagButton(x, y, w, 20, StringUtils.translate(param.name()), flagValue);
                this.addButton(button.getButton(), (btn, mouseButton) -> button.toggle());
                yield button;
            }
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
                IStringRetriever<Option> retriever = opt -> opt == null ? "" : StringUtils.translate(opt.label());
                WidgetDropDownList<Option> dropdown = new WidgetDropDownList<>(x, y, w, 18, 160, 12, opts, retriever);
                if (initial != null) {
                    dropdown.setSelectedEntry(initial);
                }
                this.addWidget(dropdown);
                yield new EnumDropdown(dropdown);
            }
            case PATTERN, MASK -> createItemPickerControl(param, x, y, w);
            default -> {
                GuiTextFieldGeneric field = new GuiTextFieldGeneric(x, y, w, 18, this.font);
                String def = param.defaultValue() == null ? "" : param.defaultValue();
                field.setValue(def);
                if (param.hint() != null && !param.hint().isBlank()) {
                    field.setHint(net.minecraft.network.chat.Component.literal(StringUtils.translate(param.hint())));
                }
                field.setMaxLength(256);
                TextFieldWrapper<GuiTextFieldGeneric> wrapper = this.addTextField(field, (textField) -> true);
                yield new TextInput(wrapper);
            }
        };
    }

    private InputControl createItemPickerControl(Param param, int x, int y, int w) {
        int btnW = 24;
        int gap = 4;
        int fieldW = w - btnW - gap;

        GuiTextFieldGeneric field = new GuiTextFieldGeneric(x, y, fieldW, 18, this.font);
        String def = param.defaultValue() == null ? "" : param.defaultValue();
        field.setValue(def);
        if (param.hint() != null && !param.hint().isBlank()) {
            field.setHint(Component.literal(StringUtils.translate(param.hint())));
        }
        field.setMaxLength(256);
        TextFieldWrapper<GuiTextFieldGeneric> wrapper = this.addTextField(field, (textField) -> true);

        ButtonGeneric pick = new ButtonGeneric(x + fieldW + gap, y, btnW, 18, StringUtils.translate("wegui.picker.button"));
        this.addButton(pick, (btn, mouseButton) -> this.mc.setScreen(new InventoryPickerScreen(this, id -> {
            // 关闭选择器后会重新进入 ParamInputScreen，这里通过 schedule 在下一 tick 设置值
            wrapper.textField().setValue(id);
            wrapper.textField().setCursorPosition(id.length());
        })));

        return new TextInput(wrapper);
    }

    private String collectValue(ParamRow row) {
        return row.control.getValue();
    }

    private void execute() {
        List<String> values = new ArrayList<>();
        for (ParamRow row : rows) {
            values.add(collectValue(row));
        }
        String commandLine = usage.buildCommand(values);
        CommandSender.send(commandLine);
        this.mc.setScreen(null);
    }

    private String computePreview() {
        List<String> values = new ArrayList<>();
        for (ParamRow row : rows) {
            values.add(collectValue(row));
        }
        return "§7" + usage.buildCommand(values);
    }

    @Override
    public void drawContents(GuiContext ctx, int mouseX, int mouseY, float partialTick) {
        int y = CONTENT_TOP;
        for (String line : headerLines) {
            this.drawString(ctx, line, MARGIN_X, y, 0xFFFFFFFF);
            y += this.fontHeight + 1;
        }
        this.drawString(ctx, computePreview(), MARGIN_X, this.height - 40, 0xFFAAAAAA);
    }

    private List<String> splitLines(String text, int maxWidth) {
        if (text == null || text.isEmpty()) return List.of();
        List<String> lines = new ArrayList<>();
        for (String raw : text.split("\\n")) {
            if (this.font.width(raw) <= maxWidth) {
                lines.add(raw);
                continue;
            }
            StringBuilder current = new StringBuilder();
            for (String word : raw.split(" ")) {
                if (this.font.width(current + " " + word) > maxWidth && !current.isEmpty()) {
                    lines.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    if (!current.isEmpty()) current.append(" ");
                    current.append(word);
                }
            }
            if (!current.isEmpty()) lines.add(current.toString());
        }
        return lines;
    }

    private static String descriptionOf(Param param) {
        String d = param.description();
        if (d != null && !d.isBlank()) return StringUtils.translate(d);
        String h = param.hint();
        return h == null ? "" : StringUtils.translate(h);
    }

    private interface InputControl {
        String getValue();
    }

    private record ParamRow(Param param, InputControl control) {
    }

    private final class FlagButton implements InputControl {
        private final ButtonGeneric button;
        private final String flagValue;
        private boolean checked;

        FlagButton(int x, int y, int w, int h, String name, String flagValue) {
            this.flagValue = flagValue;
            this.button = new ButtonGeneric(x, y, w, h, "");
            updateLabel(name);
        }

        void toggle() {
            checked = !checked;
            updateLabel();
        }

        @Override
        public String getValue() {
            return checked ? flagValue : "";
        }

        ButtonGeneric getButton() {
            return button;
        }

        private void updateLabel() {
            updateLabel(button.getHoverStrings().isEmpty() ? "" : button.getHoverStrings().get(0));
        }

        private void updateLabel(String name) {
            button.setDisplayString((checked ? "[√] " : "[ ] ") + name);
            button.setHoverStrings(name);
        }
    }

    private final class EnumDropdown implements InputControl {
        private final WidgetDropDownList<Option> dropdown;

        EnumDropdown(WidgetDropDownList<Option> dropdown) {
            this.dropdown = dropdown;
        }

        @Override
        public String getValue() {
            Option opt = dropdown.getSelectedEntry();
            return opt == null ? "" : opt.value();
        }
    }

    private final class TextInput implements InputControl {
        private final TextFieldWrapper<GuiTextFieldGeneric> wrapper;

        TextInput(TextFieldWrapper<GuiTextFieldGeneric> wrapper) {
            this.wrapper = wrapper;
        }

        @Override
        public String getValue() {
            return wrapper.textField().getValue();
        }
    }
}
