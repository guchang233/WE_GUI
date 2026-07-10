package com.sow.wegui.client.screen;

import com.sow.wegui.commands.WeCommands.Command;
import com.sow.wegui.commands.WeCommands.Usage;
import com.sow.wegui.client.CommandSender;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

/**
 * 为拥有多个用法的命令选择具体变体（malilib 风格）。
 */
public class UsageSelectScreen extends GuiBase {
    private final Screen parent;
    private final Command command;

    public UsageSelectScreen(Screen parent, Command command) {
        this.setParent(parent);
        this.parent = parent;
        this.command = command;
        this.setTitle(StringUtils.translate(command.displayName()));
    }

    @Override
    public void initGui() {
        super.initGui();

        ButtonGeneric back = new ButtonGeneric(10, 5, 60, 18, StringUtils.translate("wegui.command.back"));
        this.addButton(back, (btn, mouseButton) -> this.mc.setScreen(parent));

        this.addLabel(20, 32, this.width - 40, 12, 0xFFAAAAAA, StringUtils.translate(command.description()));

        List<Usage> usages = command.usages();
        int y = 55;
        int btnW = Math.min(360, this.width - 40);
        int x = (this.width - btnW) / 2;
        for (Usage usage : usages) {
            String label = StringUtils.translate(usage.description());
            if (label == null || label.isBlank()) label = usage.displayTemplate();
            ButtonGeneric btn = new ButtonGeneric(x, y, btnW, 20, label);
            btn.setHoverStrings(usage.displayTemplate());
            this.addButton(btn, new UsageButtonListener(this, usage));
            y += 26;
        }
    }

    @Override
    public void drawContents(GuiContext ctx, int mouseX, int mouseY, float partialTick) {
        // 额外描述已在 initGui 中通过 Label 显示
    }

    private void onUsage(Usage usage) {
        if (usage.params().isEmpty()) {
            CommandSender.send(usage.buildCommand(List.of()));
            this.mc.setScreen(null);
        } else {
            this.mc.setScreen(new ParamInputScreen(parent, command, usage));
        }
    }

    private record UsageButtonListener(UsageSelectScreen screen, Usage usage) implements IButtonActionListener {
        @Override
        public void actionPerformedWithButton(fi.dy.masa.malilib.gui.button.ButtonBase button, int mouseButton) {
            screen.onUsage(usage);
        }
    }
}
