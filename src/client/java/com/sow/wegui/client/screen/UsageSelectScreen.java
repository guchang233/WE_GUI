package com.sow.wegui.client.screen;

import com.sow.wegui.commands.WeCommands.Command;
import com.sow.wegui.commands.WeCommands.Usage;
import com.sow.wegui.client.CommandSender;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 为拥有多个用法的命令选择具体变体。
 */
public class UsageSelectScreen extends BaseScreen {
    private final Screen parent;
    private final Command command;

    public UsageSelectScreen(Screen parent, Command command) {
        super(Component.literal(command.displayName()));
        this.parent = parent;
        this.command = command;
    }

    @Override
    protected void init() {
        super.init();

        addRenderableWidget(Button.builder(
                        Component.literal("< 返回"),
                        b -> minecraft.setScreen(parent))
                .bounds(10, 5, 60, 18)
                .build());

        List<Usage> usages = command.usages();
        int y = 55;
        for (Usage usage : usages) {
            String label = usage.description();
            if (label == null || label.isBlank()) label = usage.displayTemplate();
            Button btn = Button.builder(
                            Component.literal(label),
                            b -> onUsage(usage))
                    .bounds(20, y, width - 40, 20)
                    .build();
            btn.setTooltip(Tooltip.create(Component.literal(usage.displayTemplate())));
            addRenderableWidget(btn);
            y += 26;
        }
    }

    private void onUsage(Usage usage) {
        if (usage.params().isEmpty()) {
            CommandSender.send(usage.buildCommand(List.of()));
            minecraft.setScreen(null);
        } else {
            minecraft.setScreen(new ParamInputScreen(parent, command, usage));
        }
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawString(font, Component.literal(command.description()), 20, 32, 0xFFAAAAAA);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
