package com.sow.wegui.client.screen;

import com.sow.wegui.commands.WeCommands;
import com.sow.wegui.commands.WeCommands.Category;
import com.sow.wegui.commands.WeCommands.Command;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * malilib 命令列表控件，按当前选中的分类过滤 WorldEdit 命令。
 * 每个列表条目容纳两列命令按钮。
 */
public class WidgetListCommands extends WidgetListBase<CommandRow, WidgetCommandEntry> {
    private final WeCommandScreen parent;

    public WidgetListCommands(int x, int y, int width, int height, WeCommandScreen parent) {
        super(x, y, width, height, parent);
        this.parent = parent;
        this.entryHeight = 26;
        this.allowMultiSelection = false;
        this.shouldSortList = false;
        this.allowKeyboardNavigation = false;
    }

    @Override
    protected Collection<CommandRow> getAllEntries() {
        WeCommands.init();
        Category category = parent.getSelectedCategory();
        List<Command> commands = category == null
                ? new ArrayList<>(WeCommands.all())
                : new ArrayList<>(WeCommands.byCategory(category));

        List<CommandRow> rows = new ArrayList<>();
        for (int i = 0; i < commands.size(); i += 2) {
            Command left = commands.get(i);
            Command right = i + 1 < commands.size() ? commands.get(i + 1) : null;
            rows.add(new CommandRow(left, right));
        }
        return rows;
    }

    @Override
    protected WidgetCommandEntry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, CommandRow entry) {
        return new WidgetCommandEntry(x, y, this.browserEntryWidth, this.entryHeight, entry, listIndex, parent);
    }
}
