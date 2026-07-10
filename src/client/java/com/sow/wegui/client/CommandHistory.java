package com.sow.wegui.client;

import com.sow.wegui.config.Configs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 命令面板的收藏与最近使用记录。
 * 数据以逗号分隔的字符串形式持久化在 Configs.CommandPanel 中。
 */
public final class CommandHistory {
    private CommandHistory() {}

    private static final String SEPARATOR = ",";

    public static boolean isFavorite(String id) {
        return parseIds(Configs.CommandPanel.FAVORITES.getStringValue()).contains(id);
    }

    public static void toggleFavorite(String id) {
        Set<String> favorites = new LinkedHashSet<>(parseIds(Configs.CommandPanel.FAVORITES.getStringValue()));
        if (favorites.contains(id)) {
            favorites.remove(id);
        } else {
            favorites.add(id);
        }
        Configs.CommandPanel.FAVORITES.setValueFromString(joinIds(favorites));
        Configs.INSTANCE.save();
    }

    public static List<String> getFavorites() {
        return List.copyOf(parseIds(Configs.CommandPanel.FAVORITES.getStringValue()));
    }

    public static void recordRecent(String id) {
        if (id == null || id.isBlank()) return;
        Set<String> recent = new LinkedHashSet<>();
        recent.add(id);
        recent.addAll(parseIds(Configs.CommandPanel.RECENT_COMMANDS.getStringValue()));
        int max = Configs.CommandPanel.MAX_RECENT.getIntegerValue();
        List<String> trimmed = recent.stream().limit(Math.max(0, max)).collect(Collectors.toList());
        Configs.CommandPanel.RECENT_COMMANDS.setValueFromString(joinIds(trimmed));
        Configs.INSTANCE.save();
    }

    public static List<String> getRecent() {
        return List.copyOf(parseIds(Configs.CommandPanel.RECENT_COMMANDS.getStringValue()));
    }

    public static void clearRecent() {
        Configs.CommandPanel.RECENT_COMMANDS.setValueFromString("");
        Configs.INSTANCE.save();
    }

    private static List<String> parseIds(String raw) {
        if (raw == null || raw.isBlank()) return Collections.emptyList();
        return Arrays.stream(raw.split(SEPARATOR))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static String joinIds(Iterable<String> ids) {
        StringBuilder sb = new StringBuilder();
        for (String id : ids) {
            if (id == null || id.isBlank()) continue;
            if (!sb.isEmpty()) sb.append(SEPARATOR);
            sb.append(id.trim());
        }
        return sb.toString();
    }
}
