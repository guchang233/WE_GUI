package com.sow.wegui;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * 一个轮盘（一组命令的集合）。
 */
public final class WheelProfile {
    @SerializedName("name")
    private String name = "未命名轮盘";
    @SerializedName("color")
    private int color = 0x4A90D9;
    @SerializedName("slots")
    private List<WheelSlot> slots = new ArrayList<>();

    public WheelProfile() {}
    public WheelProfile(String name) { this.name = name; }
    public WheelProfile(String name, int color) { this.name = name; this.color = color; }

    public String name() { return name; }
    public void setName(String n) { this.name = n; }

    public int color() { return color; }
    public void setColor(int c) { this.color = c; }

    public int slotCount() { return slots.size(); }

    public WheelSlot getSlot(int index) {
        if (index >= 0 && index < slots.size()) return slots.get(index);
        return null;
    }

    public void setSlot(int index, WheelSlot slot) {
        if (index >= 0 && index < slots.size()) slots.set(index, slot);
    }

    public void ensureSlotCount(int count) {
        while (slots.size() < count) slots.add(new WheelSlot());
        while (slots.size() > count) slots.remove(slots.size() - 1);
    }

    /** 通过命令 ID 添加槽位（方便预设工厂使用） */
    public void addSlot(String commandId) {
        slots.add(new WheelSlot(commandId));
    }

    public List<WheelSlot> slots() { return slots; }
}