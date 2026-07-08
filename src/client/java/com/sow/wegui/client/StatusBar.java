package com.sow.wegui.client;

import com.sow.wegui.ModConfig;
import com.sow.wegui.WeStatusSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * WE 选区状态指示器 — MiniHUD 风格极简面板。
 *
 * 显示规则：
 *   - 仅在玩家手持触发工具时显示
 *   - 仅在无 Screen 打开时显示（避免遮挡 GUI）
 *   - 支持四角锚点 + XY 偏移 + 自定义格式
 */
public final class StatusBar {
    private static WeStatusSnapshot cached = WeStatusSnapshot.noWorldEdit();
    private static int tickCounter = 0;

    private StatusBar() {}

    public static WeStatusSnapshot getCached() { return cached; }

    public static void tick(Minecraft mc) {
        if (mc.player == null) return;
        if (++tickCounter % 10 != 0) return;
        cached = WorldEditBridge.capture(mc);
    }

    public static void render(GuiGraphics g, Font font, int sw, int sh) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        if (!ToolChecker.isHoldingTriggerTool(mc.player)) return;

        ModConfig.Anchor anchor = ModConfig.get().statusBarAnchor();
        int ox = ModConfig.get().statusBarOffsetX;
        int oy = ModConfig.get().statusBarOffsetY;

        String line1 = cached.format(ModConfig.get().statusBarLine1);
        String line2 = cached.format(ModConfig.get().statusBarLine2);

        int w1 = font.width(line1), w2 = font.width(line2);
        int pw = Math.max(w1, w2) + 8;
        int ph = line2.isEmpty() ? 12 : 22;

        int x = switch (anchor) {
            case TOP_LEFT, BOTTOM_LEFT   -> 4 + ox;
            case TOP_RIGHT, BOTTOM_RIGHT -> sw - pw - 4 - ox;
        };
        int y = switch (anchor) {
            case TOP_LEFT, TOP_RIGHT     -> 4 + oy;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> sh - ph - 4 - oy;
        };

        if (x + pw < 0 || x > sw || y + ph < 0 || y > sh) return;

        g.fill(x, y, x + pw, y + ph, 0x55000000);
        g.fill(x, y, x + 1, y + ph, 0xFF4A90D9);
        g.drawString(font, line1, x + 4, y + 2, 0xFFFFFF);
        if (!line2.isEmpty()) g.drawString(font, line2, x + 4, y + 12, 0xAAAAAA);
    }
}