package com.sow.wegui.client.screen;

import com.sow.wegui.commands.WeCommands;
import com.sow.wegui.commands.WeCommands.Category;
import com.sow.wegui.commands.WeCommands.Command;
import com.sow.wegui.client.CommandHistory;
import com.sow.wegui.client.util.CommandMatcher;
import com.sow.wegui.config.Configs;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * malilib 命令列表控件，按当前选中的分类/收藏/最近使用/搜索词过滤 WorldEdit 命令。
 * 每个列表条目只显示一个命令（紧凑列表）。
 */
public class WidgetListCommands extends WidgetListBase<CommandRow, WidgetCommandEntry> {
    private final WeCommandScreen parent;

    public WidgetListCommands(int x, int y, int width, int height, WeCommandScreen parent) {
        super(x, y, width, height, parent);
        this.parent = parent;
        this.entryHeight = Configs.CommandPanel.COMPACT_MODE.getBooleanValue() ? 20 : 22;
        this.allowMultiSelection = false;
        this.shouldSortList = false;
        this.allowKeyboardNavigation = false;
    }

    @Override
    protected Collection<CommandRow> getAllEntries() {
        WeCommands.init();

        List<Command> source;
        WeCommandScreen.FilterMode mode = parent.getSelectedFilterMode();

        switch (mode) {
            case FAVORITES -> {
                List<String> ids = CommandHistory.getFavorites();
                source = new ArrayList<>();
                for (String id : ids) {
                    Command cmd = WeCommands.get(id);
                    if (cmd != null) source.add(cmd);
                }
            }
            case RECENT -> {
                List<String> ids = CommandHistory.getRecent();
                source = new ArrayList<>();
                for (String id : ids) {
                    Command cmd = WeCommands.get(id);
                    if (cmd != null) source.add(cmd);
                }
            }
            default -> {
                Category category = parent.getSelectedCategory();
                source = category == null
                        ? new ArrayList<>(WeCommands.all())
                        : new ArrayList<>(WeCommands.byCategory(category));
            }
        }

        String query = parent.getSearchQuery();
        List<CommandRow> rows = new ArrayList<>();
        for (Command cmd : source) {
            if (CommandMatcher.matches(cmd, query)) {
                rows.add(new CommandRow(cmd));
            }
        }
        return rows;
    }

    @Override
    protected WidgetCommandEntry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, CommandRow entry) {
        return new WidgetCommandEntry(x, y, this.browserEntryWidth, this.entryHeight, entry, listIndex, parent);
    }
}
