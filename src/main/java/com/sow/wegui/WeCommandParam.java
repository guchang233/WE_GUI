package com.sow.wegui;

import java.util.List;

/**
 * 命令参数定义。
 *
 * @param name         参数名（显示用）
 * @param paramType    参数类型
 * @param optional     是否可选
 * @param defaultValue 默认值（可选时用于占位）
 * @param hint         输入提示 / 示例
 * @param description  参数详细说明（显示在输入框左侧）
 * @param options      当 paramType 为 ENUM 时的可选项
 */
public record WeCommandParam(
        String name,
        WeParamType paramType,
        boolean optional,
        String defaultValue,
        String hint,
        String description,
        List<String> options
) {
    public WeCommandParam(String name, WeParamType paramType) {
        this(name, paramType, false, null, null, "", List.of());
    }

    public WeCommandParam(String name, WeParamType paramType, boolean optional) {
        this(name, paramType, optional, null, null, "", List.of());
    }

    /**
     * 带默认值的可选参数。
     */
    public WeCommandParam(String name, WeParamType paramType, String defaultValue, boolean optional) {
        this(name, paramType, optional, defaultValue, null, "", List.of());
    }

    /**
     * 带默认值的必填参数。
     */
    public WeCommandParam(String name, WeParamType paramType, String defaultValue) {
        this(name, paramType, false, defaultValue, null, "", List.of());
    }

    /**
     * 带默认值、可选性、提示的参数。
     */
    public WeCommandParam(String name, WeParamType paramType, boolean optional, String defaultValue, String hint) {
        this(name, paramType, optional, defaultValue, hint, "", List.of());
    }

    /**
     * 完整参数定义，包含详细说明。
     */
    public WeCommandParam(String name, WeParamType paramType, boolean optional, String defaultValue, String hint, String description) {
        this(name, paramType, optional, defaultValue, hint, description, List.of());
    }

    public WeCommandParam(String name, WeParamType paramType, List<String> options) {
        this(name, paramType, false, null, null, "", options);
    }

    public WeCommandParam(String name, WeParamType paramType, List<String> options, String description) {
        this(name, paramType, false, null, null, description, options);
    }

    /**
     * 链式设置描述，便于在注册代码中追加说明。
     */
    public WeCommandParam withDescription(String description) {
        return new WeCommandParam(name, paramType, optional, defaultValue, hint, description, options);
    }
}
