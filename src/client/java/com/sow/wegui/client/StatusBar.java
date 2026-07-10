package com.sow.wegui.client;

import com.sow.wegui.WeStatus;
import com.sow.wegui.config.Anchor;
import com.sow.wegui.config.Configs;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

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
        ModeIndicator.register();
    }

    private static void render(GuiGraphics g, int sw, int sh) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        if (!Configs.Generic.STATUS_BAR_ENABLED.getBooleanValue()) return;

        Anchor anchor = (Anchor) Configs.StatusBar.STATUS_BAR_ANCHOR.getOptionListValue();
        int offsetX = Configs.StatusBar.STATUS_BAR_OFFSET_X.getIntegerValue();
        int offsetY = Configs.StatusBar.STATUS_BAR_OFFSET_Y.getIntegerValue();

        String line1 = translateStatusPlaceholders(cached.format(Configs.StatusBar.STATUS_BAR_LINE1.getStringValue()));
        String line2 = translateStatusPlaceholders(cached.format(Configs.StatusBar.STATUS_BAR_LINE2.getStringValue()));

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
        g.drawString(mc.font, line1, x + 4, y + 3, 0xFFFFFFFF, false);
        if (!line2.isBlank()) {
            g.drawString(mc.font, line2, x + 4, y + 3 + h + 1, 0xFFAAAAAA, false);
        }

    }

    private static String translateStatusPlaceholders(String text) {
        if (text == null) return "";
        return text
                .replace("{status.no_worldedit}", Component.translatable("wegui.status.no_worldedit").getString())
                .replace("{status.no_selection}", Component.translatable("wegui.status.no_selection").getString())
                .replace("{clipboard.yes}", Component.translatable("wegui.status.yes").getString())
                .replace("{clipboard.no}", Component.translatable("wegui.status.no").getString());
    }
}
