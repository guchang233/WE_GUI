package com.sow.wegui.client;

import com.sow.wegui.WeCommand;
import com.sow.wegui.WeCommandParam;
import com.sow.wegui.WeCommandUsage;
import com.sow.wegui.WeParamType;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.gui.widgets.WidgetDropDownList;
import fi.dy.masa.malilib.render.GuiContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 参数输入界面 —— 左侧显示参数说明，右侧为输入控件与可选参数勾选框。
 * 支持内容区域滚动，滚动不会触发按钮点击。
 */
public final class ParamInputScreen extends GuiBase {
    private static final int TOP_MARGIN = 55;
    private static final int BOTTOM_MARGIN = 42;
    private static final int ROW_H = 52;
    private static final int SCROLL_SPEED = 18;

    private final WeCommand command;
    private final WeCommandUsage usage;
    private final List<WeCommandParam> params;
    private final List<ParamRow> rows = new ArrayList<>();
    private int scrollOffset = 0;
    private int contentHeight = 0;

    public ParamInputScreen(WeCommand command, WeCommandUsage usage) {
        super();
        this.command = command;
        this.usage = usage;
        this.params = usage.params();
        this.setTitle(usage.displayTemplate());
    }

    @Override
    public void initGui() {
        super.initGui();
        rows.clear();
        scrollOffset = 0;

        int sw = this.getScreenWidth();
        int sh = this.getScreenHeight();

        // 左侧说明面板固定留 34% 宽度，右侧为输入区
        int labelPanelW = Math.max(140, sw * 34 / 100);
        int inputX = labelPanelW + 20;
        int inputW = Math.max(120, sw - inputX - 40);
        int labelX = 14;

        int y = TOP_MARGIN;
        for (int i = 0; i < params.size(); i++) {
            WeCommandParam param = params.get(i);
            rows.add(createRow(param, i, labelX, labelPanelW, inputX, inputW, y));
            y += ROW_H;
        }
        contentHeight = params.size() * ROW_H + 8;

        // 底部操作按钮
        int cx = sw / 2;
        int btnY = sh - 28;
        ButtonGeneric exec = new ButtonNoScroll(cx - 82, btnY, 78, 20, "执行");
        ButtonGeneric preview = new ButtonNoScroll(cx - 2, btnY, 78, 20, "预览命令");
        ButtonGeneric back = new ButtonNoScroll(cx + 78, btnY, 78, 20, "返回");

        this.addButton(exec, (btn, mb) -> execute(false));
        this.addButton(preview, (btn, mb) -> execute(true));
        this.addButton(back, (btn, mb) -> {
            if (command.usages().size() > 1) {
                GuiBase.openGui(new UsageSelectionScreen(command));
            } else {
                GuiBase.openGui(new MainPanelScreen(command.category()));
            }
        });
    }

    private ParamRow createRow(WeCommandParam param, int index, int labelX, int labelPanelW,
                               int inputX, int inputW, int baseY) {
        Object input = null;
        WidgetCheckBox checkBox = null;

        if (param.paramType() == WeParamType.ENUM && !param.options().isEmpty()) {
            WidgetDropDownList<String> drop = new WidgetDropDownList<>(inputX, baseY, inputW, 18, 120, 5, param.options());
            drop.setSelectedEntry(param.defaultValue() != null ? param.defaultValue() : param.options().get(0));
            this.addWidget(drop);
            input = drop;
        } else {
            GuiTextFieldGeneric field = new GuiTextFieldGeneric(inputX, baseY, inputW, 18, this.font);
            field.setMaxLength(256);
            String placeholder = param.defaultValue() != null ? param.defaultValue()
                    : (param.hint() != null ? param.hint() : "");
            if (placeholder != null && !placeholder.isEmpty()) {
                field.setHint(Component.literal("§7" + placeholder));
            }
            this.addTextField(field, null);
            input = field;
        }

        if (param.optional()) {
            checkBox = new WidgetCheckBox(inputX + inputW + 10, baseY + 2, 14, 14, "启用", false);
            this.addWidget(checkBox);
        }

        return new ParamRow(param, index, labelX, labelPanelW, inputX, inputW, baseY, input, checkBox);
    }

    @Override
    protected void drawWidgets(GuiContext ctx, int mouseX, int mouseY) {
        updateInputPositions();
        applyContentScissor(ctx);
        super.drawWidgets(ctx, mouseX, mouseY);
        ctx.disableScissor();
    }

    @Override
    protected void drawTextFields(GuiContext ctx, int mouseX, int mouseY) {
        updateInputPositions();
        applyContentScissor(ctx);
        super.drawTextFields(ctx, mouseX, mouseY);
        ctx.disableScissor();
    }

    private void applyContentScissor(GuiContext ctx) {
        int sw = this.getScreenWidth();
        int sh = this.getScreenHeight();
        ctx.enableScissor(0, TOP_MARGIN, sw, sh - BOTTOM_MARGIN);
    }

    private void updateInputPositions() {
        for (ParamRow row : rows) {
            int y = row.baseY - scrollOffset;
            if (row.input instanceof GuiTextFieldGeneric field) {
                field.setY(y + 2);
            } else if (row.input instanceof WidgetBase widget) {
                widget.setY(y + 2);
            }
            if (row.checkBox != null) {
                row.checkBox.setY(y + 2);
            }
        }
    }

    @Override
    protected void drawContents(GuiContext ctx, int mouseX, int mouseY, float partialTicks) {
        int sw = this.getScreenWidth();
        int sh = this.getScreenHeight();
        int contentBottom = sh - BOTTOM_MARGIN;

        // 左侧说明面板背景
        int labelPanelW = rows.isEmpty() ? 0 : rows.get(0).labelPanelW;
        ctx.fill(0, TOP_MARGIN, labelPanelW, contentBottom, 0x20FFFFFF);

        // 绘制参数说明（不使用裁剪，保证左侧说明始终可见）
        for (ParamRow row : rows) {
            int y = row.baseY - scrollOffset;
            if (y + ROW_H < TOP_MARGIN || y > contentBottom) continue;
            drawRowLabels(ctx, row, y);
        }

        // 滚动条
        drawScrollbar(ctx, sw - 8, TOP_MARGIN, 6, contentBottom - TOP_MARGIN);

        // 无内容提示
        if (params.isEmpty()) {
            ctx.drawCenteredString(this.font, "该用法无需参数", sw / 2, TOP_MARGIN + 20, 0xAAAAAA);
        }
    }

    private void drawRowLabels(GuiContext ctx, ParamRow row, int y) {
        WeCommandParam param = row.param;
        String reqMark = param.optional() ? "§8[可选]" : "§c*";
        String name = reqMark + " §f" + param.name();
        String desc = getParamDescription(param);

        // 参数名（加粗白色）
        ctx.drawString(this.font, name, row.labelX, y + 4, 0xFFFFFF);

        // 详细说明（浅灰）
        if (!desc.isEmpty()) {
            String wrapped = wrapText(desc, row.labelPanelW - row.labelX - 6);
            int lineY = y + 18;
            for (String line : wrapped.split("\n")) {
                if (lineY + 8 > y + ROW_H - 2) break;
                ctx.drawString(this.font, "§7" + line, row.labelX, lineY, 0xAAAAAA);
                lineY += 10;
            }
        }
    }

    private String getParamDescription(WeCommandParam param) {
        if (param.description() != null && !param.description().isBlank()) {
            return param.description();
        }
        if (param.hint() != null && !param.hint().isBlank()) {
            return param.hint();
        }
        // 兜底：根据类型和默认值生成说明
        String typeName = switch (param.paramType()) {
            case INTEGER -> "整数";
            case DECIMAL -> "小数";
            case STRING -> "文本";
            case BOOLEAN -> "是/否";
            case PATTERN -> "方块图案";
            case MASK -> "方块掩码";
            case DIRECTION -> "方向";
            case RELATIVE_DIRECTION -> "相对方向";
            case AXIS -> "旋转轴";
            case FILENAME -> "文件名";
            case PLAYER -> "玩家名";
            case ENUM -> "选项";
            case FIXED -> "固定值";
        };
        StringBuilder sb = new StringBuilder("类型: " + typeName);
        if (param.defaultValue() != null && !param.defaultValue().isEmpty()) {
            sb.append("，默认: ").append(param.defaultValue());
        }
        if (param.optional()) {
            sb.append("（可选）");
        }
        return sb.toString();
    }

    private String wrapText(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) return text;
        StringBuilder result = new StringBuilder();
        StringBuilder line = new StringBuilder();
        for (String word : text.split("(?<=\\s)|(?=\\s)")) {
            if (word.isEmpty()) continue;
            // 单个词过长时按字符折断（适配中文无空格文本）
            if (this.font.width(word) > maxWidth) {
                for (int i = 0; i < word.length(); ) {
                    int cp = word.codePointAt(i);
                    String ch = new String(Character.toChars(cp));
                    if (this.font.width(line + ch) > maxWidth && line.length() > 0) {
                        flushLine(result, line);
                    }
                    line.append(ch);
                    i += Character.charCount(cp);
                }
                continue;
            }
            if (this.font.width(line + word) > maxWidth && line.length() > 0) {
                flushLine(result, line);
            }
            line.append(word);
        }
        flushLine(result, line);
        return result.toString();
    }

    private void flushLine(StringBuilder result, StringBuilder line) {
        if (line.length() == 0) return;
        if (result.length() > 0) result.append("\n");
        result.append(line);
        line.setLength(0);
    }

    private void drawScrollbar(GuiContext ctx, int x, int y, int w, int h) {
        int contentH = contentHeight;
        int viewH = h;
        if (contentH <= viewH) return;

        int trackH = Math.max(20, (int) ((long) viewH * viewH / contentH));
        int maxScroll = contentH - viewH;
        int trackY = y + (int) ((long) scrollOffset * (viewH - trackH) / maxScroll);

        ctx.fill(x, y, x + w, y + h, 0x30FFFFFF);
        ctx.fill(x, trackY, x + w, trackY + trackH, 0xFFAAAAAA);
    }

    @Override
    public boolean onMouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        int sh = this.getScreenHeight();
        if (mouseY >= TOP_MARGIN && mouseY <= sh - BOTTOM_MARGIN) {
            int viewH = sh - TOP_MARGIN - BOTTOM_MARGIN;
            int maxScroll = Math.max(0, contentHeight - viewH);
            if (maxScroll > 0) {
                scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - vertical * SCROLL_SPEED));
                return true;
            }
        }
        return super.onMouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean onMouseClicked(MouseButtonEvent event, boolean isLeft) {
        double my = event.y();
        int sh = this.getScreenHeight();
        if (my >= TOP_MARGIN && my <= sh - BOTTOM_MARGIN) {
            MouseButtonEvent adjusted = new MouseButtonEvent(event.x(), my + scrollOffset, event.buttonInfo());
            return super.onMouseClicked(adjusted, isLeft);
        }
        return super.onMouseClicked(event, isLeft);
    }

    @Override
    public boolean onMouseReleased(MouseButtonEvent event) {
        double my = event.y();
        int sh = this.getScreenHeight();
        if (my >= TOP_MARGIN && my <= sh - BOTTOM_MARGIN) {
            MouseButtonEvent adjusted = new MouseButtonEvent(event.x(), my + scrollOffset, event.buttonInfo());
            return super.onMouseReleased(adjusted);
        }
        return super.onMouseReleased(event);
    }

    @Override
    public boolean onMouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double my = event.y();
        int sh = this.getScreenHeight();
        if (my >= TOP_MARGIN && my <= sh - BOTTOM_MARGIN) {
            MouseButtonEvent adjusted = new MouseButtonEvent(event.x(), my + scrollOffset, event.buttonInfo());
            return super.onMouseDragged(adjusted, dragX, dragY);
        }
        return super.onMouseDragged(event, dragX, dragY);
    }

    private void execute(boolean previewOnly) {
        Minecraft mc = Minecraft.getInstance();
        List<String> values = collectValues();
        String cmd = usage.buildCommand(values);
        if (previewOnly) {
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal("§b[WE GUI] " + cmd), false);
            }
            return;
        }
        WeOperation op = WeOperationRegistry.get(usage.id());
        if (op != null) {
            op.execute(mc, usage, values);
        } else {
            error("操作未实现: " + usage.id());
        }
        mc.setScreen(null);
    }

    private List<String> collectValues() {
        List<String> values = new ArrayList<>();
        for (ParamRow row : rows) {
            WeCommandParam param = row.param;
            if (param.optional() && (row.checkBox == null || !row.checkBox.isChecked())) {
                values.add("");
                continue;
            }
            String v = "";
            if (row.input instanceof GuiTextFieldGeneric field) {
                v = field.getValue();
            } else if (row.input instanceof WidgetDropDownList<?> drop) {
                Object sel = drop.getSelectedEntry();
                v = sel != null ? sel.toString() : "";
            }
            values.add(v);
        }
        return values;
    }

    private void error(String msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("§c[WE GUI] " + msg), false);
        }
    }

    private record ParamRow(WeCommandParam param, int index, int labelX, int labelPanelW,
                            int inputX, int inputW, int baseY, Object input,
                            @Nullable WidgetCheckBox checkBox) {}
}
