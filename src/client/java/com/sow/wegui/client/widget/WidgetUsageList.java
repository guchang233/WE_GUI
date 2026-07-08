package com.sow.wegui.client.widget;

import com.sow.wegui.WeCommandUsage;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * 可滚动的用法变体列表控件。
 */
public final class WidgetUsageList extends WidgetListBase<WeCommandUsage, WidgetUsageEntry> {
    private final List<WeCommandUsage> usages;
    private final Consumer<WeCommandUsage> onClick;

    public WidgetUsageList(int x, int y, int width, int height,
                           ISelectionListener<WeCommandUsage> listener,
                           List<WeCommandUsage> usages,
                           Consumer<WeCommandUsage> onClick) {
        super(x, y, width, height, listener);
        this.usages = usages;
        this.onClick = onClick;
        this.shouldSortList = false;
        this.allowKeyboardNavigation = true;
        this.entryHeight = 22;
    }

    @Override
    protected Collection<WeCommandUsage> getAllEntries() {
        return usages;
    }

    @Override
    protected WidgetUsageEntry createListEntryWidget(int x, int y, int listIndex,
                                                     boolean isOdd, WeCommandUsage entry) {
        return new WidgetUsageEntry(x, y, this.browserEntryWidth, entry, listIndex, onClick);
    }
}
