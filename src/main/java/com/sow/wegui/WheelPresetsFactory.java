package com.sow.wegui;

import java.util.ArrayList;
import java.util.List;

/**
 * 默认轮盘预设工厂。
 *
 * 生成 7 个分类轮盘，每个轮盘 8 个槽位，填入对应分类最常用的命令。
 */
public final class WheelPresetsFactory {
    private static final int SLOTS = 8;

    private WheelPresetsFactory() {}

    public static List<WheelProfile> createDefaultWheels() {
        List<WheelProfile> wheels = new ArrayList<>();
        wheels.add(clipboardWheel());
        wheels.add(regionWheel());
        wheels.add(generationWheel());
        wheels.add(selectionWheel());
        wheels.add(navigationWheel());
        wheels.add(toolWheel());
        wheels.add(utilityWheel());
        for (WheelProfile w : wheels) w.ensureSlotCount(SLOTS);
        return wheels;
    }

    // 兼容旧调用
    public static List<WheelProfile> createDefaults() { return createDefaultWheels(); }

    private static WheelProfile clipboardWheel() {
        WheelProfile p = new WheelProfile("§b剪贴板", 0x00AAFF);
        p.addSlot("copy");
        p.addSlot("cut");
        p.addSlot("paste");
        p.addSlot("rotate");
        p.addSlot("flip");
        p.addSlot("stack");
        p.addSlot("move");
        p.addSlot("clearclipboard");
        return p;
    }

    private static WheelProfile regionWheel() {
        WheelProfile p = new WheelProfile("§6区域", 0xFFAA00);
        p.addSlot("set");
        p.addSlot("replace");
        p.addSlot("overlay");
        p.addSlot("walls");
        p.addSlot("outline");
        p.addSlot("hollow");
        p.addSlot("smooth");
        p.addSlot("center");
        return p;
    }

    private static WheelProfile generationWheel() {
        WheelProfile p = new WheelProfile("§a生成", 0x55FF55);
        p.addSlot("sphere");
        p.addSlot("hsphere");
        p.addSlot("cyl");
        p.addSlot("hcyl");
        p.addSlot("cone");
        p.addSlot("pyramid");
        p.addSlot("hpyramid");
        p.addSlot("generate");
        return p;
    }

    private static WheelProfile selectionWheel() {
        WheelProfile p = new WheelProfile("§d选区", 0xFF55FF);
        p.addSlot("pos1");
        p.addSlot("pos2");
        p.addSlot("hpos1");
        p.addSlot("hpos2");
        p.addSlot("expand");
        p.addSlot("contract");
        p.addSlot("shift");
        p.addSlot("size");
        return p;
    }

    private static WheelProfile navigationWheel() {
        WheelProfile p = new WheelProfile("§9导航", 0x5555FF);
        p.addSlot("jumpto");
        p.addSlot("thru");
        p.addSlot("ascend");
        p.addSlot("descend");
        p.addSlot("ceil");
        p.addSlot("up");
        p.addSlot("unstuck");
        p.addSlot("wand");
        return p;
    }

    private static WheelProfile toolWheel() {
        WheelProfile p = new WheelProfile("§e工具", 0xFFFF55);
        p.addSlot("brush_sphere");
        p.addSlot("brush_cylinder");
        p.addSlot("brush_smooth");
        p.addSlot("brush_clipboard");
        p.addSlot("tool_repl");
        p.addSlot("tool_tree");
        p.addSlot("tool_cycler");
        p.addSlot("tool_none");
        return p;
    }

    private static WheelProfile utilityWheel() {
        WheelProfile p = new WheelProfile("§7实用", 0xAAAAAA);
        p.addSlot("undo");
        p.addSlot("redo");
        p.addSlot("fill");
        p.addSlot("drain");
        p.addSlot("fixwater");
        p.addSlot("fixlava");
        p.addSlot("snow");
        p.addSlot("thaw");
        return p;
    }
}
