package com.sow.wegui.client.screen;

import com.sow.wegui.commands.WeCommands;
import com.sow.wegui.commands.WeCommands.Command;
import com.sow.wegui.commands.WeCommands.Type;
import com.sow.wegui.commands.WeCommands.Usage;
import com.sow.wegui.client.CommandHistory;
import com.sow.wegui.client.CommandSender;
import com.sow.wegui.config.Configs;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * malilib 列表条目：紧凑显示一个 WE 命令，左侧收藏星标，右侧命令按钮。
 */
public class WidgetCommandEntry extends WidgetListEntryBase<CommandRow> {
    private static final int STAR_WIDTH = 18;
    private static final int GAP = 2;

    private final WeCommandScreen parent;
    private final Command command;

    public WidgetCommandEntry(int x, int y, int width, int height, CommandRow entry, int listIndex, WeCommandScreen parent) {
        super(x, y, width, height, entry, listIndex);
        this.parent = parent;
        this.command = entry.command();

        int cmdX = x + STAR_WIDTH + GAP;
        int cmdW = width - STAR_WIDTH - GAP;

        addStarButton(x, y, STAR_WIDTH, height);
        addCommandButton(cmdX, y, cmdW, height);
    }

    private void addStarButton(int x, int y, int width, int height) {
        String label = CommandHistory.isFavorite(command.id()) ? "★" : "☆";
        ButtonGeneric btn = new ButtonGeneric(x, y, width, height, label);
        btn.setHoverStrings(StringUtils.translate("wegui.command.favorites"));
        this.addButton(btn, new StarButtonListener(this));
    }

    private void addCommandButton(int x, int y, int width, int height) {
        String label = buildCommandLabel(width);

        NoScrollButton button = new NoScrollButton(x, y, width, height, label);
        button.setTextCentered(false);
        button.setHoverStrings(buildHover());
        this.addButton(button, new CommandButtonListener(parent, command));
    }

    private String buildCommandLabel(int maxWidth) {
        String template = command.usages().isEmpty() ? "" : command.usages().get(0).displayTemplate();
        if (template.isBlank() || !Configs.CommandPanel.SHOW_DESCRIPTION.getBooleanValue()) {
            return command.displayName();
        }

        var font = Minecraft.getInstance().font;
        String display = command.displayName();
        int displayW = font.width(display);
        int available = maxWidth - displayW - font.width("  ");

        if (available <= 8) {
            return display;
        }

        String fullTemplate = "§7" + template;
        if (font.width(fullTemplate) <= available) {
            return display + " " + fullTemplate;
        }

        for (int i = template.length(); i > 0; i--) {
            String candidate = template.substring(0, i) + "…";
            if (font.width("§7" + candidate) <= available) {
                return display + " §7" + candidate;
            }
        }
        return display;
    }

    private List<String> buildHover() {
        List<String> lines = new ArrayList<>();
        if (!command.description().isBlank()) {
            lines.add("§f" + command.description());
        }
        if (command.aliases().length > 0) {
            lines.add("§7别名: " + String.join(", ", command.aliases()));
        }
        if (command.usages().size() > 1) {
            lines.add("§7用法: " + command.usages().size() + " 种");
        }
        for (Usage usage : command.usages()) {
            String desc = usage.description();
            if (desc == null || desc.isBlank()) desc = usage.displayTemplate();
            lines.add("§8- §7" + usage.displayTemplate() + (desc.equals(usage.displayTemplate()) ? "" : " §8(" + desc + ")"));
        }
        return lines.isEmpty() ? Collections.emptyList() : lines;
    }

    private void toggleFavorite() {
        CommandHistory.toggleFavorite(command.id());
        parent.onFavoriteChanged();
    }

    private record StarButtonListener(WidgetCommandEntry entry) implements IButtonActionListener {
        @Override
        public void actionPerformedWithButton(fi.dy.masa.malilib.gui.button.ButtonBase button, int mouseButton) {
            entry.toggleFavorite();
        }
    }

    private record CommandButtonListener(WeCommandScreen parent, Command command) implements IButtonActionListener {
        @Override
        public void actionPerformedWithButton(fi.dy.masa.malilib.gui.button.ButtonBase button, int mouseButton) {
            Minecraft mc = Minecraft.getInstance();
            if (command.usages().isEmpty()) return;

            Usage usage = command.usages().get(0);
            if (command.usages().size() == 1 && usage.type() == Type.INSTANT) {
                CommandSender.send(usage.baseCommand());
                CommandHistory.recordRecent(command.id());
                mc.setScreenAndShow(null);
            } else {
                mc.setScreenAndShow(new ParamInputScreen(parent, command, usage));
            }
        }
    }
}
