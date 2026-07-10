package com.sow.wegui.client.screen;

import com.sow.wegui.commands.WeCommands.Category;

import javax.annotation.Nullable;

/**
 * 命令面板分类下拉框条目，null 表示“全部”。
 */
public record CategoryEntry(@Nullable Category category, String displayName) {
    public boolean isAll() {
        return category == null;
    }
}
