package com.sow.wegui.client.mixin;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 访问 {@link RenderType#create(String, RenderSetup)}（package-private），
 * 用于创建自定义 RenderType（如 cull=false 的 ghost block 渲染类型）。
 */
@Mixin(RenderType.class)
public interface RenderTypeAccessor {

    @Invoker("create")
    static RenderType wegui$create(String name, RenderSetup setup) {
        throw new AssertionError();
    }
}
