package com.sow.wegui.wheel;

import java.util.ArrayList;
import java.util.List;

/**
 * 一个命令轮盘的配置。
 */
public class WheelProfile {
    private String name = "";
    private int color = 0xFF55AAFF;
    private final List<WheelSlot> slots = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public List<WheelSlot> getSlots() {
        return slots;
    }

    public void resize(int count) {
        while (slots.size() < count) slots.add(new WheelSlot());
        while (slots.size() > count) slots.removeLast();
    }
}
