package com.sow.wegui.client.widget;

import com.sow.wegui.WeCommand;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * 可滚动的命令列表控件。
 */
public final class WidgetCommandList extends WidgetListBase<WeCommand, WidgetCommandEntry> {
    private final List<WeCommand> commands;
    private final Consumer<WeCommand> onClick;

    public WidgetCommandList(int x, int y, int width, int height,
                             ISelectionListener<WeCommand> listener,
                             List<WeCommand> commands,
                             Consumer<WeCommand> onClick) {
        super(x, y, width, height, listener);
        this.commands = commands;
        this.onClick = onClick;
        this.shouldSortList = false;
        this.allowKeyboardNavigation = true;
        this.entryHeight = 22;
    }

    @Override
    protected Collection<WeCommand> getAllEntries() {
        return commands;
    }

    @Override
    protected WidgetCommandEntry createListEntryWidget(int x, int y, int listIndex,
                                                       boolean isOdd, WeCommand entry) {
        return new WidgetCommandEntry(x, y, this.browserEntryWidth, entry, listIndex, onClick);
    }
}
