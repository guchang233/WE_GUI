package com.sow.wegui.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sow.wegui.config.Configs;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
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
    private static final double GHOST_RENDER_DISTANCE_SQ = 64.0 * 64.0;
    private static final int GHOST_RENDER_LIMIT = 512;

    private static Map<BlockPos, BlockState> cachedBlocks;
    private static AABB cachedPasteBox;
    private static BlockPos cachedPasteOrigin;
    private static Object cachedClipboardHolder; // ClipboardHolder 引用
    private static Object cachedTransform; // Transform 引用
    private static WorldEditBridge.Bounds cachedSelectionBounds;
    private static WorldEditBridge.PartialCornerPositions cachedPartialCorners;
    private static int selectionCacheTick = 0;
    private static final int SELECTION_CACHE_INTERVAL = 10;

    public static void register() {
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(context -> render(context));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            selectionCacheTick++;
            if (selectionCacheTick >= SELECTION_CACHE_INTERVAL) {
                selectionCacheTick = 0;
                cachedSelectionBounds = null;
                cachedPartialCorners = null;
            }
        });
    }

    /**
     * 已废弃，保留兼容旧调用。剪贴板现在每帧直接读取，无需手动刷新。
     */
    public static void invalidateCache() {
    }

    private static void render(WorldRenderContext context) {
        try {
            renderInternal(context);
        } catch (Throwable e) {
            com.sow.wegui.WeGuiMod.LOGGER.debug("WorldEdit GUI 渲染预览时出错: {}", e.toString());
        }
    }

    private static void renderInternal(WorldRenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Vec3 cam = context.gameRenderer().getMainCamera().position();
        PoseStack pose = context.matrices();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);

        VertexConsumer buffer = context.consumers().getBuffer(RenderTypes.lines());
        Matrix4f matrix = pose.last().pose();

        // 1) 当前 WorldEdit 选区
        if (Configs.Generic.SELECTION_BOUNDS_ENABLED.getBooleanValue()) {
            try {
                if (cachedSelectionBounds == null) {
                    cachedSelectionBounds = WorldEditBridge.getSelectionBounds(mc);
                }
                WorldEditBridge.Bounds sel = cachedSelectionBounds;
                if (sel != null) {
                    AABB selectionBox = new AABB(sel.minX(), sel.minY(), sel.minZ(),
                            sel.minX() + sel.w(), sel.minY() + sel.h(), sel.minZ() + sel.l());
                    var c = Configs.PastePreview.SELECTION_BOX_COLOR.getColor();
                    drawBox(buffer, matrix, selectionBox, c.r, c.g, c.b, c.a);
                }
            } catch (Throwable e) {
                com.sow.wegui.WeGuiMod.LOGGER.debug("渲染选区大框时出错: {}", e.toString());
            }

            // 为第一、二选点单独渲染角点框（略大于 1x1x1，蓝色比红色略大，均在方块外侧）
            try {
                if (cachedPartialCorners == null) {
                    cachedPartialCorners = WorldEditBridge.getPartialSelectionCorners(mc);
                }
                WorldEditBridge.PartialCornerPositions partial = cachedPartialCorners;
                if (partial != null) {
                    if (partial.pos1() != null) {
                        var c1 = Configs.PastePreview.SELECTION_POS1_COLOR.getColor();
                        drawCornerBox(buffer, matrix, partial.pos1(), 1.05f, c1.r, c1.g, c1.b, c1.a);
                    }
                    if (partial.pos2() != null) {
                        var c2 = Configs.PastePreview.SELECTION_POS2_COLOR.getColor();
                        drawCornerBox(buffer, matrix, partial.pos2(), 1.10f, c2.r, c2.g, c2.b, c2.a);
                    }
                }
            } catch (Throwable e) {
                com.sow.wegui.WeGuiMod.LOGGER.debug("渲染选点角框时出错: {}", e.toString());
            }
        }

        // 2) copy 后的粘贴预览
        if (Configs.Generic.PASTE_PREVIEW_ENABLED.getBooleanValue()) {
            BlockPos origin = AxeModeHandler.getPasteOrigin(mc.player);
            Map<BlockPos, BlockState> blocks = getClipboardBlocksCached(mc, origin);
            if (blocks != null && !blocks.isEmpty()) {
                AABB pasteBox = cachedPasteBox != null ? cachedPasteBox : computePasteBounds(blocks, origin);
                drawBox(buffer, matrix, pasteBox, 0.3f, 1.0f, 0.5f, 0.8f);

                // 3) 真实材质半透明预览
                renderGhostBlocks(mc, pose, context.consumers(), blocks, origin, cam);

                // 4) 每个非空气方块单独边框（投影同款）
                if (Configs.Generic.BLOCK_OUTLINE_ENABLED.getBooleanValue()) {
                    var c = Configs.PastePreview.BLOCK_OUTLINE_COLOR.getColor();
                    renderBlockOutlines(buffer, matrix, blocks, origin, c.r, c.g, c.b, c.a, cam);
                }
            }
        }

        pose.popPose();
    }

    @Nullable
    private static Map<BlockPos, BlockState> getClipboardBlocksCached(Minecraft mc, BlockPos origin) {
        try {
            LocalSession session = WorldEditAdapter.session(mc.player);
            if (session == null) {
                cachedBlocks = null;
                cachedPasteBox = null;
                cachedClipboardHolder = null;
                cachedTransform = null;
                return null;
            }
            ClipboardHolder holder;
            try {
                holder = session.getClipboard();
            } catch (Throwable e) {
                cachedBlocks = null;
                cachedPasteBox = null;
                cachedClipboardHolder = null;
                cachedTransform = null;
                return null;
            }
            if (holder == null) {
                cachedBlocks = null;
                cachedPasteBox = null;
                cachedClipboardHolder = null;
                cachedTransform = null;
                return null;
            }
            Transform transform = holder.getTransform();
            // 缓存命中：origin、holder 引用、transform 引用均未变
            if (cachedBlocks != null
                    && origin.equals(cachedPasteOrigin)
                    && holder == cachedClipboardHolder
                    && transform == cachedTransform) {
                return cachedBlocks;
            }
            // 缓存未命中：重新计算
            Map<BlockPos, BlockState> blocks = WorldEditBridge.getClipboardBlocks(mc);
            if (blocks == null) {
                cachedBlocks = null;
                cachedPasteBox = null;
                cachedClipboardHolder = null;
                cachedTransform = null;
                return null;
            }
            cachedBlocks = blocks;
            cachedPasteOrigin = origin;
            cachedClipboardHolder = holder;
            cachedTransform = transform;
            cachedPasteBox = computePasteBounds(blocks, origin);
            return blocks;
        } catch (Throwable e) {
            return null;
        }
    }

    private static AABB computePasteBounds(Map<BlockPos, BlockState> blocks, BlockPos origin) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos rel : blocks.keySet()) {
            BlockPos pos = origin.offset(rel);
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX() + 1);
            maxY = Math.max(maxY, pos.getY() + 1);
            maxZ = Math.max(maxZ, pos.getZ() + 1);
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static void renderGhostBlocks(Minecraft mc, PoseStack pose, MultiBufferSource bufferSource,
                                          Map<BlockPos, BlockState> blocks, BlockPos origin, Vec3 cam) {
        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
        AlphaMultiBufferSource alphaSource = new AlphaMultiBufferSource(bufferSource, GHOST_ALPHA);
        int rendered = 0;
        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            try {
                BlockPos target = origin.offset(entry.getKey());
                double dx = target.getX() + 0.5 - cam.x;
                double dy = target.getY() + 0.5 - cam.y;
                double dz = target.getZ() + 0.5 - cam.z;
                if (dx * dx + dy * dy + dz * dz > GHOST_RENDER_DISTANCE_SQ) continue;
                if (rendered >= GHOST_RENDER_LIMIT) break;
                BlockState state = entry.getValue();
                pose.pushPose();
                pose.translate(target.getX(), target.getY(), target.getZ());
                dispatcher.renderSingleBlock(state, pose, alphaSource, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                pose.popPose();
                rendered++;
            } catch (Throwable e) {
                com.sow.wegui.WeGuiMod.LOGGER.debug("跳过无法渲染的预览方块: {}", e.toString());
            }
        }
    }

    private static void drawCornerBox(VertexConsumer buffer, Matrix4f matrix, BlockPos pos, float scale,
                                      float r, float g, float b, float a) {
        float inset = (scale - 1.0f) / 2.0f;
        drawBox(buffer, matrix, new AABB(pos.getX() - inset, pos.getY() - inset, pos.getZ() - inset,
                pos.getX() + 1 + inset, pos.getY() + 1 + inset, pos.getZ() + 1 + inset), r, g, b, a);
    }

    private static void renderBlockOutlines(VertexConsumer buffer, Matrix4f matrix,
                                            Map<BlockPos, BlockState> blocks, BlockPos origin,
                                            float r, float g, float b, float a, Vec3 cam) {
        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            BlockPos target = origin.offset(entry.getKey());
            double dx = target.getX() + 0.5 - cam.x;
            double dy = target.getY() + 0.5 - cam.y;
            double dz = target.getZ() + 0.5 - cam.z;
            if (dx * dx + dy * dy + dz * dz > GHOST_RENDER_DISTANCE_SQ) continue;
            drawBox(buffer, matrix, new AABB(target.getX(), target.getY(), target.getZ(),
                    target.getX() + 1, target.getY() + 1, target.getZ() + 1), r, g, b, a);
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
