package com.sow.wegui;

/**
 * 命令交互类型。
 * INSTANT     → 无需参数，直接执行
 * PARAMETRIC  → 需要参数，点击后弹出参数输入界面
 * BIND        → 工具/笔刷绑定命令
 */
public enum WeCommandType {
    INSTANT,
    PARAMETRIC,
    BIND
}