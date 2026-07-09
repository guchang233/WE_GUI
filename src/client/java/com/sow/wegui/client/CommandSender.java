package com.sow.wegui.client;

import com.sow.wegui.WeGuiMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * 将命令字符串发送到服务端。
 */
public final class CommandSender {
    private CommandSender() {}

    public static void send(String command) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.connection == null || command == null || command.isBlank()) return;

        String trimmed = command.trim();
        WeGuiMod.LOGGER.debug("[WE GUI] 发送命令: {}", trimmed);
        try {
            if (trimmed.startsWith("//")) {
                // WorldEdit 主命令注册为 Brigadier 根节点 "/xxx"，用命令包发送更可靠
                player.connection.sendCommand(trimmed.substring(1));
            } else if (trimmed.startsWith("/.")) {
                // WorldEdit CUI 协议消息，走聊天通道
                player.connection.sendChat(trimmed);
            } else if (trimmed.startsWith("/")) {
                player.connection.sendCommand(trimmed.substring(1));
            } else {
                player.connection.sendCommand(trimmed);
            }

            // 剪贴板相关命令会改变 preview 内容，立即刷新缓存
            if (isClipboardCommand(trimmed)) {
                PastePreviewRenderer.invalidateCache();
            }
        } catch (Throwable t) {
            WeGuiMod.LOGGER.error("[WE GUI] 发送命令失败: {}", trimmed, t);
        }
    }

    private static boolean isClipboardCommand(String command) {
        String lower = command.toLowerCase();
        return lower.startsWith("//copy") || lower.startsWith("//cut") ||
               lower.startsWith("//flip") || lower.startsWith("//rotate") ||
               lower.startsWith("//clearclipboard") || lower.startsWith("//load");
    }
}
