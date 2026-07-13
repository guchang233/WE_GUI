package com.sow.wegui.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

/**
 * 渲染风格预设，可一键切换一组配色和渲染参数。
 * - DEFAULT：WeGui 原始风格（黄色选区框、绿色半透明面）
 * - LITEMATICA：还原 Litematica 投影模组风格（白色选区框、青色半透明面、每方块边框开启）
 */
public enum RenderStylePreset implements IConfigOptionListEntry {
    DEFAULT("default"),
    LITEMATICA("litematica");

    private final String name;

    RenderStylePreset(String name) {
        this.name = name;
    }

    @Override
    public String getStringValue() {
        return this.name;
    }

    @Override
    public String getDisplayName() {
        return StringUtils.translate("wegui.config.render_style_preset." + this.name);
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
    public RenderStylePreset fromString(String value) {
        for (RenderStylePreset val : values()) {
            if (val.name.equalsIgnoreCase(value)) {
                return val;
            }
        }
        return DEFAULT;
    }
}
