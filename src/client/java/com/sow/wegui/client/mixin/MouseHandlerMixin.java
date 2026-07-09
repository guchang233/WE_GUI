package com.sow.wegui.client.mixin;

import com.sow.wegui.client.AxeModeHandler;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 注入鼠标滚动事件，用于小木斧编辑选区模式下的 Alt+滚轮移动。
 */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void wegui$onScroll(long window, double xoffset, double yoffset, CallbackInfo ci) {
        if (AxeModeHandler.handleMouseScroll(yoffset)) {
            ci.cancel();
        }
    }
}
