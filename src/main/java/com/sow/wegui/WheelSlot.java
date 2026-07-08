package com.sow.wegui;

import com.google.gson.annotations.SerializedName;

import java.util.Optional;

/**
 * 轮盘槽位 — 绑定一条 WE 命令，可选参数覆盖。
 */
public final class WheelSlot {
    @SerializedName("command_id")
    private String commandId = "";
    @SerializedName("overrides")
    private String overrides = null;

    public WheelSlot() {}
    public WheelSlot(String commandId) { this.commandId = commandId; }
    public WheelSlot(String commandId, String overrides) { this.commandId = commandId; this.overrides = overrides; }

    public String commandId() { return commandId; }
    public void setCommandId(String id) { this.commandId = id; }

    public Optional<String> overridesOpt() {
        return Optional.ofNullable(overrides).filter(s -> !s.isBlank());
    }

    /**
     * 解析为最终命令字符串。
     * overrides 追加到完整命令末尾，不破坏多词命令。
     * 如 "//schematic load" + "mybuild" → "//schematic load mybuild"
     */
    public String resolveCommand() {
        return WeCommandRegistry.getById(commandId)
                .map(cmd -> {
                    String base = cmd.command();
                    return overridesOpt().map(ov -> base + " " + ov.trim()).orElse(base);
                })
                .orElse("");
    }
}