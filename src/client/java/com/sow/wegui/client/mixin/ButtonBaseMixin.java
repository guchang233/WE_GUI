package com.sow.wegui.client.mixin;

import fi.dy.masa.malilib.gui.button.ButtonBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 全局禁用 malilib 按钮的滚轮触发点击行为。
 * 默认情况下 malilib ButtonBase 会把滚轮事件转换为鼠标点击，
 * 在含滚动列表的界面中容易误触按钮。这里统一拦截，让所有按钮不响应滚轮。
 */
@Mixin(ButtonBase.class)
public class ButtonBaseMixin {

    @Inject(method = "onMouseScrolledImpl", at = @At("HEAD"), cancellable = true)
    private void wegui$disableScrollClick(double mouseX, double mouseY, double scrollX, double scrollY,
                                          CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
