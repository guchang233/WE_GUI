package com.sow.wegui.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum Anchor implements IConfigOptionListEntry {
    TOP_LEFT("top_left"),
    TOP_RIGHT("top_right"),
    BOTTOM_LEFT("bottom_left"),
    BOTTOM_RIGHT("bottom_right");

    private final String name;

    Anchor(String name) {
        this.name = name;
    }

    @Override
    public String getStringValue() {
        return this.name;
    }

    @Override
    public String getDisplayName() {
        return StringUtils.translate("wegui.config.anchor." + this.name);
    }

    @Override
    public IConfigOptionListEntry cycle(boolean forward) {
        int id = this.ordinal();
        if (forward) {
            if (++id >= values().length) {
                id = 0;
            }
        } else {
            if (--id < 0) {
                id = values().length - 1;
            }
        }
        return values()[id];
    }

    @Override
    public Anchor fromString(String value) {
        for (Anchor val : values()) {
            if (val.name.equalsIgnoreCase(value)) {
                return val;
            }
        }
        return TOP_LEFT;
    }
}
