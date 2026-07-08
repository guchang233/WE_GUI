package com.sow.wegui;

/**
 * 命令参数类型。
 * UI 层根据类型渲染不同的输入控件。
 */
public enum WeParamType {
    /** 方块/图案表达式，如 minecraft:stone、50%stone,50%cobblestone、#clipboard */
    PATTERN("图案"),
    /** 掩码表达式，如 !air、stone、>water */
    MASK("掩码"),
    /** 基本方向：north/south/east/west/up/down */
    DIRECTION("方向"),
    /** 基于玩家朝向的方向：forward/back/left/right */
    RELATIVE_DIRECTION("相对方向"),
    /** 任意轴向，如 up/down/north/forward/0,1,0 */
    AXIS("轴向"),
    /** 整数 */
    INTEGER("整数"),
    /** 小数 */
    DECIMAL("小数"),
    /** 布尔开关，渲染为 true/false 或省略 */
    BOOLEAN("开关"),
    /** 字符串 */
    STRING("文本"),
    /** 玩家名 */
    PLAYER("玩家"),
    /** 文件名 / schematic 名 */
    FILENAME("文件名"),
    /** 预定义枚举 */
    ENUM("选项"),
    /** 命令自身携带的固定后缀（不渲染输入） */
    FIXED("固定");

    private final String displayName;

    WeParamType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
