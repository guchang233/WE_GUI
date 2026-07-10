package com.sow.wegui.client.screen;

import com.sow.wegui.commands.WeCommands.Command;
import com.sow.wegui.commands.WeCommands.Type;
import com.sow.wegui.commands.WeCommands.Usage;
import com.sow.wegui.client.CommandSender;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.Minecraft;

import java.util.Collections;
import java.util.List;

/**
 * malilib 列表条目：一行最多显示两个 WE 命令按钮。
 */
public class WidgetCommandEntry extends WidgetListEntryBase<CommandRow> {
    private static final int GAP = 4;

    public WidgetCommandEntry(int x, int y, int width, int height, CommandRow entry, int listIndex, WeCommandScreen parent) {
        super(x, y, width, height, entry, listIndex);

        int half = (width - GAP) / 2;
        addCommandButton(x, y, half, height, entry.left());
        if (entry.right() != null) {
            addCommandButton(x + half + GAP, y, half, height, entry.right());
        }
    }

    private void addCommandButton(int x, int y, int width, int height, Command command) {
        NoScrollButton button = new NoScrollButton(x, y, width, height, StringUtils.translate(command.displayName()));
        button.setTextCentered(true);
        if (!command.description().isBlank()) {
            button.setHoverStrings(StringUtils.translate(command.description()));
        }
        this.addButton(button, new CommandButtonListener(command));
    }

    private record CommandButtonListener(Command command) implements IButtonActionListener {
        @Override
        public void actionPerformedWithButton(fi.dy.masa.malilib.gui.button.ButtonBase button, int mouseButton) {
            Minecraft mc = Minecraft.getInstance();
            List<Usage> usages = command.usages();
            if (usages.isEmpty()) return;

            if (usages.size() == 1) {
                Usage usage = usages.get(0);
                if (usage.type() == Type.INSTANT) {
                    CommandSender.send(usage.buildCommand(Collections.emptyList()));
                    mc.setScreen(null);
                } else {
                    mc.setScreen(new ParamInputScreen(mc.screen instanceof WeCommandScreen ? mc.screen : null, command, usage));
                }
            } else {
                mc.setScreen(new UsageSelectScreen(mc.screen instanceof WeCommandScreen ? mc.screen : null, command));
            }
        }
    }
}
