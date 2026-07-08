package com.sow.wegui.client;

import com.sow.wegui.ModConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Alt 键处理器 — 按下切换打开列表式命令面板。
 *
 * 行为：
 *   - 按下 Alt（或 Alt+额外键）一次 → 打开 MainPanelScreen
 *   - 再次按下 → 关闭
 *   - 松开 Alt 不触发任何动作
 */
public final class AltKeyHandler {
    private static final int ALT_LEFT = GLFW.GLFW_KEY_LEFT_ALT;
    private static final int ALT_RIGHT = GLFW.GLFW_KEY_RIGHT_ALT;

    private boolean wasDown = false;
    private boolean panelOpen = false;

    public void register() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(Minecraft mc) {
        if (mc.player == null) {
            setClosed(mc);
            return;
        }

        boolean altDown = isAltDown(mc);
        int extraKey = ModConfig.get().extraKey;
        boolean extraDown = extraKey < 0 || isExtraDown(mc);
        boolean modifierDown = altDown && extraDown;

        if (modifierDown && !wasDown) {
            if (panelOpen) {
                setClosed(mc);
            } else if (ToolChecker.isHoldingTriggerTool(mc.player)) {
                setOpen(mc);
            }
        }

        wasDown = modifierDown;

        if (panelOpen && !(mc.screen instanceof MainPanelScreen)) {
            panelOpen = false;
        }
    }

    private void setOpen(Minecraft mc) {
        panelOpen = true;
        mc.setScreen(new MainPanelScreen());
    }

    private void setClosed(Minecraft mc) {
        panelOpen = false;
        if (mc.screen instanceof MainPanelScreen) {
            mc.setScreen(null);
        }
    }

    private boolean isAltDown(Minecraft mc) {
        long handle = mc.getWindow().handle();
        return GLFW.glfwGetKey(handle, ALT_LEFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(handle, ALT_RIGHT) == GLFW.GLFW_PRESS;
    }

    private boolean isExtraDown(Minecraft mc) {
        int key = ModConfig.get().extraKey;
        if (key < 0) return false;
        return GLFW.glfwGetKey(mc.getWindow().handle(), key) == GLFW.GLFW_PRESS;
    }
}