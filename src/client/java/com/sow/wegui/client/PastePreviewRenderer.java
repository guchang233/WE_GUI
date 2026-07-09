package com.sow.wegui.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sow.wegui.config.Config;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * copy 后在世界中渲染 paste 位置预览：保留线框，并叠加半透明真实方块材质。
 */
public final class PastePreviewRenderer {
    private PastePreviewRenderer() {}

    private static final float GHOST_ALPHA = 0.5f;
    private static final int CLIPBOARD_CACHE_TICKS = 20;

    @Nullable
    private static Map<BlockPos, BlockState> cachedBlocks;
    private static WorldEditBridge.ClipboardBounds cachedBounds;
    private static long cacheTick = -CLIPBOARD_CACHE_TICKS;

    public static void register() {
        // BEFORE_TRANSLUCENT 每帧都会触发，不需要玩家指着方块
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(context -> render(context));
    }

    /**
     * 强制刷新剪贴板方块缓存。应在 //copy、//cut、//flip、//rotate 等修改剪贴板后调用。
     */
    public static void invalidateCache() {
        cachedBlocks = null;
        cachedBounds = null;
        cacheTick = -CLIPBOARD_CACHE_TICKS;
    }

    private static void render(WorldRenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!Config.get().isPastePreviewEnabled()) return;

        Vec3 cam = context.gameRenderer().getMainCamera().position();
        PoseStack pose = context.matrices();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);

        VertexConsumer buffer = context.consumers().getBuffer(RenderTypes.lines());
        Matrix4f matrix = pose.last().pose();

        // 1) 当前 WorldEdit 选区（黄色）
            if (Config.get().isSelectionBoundsEnabled()) {
                WorldEditBridge.Bounds sel = WorldEditBridge.getSelectionBounds(mc);
                if (sel != null) {
                    AABB selectionBox = new AABB(sel.minX(), sel.minY(), sel.minZ(),
                            sel.minX() + sel.w(), sel.minY() + sel.h(), sel.minZ() + sel.l());
                    drawBox(buffer, matrix, selectionBox, 1.0f, 0.85f, 0.2f, 0.8f);
                }
            }

        // 2) copy 后的粘贴预览（绿色线框 + 半透明材质），原点在玩家所在方块
        WorldEditBridge.ClipboardBounds cb = WorldEditBridge.getClipboardBounds(mc);
        if (cb != null) {
            BlockPos playerPos = BlockPos.containing(mc.player.getX(), mc.player.getY(), mc.player.getZ());
            double minX = cb.minX() - cb.originX() + playerPos.getX();
            double minY = cb.minY() - cb.originY() + playerPos.getY();
            double minZ = cb.minZ() - cb.originZ() + playerPos.getZ();
            AABB pasteBox = new AABB(minX, minY, minZ, minX + cb.w(), minY + cb.h(), minZ + cb.l());
            drawBox(buffer, matrix, pasteBox, 0.3f, 1.0f, 0.5f, 0.8f);

            // 3) 真实材质半透明预览
            Map<BlockPos, BlockState> blocks = getClipboardBlocksCached(mc, cb);
            if (blocks != null && !blocks.isEmpty()) {
                renderGhostBlocks(mc, pose, context.consumers(), blocks, playerPos);
            }
        }

        pose.popPose();
    }

    @Nullable
    private static Map<BlockPos, BlockState> getClipboardBlocksCached(Minecraft mc, WorldEditBridge.ClipboardBounds cb) {
        long tick = mc.level.getGameTime();
        if (cachedBlocks != null && cb.equals(cachedBounds) && tick - cacheTick < CLIPBOARD_CACHE_TICKS) {
            return cachedBlocks;
        }
        cachedBounds = cb;
        cacheTick = tick;
        cachedBlocks = WorldEditBridge.getClipboardBlocks(mc);
        return cachedBlocks;
    }

    private static void renderGhostBlocks(Minecraft mc, PoseStack pose, MultiBufferSource bufferSource,
                                          Map<BlockPos, BlockState> blocks, BlockPos origin) {
        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
        AlphaMultiBufferSource alphaSource = new AlphaMultiBufferSource(bufferSource, GHOST_ALPHA);
        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            BlockPos target = origin.offset(entry.getKey());
            BlockState state = entry.getValue();
            pose.pushPose();
            pose.translate(target.getX(), target.getY(), target.getZ());
            dispatcher.renderSingleBlock(state, pose, alphaSource, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
    }

    private static void drawBox(VertexConsumer buffer, Matrix4f matrix, AABB box, float r, float g, float b, float a) {
        drawLine(buffer, matrix, (float) box.minX, (float) box.minY, (float) box.minZ, (float) box.maxX, (float) box.minY, (float) box.minZ, r, g, b, a);
        drawLine(buffer, matrix, (float) box.minX, (float) box.minY, (float) box.minZ, (float) box.minX, (float) box.maxY, (float) box.minZ, r, g, b, a);
        drawLine(buffer, matrix, (float) box.minX, (float) box.minY, (float) box.minZ, (float) box.minX, (float) box.minY, (float) box.maxZ, r, g, b, a);
        drawLine(buffer, matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ, (float) box.minX, (float) box.maxY, (float) box.maxZ, r, g, b, a);
        drawLine(buffer, matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ, (float) box.maxX, (float) box.minY, (float) box.maxZ, r, g, b, a);
        drawLine(buffer, matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ, (float) box.maxX, (float) box.maxY, (float) box.minZ, r, g, b, a);
        drawLine(buffer, matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ, (float) box.minX, (float) box.maxY, (float) box.minZ, r, g, b, a);
        drawLine(buffer, matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ, (float) box.minX, (float) box.minY, (float) box.maxZ, r, g, b, a);
        drawLine(buffer, matrix, (float) box.maxX, (float) box.minY, (float) box.minZ, (float) box.maxX, (float) box.minY, (float) box.maxZ, r, g, b, a);
        drawLine(buffer, matrix, (float) box.maxX, (float) box.minY, (float) box.minZ, (float) box.maxX, (float) box.maxY, (float) box.minZ, r, g, b, a);
        drawLine(buffer, matrix, (float) box.minX, (float) box.maxY, (float) box.minZ, (float) box.maxX, (float) box.maxY, (float) box.minZ, r, g, b, a);
        drawLine(buffer, matrix, (float) box.minX, (float) box.minY, (float) box.maxZ, (float) box.maxX, (float) box.minY, (float) box.maxZ, r, g, b, a);
    }

    private static void drawLine(VertexConsumer buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
        buffer.addVertex(matrix, x1, y1, z1)
                .setColor(r, g, b, a)
                .setNormal(x2 - x1, y2 - y1, z2 - z1)
                .setLineWidth(2.0f);
        buffer.addVertex(matrix, x2, y2, z2)
                .setColor(r, g, b, a)
                .setNormal(x1 - x2, y1 - y2, z1 - z2)
                .setLineWidth(2.0f);
    }

    /**
     * 把任意请求都转到半透明 moving block RenderType，使后续 VertexConsumer 的 alpha 生效。
     */
    private static final class AlphaMultiBufferSource implements MultiBufferSource {
        private final MultiBufferSource delegate;
        private final float alpha;
        private final Map<RenderType, VertexConsumer> buffers = new IdentityHashMap<>();

        AlphaMultiBufferSource(MultiBufferSource delegate, float alpha) {
            this.delegate = delegate;
            this.alpha = alpha;
        }

        @Override
        public VertexConsumer getBuffer(RenderType renderType) {
            return buffers.computeIfAbsent(renderType, rt ->
                    new AlphaVertexConsumer(delegate.getBuffer(RenderTypes.translucentMovingBlock()), alpha));
        }
    }

    /**
     * 在顶点颜色上应用半透明 alpha 倍率。
     */
    private static final class AlphaVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final int alpha; // 0-255

        AlphaVertexConsumer(VertexConsumer delegate, float alpha) {
            this.delegate = delegate;
            this.alpha = Math.round(alpha * 255f);
        }

        public VertexConsumer addVertex(float x, float y, float z) {
            return delegate.addVertex(x, y, z);
        }

        public VertexConsumer setColor(int color) {
            int a = (color >> 24) & 0xFF;
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            a = (a * alpha) / 255;
            return delegate.setColor((a << 24) | (r << 16) | (g << 8) | b);
        }

        public VertexConsumer setColor(int r, int g, int b, int a) {
            return delegate.setColor(r, g, b, (a * alpha) / 255);
        }

        public VertexConsumer setColor(float r, float g, float b, float a) {
            return delegate.setColor(r, g, b, a * (alpha / 255f));
        }

        public VertexConsumer setUv(float u, float v) {
            return delegate.setUv(u, v);
        }

        public VertexConsumer setUv1(int u, int v) {
            return delegate.setUv1(u, v);
        }

        public VertexConsumer setUv2(int u, int v) {
            return delegate.setUv2(u, v);
        }

        public VertexConsumer setNormal(float x, float y, float z) {
            return delegate.setNormal(x, y, z);
        }

        public VertexConsumer setOverlay(int overlay) {
            return delegate.setOverlay(overlay);
        }

        public VertexConsumer setLight(int light) {
            return delegate.setLight(light);
        }

        public VertexConsumer setLineWidth(float width) {
            return delegate.setLineWidth(width);
        }

        public void putBulkData(PoseStack.Pose pose, BakedQuad quad, float red, float green, float blue, float alpha, int light, int overlay) {
            delegate.putBulkData(pose, quad, red, green, blue, alpha * (this.alpha / 255f), light, overlay);
        }

        public void putBulkData(PoseStack.Pose pose, BakedQuad quad, float[] brightness, float red, float green, float blue, float alpha, int[] lightmap, int overlay) {
            delegate.putBulkData(pose, quad, brightness, red, green, blue, alpha * (this.alpha / 255f), lightmap, overlay);
        }
    }
}
