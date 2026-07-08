package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * WorldEdit 操作抽象。每个实现对应一个具体的 WE 功能，
 * 直接调用 WorldEdit API 而不是发送命令字符串。
 */
public interface WeOperation {
    /**
     * 执行操作。
     *
     * @param mc     Minecraft 实例
     * @param usage  命令用法定义
     * @param values 用户输入的参数值
     * @return 是否执行成功；false 将回退到命令字符串发送
     */
    boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values);

    default void feedback(Minecraft mc, String message) {
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("§7[WE GUI] §f" + message), false);
        }
    }

    default void error(Minecraft mc, String message) {
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("§c[WE GUI] §f" + message), false);
        }
    }
}
