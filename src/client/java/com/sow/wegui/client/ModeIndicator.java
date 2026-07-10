package com.sow.wegui.client;

import com.sow.wegui.config.Configs;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 左下角投影风格的工具模式 HUD 提示。
 * 仅在玩家手持当前配置的 WE 绑定工具时显示。
 */
public final class ModeIndicator {
    private ModeIndicator() {}

    public static void register() {
        HudRenderCallback.EVENT.register(ModeIndicator::render);
    }

    private static void render(GuiGraphics g, DeltaTracker tickCounter) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        if (!Configs.ModeIndicator.ENABLED.getBooleanValue()) return;
        if (!AxeModeHandler.isHoldingConfiguredWand(mc.player)) return;

        AxeModeHandler.AxeMode mode = AxeModeHandler.getMode();
        String modeKey = mode == AxeModeHandler.AxeMode.NORMAL
                ? "wegui.mode.normal"
                : "wegui.mode.edit_selection";
        String modeName = Component.translatable(modeKey).getString();

        String toolName = mc.player.getMainHandItem().getHoverName().getString();
        if (toolName.isBlank()) {
            toolName = Component.translatable("wegui.mode.unknown_tool").getString();
        }

        List<String> lines = new ArrayList<>();
        lines.add(Component.translatable("wegui.mode.title").getString());
        lines.add(Component.translatable("wegui.mode.tool", toolName).getString());
        lines.add(Component.translatable("wegui.mode.current", modeName).getString());
        if (mode == AxeModeHandler.AxeMode.EDIT_SELECTION) {
            lines.add(Component.translatable("wegui.mode.hint.edit").getString());
        } else {
            lines.add(Component.translatable("wegui.mode.hint.normal").getString());
        }

        int maxW = 0;
        for (String line : lines) {
            maxW = Math.max(maxW, mc.font.width(line));
        }

        int lineHeight = mc.font.lineHeight;
        int paddingX = 6;
        int paddingY = 5;
        int panelW = maxW + paddingX * 2;
        int panelH = lines.size() * lineHeight + (lines.size() - 1) + paddingY * 2;

        int offsetX = Configs.ModeIndicator.OFFSET_X.getIntegerValue();
        int offsetY = Configs.ModeIndicator.OFFSET_Y.getIntegerValue();
        float scale = Configs.ModeIndicator.SCALE.getIntegerValue() / 100.0f;

        int x = offsetX;
        int y = mc.getWindow().getGuiScaledHeight() - (int) (panelH * scale) - offsetY;

        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);

        g.fill(0, 0, panelW, panelH, 0xCC000000);
        g.renderOutline(0, 0, panelW, panelH, 0xFF444444);

        int textY = paddingY;
        for (int i = 0; i < lines.size(); i++) {
            int color = switch (i) {
                case 0 -> 0xFFFFFF00;
                case 2 -> 0xFF55FF55;
                default -> 0xFFFFFFFF;
            };
            g.drawString(mc.font, lines.get(i), paddingX, textY, color, false);
            textY += lineHeight + 1;
        }

        g.pose().popMatrix();
    }
}
