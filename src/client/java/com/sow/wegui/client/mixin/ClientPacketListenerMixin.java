package com.sow.wegui.client.mixin;

import com.sow.wegui.config.Configs;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 当关闭选区提示消息时，拦截 WorldEdit 的原生选区提示。
 */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    private static final List<Pattern> SELECTION_MESSAGE_PATTERNS = List.of(
            // 中文（WorldEdit 7.4+ 官方翻译）
            Pattern.compile("第一选取点已设置为 .*"),
            Pattern.compile("第二选取点已设置为 .*"),
            // 中文：重复点击同一位置时的“位置已设置。”提示（刷屏元凶）
            Pattern.compile("位置已设置.*"),
            // 英文
            Pattern.compile("First position set to .*"),
            Pattern.compile("Second position set to .*"),
            Pattern.compile("Position already set.*"),
            Pattern.compile("Position set.*")
    );

    @Inject(method = {"handleSystemChat", "m_246046_"}, at = @At("HEAD"), cancellable = true)
    private void wegui$onSystemChat(ClientboundSystemChatPacket packet, CallbackInfo ci) {
        if (shouldFilter(packet.content())) {
            ci.cancel();
        }
    }

    @Inject(method = {"setActionBarText", "m_246039_"}, at = @At("HEAD"), cancellable = true)
    private void wegui$onActionBar(ClientboundSetActionBarTextPacket packet, CallbackInfo ci) {
        if (shouldFilter(packet.text())) {
            ci.cancel();
        }
    }

    private static boolean shouldFilter(Component message) {
        if (Configs.Generic.SELECTION_MESSAGE_ENABLED.getBooleanValue()) {
            return false;
        }
        String text = message.getString();
        for (Pattern pattern : SELECTION_MESSAGE_PATTERNS) {
            if (pattern.matcher(text).matches()) {
                return true;
            }
        }
        return false;
    }
}
