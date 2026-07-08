package com.sow.wegui;

/**
 * WorldEdit 状态快照。
 */
public record WeStatusSnapshot(
        StatusType type,
        int width, int height, int length,
        long blockCount,
        boolean hasClipboard,
        String worldEditVersion,
        String minPos,
        String maxPos
) {
    public enum StatusType { NO_WORLDEDIT, NO_SELECTION, READY }

    public static WeStatusSnapshot noWorldEdit() {
        return new WeStatusSnapshot(StatusType.NO_WORLDEDIT, 0, 0, 0, 0, false, "", "-", "-");
    }

    public static WeStatusSnapshot noSelection(boolean clipboard, String version) {
        return new WeStatusSnapshot(StatusType.NO_SELECTION, 0, 0, 0, 0, clipboard, version == null ? "" : version, "-", "-");
    }

    public static WeStatusSnapshot ready(int w, int h, int l, long count, boolean clipboard, String version, String min, String max) {
        return new WeStatusSnapshot(StatusType.READY, w, h, l, count, clipboard, version == null ? "" : version, min, max);
    }

    /**
     * 用占位符格式化：
     *   {size} {count} {width} {height} {length}
     *   {clipboard} {version}
     *   {min} {max}
     */
    public String format(String pattern) {
        if (pattern == null || pattern.isBlank()) return "";
        String size, count;
        if (type == StatusType.NO_WORLDEDIT) {
            size = "§c未加载";
            count = "-";
        } else if (type == StatusType.NO_SELECTION) {
            size = "§8无选区";
            count = "-";
        } else {
            size = width + "×" + height + "×" + length;
            count = formatCount(blockCount);
        }
        return pattern
                .replace("{width}", String.valueOf(width))
                .replace("{height}", String.valueOf(height))
                .replace("{length}", String.valueOf(length))
                .replace("{size}", size)
                .replace("{count}", count)
                .replace("{clipboard}", hasClipboard ? "§a有" : "§8无")
                .replace("{version}", worldEditVersion == null ? "" : worldEditVersion)
                .replace("{min}", minPos == null ? "-" : minPos)
                .replace("{max}", maxPos == null ? "-" : maxPos);
    }

    private static String formatCount(long c) {
        if (c >= 1_000_000) return String.format("%.1fM", c / 1_000_000.0);
        if (c >= 1_000)     return String.format("%.1fK", c / 1_000.0);
        return String.valueOf(c);
    }
}
