package com.sow.wegui;

/**
 * WorldEdit 命令分类。
 */
public enum WeCommandCategory {
    GENERAL("通用"),
    NAVIGATION("导航"),
    SELECTION("选区"),
    REGION("区域"),
    GENERATION("生成"),
    CLIPBOARD("剪贴板"),
    TOOL("工具"),
    SUPER_PICK("超级镐"),
    BRUSH("笔刷"),
    BIOME("生物群系"),
    CHUNK("区块"),
    SNAPSHOT("快照"),
    SCRIPT("脚本"),
    UTILITY("实用工具");

    private final String displayName;

    WeCommandCategory(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }
}