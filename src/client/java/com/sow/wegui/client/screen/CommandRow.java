package com.sow.wegui.client.screen;

import com.sow.wegui.commands.WeCommands.Command;

import javax.annotation.Nullable;

/**
 * 命令网格中的一行，最多容纳两个命令按钮。
 */
public record CommandRow(@Nullable Command left, @Nullable Command right) {
}
