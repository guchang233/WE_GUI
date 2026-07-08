package com.sow.wegui.client;

import com.sow.wegui.WeCommand;
import com.sow.wegui.WeCommandCategory;
import com.sow.wegui.WeCommandRegistry;
import com.sow.wegui.WeCommandType;
import com.sow.wegui.WeCommandUsage;
import com.sow.wegui.client.widget.WidgetCommandEntry;
import com.sow.wegui.client.widget.WidgetCommandList;
import com.sow.wegui.config.WeGuiConfigs;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.render.GuiContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * WorldEdit GUI 主浏览器 —— 顶部按分类显示标签页，下方列出该分类的命令。
 * 结构类似 Litematica / Tweakeroo / MiniHUD 的配置界面。
 */
public final class MainPanelScreen extends GuiListBase<WeCommand, WidgetCommandEntry, WidgetCommandList> {
    private static final int TOP_MARGIN = 62;
    private static final int BOTTOM_MARGIN = 36;
    private static final int TAB_H = 18;
    private static final int TAB_PAD = 4;

    private final WeCommandCategory currentCategory;

    public MainPanelScreen() {
        this(WeCommandCategory.GENERAL);
    }

    public MainPanelScreen(WeCommandCategory category) {
        super(10, TOP_MARGIN);
        this.currentCategory = category;
        this.setTitle("WorldEdit GUI");
    }

    @Override
    public void initGui() {
        super.initGui();
        createCategoryTabs();
        createBottomButtons();
    }

    private void createCategoryTabs() {
        int sw = this.getScreenWidth();
        int x = 10;
        int y = 32;

        for (WeCommandCategory cat : WeCommandCategory.values()) {
            int count = WeCommandRegistry.getByCategory(cat).size();
            if (count == 0) continue;

            String label = cat.getDisplayName();
            boolean active = cat == currentCategory;
            int w = this.font.width(label) + 14;

            // 当前分类使用高亮样式
            ButtonGeneric tab = new ButtonNoScroll(x, y, w, TAB_H, active ? "§n" + label : label);
            this.addButton(tab, (btn, mb) -> {
                if (cat != currentCategory) {
                    GuiBase.openGui(new MainPanelScreen(cat));
                }
            });
            x += w + TAB_PAD;

            // 如果一行放不下，换行
            if (x + w > sw - 10) {
                x = 10;
                y += TAB_H + TAB_PAD;
            }
        }
    }

    private void createBottomButtons() {
        int sw = this.getScreenWidth();
        int sh = this.getScreenHeight();
        int cx = sw / 2;
        int y = sh - 26;

        ButtonGeneric close = new ButtonNoScroll(cx - 40, y, 80, 18, "关闭");
        this.addButton(close, (btn, mb) -> Minecraft.getInstance().setScreen(null));
    }

    @Override
    protected WidgetCommandList createListWidget(int x, int y) {
        return new WidgetCommandList(x, y, getBrowserWidth(), getBrowserHeight(),
                getSelectionListener(), WeCommandRegistry.getByCategory(currentCategory), this::onCommandClicked);
    }

    @Override
    protected int getBrowserWidth() {
        int sw = this.getScreenWidth();
        return Math.max(200, Math.min(560, sw - 40));
    }

    @Override
    protected int getBrowserHeight() {
        return Math.max(80, this.getScreenHeight() - TOP_MARGIN - BOTTOM_MARGIN);
    }

    @Override
    protected ISelectionListener<WeCommand> getSelectionListener() {
        return null;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        super.render(g, mouseX, mouseY, partialTicks);

        GuiContext ctx = GuiContext.fromGuiGraphics(g);
        int cx = this.getScreenWidth() / 2;
        this.drawString(ctx, "§7触发物品: §f" + WeGuiConfigs.getTriggerItem(), cx - 80, this.getScreenHeight() - 48, 0xFFAAAAAA);
    }

    private void onCommandClicked(WeCommand cmd) {
        Minecraft mc = Minecraft.getInstance();
        if (cmd.usages().size() == 1) {
            WeCommandUsage usage = cmd.usages().get(0);
            if (usage.type() == WeCommandType.INSTANT) {
                executeInstant(usage);
            } else {
                GuiBase.openGui(new ParamInputScreen(cmd, usage));
            }
        } else {
            GuiBase.openGui(new UsageSelectionScreen(cmd));
        }
    }

    private void executeInstant(WeCommandUsage usage) {
        Minecraft mc = Minecraft.getInstance();
        WeOperation op = WeOperationRegistry.get(usage.id());
        if (op != null) {
            op.execute(mc, usage, List.of());
        } else {
            error("操作未实现: " + usage.id());
        }
        mc.setScreen(null);
    }

    private void error(String msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("§c[WE GUI] " + msg), false);
        }
    }
}
