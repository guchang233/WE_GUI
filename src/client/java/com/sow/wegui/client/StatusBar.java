package com.sow.wegui.client;

import com.sow.wegui.WeStatus;
import com.sow.wegui.config.Config;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Items;

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

        renderAxeModeIndicator(g, mc, sw, sh);
    }

    private static void renderAxeModeIndicator(GuiGraphics g, Minecraft mc, int sw, int sh) {
        if (mc.player == null || mc.screen != null) return;
        if (AxeModeHandler.getMode() != AxeModeHandler.AxeMode.EDIT_SELECTION) return;
        if (!mc.player.getMainHandItem().is(Items.WOODEN_AXE)
                && !mc.player.getOffhandItem().is(Items.WOODEN_AXE)) {
            return;
        }

        String text = "§a[WE GUI] §f小木斧: 编辑选区模式 §7(Alt+滚轮移动)";
        int w = mc.font.width(text);
        int x = 4;
        int y = sh - mc.font.lineHeight - 4;
        g.fill(x - 2, y - 2, x + w + 4, y + mc.font.lineHeight + 2, 0xAA000000);
        g.drawString(mc.font, text, x, y, 0xFFFFFFFF);
    }
}
