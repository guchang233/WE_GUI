package com.sow.wegui.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

/**
 * 粘贴预览的放置模式：
 * - FOLLOW_PLAYER：预览跟随玩家移动（默认行为）
 * - FIXED：将预览固定在当前位置并真正执行 WE paste，类似投影模组放置原理图
 */
public enum PastePlacementMode implements IConfigOptionListEntry {
    FOLLOW_PLAYER("follow_player"),
    FIXED("fixed");

    private final String name;

    PastePlacementMode(String name) {
        this.name = name;
    }

    @Override
    public String getStringValue() {
        return this.name;
    }

    @Override
    public String getDisplayName() {
        return StringUtils.translate("wegui.config.paste_placement_mode." + this.name);
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
    public PastePlacementMode fromString(String value) {
        for (PastePlacementMode val : values()) {
            if (val.name.equalsIgnoreCase(value)) {
                return val;
            }
        }
        return FOLLOW_PLAYER;
    }
}
