package com.sow.wegui.client;

import com.sow.wegui.WeCommand;
import com.sow.wegui.WeCommandType;
import com.sow.wegui.WeCommandUsage;
import com.sow.wegui.client.widget.WidgetUsageEntry;
import com.sow.wegui.client.widget.WidgetUsageList;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 用法变体选择界面 —— 当一条命令有多个用法时列出供选择。
 */
public final class UsageSelectionScreen extends GuiListBase<WeCommandUsage, WidgetUsageEntry, WidgetUsageList> {
    private static final int TOP_MARGIN = 40;
    private static final int BOTTOM_MARGIN = 36;

    private final WeCommand command;

    public UsageSelectionScreen(WeCommand command) {
        super(10, TOP_MARGIN);
        this.command = command;
        this.setTitle(command.displayName() + " - 选择用法");
    }

    @Override
    protected WidgetUsageList createListWidget(int x, int y) {
        return new WidgetUsageList(x, y, getBrowserWidth(), getBrowserHeight(),
                getSelectionListener(), command.usages(), this::onUsageClicked);
    }

    @Override
    protected int getBrowserWidth() {
        int sw = this.getScreenWidth();
        return Math.max(200, Math.min(520, sw - 40));
    }

    @Override
    protected int getBrowserHeight() {
        return Math.max(80, this.getScreenHeight() - TOP_MARGIN - BOTTOM_MARGIN);
    }

    @Override
    public void initGui() {
        super.initGui();

        int sw = this.getScreenWidth();
        int sh = this.getScreenHeight();
        int cy = sw / 2;
        int y = sh - 26;

        ButtonGeneric back = new ButtonNoScroll(cy - 80, y, 75, 18, "返回");
        ButtonGeneric close = new ButtonNoScroll(cy + 5, y, 75, 18, "关闭");

        this.addButton(back, (btn, mb) -> GuiBase.openGui(new CommandCategoryScreen(command.category())));
        this.addButton(close, (btn, mb) -> Minecraft.getInstance().setScreen(null));
    }

    @Override
    protected ISelectionListener<WeCommandUsage> getSelectionListener() {
        return null;
    }

    private void onUsageClicked(WeCommandUsage usage) {
        Minecraft mc = Minecraft.getInstance();
        if (usage.type() == WeCommandType.INSTANT && usage.params().isEmpty()) {
            WeOperation op = WeOperationRegistry.get(usage.id());
            if (op != null) {
                op.execute(mc, usage, List.of());
            } else {
                error("操作未实现: " + usage.id());
            }
            mc.setScreen(null);
        } else {
            GuiBase.openGui(new ParamInputScreen(command, usage));
        }
    }

    private void error(String msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("§c[WE GUI] " + msg), false);
        }
    }
}
