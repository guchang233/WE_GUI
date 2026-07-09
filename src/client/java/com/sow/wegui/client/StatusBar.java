package com.sow.wegui.client;

import com.sow.wegui.WeStatus;
import com.sow.wegui.config.Config;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 在 HUD 上实时显示 WorldEdit 选区状态。
 */
public final class StatusBar {
    private static WeStatus cached = WeStatus.noWorldEdit();

    private StatusBar() {}

    public static void register() {
        HudRenderCallback.EVENT.register((GuiGraphics drawContext, DeltaTracker tickCounter) -> {
            Minecraft mc = Minecraft.getInstance();
            cached = WorldEditBridge.capture(mc);
            render(drawContext, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        });
    }

    private static void render(GuiGraphics g, int sw, int sh) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        Config cfg = Config.get();
        if (!cfg.isStatusBarEnabled()) return;

        Config.Anchor anchor = cfg.getStatusBarAnchor();
        int offsetX = cfg.getStatusBarOffsetX();
        int offsetY = cfg.getStatusBarOffsetY();

        String line1 = cached.format(cfg.getStatusBarLine1());
        String line2 = cached.format(cfg.getStatusBarLine2());

        int w1 = mc.font.width(line1);
        int w2 = mc.font.width(line2);
        int h = mc.font.lineHeight;
        int panelW = Math.max(w1, w2) + 8;
        int panelH = line2.isBlank() ? h + 6 : h * 2 + 8;

        int x = switch (anchor) {
            case TOP_RIGHT, BOTTOM_RIGHT -> sw - panelW - offsetX;
            default -> offsetX;
        };
        int y = switch (anchor) {
            case BOTTOM_LEFT, BOTTOM_RIGHT -> sh - panelH - offsetY;
            default -> offsetY;
        };

        g.fill(x, y, x + panelW, y + panelH, 0xAA000000);
        g.drawString(mc.font, line1, x + 4, y + 3, 0xFFFFFFFF);
        if (!line2.isBlank()) {
            g.drawString(mc.font, line2, x + 4, y + 3 + h + 1, 0xFFAAAAAA);
        }
    }
}
