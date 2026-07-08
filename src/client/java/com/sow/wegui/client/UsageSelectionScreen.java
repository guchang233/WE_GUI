package com.sow.wegui.client;

import com.sow.wegui.WeCommand;
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
 * 用法选择界面 —— 一个命令存在多个用法变体时显示。
 */
public final class UsageSelectionScreen extends GuiBase {
    private static final int BTN_H = 24;
    private static final int PAD = 6;

    private final WeCommand command;
    private final List<WeCommandUsage> usages;

    public UsageSelectionScreen(WeCommand command) {
        super();
        this.command = command;
        this.usages = command.usages();
        this.setTitle(command.displayName() + " — 选择用法");
    }

    @Override
    public void initGui() {
        super.initGui();

        int cx = this.getScreenWidth() / 2;
        int listW = Math.min(this.getScreenWidth() - 40, 520);
        int listX = cx - listW / 2;
        int startY = 70;

        for (int i = 0; i < usages.size(); i++) {
            WeCommandUsage usage = usages.get(i);
            int y = startY + i * (BTN_H + PAD);
            ButtonGeneric btn = new ButtonGeneric(listX, y, listW, BTN_H,
                    usage.displayTemplate(), usage.description());
            this.addButton(btn, new UsageButtonListener(this, command, usage));
        }

        int bottomY = this.getScreenHeight() - 24;
        ButtonGeneric backBtn = new ButtonGeneric(cx - 50, bottomY, 100, BTN_H, "返回");
        this.addButton(backBtn, (button, mouseButton) -> this.mc.setScreen(this.getParent()));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        super.render(g, mouseX, mouseY, partialTicks);

        GuiContext ctx = GuiContext.fromGuiGraphics(g);
        int cx = this.getScreenWidth() / 2;
        String name = "§b§l" + command.displayName();
        this.drawStringWithShadow(ctx, name, cx - this.getStringWidth(name) / 2, 24, 0xFFFFFF);
        this.drawString(ctx, "§7" + command.description(), cx - this.getStringWidth(command.description()) / 2, 40, 0xAAAAAA);
        this.drawString(ctx, "§8选择一种用法继续", cx - 50, 54, 0x888888);
    }

    private record UsageButtonListener(UsageSelectionScreen parent, WeCommand command, WeCommandUsage usage) implements IButtonActionListener {
        @Override
        public void actionPerformedWithButton(fi.dy.masa.malilib.gui.button.ButtonBase button, int mouseButton) {
            Minecraft mc = Minecraft.getInstance();
            WeOperation op = WeOperationRegistry.get(usage.id());
            if (op != null && usage.params().isEmpty() && usage.type() == WeCommandType.INSTANT) {
                op.execute(mc, usage, List.of());
                mc.setScreen(null);
                return;
            }
            if (usage.params().isEmpty() && usage.type() == WeCommandType.INSTANT) {
                if (mc.player != null) {
                    mc.player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "§c[WE GUI] §f该功能尚未通过 API 实现: " + usage.displayTemplate()), false);
                }
                return;
            }
            ParamInputScreen screen = new ParamInputScreen(command, usage);
            screen.setParent(parent);
            GuiBase.openGui(screen);
        }
    }
}
