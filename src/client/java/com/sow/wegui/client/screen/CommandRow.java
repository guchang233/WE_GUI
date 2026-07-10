package com.sow.wegui.client.screen;

import com.sow.wegui.commands.WeCommands.Command;

/**
 * 紧凑命令列表中的一行，对应一个命令。
 */
public record CommandRow(Command command) {
}
