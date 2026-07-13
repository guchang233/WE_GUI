package com.sow.wegui.client;

import com.sow.wegui.WeStatus;
import com.sow.wegui.config.Anchor;
import com.sow.wegui.config.Configs;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
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
    private static int tickCounter = 0;
    private static final int CAPTURE_INTERVAL_TICKS = 10;
    private static String cachedClipboardYes;
    private static String cachedClipboardNo;
    private static String cachedStatusNoWorldedit;
    private static String cachedStatusNoSelection;
    private static String cachedPlaceholderLang;

    // 渲染文本缓存：仅在 cached 快照或模板配置变化时重算，避免每帧重复字符串替换
    private static WeStatus renderedSnapshot = null;
    private static String renderedLine1Template = null;
    private static String renderedLine2Template = null;
    private static String renderedLang = null;
    private static String cachedLine1 = "";
    private static String cachedLine2 = "";

    private StatusBar() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            tickCounter++;
            if (tickCounter >= CAPTURE_INTERVAL_TICKS) {
                tickCounter = 0;
                cached = WorldEditBridge.capture(mc);
            }
        });
        HudRenderCallback.EVENT.register((GuiGraphics drawContext, DeltaTracker tickCounter) -> {
            Minecraft mc = Minecraft.getInstance();
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

        String line1Template = Configs.StatusBar.STATUS_BAR_LINE1.getStringValue();
        String line2Template = Configs.StatusBar.STATUS_BAR_LINE2.getStringValue();
        String lang = mc.options.languageCode;

        // 仅在快照、模板或语言变化时重新格式化（每 10 tick 一次而非每帧）
        if (renderedSnapshot != cached
                || !java.util.Objects.equals(renderedLine1Template, line1Template)
                || !java.util.Objects.equals(renderedLine2Template, line2Template)
                || !java.util.Objects.equals(renderedLang, lang)) {
            renderedSnapshot = cached;
            renderedLine1Template = line1Template;
            renderedLine2Template = line2Template;
            renderedLang = lang;
            cachedLine1 = translateStatusPlaceholders(cached.format(line1Template));
            cachedLine2 = translateStatusPlaceholders(cached.format(line2Template));
        }

        String line1 = cachedLine1;
        String line2 = cachedLine2;

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

    private static void refreshPlaceholderCacheIfNeeded() {
        Minecraft mc = Minecraft.getInstance();
        String lang = mc.options.languageCode;
        if (lang != null && lang.equals(cachedPlaceholderLang) && cachedClipboardYes != null) return;
        cachedPlaceholderLang = lang;
        cachedClipboardYes = Component.translatable("wegui.status.yes").getString();
        cachedClipboardNo = Component.translatable("wegui.status.no").getString();
        cachedStatusNoWorldedit = Component.translatable("wegui.status.no_worldedit").getString();
        cachedStatusNoSelection = Component.translatable("wegui.status.no_selection").getString();
    }

    private static String translateStatusPlaceholders(String text) {
        if (text == null) return "";
        refreshPlaceholderCacheIfNeeded();
        return text
                .replace("{status.no_worldedit}", cachedStatusNoWorldedit)
                .replace("{status.no_selection}", cachedStatusNoSelection)
                .replace("{clipboard.yes}", cachedClipboardYes)
                .replace("{clipboard.no}", cachedClipboardNo);
    }
}
