package com.sow.wegui.client.screen;

import com.sow.wegui.WeGuiMod;
import com.sow.wegui.client.LitematicaBridge;
import com.sow.wegui.client.SchematicExporter;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;

/**
 * 原理图保存界面：输入文件名、选择格式（litematic/nbt），将当前 Litematica 投影导出为原理图。
 * 默认保存目录 ./we_schematics。
 */
public class SaveSchematicScreen extends GuiBase {
    private static final int MARGIN_X = 20;
    private static final int CONTENT_TOP = 60;
    private static final int LABEL_WIDTH = 100;
    private static final int GAP = 12;
    private static final int ROW_HEIGHT = 28;

    private final Screen parent;
    private GuiTextFieldGeneric fileNameField;
    private SchematicExporter.Format selectedFormat = SchematicExporter.Format.LITEMATIC;
    private String statusMessage = "";
    private int statusColor = 0xFFFFFFFF;

    public SaveSchematicScreen(Screen parent) {
        this.setParent(parent);
        this.parent = parent;
        this.setTitle(StringUtils.translate("wegui.schematic.title"));
    }

    @Override
    public void initGui() {
        super.initGui();
        this.clearElements();

        int controlX = MARGIN_X + LABEL_WIDTH + GAP;
        int controlW = this.width - controlX - MARGIN_X;
        int y = CONTENT_TOP;

        // 文件名输入框
        this.addLabel(MARGIN_X, y + 6, LABEL_WIDTH, 12, 0xFFFFFF00,
                StringUtils.translate("wegui.schematic.file_name"));
        fileNameField = new GuiTextFieldGeneric(controlX, y, controlW, 18, this.font);
        fileNameField.setMaxLength(128);
        fileNameField.setValue(suggestDefaultName());
        this.addTextField(fileNameField, (textField) -> true);
        y += ROW_HEIGHT;

        // 格式选择按钮：选中态用 [√] 前缀标记，所有按钮均可点击切换
        this.addLabel(MARGIN_X, y + 6, LABEL_WIDTH, 12, 0xFFFFFF00,
                StringUtils.translate("wegui.schematic.format"));
        int formatBtnX = controlX;
        int formatBtnW = Math.max(90, (controlW - GAP) / 2);
        for (SchematicExporter.Format fmt : SchematicExporter.Format.values()) {
            String label = StringUtils.translate(fmt.getTranslationKey());
            boolean isSelected = (fmt == selectedFormat);
            String displayText = isSelected ? ("\u00A7a[ \u221A ]\u00A7r " + label) : label;
            ButtonGeneric fmtBtn = new ButtonGeneric(formatBtnX, y, formatBtnW, 18, displayText);
            this.addButton(fmtBtn, new FormatButtonListener(this, fmt));
            formatBtnX += formatBtnW + GAP;
        }
        y += ROW_HEIGHT;

        // 保存目录显示
        this.addLabel(MARGIN_X, y + 6, LABEL_WIDTH, 12, 0xFFAAAAAA,
                StringUtils.translate("wegui.schematic.save_dir"));
        String dirText = SchematicExporter.getSchematicsDir().toString();
        this.addLabel(controlX, y + 6, controlW, 12, 0xFFCCCCCC, dirText);
        y += ROW_HEIGHT;

        // 底部按钮
        addBottomButtons();

        // 状态初始检查
        if (!LitematicaBridge.hasExportableSchematic()) {
            statusMessage = StringUtils.translate("wegui.schematic.no_schematic");
            statusColor = 0xFFFFAA00;
        }
    }

    private String suggestDefaultName() {
        return "wegui_export_" + System.currentTimeMillis();
    }

    private void addBottomButtons() {
        int bw = 80;
        ButtonGeneric save = new ButtonGeneric(this.width / 2 - bw - 6, this.height - 30, bw, 20,
                StringUtils.translate("wegui.schematic.save"));
        this.addButton(save, (btn, mouseButton) -> doSave());

        ButtonGeneric back = new ButtonGeneric(this.width / 2 + 6, this.height - 30, bw, 20,
                StringUtils.translate("wegui.schematic.cancel"));
        this.addButton(back, (btn, mouseButton) -> this.mc.setScreen(parent));
    }

    private void doSave() {
        if (!LitematicaBridge.hasExportableSchematic()) {
            statusMessage = StringUtils.translate("wegui.schematic.no_schematic");
            statusColor = 0xFFFF5555;
            return;
        }
        String fileName = fileNameField.getValue().trim();
        if (fileName.isEmpty()) {
            statusMessage = StringUtils.translate("wegui.schematic.empty_name");
            statusColor = 0xFFFF5555;
            return;
        }
        // 移除可能存在的扩展名，避免重复
        for (SchematicExporter.Format fmt : SchematicExporter.Format.values()) {
            String ext = fmt.getExtension();
            if (fileName.endsWith(ext)) {
                fileName = fileName.substring(0, fileName.length() - ext.length());
                break;
            }
        }
        Path dir = SchematicExporter.getSchematicsDir();
        boolean ok = SchematicExporter.save(LitematicaBridge.getCurrentSchematic(), dir, fileName, selectedFormat);
        if (ok) {
            statusMessage = Component.translatable("wegui.schematic.saved",
                    dir.resolve(fileName + selectedFormat.getExtension()).toString()).getString();
            statusColor = 0xFF55FF55;
            WeGuiMod.LOGGER.info("[WeGui] 原理图已保存: {}/{}", dir, fileName + selectedFormat.getExtension());
        } else {
            statusMessage = StringUtils.translate("wegui.schematic.save_failed");
            statusColor = 0xFFFF5555;
        }
    }

    @Override
    public void drawContents(GuiContext ctx, int mouseX, int mouseY, float partialTick) {
        if (!statusMessage.isEmpty()) {
            this.drawString(ctx, statusMessage, MARGIN_X, this.height - 50, statusColor);
        }
        // 当无可用 schematic 时显示提示
        if (!LitematicaBridge.hasExportableSchematic()) {
            String hint = StringUtils.translate("wegui.schematic.hint_no_schematic");
            this.drawString(ctx, "§7" + hint, MARGIN_X, CONTENT_TOP + ROW_HEIGHT * 3 + 4, 0xFFFFFFFF);
        }
    }

    private record FormatButtonListener(SaveSchematicScreen screen, SchematicExporter.Format format)
            implements IButtonActionListener {
        @Override
        public void actionPerformedWithButton(fi.dy.masa.malilib.gui.button.ButtonBase button, int mouseButton) {
            screen.selectedFormat = format;
            screen.initGui();
        }
    }
}
