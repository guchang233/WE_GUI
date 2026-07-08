package com.sow.wegui.client;

import com.sow.wegui.WeCommand;
import com.sow.wegui.WeCommandCategory;
import com.sow.wegui.WeCommandRegistry;
import com.sow.wegui.WeCommandType;
import com.sow.wegui.WeCommandUsage;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.render.GuiContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/**
 * 分类命令列表 —— 显示某一分类下的所有 WeCommand。
 */
public final class CommandCategoryScreen extends GuiBase {
    private static final int BTN_H = 18;
    private static final int PAD = 4;
    private static final int COLS = 3;

    private final WeCommandCategory category;
    private final List<WeCommand> commands;

    public CommandCategoryScreen(WeCommandCategory category) {
        super();
        this.category = category;
        this.commands = WeCommandRegistry.getByCategory(category);
        this.setTitle(category.getDisplayName());
    }

    @Override
    public void initGui() {
        super.initGui();

        int sw = this.getScreenWidth();
        int sh = this.getScreenHeight();
        int marginX = 20;
        int marginY = 40;
        int bottomH = 30;

        int usableW = sw - marginX * 2;
        int btnW = (usableW - (COLS - 1) * PAD) / COLS;
        int maxRows = Math.max(1, (sh - marginY - bottomH) / (BTN_H + PAD));

        for (int i = 0; i < commands.size(); i++) {
            WeCommand cmd = commands.get(i);
            int col = i % COLS;
            int row = i / COLS;
            int x = marginX + col * (btnW + PAD);
            int y = marginY + row * (BTN_H + PAD);

            String typeMark = cmd.type() == WeCommandType.PARAMETRIC ? "§7[参] "
                    : cmd.type() == WeCommandType.BIND ? "§7[绑] " : "§7[即] ";
            ButtonGeneric btn = new ButtonGeneric(x, y, btnW, BTN_H, cmd.displayName(),
                    typeMark + cmd.description());
            this.addButton(btn, new CommandButtonListener(cmd));
        }

        // 底部按钮
        int cx = sw / 2;
        int bottomY = sh - 24;
        ButtonGeneric backBtn = new ButtonGeneric(cx - 80, bottomY, 75, BTN_H, "返回");
        ButtonGeneric closeBtn = new ButtonGeneric(cx + 5, bottomY, 75, BTN_H, "关闭");
        this.addButton(backBtn, (button, mouseButton) -> this.mc.setScreen(this.getParent()));
        this.addButton(closeBtn, (button, mouseButton) -> this.mc.setScreen(null));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        super.render(g, mouseX, mouseY, partialTicks);

        GuiContext ctx = GuiContext.fromGuiGraphics(g);
        int cx = this.getScreenWidth() / 2;
        String title = "§b§l" + category.getDisplayName();
        this.drawStringWithShadow(ctx, title, cx - this.getStringWidth(title) / 2, 18, 0xFFFFFF);
        this.drawString(ctx, "§7共 " + commands.size() + " 条命令", cx - 40, 32, 0xAAAAAA);
    }

    private record CommandButtonListener(WeCommand command) implements IButtonActionListener {
        @Override
        public void actionPerformedWithButton(fi.dy.masa.malilib.gui.button.ButtonBase button, int mouseButton) {
            Minecraft mc = Minecraft.getInstance();
            if (command.isSingleUsage()) {
                WeCommandUsage usage = command.usages().get(0);
                if (usage.params().isEmpty() && usage.type() == WeCommandType.INSTANT) {
                    executeInstant(mc, usage);
                } else {
                    GuiBase.openGui(new ParamInputScreen(command, usage));
                }
            } else {
                GuiBase.openGui(new UsageSelectionScreen(command));
            }
        }

        private void executeInstant(Minecraft mc, WeCommandUsage usage) {
            WeOperation op = WeOperationRegistry.get(usage.id());
            if (op != null) {
                op.execute(mc, usage, List.of());
            } else {
                if (mc.player != null) {
                    mc.player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "§c[WE GUI] §f该功能尚未通过 API 实现: " + usage.displayTemplate()), false);
                }
            }
            mc.setScreen(null);
        }
    }
}
