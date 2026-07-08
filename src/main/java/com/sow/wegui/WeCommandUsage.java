package com.sow.wegui;

import java.util.List;

/**
 * 命令的一个具体用法变体。
 *
 * @param id          用法 ID
 * @param template    命令模板（如 "//flip [direction]"）
 * @param baseCommand 基础命令部分（如 "//flip"）
 * @param description 用法描述
 * @param params      参数定义列表
 * @param type        交互类型
 */
public record WeCommandUsage(
        String id,
        String template,
        String baseCommand,
        String description,
        List<WeCommandParam> params,
        WeCommandType type
) {
    public WeCommandUsage {
        params = params == null ? List.of() : List.copyOf(params);
    }

    public List<WeCommandParam> params() {
        return params;
    }

    /**
     * 根据用户输入的值组装最终命令字符串。
     */
    public String buildCommand(List<String> values) {
        StringBuilder sb = new StringBuilder(baseCommand);
        for (int i = 0; i < params.size(); i++) {
            WeCommandParam param = params.get(i);
            String value = i < values.size() ? values.get(i) : null;
            if (value == null || value.isBlank()) {
                value = param.defaultValue();
            }
            if (value == null || value.isBlank()) {
                if (param.optional()) continue;
                value = "";
            }
            if (param.paramType() == WeParamType.FIXED) {
                sb.append(' ').append(value);
            } else {
                sb.append(' ').append(value);
            }
        }
        return sb.toString().trim();
    }

    /**
     * 获取完整模板字符串用于展示。
     */
    public String displayTemplate() {
        return template.isBlank() ? baseCommand : template;
    }
}
