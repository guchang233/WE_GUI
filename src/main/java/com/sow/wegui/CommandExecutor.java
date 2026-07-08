package com.sow.wegui;

/**
 * 命令执行器 — 将命令字符串发送到服务端。
 * Sender 由客户端初始化时注入。
 */
public final class CommandExecutor {
    private static volatile Sender sender = cmd ->
            WeGuiMod.LOGGER.warn("[WE GUI] 命令未发送（客户端未初始化）: {}", cmd);

    @FunctionalInterface
    public interface Sender { void send(String command); }

    private CommandExecutor() {}

    public static void setSender(Sender s) { sender = s; }
    public static void execute(String raw) { if (raw != null && !raw.isBlank()) sender.send(raw.trim()); }
    public static void execute(WeCommand cmd) { execute(cmd.command()); }
}