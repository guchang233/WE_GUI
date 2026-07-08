package com.sow.wegui.client;

import com.sow.wegui.WeGuiMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;

/**
 * 命令发送器 — 通过客户端网络层将命令发送到服务端。
 *
 * 规则：
 *  - //cmd  → sendCommand("worldedit cmd")
 *  - /cmd   → sendCommand("cmd")
 *  - cmd    → sendCommand("cmd")
 *  - //     → sendChat("//")           (超级镐切换，WE 聊天别名)
 *  - /.xxx  → sendChat("/.xxx")        (WE 脚本别名)
 */
@Environment(EnvType.CLIENT)
public final class CommandSender {
    private CommandSender() {}

    public static void send(String rawCommand) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ClientPacketListener conn = mc.player.connection;
        if (conn == null) return;

        String cmd = rawCommand.trim();
        if (cmd.isBlank()) return;

        try {
            // WorldEdit 的 // 快捷命令通过聊天消息发送，由 WE 客户端自己解析执行。
            if (cmd.startsWith("//") || cmd.startsWith("/.")) {
                conn.sendChat(cmd);
                mc.player.displayClientMessage(
                        Component.literal("§7[WE GUI] §f→ §e" + cmd), false);
                WeGuiMod.LOGGER.info("[WE GUI] 已发送命令（聊天）: {}", rawCommand);
                return;
            }

            String send;
            String display;
            if (cmd.startsWith("/")) {
                send = cmd.substring(1).trim();
                display = cmd;
            } else {
                send = cmd;
                display = "/" + cmd;
            }

            if (!send.isBlank()) {
                conn.sendCommand(send);
                mc.player.displayClientMessage(
                        Component.literal("§7[WE GUI] §f→ §e" + display), false);
            }
            WeGuiMod.LOGGER.info("[WE GUI] 已发送命令: {}", rawCommand);
        } catch (Exception e) {
            WeGuiMod.LOGGER.warn("[WE GUI] 命令发送失败: {}", rawCommand, e);
            if (mc.player != null) {
                mc.player.displayClientMessage(
                        Component.literal("§c[WE GUI] 发送失败: " + rawCommand), false);
            }
        }
    }
}