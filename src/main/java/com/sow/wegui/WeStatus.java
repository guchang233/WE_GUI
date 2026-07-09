package com.sow.wegui;

/**
 * 从 WorldEdit 读取到的状态快照。
 */
public record WeStatus(
        Type type,
        String version,
        String regionType,
        int width,
        int height,
        int length,
        long blockCount,
        boolean hasClipboard,
        int clipboardWidth,
        int clipboardHeight,
        int clipboardLength
) {
    public enum Type {
        NO_WORLDEDIT,
        NO_SELECTION,
        READY
    }

    public static WeStatus noWorldEdit() {
        return new WeStatus(Type.NO_WORLDEDIT, "", "", 0, 0, 0, 0, false, 0, 0, 0);
    }

    public static WeStatus noSelection(String version, boolean hasClipboard, int cw, int ch, int cl) {
        return new WeStatus(Type.NO_SELECTION, version, "", 0, 0, 0, 0, hasClipboard, cw, ch, cl);
    }

    public static WeStatus ready(String version, String regionType, int w, int h, int l, long count,
                                  boolean hasClipboard, int cw, int ch, int cl) {
        return new WeStatus(Type.READY, version, regionType, w, h, l, count, hasClipboard, cw, ch, cl);
    }

    public String format(String template) {
        if (template == null || template.isBlank()) {
            return defaultText();
        }
        return template
                .replace("{version}", version)
                .replace("{regionType}", regionType)
                .replace("{width}", String.valueOf(width))
                .replace("{height}", String.valueOf(height))
                .replace("{length}", String.valueOf(length))
                .replace("{size}", width + "×" + height + "×" + length)
                .replace("{count}", String.valueOf(blockCount))
                .replace("{clipboard}", hasClipboard ? "有" : "无")
                .replace("{clipboardSize}", clipboardWidth + "×" + clipboardHeight + "×" + clipboardLength);
    }

    private String defaultText() {
        return switch (type) {
            case NO_WORLDEDIT -> "WorldEdit 未加载";
            case NO_SELECTION -> "无选区";
            case READY -> width + "×" + height + "×" + length;
        };
    }
}
