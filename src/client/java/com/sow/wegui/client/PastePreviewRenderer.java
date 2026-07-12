package com.sow.wegui.client;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sow.wegui.WeGuiMod;
import com.sow.wegui.client.mixin.RenderTypeAccessor;
import com.sow.wegui.config.Configs;
import com.sow.wegui.config.PastePlacementMode;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.render.RenderContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * paste 位置预览渲染器，借鉴 litematica 风格。
 *
 * 渲染分层：
 * ① 选区大框 + pos1/pos2 角点框 — malilib RenderUtils.renderAreaOutlineNoCorners + renderBlockOutline
 * ② paste 整体外框 + 半透明面   — malilib RenderUtils.renderAreaOutlineNoCorners + renderAreaSides
 * ③ ghost blocks 真实材质       — BlockRenderDispatcher.renderSingleBlock + chunk 分组距离剔除
 * ④ 每方块边框（可选）          — malilib RenderContext + drawBlockBoundingBoxOutlinesBatchedLinesSimple
 *
 * 注册方式：
 * - 层 ③ 在 WorldRenderEvents.BEFORE_TRANSLUCENT 中渲染（需要 translucent buffer 正确时序）
 * - 层 ①②④ 通过 malilib IRenderer.onRenderWorldLastAdvanced 渲染
 */
public final class PastePreviewRenderer implements IRenderer {

    private static final PastePreviewRenderer INSTANCE = new PastePreviewRenderer();

    // ---- 自定义 ghost block 渲染管线 ----
    // 基于 RenderPipelines.TRANSLUCENT_MOVING_BLOCK，但禁用背面剔除（cull=false），
    // 并使用 MAIN_TARGET 而非 ITEM_ENTITY_TARGET，确保半透明 ghost block 的所有面都被渲染。
    // 原 translucentMovingBlock 的 cull=true 会导致半透明方块的背面被剔除，
    // 透过半透明前面看到"空"的内部，视觉上表现为"部分顶点渲染不完整"。
    private static final RenderPipeline.Snippet MATRICES_PROJECTION_SNIPPET =
            RenderPipeline.builder()
                    .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
                    .withUniform("Projection", UniformType.UNIFORM_BUFFER)
                    .buildSnippet();

    private static final RenderPipeline GHOST_BLOCK_PIPELINE = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
            .withLocation("wegui/pipeline/ghost_block")
            .withVertexShader("core/rendertype_translucent_moving_block")
            .withFragmentShader("core/rendertype_translucent_moving_block")
            .withSampler("Sampler0")
            .withSampler("Sampler2")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withVertexFormat(DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS)
            .withCull(false)
            .build();

    private static final RenderType GHOST_BLOCK_TYPE = RenderTypeAccessor.wegui$create(
            "wegui:ghost_block",
            RenderSetup.builder(GHOST_BLOCK_PIPELINE)
                    .useLightmap()
                    .withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS)
                    .setOutputTarget(OutputTarget.MAIN_TARGET)
                    .sortOnUpload()
                    .bufferSize(786432)
                    .createRenderSetup());

    // ---- clipboard 缓存 ----
    private Map<BlockPos, BlockState> cachedBlocks;
    private AABB cachedPasteBox;
    private BlockPos cachedPasteOrigin;
    private Object cachedClipboardHolder;
    private Object cachedTransform;

    // ---- chunk 分组缓存（用于 frustum / distance 剔除）----
    private final ClipboardChunkCache chunkCache = new ClipboardChunkCache();
    private boolean chunkCacheDirty = true;

    // ---- 固定放置模式 ----
    @Nullable
    private BlockPos fixedOrigin = null;
    private PastePlacementMode lastMode = PastePlacementMode.FOLLOW_PLAYER;

    private PastePreviewRenderer() {}

    public static PastePreviewRenderer getInstance() {
        return INSTANCE;
    }

    /**
     * 注册渲染器：
     * - ghost blocks（层③）注册到 BEFORE_TRANSLUCENT（translucent buffer 时序需要）
     * - overlays（层①②④）通过返回 getInstance() 由调用方注册到 malilib RenderEventHandler
     */
    public static void register() {
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(PastePreviewRenderer::renderGhostBlocksHook);
    }

    /** 旧 API 兼容：现在每帧直接读取剪贴板，无需手动刷新 */
    public static void invalidateCache() {
        INSTANCE.chunkCacheDirty = true;
    }

    // ========================================================================
    // 放置模式（随玩家移动 / 固定）
    // ========================================================================

    /**
     * 获取当前生效的预览原点。
     * - FIXED 模式且已记录固定位置：返回 fixedOrigin
     * - 否则：返回玩家位置 + 手动偏移
     */
    private BlockPos getEffectiveOrigin(Minecraft mc) {
        if (lastMode == PastePlacementMode.FIXED && fixedOrigin != null) {
            return fixedOrigin;
        }
        return AxeModeHandler.getPasteOrigin(mc.player);
    }

    /**
     * 处理放置模式切换。
     * - 切换到 FIXED：记录当前预览位置作为固定原点，用户可通过 //paste 在此位置执行真正的 paste
     * - 切换到 FOLLOW_PLAYER：清除固定位置，预览恢复跟随玩家
     */
    private void handlePlacementModeChange(PastePlacementMode newMode, Minecraft mc) {
        if (mc.player == null) return;

        if (newMode == PastePlacementMode.FIXED) {
            // 前置检查：单人世界才能直接 paste
            if (!WorldEditBridge.canUseDirectPaste()) {
                mc.player.displayClientMessage(
                        Component.translatable("wegui.message.fixed_mode_multiplayer_disabled")
                                .withStyle(ChatFormatting.RED), true);
                Configs.PastePreview.PASTE_PLACEMENT_MODE.setOptionListValue(PastePlacementMode.FOLLOW_PLAYER);
                return;
            }

            // 前置检查：必须有剪贴板
            BlockPos currentOrigin = AxeModeHandler.getPasteOrigin(mc.player);
            Map<BlockPos, BlockState> blocks = getClipboardBlocksCached(mc, currentOrigin);
            if (blocks == null || blocks.isEmpty()) {
                mc.player.displayClientMessage(
                        Component.translatable("wegui.message.fixed_mode_no_clipboard")
                                .withStyle(ChatFormatting.RED), true);
                Configs.PastePreview.PASTE_PLACEMENT_MODE.setOptionListValue(PastePlacementMode.FOLLOW_PLAYER);
                return;
            }

            // 只固定渲染位置，不自动 paste（避免异步 paste 失败时用户无法感知）
            // 用户通过 //paste 在固定位置执行真正的 paste，渲染与 paste 同步
            fixedOrigin = currentOrigin;
            mc.player.displayClientMessage(
                    Component.translatable("wegui.message.fixed_mode_enabled")
                            .withStyle(ChatFormatting.GREEN), true);
        } else {
            // 切换回 FOLLOW_PLAYER：清除固定位置
            fixedOrigin = null;
        }
    }

    /** 当前是否处于固定模式（直接读配置，不依赖渲染线程的 lastMode） */
    public static boolean isFixedMode() {
        PastePlacementMode mode = (PastePlacementMode) Configs.PastePreview.PASTE_PLACEMENT_MODE.getOptionListValue();
        return mode == PastePlacementMode.FIXED && INSTANCE.fixedOrigin != null;
    }

    /** 获取固定位置（固定模式下），否则返回 null */
    @Nullable
    public static BlockPos getFixedOrigin() {
        return isFixedMode() ? INSTANCE.fixedOrigin : null;
    }

    /** 设置固定位置（用于 pasteAtPreview 在 fixedOrigin 未初始化时回填） */
    public static void setFixedOrigin(BlockPos origin) {
        INSTANCE.fixedOrigin = origin;
    }

    // ========================================================================
    // 层 ③：ghost blocks（BEFORE_TRANSLUCENT）
    // ========================================================================

    private static void renderGhostBlocksHook(WorldRenderContext context) {
        try {
            INSTANCE.renderGhostBlocks(context);
        } catch (Throwable e) {
            WeGuiMod.LOGGER.debug("WeGui ghost blocks 渲染出错: {}", e.toString());
        }
    }

    private void renderGhostBlocks(WorldRenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!Configs.Generic.PASTE_PREVIEW_ENABLED.getBooleanValue()) return;

        // 检测放置模式变化
        PastePlacementMode currentMode = (PastePlacementMode) Configs.PastePreview.PASTE_PLACEMENT_MODE.getOptionListValue();
        if (currentMode != lastMode) {
            handlePlacementModeChange(currentMode, mc);
            lastMode = (PastePlacementMode) Configs.PastePreview.PASTE_PLACEMENT_MODE.getOptionListValue();
        }

        BlockPos origin = getEffectiveOrigin(mc);
        Map<BlockPos, BlockState> blocks = getClipboardBlocksCached(mc, origin);
        if (blocks == null || blocks.isEmpty()) return;

        // 确保 chunk 缓存有效
        ensureChunkCache(blocks, origin);

        float alpha = (float) Configs.PastePreview.GHOST_BLOCK_ALPHA.getDoubleValue();
        if (alpha <= 0.0f) return;

        int renderDistance = Configs.PastePreview.GHOST_RENDER_DISTANCE.getIntegerValue();
        double renderDistSq = (double) renderDistance * renderDistance;

        Vec3 cam = context.gameRenderer().getMainCamera().position();
        PoseStack pose = context.matrices();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);

        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
        MultiBufferSource source = context.consumers();
        if (source == null) {
            pose.popPose();
            return;
        }
        AlphaMultiBufferSource alphaSource = new AlphaMultiBufferSource(source, alpha);

        for (ClipboardChunkCache.ChunkGroup group : chunkCache.getGroups()) {
            // 距离剔除（按 chunk section 中心）
            double cx = (group.worldAabb.minX + group.worldAabb.maxX) * 0.5;
            double cy = (group.worldAabb.minY + group.worldAabb.maxY) * 0.5;
            double cz = (group.worldAabb.minZ + group.worldAabb.maxZ) * 0.5;
            double distSq = (cx - cam.x) * (cx - cam.x) + (cy - cam.y) * (cy - cam.y) + (cz - cam.z) * (cz - cam.z);
            if (distSq > renderDistSq) continue;

            List<BlockPos> positions = group.relPositions;
            List<BlockState> states = group.states;
            for (int i = 0; i < positions.size(); i++) {
                try {
                    BlockPos target = origin.offset(positions.get(i));
                    BlockState state = states.get(i);
                    pose.pushPose();
                    pose.translate(target.getX(), target.getY(), target.getZ());
                    dispatcher.renderSingleBlock(state, pose, alphaSource, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                    pose.popPose();
                } catch (Throwable e) {
                    WeGuiMod.LOGGER.debug("跳过无法渲染的预览方块: {}", e.toString());
                }
            }
        }

        pose.popPose();
    }

    // ========================================================================
    // 层 ①②④：overlays（IRenderer.onRenderWorldLastAdvanced）
    // ========================================================================

    @Override
    public void onRenderWorldLastAdvanced(RenderTarget target, Matrix4f posMatrix, Matrix4f projMatrix,
                                          Frustum frustum, Camera camera, RenderBuffers buffers,
                                          ProfilerFiller profiler) {
        try {
            renderOverlays(target, posMatrix, projMatrix, frustum, camera, buffers, profiler);
        } catch (Throwable e) {
            WeGuiMod.LOGGER.debug("WeGui overlay 渲染出错: {}", e.toString());
        }
    }

    private void renderOverlays(RenderTarget target, Matrix4f posMatrix, Matrix4f projMatrix,
                                Frustum frustum, Camera camera, RenderBuffers buffers,
                                ProfilerFiller profiler) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        profiler.push("wegui_overlays");

        // 层 ①：WorldEdit 选区
        if (Configs.Generic.SELECTION_BOUNDS_ENABLED.getBooleanValue()) {
            profiler.push("selection");
            renderSelectionBounds(mc);
            profiler.pop();
        }

        // 层 ②③④：paste preview
        if (Configs.Generic.PASTE_PREVIEW_ENABLED.getBooleanValue()) {
            BlockPos origin = getEffectiveOrigin(mc);
            Map<BlockPos, BlockState> blocks = getClipboardBlocksCached(mc, origin);
            if (blocks != null && !blocks.isEmpty()) {
                ensureChunkCache(blocks, origin);

                // 层 ②：paste 外框 + 半透明面
                profiler.push("paste_box");
                renderPasteBox(frustum);
                profiler.pop();

                // 层 ④：每方块边框
                if (Configs.Generic.BLOCK_OUTLINE_ENABLED.getBooleanValue()) {
                    profiler.push("block_outlines");
                    renderBlockOutlinesBatched(frustum, camera, origin);
                    profiler.pop();
                }
            }
        }

        profiler.pop();
    }

    // ------------------------------------------------------------------
    // 层 ①：选区大框 + pos1/pos2 角点框
    // ------------------------------------------------------------------

    private void renderSelectionBounds(Minecraft mc) {
        // 选区大框
        try {
            WorldEditBridge.Bounds sel = WorldEditBridge.getSelectionBounds(mc);
            if (sel != null && sel.w() > 0 && sel.h() > 0 && sel.l() > 0) {
                BlockPos pos1 = new BlockPos(sel.minX(), sel.minY(), sel.minZ());
                BlockPos pos2 = new BlockPos(sel.minX() + sel.w() - 1, sel.minY() + sel.h() - 1, sel.minZ() + sel.l() - 1);
                Color4f colorX = new Color4f(1.0f, 0.25f, 0.25f, 1.0f);
                Color4f colorY = new Color4f(0.25f, 1.0f, 0.25f, 1.0f);
                Color4f colorZ = new Color4f(0.25f, 0.25f, 1.0f, 1.0f);
                RenderUtils.renderAreaOutlineNoCorners(pos1, pos2, 2.0f, colorX, colorY, colorZ);
            }
        } catch (Throwable e) {
            WeGuiMod.LOGGER.debug("渲染选区大框时出错: {}", e.toString());
        }

        // pos1/pos2 角点框
        try {
            WorldEditBridge.PartialCornerPositions partial = WorldEditBridge.getPartialSelectionCorners(mc);
            if (partial != null) {
                if (partial.pos1() != null) {
                    var c1 = Configs.PastePreview.SELECTION_POS1_COLOR.getColor();
                    RenderUtils.renderBlockOutline(partial.pos1(), 0.002f, 3.0f,
                            new Color4f(c1.r, c1.g, c1.b, c1.a), false);
                }
                if (partial.pos2() != null) {
                    var c2 = Configs.PastePreview.SELECTION_POS2_COLOR.getColor();
                    RenderUtils.renderBlockOutline(partial.pos2(), 0.002f, 3.0f,
                            new Color4f(c2.r, c2.g, c2.b, c2.a), false);
                }
            }
        } catch (Throwable e) {
            WeGuiMod.LOGGER.debug("渲染选点角框时出错: {}", e.toString());
        }
    }

    // ------------------------------------------------------------------
    // 层 ②：paste 整体外框 + 半透明面
    // ------------------------------------------------------------------

    private void renderPasteBox(Frustum frustum) {
        if (cachedPasteBox == null) return;

        // frustum 剔除
        if (!frustum.isVisible(cachedPasteBox)) return;

        BlockPos pos1 = new BlockPos(
                (int) Math.floor(cachedPasteBox.minX),
                (int) Math.floor(cachedPasteBox.minY),
                (int) Math.floor(cachedPasteBox.minZ));
        BlockPos pos2 = new BlockPos(
                (int) Math.ceil(cachedPasteBox.maxX) - 1,
                (int) Math.ceil(cachedPasteBox.maxY) - 1,
                (int) Math.ceil(cachedPasteBox.maxZ) - 1);

        var c = Configs.PastePreview.BOX_SIDE_COLOR.getColor();
        Color4f outlineColor = new Color4f(c.r, c.g, c.b, 1.0f);

        // 外框线
        RenderUtils.renderAreaOutlineNoCorners(pos1, pos2, 2.0f, outlineColor, outlineColor, outlineColor);

        // 半透明面
        float sideAlpha = (float) Configs.PastePreview.BOX_SIDE_ALPHA.getDoubleValue();
        if (sideAlpha > 0.0f) {
            Color4f sideColor = new Color4f(c.r, c.g, c.b, sideAlpha);
            Matrix4f matrix = RenderSystem.getModelViewStack();
            RenderUtils.renderAreaSides(pos1, pos2, sideColor, matrix);
        }
    }

    // ------------------------------------------------------------------
    // 层 ④：每方块边框（batched，frustum 剔除）
    // ------------------------------------------------------------------

    private void renderBlockOutlinesBatched(Frustum frustum, Camera camera, BlockPos origin) {
        if (chunkCache.isEmpty()) return;

        var c = Configs.PastePreview.BLOCK_OUTLINE_COLOR.getColor();
        Color4f color = new Color4f(c.r, c.g, c.b, c.a);
        int renderDistance = Configs.PastePreview.GHOST_RENDER_DISTANCE.getIntegerValue();
        double renderDistSq = (double) renderDistance * renderDistance;
        Vec3 camPos = camera.position();

        RenderContext ctx = new RenderContext(
                () -> "wegui:block_outlines",
                MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_NO_DEPTH_NO_CULL);
        BufferBuilder buffer = ctx.start(
                () -> "wegui:block_outlines",
                MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_NO_DEPTH_NO_CULL);

        for (ClipboardChunkCache.ChunkGroup group : chunkCache.getGroups()) {
            // frustum 剔除
            if (!frustum.isVisible(group.worldAabb)) continue;

            // 距离剔除
            double cx = (group.worldAabb.minX + group.worldAabb.maxX) * 0.5;
            double cy = (group.worldAabb.minY + group.worldAabb.maxY) * 0.5;
            double cz = (group.worldAabb.minZ + group.worldAabb.maxZ) * 0.5;
            double distSq = (cx - camPos.x) * (cx - camPos.x)
                    + (cy - camPos.y) * (cy - camPos.y)
                    + (cz - camPos.z) * (cz - camPos.z);
            if (distSq > renderDistSq) continue;

            List<BlockPos> positions = group.relPositions;
            for (BlockPos rel : positions) {
                BlockPos target = origin.offset(rel);
                RenderUtils.drawBlockBoundingBoxOutlinesBatchedLinesSimple(target, color, 0.002, 1.5f, buffer);
            }
        }

        try {
            MeshData meshData = buffer.build();
            if (meshData != null) {
                ctx.draw(meshData, false, true);
                meshData.close();
            }
            ctx.reset();
        } catch (Exception ignored) {
        }
    }

    // ========================================================================
    // clipboard 缓存
    // ========================================================================

    @Nullable
    private Map<BlockPos, BlockState> getClipboardBlocksCached(Minecraft mc, BlockPos origin) {
        try {
            LocalSession session = WorldEditAdapter.session(mc.player);
            if (session == null) {
                clearCache();
                return null;
            }
            ClipboardHolder holder;
            try {
                holder = session.getClipboard();
            } catch (Throwable e) {
                clearCache();
                return null;
            }
            if (holder == null) {
                clearCache();
                return null;
            }
            Transform transform = holder.getTransform();
            if (cachedBlocks != null
                    && origin.equals(cachedPasteOrigin)
                    && holder == cachedClipboardHolder
                    && transform == cachedTransform) {
                return cachedBlocks;
            }
            Map<BlockPos, BlockState> blocks = WorldEditBridge.getClipboardBlocks(mc);
            if (blocks == null) {
                clearCache();
                return null;
            }
            cachedBlocks = blocks;
            cachedPasteOrigin = origin;
            cachedClipboardHolder = holder;
            cachedTransform = transform;
            cachedPasteBox = computePasteBounds(blocks, origin);
            chunkCacheDirty = true;
            return blocks;
        } catch (Throwable e) {
            return null;
        }
    }

    private void clearCache() {
        cachedBlocks = null;
        cachedPasteBox = null;
        cachedPasteOrigin = null;
        cachedClipboardHolder = null;
        cachedTransform = null;
        chunkCacheDirty = true;
    }

    private void ensureChunkCache(Map<BlockPos, BlockState> blocks, BlockPos origin) {
        if (chunkCacheDirty) {
            chunkCache.rebuild(blocks, origin);
            chunkCacheDirty = false;
        }
    }

    private static AABB computePasteBounds(Map<BlockPos, BlockState> blocks, BlockPos origin) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
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

    // ========================================================================
    // AlphaMultiBufferSource / AlphaVertexConsumer（ghost block 半透明）
    // ========================================================================

    /**
     * 把任意请求都转到自定义 ghost block RenderType（cull=false），使后续 VertexConsumer 的 alpha 生效。
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
                    new AlphaVertexConsumer(delegate.getBuffer(GHOST_BLOCK_TYPE), alpha));
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
