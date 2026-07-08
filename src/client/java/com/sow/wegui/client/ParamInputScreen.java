package com.sow.wegui.client;

import com.sow.wegui.WeCommand;
import com.sow.wegui.WeCommandParam;
import com.sow.wegui.WeCommandUsage;
import com.sow.wegui.WeParamType;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.widgets.WidgetDropDownList;
import fi.dy.masa.malilib.render.GuiContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * 参数输入界面 —— 使用 MaLiLib 的 GuiBase。
 * 根据命令用法的参数定义动态渲染输入控件。
 */
public final class ParamInputScreen extends GuiBase {
    private static final int ROW_H = 24;
    private static final int LABEL_W = 120;
    private static final int INPUT_W = 220;
    private static final int CHECK_W = 70;

    private final WeCommand command;
    private final WeCommandUsage usage;
    private final List<WeCommandParam> params;

    private final List<GuiTextFieldGeneric> textFields = new ArrayList<>();
    private final List<WidgetDropDownList<String>> dropDowns = new ArrayList<>();
    private final List<WidgetCheckBox> checkBoxes = new ArrayList<>();

    public ParamInputScreen(WeCommand command, WeCommandUsage usage) {
        super();
        this.command = command;
        this.usage = usage;
        this.params = usage.params();
        this.setTitle(command.displayName() + " — 参数");
    }

    @Override
    public void initGui() {
        super.initGui();

        textFields.clear();
        dropDowns.clear();
        checkBoxes.clear();

        int cx = this.getScreenWidth() / 2;
        int startY = 70;
        int labelX = cx - LABEL_W - 12;
        int inputX = cx - 4;

        for (int i = 0; i < params.size(); i++) {
            WeCommandParam param = params.get(i);
            int y = startY + i * (ROW_H + 8);

            if (param.optional()) {
                WidgetCheckBox cb = new WidgetCheckBox(inputX + INPUT_W + 8, y + 4, CHECK_W, ROW_H, "启用", false);
                this.addWidget(cb);
                checkBoxes.add(cb);
            } else {
                checkBoxes.add(null);
            }

            createInput(param, inputX, y);
        }

        int btnY = startY + params.size() * (ROW_H + 8) + 24;
        ButtonGeneric execBtn = new ButtonGeneric(cx - 110, btnY, 100, 20, "§a执行");
        ButtonGeneric backBtn = new ButtonGeneric(cx + 10, btnY, 70, 20, "§7返回");
        ButtonGeneric resetBtn = new ButtonGeneric(cx + 85, btnY, 70, 20, "§7重置");

        this.addButton(execBtn, (button, mouseButton) -> execute());
        this.addButton(backBtn, (button, mouseButton) -> this.mc.setScreen(this.getParent()));
        this.addButton(resetBtn, (button, mouseButton) -> reset());
    }

    private void createInput(WeCommandParam param, int x, int y) {
        WeParamType type = param.paramType();
        String defaultValue = param.defaultValue() != null ? param.defaultValue() : "";

        // 固定参数不渲染输入控件，仅显示标签即可
        if (type == WeParamType.FIXED) {
            textFields.add(null);
            dropDowns.add(null);
            return;
        }

        // 下拉选项型参数
        if (type == WeParamType.ENUM || type == WeParamType.DIRECTION
                || type == WeParamType.RELATIVE_DIRECTION || type == WeParamType.AXIS
                || type == WeParamType.BOOLEAN) {
            List<String> options = buildOptions(param, type);
            if (!options.isEmpty()) {
                WidgetDropDownList<String> dd = new WidgetDropDownList<>(x, y, INPUT_W, ROW_H, 120, 5, options);
                String initial = options.contains(defaultValue) ? defaultValue : options.get(0);
                dd.setSelectedEntry(initial);
                this.addWidget(dd);
                textFields.add(null);
                dropDowns.add(dd);
                return;
            }
            // 没有预定义选项时回退到文本框（如 AXIS 的自由输入）
        }

        GuiTextFieldGeneric field = new GuiTextFieldGeneric(x, y, INPUT_W, ROW_H, this.font);
        field.setValue(defaultValue);
        field.setHint(Component.literal(param.hint() != null && !param.hint().isBlank() ? param.hint() : param.name()));
        if (type == WeParamType.INTEGER || type == WeParamType.DECIMAL) {
            field.setFilter(s -> s.matches("-?\\d*\\.?\\d*"));
        }
        this.addTextField(field, null);
        textFields.add(field);
        dropDowns.add(null);
    }

    private List<String> buildOptions(WeCommandParam param, WeParamType type) {
        if (type == WeParamType.BOOLEAN) {
            return List.of("true", "false");
        }
        List<String> opts = new ArrayList<>(param.options());
        if (type == WeParamType.AXIS && opts.isEmpty()) {
            opts = new ArrayList<>(List.of("north", "south", "east", "west", "up", "down", "forward", "0,1,0"));
        }
        return opts;
    }

    private List<String> collectValues() {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < params.size(); i++) {
            WeCommandParam param = params.get(i);
            WidgetCheckBox cb = checkBoxes.get(i);

            if (cb != null && !cb.isChecked()) {
                values.add("");
                continue;
            }

            if (param.paramType() == WeParamType.FIXED) {
                values.add(param.defaultValue() != null ? param.defaultValue() : "");
                continue;
            }

            WidgetDropDownList<String> dd = dropDowns.get(i);
            if (dd != null) {
                String val = dd.getSelectedEntry();
                values.add(val != null ? val : "");
                continue;
            }

            GuiTextFieldGeneric field = textFields.get(i);
            values.add(field != null ? field.getValue().trim() : "");
        }
        return values;
    }

    private String previewCommand() {
        return usage.buildCommand(collectValues());
    }

    private void execute() {
        Minecraft mc = Minecraft.getInstance();
        List<String> values = collectValues();
        WeOperation op = WeOperationRegistry.get(usage.id());
        if (op != null) {
            op.execute(mc, usage, values);
        } else {
            error(mc, "该功能尚未通过 API 实现: " + usage.displayTemplate());
        }
        mc.setScreen(null);
    }

    private void reset() {
        for (int i = 0; i < params.size(); i++) {
            WeCommandParam param = params.get(i);
            WidgetCheckBox cb = checkBoxes.get(i);
            if (cb != null) {
                cb.setChecked(false);
            }

            String defaultValue = param.defaultValue() != null ? param.defaultValue() : "";
            WidgetDropDownList<String> dd = dropDowns.get(i);
            if (dd != null) {
                List<String> options = buildOptions(param, param.paramType());
                String initial = options.contains(defaultValue) ? defaultValue : options.get(0);
                dd.setSelectedEntry(initial);
                continue;
            }

            GuiTextFieldGeneric field = textFields.get(i);
            if (field != null) {
                field.setValue(defaultValue);
            }
        }
    }

    private void error(Minecraft mc, String message) {
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("§c[WE GUI] §f" + message), false);
        }
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            execute();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        super.render(g, mouseX, mouseY, partialTicks);

        GuiContext ctx = GuiContext.fromGuiGraphics(g);
        int cx = this.getScreenWidth() / 2;
        int startY = 70;
        int labelX = cx - LABEL_W - 12;

        this.drawStringWithShadow(ctx, "§b§l" + command.displayName(), cx - this.getStringWidth(command.displayName()) / 2, 18, 0xFFFFFF);
        this.drawString(ctx, "§8" + usage.displayTemplate(), cx - this.getStringWidth(usage.displayTemplate()) / 2, 34, 0x888888);
        this.drawString(ctx, "§7" + usage.description(), cx - this.getStringWidth(usage.description()) / 2, 48, 0xAAAAAA);

        for (int i = 0; i < params.size(); i++) {
            WeCommandParam param = params.get(i);
            int y = startY + i * (ROW_H + 8) + 6;
            String label = param.name() + (param.optional() ? " §8(可选)" : "");
            this.drawString(ctx, "§7" + label, labelX, y, 0xAAAAAA);

            String desc = param.description();
            if (desc != null && !desc.isBlank()) {
                this.drawString(ctx, "§8" + desc, labelX, y + 10, 0x666666);
            }

            if (param.paramType() == WeParamType.FIXED) {
                this.drawString(ctx, "§f" + param.defaultValue(), cx - 4, y, 0xFFFFFF);
            }
        }

        int previewY = startY + params.size() * (ROW_H + 8) + 8;
        this.drawString(ctx, "§a" + previewCommand(), cx - this.getStringWidth(previewCommand()) / 2, previewY, 0xFFFFFF);
    }
}
