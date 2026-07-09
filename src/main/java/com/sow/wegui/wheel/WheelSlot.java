package com.sow.wegui.wheel;

import java.util.ArrayList;
import java.util.List;

/**
 * 轮盘上的一个槽位。
 */
public class WheelSlot {
    private String commandId = "";
    private String displayName = "";
    private final List<String> overrides = new ArrayList<>();

    public String getCommandId() {
        return commandId;
    }

    public void setCommandId(String commandId) {
        this.commandId = commandId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<String> getOverrides() {
        return overrides;
    }

    public void setOverrides(List<String> overrides) {
        this.overrides.clear();
        if (overrides != null) this.overrides.addAll(overrides);
    }
}
