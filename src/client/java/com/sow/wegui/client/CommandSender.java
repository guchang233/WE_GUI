package com.sow.wegui.client;

import com.sow.wegui.WeGuiMod;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * 将命令字符串发送到服务端。
 * 同时拦截聊天框输入的 //paste 和剪贴板命令，确保偏移粘贴和 offset 重置生效。
 */
public final class CommandSender {
    private CommandSender() {}

    /** 重入保护：send 内部调用 sendCommand 时不允许 ALLOW_COMMAND 再次拦截 */
    private static boolean sending = false;

    /**
     * 触发类初始化，注册聊天/命令拦截器。必须在客户端初始化时调用。
     */
    public static void init() {
        WeGuiMod.LOGGER.info("[WE GUI] CommandSender 初始化，注册命令拦截器");
    }

    static {
        // 玩家在聊天框输入 //xxx → Minecraft 去掉第一个 / → 命令 /xxx
        // ALLOW_COMMAND 参数为去掉第一个 / 后的字符串，即 /xxx
        ClientSendMessageEvents.ALLOW_COMMAND.register(command -> {
            if (sending) return true; // 重入保护，内部 send 不拦截
            if (command == null) return true;
            String trimmed = command.trim();
            if (trimmed.startsWith("/")) {
                // WorldEdit 命令 //xxx，重构后走 send 处理
                WeGuiMod.LOGGER.info("[WE GUI] 拦截聊天命令 //{}", trimmed.substring(1));
                send("//" + trimmed.substring(1));
                return false;
            }
            return true;
        });
        // 兜底：某些版本可能将 //xxx 当作聊天消息
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            if (sending) return true;
            if (message == null) return true;
            String trimmed = message.trim();
            if (trimmed.startsWith("//")) {
                send(trimmed);
                return false;
            }
            return true;
        });
    }

    public static void send(String command) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.connection == null || command == null || command.isBlank()) return;

        String trimmed = command.trim();
        WeGuiMod.LOGGER.info("[WE GUI] send: {}", trimmed);
        try {
            boolean handled = false;
            if (trimmed.equalsIgnoreCase("//paste") || trimmed.toLowerCase().startsWith("//paste ")) {
                AxeModeHandler.executePasteAtPreview(player);
                handled = true;
            }

            if (!handled) {
                if (trimmed.startsWith("//")) {
                    sendRawCommand(trimmed.substring(1));
                } else if (trimmed.startsWith("/.")) {
                    sending = true;
                    try {
                        player.connection.sendChat(trimmed);
                    } finally {
                        sending = false;
                    }
                } else if (trimmed.startsWith("/")) {
                    sendRawCommand(trimmed.substring(1));
                } else {
                    sendRawCommand(trimmed);
                }
            }

            // 剪贴板相关命令会改变 preview 内容，复位手动偏移
            if (isClipboardCommand(trimmed)) {
                AxeModeHandler.resetPastePreviewOffset();
            }
        } catch (Throwable t) {
            WeGuiMod.LOGGER.error("[WE GUI] 发送命令失败: {}", trimmed, t);
        }
    }

    /**
     * 直接发送原始命令字符串，设置重入保护避免 ALLOW_COMMAND 拦截。
     * command 参数不含前导 /（与 player.connection.sendCommand 一致）。
     */
    public static void sendRawCommand(String command) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.connection == null || command == null) return;
        sending = true;
        try {
            player.connection.sendCommand(command);
        } finally {
            sending = false;
        }
    }

    private static boolean isClipboardCommand(String command) {
        String lower = command.toLowerCase();
        return lower.startsWith("//copy") || lower.startsWith("//cut") ||
               lower.startsWith("//flip") || lower.startsWith("//rotate") ||
               lower.startsWith("//clearclipboard") || lower.startsWith("//load");
    }
}
