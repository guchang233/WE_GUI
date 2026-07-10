package com.sow.wegui.client.screen;

import com.sow.wegui.client.CommandHistory;
import com.sow.wegui.client.CommandSender;
import com.sow.wegui.commands.WeCommands.Command;
import com.sow.wegui.commands.WeCommands.Param;
import com.sow.wegui.commands.WeCommands.Usage;
import com.sow.wegui.client.screen.widget.IParamControl;
import com.sow.wegui.client.screen.widget.ParamControlFactory;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * 参数输入界面：两列表单、按类型自动选择控件、实时预览、基础校验、多用法页内切换。
 */
public class ParamInputScreen extends GuiBase {
    private static final int MARGIN_X = 20;
    private static final int CONTENT_TOP = 52;
    private static final int BOTTOM_AREA = 46;
    private static final int ROW_HEIGHT = 44;
    private static final int LABEL_COL_W = 120;
    private static final int GAP = 12;
    private static final int USAGE_BTN_HEIGHT = 14;
    private static final int USAGE_GAP = 4;

    private final Screen parent;
    private final Command command;
    private final List<Usage> usages;
    private Usage selectedUsage;
    private final List<ParamRow> rows = new ArrayList<>();
    private final List<String> headerLines = new ArrayList<>();
    private String errorMessage = "";
    private int focusedIndex = -1;

    public ParamInputScreen(Screen parent, Command command, Usage usage) {
        this.setParent(parent);
        this.parent = parent;
        this.command = command;
        this.usages = new ArrayList<>(command.usages());
        this.selectedUsage = usage != null && usages.contains(usage) ? usage : (usages.isEmpty() ? null : usages.get(0));
        this.setTitle(command.displayName());
    }

    @Override
    public void initGui() {
        super.initGui();
        rebuildAll();
    }

    private void rebuildAll() {
        this.clearElements();
        rows.clear();
        headerLines.clear();
        focusedIndex = -1;

        rebuildHeader();
        rebuildUsageSelector();
        rebuildParams();
        addButtons();
    }

    private void rebuildHeader() {
        int maxW = this.width - MARGIN_X * 2;
        if (selectedUsage != null) {
            headerLines.addAll(splitLines("用法: " + selectedUsage.displayTemplate(), maxW));
            if (!selectedUsage.description().isBlank()) {
                headerLines.addAll(splitLines(selectedUsage.description(), maxW));
            }
        }
        if (!command.description().isBlank()) {
            headerLines.addAll(splitLines(command.description(), maxW));
        }
    }

    private void rebuildUsageSelector() {
        if (usages.size() <= 1 || selectedUsage == null) {
            return;
        }

        int y = CONTENT_TOP + headerLines.size() * (this.fontHeight + 1) + 6;
        int x = MARGIN_X;
        int remaining = this.width - MARGIN_X * 2;

        int maxUsageWidth = (int) (this.width * 0.4);
        for (Usage usage : usages) {
            String label = usage.description();
            if (label == null || label.isBlank()) label = usage.displayTemplate();
            int desired = Math.max(this.font.width(label) + 12, 48);
            int w = Math.min(Math.min(desired, maxUsageWidth), remaining);
            if (this.font.width(label) + 12 > w) {
                for (int i = label.length(); i > 0; i--) {
                    String candidate = label.substring(0, i) + "…";
                    if (this.font.width(candidate) + 12 <= w) {
                        label = candidate;
                        break;
                    }
                }
            }
            if (x + w > this.width - MARGIN_X) {
                x = MARGIN_X;
                y += USAGE_BTN_HEIGHT + USAGE_GAP;
            }
            ButtonGeneric btn = new ButtonGeneric(x, y, w, USAGE_BTN_HEIGHT, label);
            btn.setEnabled(usage != selectedUsage);
            if (!usage.description().isBlank()) {
                btn.setHoverStrings(usage.description());
            }
            this.addButton(btn, new UsageButtonListener(this, usage));
            x += w + USAGE_GAP;
        }
    }

    private void rebuildParams() {
        if (selectedUsage == null) return;

        int selectorLines = usages.size() <= 1 ? 0 : 1;
        int startY = CONTENT_TOP + headerLines.size() * (this.fontHeight + 1) + 10 + selectorLines * (USAGE_BTN_HEIGHT + USAGE_GAP);
        int controlX = MARGIN_X + LABEL_COL_W + GAP;
        int controlW = this.width - controlX - MARGIN_X;

        int y = startY;
        for (Param param : selectedUsage.params()) {
            addParamRow(param, MARGIN_X, y, LABEL_COL_W, controlX, controlW);
            y += ROW_HEIGHT;
        }

        if (!rows.isEmpty()) {
            focusedIndex = 0;
            rows.get(0).control.setFocused(true);
        }
    }

    private void addButtons() {
        int bw = 80;
        ButtonGeneric exec = new ButtonGeneric(this.width / 2 - bw - 6, this.height - 30, bw, 20, StringUtils.translate("wegui.command.execute"));
        this.addButton(exec, (btn, mouseButton) -> execute());

        ButtonGeneric back = new ButtonGeneric(this.width / 2 + 6, this.height - 30, bw, 20, StringUtils.translate("wegui.command.back"));
        this.addButton(back, (btn, mouseButton) -> this.mc.setScreen(parent));
    }

    private void addParamRow(Param param, int labelX, int y, int labelW, int controlX, int controlW) {
        this.addLabel(labelX, y, labelW, 12, 0xFFFFFF00, param.name());

        String desc = descriptionOf(param);
        if (!desc.isBlank()) {
            this.addLabel(labelX, y + 12, labelW, 12, 0xFFAAAAAA, desc);
        }

        IParamControl control = ParamControlFactory.create(this, controlX, y, controlW, param);
        rows.add(new ParamRow(param, control));
    }

    private void onUsageSelected(Usage usage) {
        if (usage == selectedUsage) return;
        selectedUsage = usage;
        rebuildAll();
    }

    private void execute() {
        errorMessage = "";
        List<String> values = new ArrayList<>();
        for (ParamRow row : rows) {
            if (!row.control.isValid(row.param)) {
                errorMessage = StringUtils.translate("wegui.param.invalid", row.param.name());
                return;
            }
            values.add(row.control.getValue());
        }
        String commandLine = selectedUsage.buildCommand(values);
        CommandSender.send(commandLine);
        CommandHistory.recordRecent(command.id());
        this.mc.setScreen(null);
    }

    private String computePreview() {
        if (selectedUsage == null) return "";
        List<String> values = new ArrayList<>();
        for (ParamRow row : rows) {
            values.add(row.control.getValue());
        }
        return selectedUsage.buildCommand(values);
    }

    @Override
    public void drawContents(GuiContext ctx, int mouseX, int mouseY, float partialTick) {
        int y = CONTENT_TOP;
        for (String line : headerLines) {
            this.drawString(ctx, line, MARGIN_X, y, 0xFFFFFFFF);
            y += this.fontHeight + 1;
        }

        int previewY = this.height - 46;
        String preview = computePreview();
        int maxPreviewWidth = this.width - MARGIN_X * 2;
        String displayPreview = "§7" + truncateText(preview, maxPreviewWidth);
        this.drawString(ctx, displayPreview, MARGIN_X, previewY, 0xFFFFFFFF);

        if (!errorMessage.isEmpty()) {
            this.drawString(ctx, "§c" + errorMessage, MARGIN_X, previewY - 14, 0xFFFFFFFF);
        }
    }

    private String truncateText(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        for (int i = text.length(); i > 0; i--) {
            String candidate = text.substring(0, i) + "…";
            if (this.font.width(candidate) <= maxWidth) {
                return candidate;
            }
        }
        return text;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_TAB && !rows.isEmpty()) {
            boolean shift = GuiBase.isShiftDown();
            if (focusedIndex >= 0 && focusedIndex < rows.size()) {
                rows.get(focusedIndex).control.setFocused(false);
            }
            if (shift) {
                focusedIndex--;
                if (focusedIndex < 0) focusedIndex = rows.size() - 1;
            } else {
                focusedIndex++;
                if (focusedIndex >= rows.size()) focusedIndex = 0;
            }
            rows.get(focusedIndex).control.setFocused(true);
            return true;
        }
        return super.keyPressed(event);
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
        if (d != null && !d.isBlank()) return d;
        String h = param.hint();
        return h == null ? "" : h;
    }

    private record ParamRow(Param param, IParamControl control) {
    }

    private record UsageButtonListener(ParamInputScreen screen, Usage usage) implements IButtonActionListener {
        @Override
        public void actionPerformedWithButton(fi.dy.masa.malilib.gui.button.ButtonBase button, int mouseButton) {
            screen.onUsageSelected(usage);
        }
    }
}
