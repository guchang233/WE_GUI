package com.sow.wegui.client.util;

import com.sow.wegui.commands.WeCommands;
import com.sow.wegui.commands.WeCommands.Command;

import java.util.Locale;

/**
 * 命令搜索匹配工具：跨 displayName、id、description、aliases、usages 做大小写不敏感子串匹配。
 */
public final class CommandMatcher {
    private CommandMatcher() {}

    public static boolean matches(Command cmd, String query) {
        if (query.isBlank()) return true;
        String q = query.toLowerCase(Locale.ROOT);
        if (cmd.displayName().toLowerCase(Locale.ROOT).contains(q)) return true;
        if (cmd.id().toLowerCase(Locale.ROOT).contains(q)) return true;
        if (cmd.description().toLowerCase(Locale.ROOT).contains(q)) return true;
        for (String alias : cmd.aliases()) {
            if (alias.toLowerCase(Locale.ROOT).contains(q)) return true;
        }
        for (WeCommands.Usage usage : cmd.usages()) {
            if (usage.displayTemplate().toLowerCase(Locale.ROOT).contains(q)) return true;
            if (usage.description() != null && usage.description().toLowerCase(Locale.ROOT).contains(q)) return true;
        }
        return false;
    }
}
