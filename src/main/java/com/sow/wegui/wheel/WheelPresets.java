package com.sow.wegui.wheel;

import java.util.ArrayList;
import java.util.List;

/**
 * 轮盘默认预设工厂。
 */
public class WheelPresets {
    public static List<WheelProfile> createDefaults() {
        List<WheelProfile> list = new ArrayList<>();
        list.add(create("剪贴板", 0xFF55AA55, List.of("copy", "cut", "paste", "undo", "redo", "clearclipboard")));
        list.add(create("区域", 0xFFAA5555, List.of("set", "replace", "walls", "outline", "hollow", "smooth", "naturalize", "move")));
        list.add(create("生成", 0xFF55AAFF, List.of("sphere", "hsphere", "cyl", "hcyl", "pyramid", "hpyramid", "cone", "forest")));
        list.add(create("选区", 0xFFFFAA55, List.of("pos1", "pos2", "hpos1", "hpos2", "chunk", "expand", "contract", "outset")));
        list.add(create("导航", 0xFFAA55FF, List.of("up", "ceil", "thru", "jumpto", "ascend", "descend")));
        list.add(create("工具", 0xFF55FFAA, List.of("wand", "toggleeditwand", "farwand", "lrbuild", "tree", "deltree")));
        list.add(create("实用", 0xFFFFFFFF, List.of("calc", "remove", "butcher", "extinguish", "drain", "green")));
        return list;
    }

    private static WheelProfile create(String name, int color, List<String> commandIds) {
        WheelProfile profile = new WheelProfile();
        profile.setName(name);
        profile.setColor(color);
        for (String id : commandIds) {
            WheelSlot slot = new WheelSlot();
            slot.setCommandId(id);
            profile.getSlots().add(slot);
        }
        return profile;
    }
}
