package com.sow.wegui;

import java.util.List;

/**
 * WorldEdit 命令数据模型。
 * 一个逻辑命令包含一个或多个具体用法变体（WeCommandUsage）。
 *
 * @param id          命令 ID
 * @param displayName 显示名称
 * @param description 命令简介
 * @param category    所属分类
 * @param usages      用法变体列表
 */
public record WeCommand(
        String id,
        String displayName,
        String description,
        WeCommandCategory category,
        List<WeCommandUsage> usages
) {
    /**
     * 兼容旧 API：返回默认用法（第一个）。
     */
    public String command() {
        return usages.isEmpty() ? "" : usages.get(0).baseCommand();
    }

    /**
     * 兼容旧 API：返回默认用法的类型。
     */
    public WeCommandType type() {
        return usages.isEmpty() ? WeCommandType.INSTANT : usages.get(0).type();
    }

    /**
     * 是否只有一个用法。
     */
    public boolean isSingleUsage() {
        return usages.size() <= 1;
    }

    /**
     * 获取第一个需要参数或绑定的用法，用于快速判断。
     */
    public boolean needsInput() {
        return usages.stream().anyMatch(u ->
                u.type() == WeCommandType.PARAMETRIC || u.type() == WeCommandType.BIND);
    }
}
