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
import com.sow.wegui.config.RenderStylePreset;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * paste 位置预览渲染器，全面对齐 Litematica 26.2 的渲染架构与视觉风格。
 *
 * <p>渲染分层（与 Litematica OverlayRenderer / WorldRendererSchematic 一致）：
 * <ul>
 *   <li>层① 选区大框 + pos1/pos2 角点框 — 对齐 {@code OverlayRenderer.renderSelectionBox}：
 *     选区大框使用 RGB 轴色（colorX/Y/Z），角点框使用 colorPos1/colorPos2，线宽 2.0f（blockBox）/1.5f（area）</li>
 *   <li>层② paste 整体外框 + 半透明面 — 对齐 {@code OverlayRenderer.renderSelectionBox} 的 PLACEMENT 类型：
 *     单色外框 + PLACEMENT_BOX_SIDE_ALPHA 半透明面</li>
 *   <li>层③ ghost blocks 真实材质 — 对齐 {@code WorldRendererSchematic.renderBlockLayer}：
 *     BlockRenderDispatcher.renderSingleBlock + chunk section 分组距离剔除</li>
 *   <li>层④ Mismatch 渲染（验证模式，始终穿墙）— 对齐 {@code OverlayRenderer.renderSchematicMismatches}：
 *     DEBUG_LINES_MASA_SIMPLE_NO_DEPTH_NO_CULL 管线，lineWidth=2.0f，注视方块加粗 lineWidth=6.0f</li>
 *   <li>层⑤ Schematic Overlay（可配置穿墙）— 对齐 {@code WorldRendererSchematic.renderBlockOverlays}：
 *     正常模式 OFFSET_2 防 z-fighting，透墙模式 NO_DEPTH_NO_CULL</li>
 * </ul>
 *
 * <p>颜色常量严格对齐 Litematica 源码：
 * <ul>
 *   <li>{@link #COLOR_POS1} = (1, 0.0625, 0.0625) — OverlayRenderer.colorPos1</li>
 *   <li>{@link #COLOR_POS2} = (0.0625, 0.0625, 1) — OverlayRenderer.colorPos2</li>
 *   <li>{@link #COLOR_X} = (1, 0.25, 0.25) — OverlayRenderer.colorX</li>
 *   <li>{@link #COLOR_Y} = (0.25, 1, 0.25) — OverlayRenderer.colorY</li>
 *   <li>{@link #COLOR_Z} = (0.25, 0.25, 1) — OverlayRenderer.colorZ</li>
 *   <li>{@link #COLOR_AREA} = (1, 1, 1) — OverlayRenderer.colorArea（粘贴外框默认色）</li>
 *   <li>VERIFY_*_COLOR 默认值对齐 MismatchType 颜色常量</li>
 * </ul>
 *
 * <p>注册方式：
 * <ul>
 *   <li>层 ③ 在 {@code WorldRenderEvents.BEFORE_TRANSLUCENT} 中渲染（translucent buffer 时序需要）</li>
 *   <li>层 ①②④⑤ 通过 {@code IRenderer.onRenderWorldLastAdvanced} 渲染</li>
 * </ul>
 */
public final class PastePreviewRenderer implements IRenderer {

    private static final PastePreviewRenderer INSTANCE = new PastePreviewRenderer();

    // ========================================================================
    // Litematica 颜色常量（OverlayRenderer.java 同款数值，像素级一致）
    // ========================================================================

    /** pos1 角点框颜色 — 对齐 Litematica OverlayRenderer.colorPos1 = (1, 0.0625, 0.0625) */
    private static final Color4f COLOR_POS1 = new Color4f(1.0f, 0.0625f, 0.0625f, 1.0f);
    /** pos2 角点框颜色 — 对齐 Litematica OverlayRenderer.colorPos2 = (0.0625, 0.0625, 1) */
    private static final Color4f COLOR_POS2 = new Color4f(0.0625f, 0.0625f, 1.0f, 1.0f);
    /** 选区 X 轴线颜色 — 对齐 Litematica OverlayRenderer.colorX = (1, 0.25, 0.25) */
    private static final Color4f COLOR_X = new Color4f(1.0f, 0.25f, 0.25f, 1.0f);
    /** 选区 Y 轴线颜色 — 对齐 Litematica OverlayRenderer.colorY = (0.25, 1, 0.25) */
    private static final Color4f COLOR_Y = new Color4f(0.25f, 1.0f, 0.25f, 1.0f);
    /** 选区 Z 轴线颜色 — 对齐 Litematica OverlayRenderer.colorZ = (0.25, 0.25, 1) */
    private static final Color4f COLOR_Z = new Color4f(0.25f, 0.25f, 1.0f, 1.0f);
    /** 粘贴外框默认色 — 对齐 Litematica OverlayRenderer.colorArea = (1, 1, 1) */
    private static final Color4f COLOR_AREA = new Color4f(1.0f, 1.0f, 1.0f, 1.0f);

    /** Litematica 线宽常量 — OverlayRenderer.lineWidthBlockBox = 2.0f（角点框） */
    private static final float LINE_WIDTH_BLOCK_BOX = 2.0f;
    /** Litematica 线宽常量 — OverlayRenderer.lineWidthArea = 1.5f（选区大框/粘贴外框） */
    private static final float LINE_WIDTH_AREA = 1.5f;
    /** Litematica 线宽常量 — OverlayRenderer.renderSchematicMismatches 普通边框 = 2.0f */
    private static final float LINE_WIDTH_MISMATCH = 2.0f;
    /** Litematica 线宽常量 — OverlayRenderer.renderSchematicMismatches 注视方块加粗 = 6.0f */
    private static final float LINE_WIDTH_MISMATCH_LOOKED = 6.0f;
    /** Litematica 默认填充面透明度（schematic overlay 与 mismatch 共用，InfoOverlays.VERIFIER_ERROR_HILIGHT_ALPHA=0.2） */
    private static final float DEFAULT_SIDE_ALPHA = 0.2f;
    /** Block outline expand 值（Litematica OverlayRenderer.expand = 0.001f） */
    private static final float OUTLINE_EXPAND = 0.002f;

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

    // ---- 渲染风格预设 ----
    private RenderStylePreset lastPreset = RenderStylePreset.DEFAULT;

    private PastePreviewRenderer() {}

    public static PastePreviewRenderer getInstance() {
        return INSTANCE;
    }

    /**
     * 注册渲染器：
     * - ghost blocks（层③）注册到 BEFORE_TRANSLUCENT（translucent buffer 时序需要）
     * - overlays（层①②④⑤）通过返回 getInstance() 由调用方注册到 malilib RenderEventHandler
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
    // 渲染风格预设
    // ========================================================================

    /**
     * 检测渲染风格预设变化，变化时批量应用对应的配色和参数。
     * 在每帧渲染时调用。
     */
    private void checkPresetChange() {
        RenderStylePreset current = (RenderStylePreset) Configs.PastePreview.RENDER_STYLE_PRESET.getOptionListValue();
        if (current != lastPreset) {
            applyPreset(current);
            lastPreset = current;
        }
    }

    /**
     * 应用渲染风格预设，批量设置配色和参数。
     * 使用 setValueFromString 传入 "#RRGGBBAA" 格式。
     * 所有引用均使用 Configs.RenderStyles.* 统一分类。
     */
    private void applyPreset(RenderStylePreset preset) {
        WeGuiMod.LOGGER.info("[WE GUI] 应用渲染风格预设: {}", preset.getStringValue());
        switch (preset) {
            case LITEMATICA -> {
                // Litematica 投影模组风格：实心 schematic blocks + per-block 染色 overlay + 无 paste box 面
                // 角点颜色对齐 Litematica colorPos1/colorPos2
                Configs.RenderStyles.SELECTION_POS1_COLOR.setValueFromString("#FF1010FF");
                Configs.RenderStyles.SELECTION_POS2_COLOR.setValueFromString("#1010FFFF");
                // 粘贴外框对齐 Litematica colorArea（白色）
                Configs.RenderStyles.PASTE_BOX_COLOR.setValueFromString("#FFFFFFFF");
                // 非验证 overlay 颜色（青色，与 mismatch MISSING 区分）
                Configs.RenderStyles.BLOCK_OVERLAY_COLOR.setValueFromString("#00FFFFFF");
                // 仿 Litematica: RENDER_BLOCKS_AS_TRANSLUCENT=false（实心），GHOST_BLOCK_ALPHA 仅半透明模式生效
                Configs.RenderStyles.RENDER_BLOCKS_AS_TRANSLUCENT.setBooleanValue(false);
                Configs.RenderStyles.GHOST_BLOCK_ALPHA.setDoubleValue(0.5);
                // 仿 Litematica: RENDER_PASTE_BOX_SIDES=false（仅线框，无半透明面）
                Configs.RenderStyles.RENDER_PASTE_BOX_SIDES.setBooleanValue(false);
                Configs.RenderStyles.PASTE_BOX_SIDE_ALPHA.setDoubleValue(0.2);
                Configs.Generic.BLOCK_OUTLINE_ENABLED.setBooleanValue(true);
                // Litematica 方块验证配色（对齐 MismatchType 颜色常量，alpha=1.0 边框色）：
                // MISSING=0x00FFFF（青）、WRONG_BLOCK=0xFF0000（红）、WRONG_STATE=0xFFAF00（橙）、EXTRA=0xFF00CF（品红）
                Configs.RenderStyles.VERIFY_MISSING_COLOR.setValueFromString("#00FFFF");
                Configs.RenderStyles.VERIFY_WRONG_BLOCK_COLOR.setValueFromString("#FF0000");
                Configs.RenderStyles.VERIFY_WRONG_STATE_COLOR.setValueFromString("#FFAF00");
                Configs.RenderStyles.VERIFY_EXTRA_COLOR.setValueFromString("#FF00CF");
                Configs.Generic.BLOCK_VERIFICATION_ENABLED.setBooleanValue(true);
                // 仿 Litematica: RENDER_ERROR_MARKER_SIDES=true（mismatch 填充面，始终穿墙）
                Configs.RenderStyles.RENDER_ERROR_MARKER_SIDES.setBooleanValue(true);
                Configs.RenderStyles.VERIFY_HILIGHT_ALPHA.setDoubleValue(0.2);
                // 仿 Litematica: OVERLAY_ENABLE_SIDES=true（带深度染色面，落地感）
                Configs.RenderStyles.OVERLAY_ENABLE_SIDES.setBooleanValue(true);
            }
            case DEFAULT -> {
                // WeGui 原始风格：黄色粘贴外框、绿色半透明面、每方块边框关闭
                Configs.RenderStyles.SELECTION_POS1_COLOR.setValueFromString("#FF0F0FFF");
                Configs.RenderStyles.SELECTION_POS2_COLOR.setValueFromString("#0F0FFFFF");
                Configs.RenderStyles.PASTE_BOX_COLOR.setValueFromString("#FFD100FF");
                Configs.RenderStyles.BLOCK_OVERLAY_COLOR.setValueFromString("#00FF80FF");
                Configs.RenderStyles.RENDER_BLOCKS_AS_TRANSLUCENT.setBooleanValue(true);
                Configs.RenderStyles.GHOST_BLOCK_ALPHA.setDoubleValue(0.5);
                Configs.RenderStyles.RENDER_PASTE_BOX_SIDES.setBooleanValue(true);
                Configs.RenderStyles.PASTE_BOX_SIDE_ALPHA.setDoubleValue(0.2);
                Configs.Generic.BLOCK_OUTLINE_ENABLED.setBooleanValue(false);
                Configs.Generic.BLOCK_VERIFICATION_ENABLED.setBooleanValue(false);
                Configs.RenderStyles.RENDER_ERROR_MARKER_SIDES.setBooleanValue(true);
                Configs.RenderStyles.VERIFY_HILIGHT_ALPHA.setDoubleValue(0.2);
                Configs.RenderStyles.OVERLAY_ENABLE_SIDES.setBooleanValue(false);
            }
        }
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
        if (!Configs.RenderStyles.ENABLE_RENDERING.getBooleanValue()) return;
        if (!Configs.RenderStyles.ENABLE_GHOST_BLOCKS.getBooleanValue()) return;

        // 检测渲染风格预设变化
        checkPresetChange();

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

        // 实心模式（仿 Litematica RENDER_BLOCKS_AS_TRANSLUCENT=false）：使用原生 RenderType，
        // 方块以完整不透明度渲染（SOLID/CUTOUT/CUTOUT_MIPPED/TRANSLUCENT 各自正确）。
        // 半透明模式：使用 AlphaMultiBufferSource 统一施加 alpha 倍率。
        boolean solid = !Configs.RenderStyles.RENDER_BLOCKS_AS_TRANSLUCENT.getBooleanValue();
        float alpha = solid ? 1.0f : (float) Configs.RenderStyles.GHOST_BLOCK_ALPHA.getDoubleValue();
        if (alpha <= 0.0f) return;

        int renderDistance = Configs.RenderStyles.GHOST_RENDER_DISTANCE.getIntegerValue();
        double renderDistSq = (double) renderDistance * renderDistance;

        // 光照：默认全亮度；启用假光照时使用固定光照等级（仿 Litematica FAKE_LIGHTING）
        boolean fakeLighting = Configs.RenderStyles.ENABLE_FAKE_LIGHTING.getBooleanValue();
        int lightLevel = fakeLighting ? Configs.RenderStyles.RENDER_FAKE_LIGHTING_LEVEL.getIntegerValue() : 15;
        int blockLight = fakeLighting ? lightLevel : 15;
        int skyLight = fakeLighting ? lightLevel : 15;
        int packedLight = LightTexture.pack(blockLight, skyLight);

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
        // 实心模式直接用 source（vanilla RenderType）；半透明模式用 AlphaMultiBufferSource
        MultiBufferSource renderSource = solid ? source : new AlphaMultiBufferSource(source, alpha);

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
                    dispatcher.renderSingleBlock(state, pose, renderSource, packedLight, OverlayTexture.NO_OVERLAY);
                    pose.popPose();
                } catch (Throwable e) {
                    WeGuiMod.LOGGER.debug("跳过无法渲染的预览方块: {}", e.toString());
                }
            }
        }

        pose.popPose();
    }

    // ========================================================================
    // 层 ①②④⑤：overlays（IRenderer.onRenderWorldLastAdvanced）
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
        if (!Configs.RenderStyles.ENABLE_RENDERING.getBooleanValue()) return;

        profiler.push("wegui_overlays");

        // 层 ①：WorldEdit 选区
        if (Configs.Generic.SELECTION_BOUNDS_ENABLED.getBooleanValue()
                && Configs.RenderStyles.ENABLE_SELECTION_RENDERING.getBooleanValue()) {
            profiler.push("selection");
            renderSelectionBounds(mc);
            profiler.pop();
        }

        // 层 ②④⑤：paste preview
        if (Configs.Generic.PASTE_PREVIEW_ENABLED.getBooleanValue()) {
            BlockPos origin = getEffectiveOrigin(mc);
            Map<BlockPos, BlockState> blocks = getClipboardBlocksCached(mc, origin);
            if (blocks != null && !blocks.isEmpty()) {
                ensureChunkCache(blocks, origin);

                // 层 ②：paste 外框 + 半透明面
                profiler.push("paste_box");
                renderPasteBox(frustum);
                profiler.pop();

                // 层 ④：Mismatch 渲染（始终穿墙，仅验证模式，复刻 Litematica renderSchematicMismatches）
                if (Configs.Generic.BLOCK_VERIFICATION_ENABLED.getBooleanValue()) {
                    profiler.push("mismatch");
                    renderSchematicMismatches(frustum, camera, origin, mc.level);
                    profiler.pop();
                }

                // 层 ⑤：Schematic Overlay（可配置穿墙，复刻 Litematica renderBlockOverlays）
                // 始终按验证状态着色（CORRECT 跳过，WRONG_BLOCK/WRONG_STATE/MISSING/EXTRA 各自颜色）
                if (Configs.RenderStyles.ENABLE_OVERLAY.getBooleanValue()) {
                    profiler.push("schematic_overlay");
                    renderSchematicOverlay(frustum, camera, origin, mc.level);
                    profiler.pop();
                }
            }
        }

        profiler.pop();
    }

    // ------------------------------------------------------------------
    // 层 ①：选区大框 + pos1/pos2 角点框（复刻 Litematica OverlayRenderer.renderSelectionBox 的 AREA_SELECTED 类型）
    // ------------------------------------------------------------------

    private void renderSelectionBounds(Minecraft mc) {
        // 选区大框（使用 RGB 轴色 colorX/Y/Z，对齐 Litematica AREA_SELECTED）
        try {
            WorldEditBridge.Bounds sel = WorldEditBridge.getSelectionBounds(mc);
            if (sel != null && sel.w() > 0 && sel.h() > 0 && sel.l() > 0) {
                BlockPos pos1 = new BlockPos(sel.minX(), sel.minY(), sel.minZ());
                BlockPos pos2 = new BlockPos(sel.minX() + sel.w() - 1, sel.minY() + sel.h() - 1, sel.minZ() + sel.l() - 1);
                // 选区大框使用 RGB 轴色（Litematica AREA_SELECTED 类型）
                RenderUtils.renderAreaOutlineNoCorners(pos1, pos2, LINE_WIDTH_AREA, COLOR_X, COLOR_Y, COLOR_Z);
                // 可选半透明面：RENDER_AREA_SELECTION_BOX_SIDES 开关 + AREA_SELECTION_BOX_SIDE_COLOR
                if (Configs.RenderStyles.RENDER_AREA_SELECTION_BOX_SIDES.getBooleanValue()) {
                    var sideC = Configs.RenderStyles.AREA_SELECTION_BOX_SIDE_COLOR.getColor();
                    Color4f sideColor = new Color4f(sideC.r, sideC.g, sideC.b, sideC.a);
                    Matrix4f matrix = RenderSystem.getModelViewStack();
                    RenderUtils.renderAreaSides(pos1, pos2, sideColor, matrix);
                }
            }
        } catch (Throwable e) {
            WeGuiMod.LOGGER.debug("渲染选区大框时出错: {}", e.toString());
        }

        // pos1/pos2 角点框（使用 SELECTION_POS1_COLOR / SELECTION_POS2_COLOR，默认值对齐 Litematica colorPos1/colorPos2）
        try {
            WorldEditBridge.PartialCornerPositions partial = WorldEditBridge.getPartialSelectionCorners(mc);
            if (partial != null) {
                if (partial.pos1() != null) {
                    var c1 = Configs.RenderStyles.SELECTION_POS1_COLOR.getColor();
                    Color4f color1 = new Color4f(c1.r, c1.g, c1.b, c1.a);
                    RenderUtils.renderBlockOutline(partial.pos1(), OUTLINE_EXPAND, LINE_WIDTH_BLOCK_BOX, color1, false);
                }
                if (partial.pos2() != null) {
                    var c2 = Configs.RenderStyles.SELECTION_POS2_COLOR.getColor();
                    Color4f color2 = new Color4f(c2.r, c2.g, c2.b, c2.a);
                    RenderUtils.renderBlockOutline(partial.pos2(), OUTLINE_EXPAND, LINE_WIDTH_BLOCK_BOX, color2, false);
                }
            }
        } catch (Throwable e) {
            WeGuiMod.LOGGER.debug("渲染选点角框时出错: {}", e.toString());
        }
    }

    // ------------------------------------------------------------------
    // 层 ②：paste 整体外框 + 半透明面（复刻 Litematica OverlayRenderer.renderSelectionBox 的 PLACEMENT_UNSELECTED 类型）
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

        // 粘贴外框颜色（PASTE_BOX_COLOR，默认白色对齐 Litematica colorArea）
        var c = Configs.RenderStyles.PASTE_BOX_COLOR.getColor();
        Color4f outlineColor = new Color4f(c.r, c.g, c.b, 1.0f);

        // 外框线（线宽 LINE_WIDTH_AREA = 1.5f，与 Litematica placement box lineWidthArea 一致）
        RenderUtils.renderAreaOutlineNoCorners(pos1, pos2, LINE_WIDTH_AREA, outlineColor, outlineColor, outlineColor);

        // 半透明面：需 RENDER_PASTE_BOX_SIDES 开关且 alpha > 0
        float sideAlpha = (float) Configs.RenderStyles.PASTE_BOX_SIDE_ALPHA.getDoubleValue();
        if (Configs.RenderStyles.RENDER_PASTE_BOX_SIDES.getBooleanValue() && sideAlpha > 0.0f) {
            Color4f sideColor = new Color4f(c.r, c.g, c.b, sideAlpha);
            Matrix4f matrix = RenderSystem.getModelViewStack();
            RenderUtils.renderAreaSides(pos1, pos2, sideColor, matrix);
        }
    }

    // ------------------------------------------------------------------
    // 层 ④：Mismatch 渲染（复刻 Litematica OverlayRenderer.renderSchematicMismatches）
    // ------------------------------------------------------------------

    /**
     * 方块验证状态（仿 Litematica OverlayType）。
     * 对比剪贴板期望状态与世界实际状态，得出 5 种结果：
     * - CORRECT：方块类型与状态完全一致
     * - MISSING：期望非空气，但世界中为空气
     * - WRONG_BLOCK：方块类型不同
     * - WRONG_STATE：方块类型相同但状态不同（如朝向、半边等）
     * - EXTRA：期望为空气，但世界中存在方块（多余方块）
     */
    private enum BlockMatchStatus {
        CORRECT, MISSING, WRONG_BLOCK, WRONG_STATE, EXTRA
    }

    /**
     * 对比期望方块状态与世界实际方块状态，返回验证结果。
     * 严格复刻 Litematica {@code ChunkRendererSchematicVbo.getOverlayType} 的判断顺序：
     * <ol>
     *   <li>{@code stateSchematic == stateClient}（引用相等，BlockState 已规范化）→ CORRECT</li>
     *   <li>两者皆空气 → CORRECT</li>
     *   <li>期望非空 + 实际空气 → MISSING（青色）</li>
     *   <li>期望空气 + 实际非空 → EXTRA（品红）</li>
     *   <li>方块类型不同 → WRONG_BLOCK（红色）</li>
     *   <li>方块类型相同但状态不同 → WRONG_STATE（橙色）</li>
     * </ol>
     * 与 Litematica 一致使用 {@code ==} 引用比较（BlockState 在注册表中规范化）。
     */
    private BlockMatchStatus verifyBlock(BlockState expected, BlockState found) {
        // Litematica 第一步：引用相等（规范化的 BlockState）
        if (expected == found) {
            return BlockMatchStatus.CORRECT;
        }
        boolean expectedAir = expected.isAir();
        boolean foundAir = found.isAir();
        if (expectedAir && foundAir) {
            return BlockMatchStatus.CORRECT;
        }
        if (expectedAir) {
            // 期望空气，实际非空气 → EXTRA
            return BlockMatchStatus.EXTRA;
        }
        if (foundAir) {
            // 期望非空气，实际空气 → MISSING
            return BlockMatchStatus.MISSING;
        }
        // 两者均非空气
        if (expected.getBlock() != found.getBlock()) {
            // 方块类型不同 → WRONG_BLOCK
            // 注：Litematica 在此处还有 DIFF_BLOCK 分支（同组方块），需要 malilib BlockUtils.isInSameGroup，
            // 当前实现跳过该细分，统一作为 WRONG_BLOCK 处理
            return BlockMatchStatus.WRONG_BLOCK;
        }
        // 方块类型相同但状态不同 → WRONG_STATE
        return BlockMatchStatus.WRONG_STATE;
    }

    /**
     * 获取 mismatch 渲染颜色（VERIFY_*_COLOR，对齐 Litematica MismatchType.getColor()）。
     * CORRECT 返回 null（不渲染）。
     */
    @Nullable
    private Color4f getVerifyColor(BlockMatchStatus status) {
        var c = switch (status) {
            case CORRECT -> null;
            case MISSING -> Configs.RenderStyles.VERIFY_MISSING_COLOR.getColor();
            case WRONG_BLOCK -> Configs.RenderStyles.VERIFY_WRONG_BLOCK_COLOR.getColor();
            case WRONG_STATE -> Configs.RenderStyles.VERIFY_WRONG_STATE_COLOR.getColor();
            case EXTRA -> Configs.RenderStyles.VERIFY_EXTRA_COLOR.getColor();
        };
        return c == null ? null : new Color4f(c.r, c.g, c.b, c.a);
    }

    /** 判断该 mismatch 类型是否应被渲染（受 OVERLAY_TYPE_* 开关控制，对齐 Litematica SCHEMATIC_OVERLAY_TYPE_*） */
    private boolean shouldRenderMismatchType(BlockMatchStatus status) {
        return switch (status) {
            case MISSING -> Configs.RenderStyles.OVERLAY_TYPE_MISSING.getBooleanValue();
            case WRONG_BLOCK -> Configs.RenderStyles.OVERLAY_TYPE_WRONG_BLOCK.getBooleanValue();
            case WRONG_STATE -> Configs.RenderStyles.OVERLAY_TYPE_WRONG_STATE.getBooleanValue();
            case EXTRA -> Configs.RenderStyles.OVERLAY_TYPE_EXTRA.getBooleanValue();
            case CORRECT -> false;
        };
    }

    /**
     * 获取玩家当前注视的方块位置（仅在注视方块时返回非 null）。
     * 用于 Litematica 风格的注视方块加粗渲染。
     */
    @Nullable
    private BlockPos getLookPos(Minecraft mc) {
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            return ((BlockHitResult) mc.hitResult).getBlockPos();
        }
        return null;
    }

    /**
     * Mismatch 渲染（复刻 Litematica OverlayRenderer.renderSchematicMismatches）
     *
     * <p>始终穿墙（NO_DEPTH_NO_CULL），仅渲染 mismatch 方块，使用 VERIFY_*_COLOR。
     * 不受 OVERLAY_RENDER_THROUGH 控制（始终穿墙可见，与 Litematica 一致）。</p>
     *
     * <p>层1: 边框线 (batched_lines) — 所有 mismatch 方块（排除注视方块），lineWidth=2.0f<br>
     * 层2: 注视方块加粗 (outlines) — 仅注视的 mismatch 方块，lineWidth=6.0f<br>
     * 层3: 半透明填充面 (side_quads) — 所有 mismatch 方块，alpha=VERIFY_HILIGHT_ALPHA</p>
     */
    private void renderSchematicMismatches(Frustum frustum, Camera camera, BlockPos origin, Level level) {
        if (chunkCache.isEmpty()) return;
        if (!Configs.RenderStyles.ENABLE_OVERLAY.getBooleanValue()) return;
        if (!Configs.Generic.BLOCK_VERIFICATION_ENABLED.getBooleanValue()) return;

        Minecraft mc = Minecraft.getInstance();
        int renderDistance = Configs.RenderStyles.GHOST_RENDER_DISTANCE.getIntegerValue();
        double renderDistSq = (double) renderDistance * renderDistance;
        Vec3 camPos = camera.position();

        boolean enableCulling = Configs.RenderStyles.ENABLE_OVERLAY_CULLING.getBooleanValue();
        boolean reducedInnerSides = Configs.RenderStyles.OVERLAY_REDUCED_INNER_SIDES.getBooleanValue();
        boolean renderConnections = Configs.RenderStyles.RENDER_ERROR_MARKER_CONNECTIONS.getBooleanValue();
        boolean enableOutlines = Configs.RenderStyles.OVERLAY_ENABLE_OUTLINES.getBooleanValue();
        boolean renderSides = Configs.RenderStyles.RENDER_ERROR_MARKER_SIDES.getBooleanValue();
        float sideAlpha = (float) Configs.RenderStyles.VERIFY_HILIGHT_ALPHA.getDoubleValue();

        BlockPos lookPos = getLookPos(mc);
        BlockPos lookedTarget = null;
        Color4f lookedColor = null;

        // 收集 mismatch 方块（带 culling、距离剔除、类型过滤）
        List<BlockPos> mismatchPositions = new ArrayList<>();
        List<Color4f> mismatchColors = new ArrayList<>();

        for (ClipboardChunkCache.ChunkGroup group : chunkCache.getGroups()) {
            if (enableCulling && !frustum.isVisible(group.worldAabb)) continue;

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
                BlockState expected = cachedBlocks != null ? cachedBlocks.get(rel) : null;
                if (expected == null) continue;
                BlockState found = level.getBlockState(target);
                BlockMatchStatus status = verifyBlock(expected, found);
                if (!shouldRenderMismatchType(status)) continue;
                Color4f color = getVerifyColor(status);
                if (color == null) continue;
                mismatchPositions.add(target);
                mismatchColors.add(color);
                if (lookPos != null && lookPos.equals(target)) {
                    lookedTarget = target;
                    lookedColor = color;
                }
            }
        }

        // 位置限制（按距离截断，对齐 Litematica VERIFIER_ERROR_HILIGHT_MAX_POSITIONS）
        if (!mismatchPositions.isEmpty()) {
            int maxPositions = Configs.RenderStyles.VERIFIER_ERROR_HILIGHT_MAX_POSITIONS.getIntegerValue();
            if (mismatchPositions.size() > maxPositions) {
                final List<BlockPos> positionsForSort = mismatchPositions;
                final List<Color4f> colorsForSort = mismatchColors;
                List<Integer> indices = new ArrayList<>();
                for (int i = 0; i < positionsForSort.size(); i++) indices.add(i);
                indices.sort((a, b) -> {
                    BlockPos pa = positionsForSort.get(a);
                    BlockPos pb = positionsForSort.get(b);
                    double da = (pa.getX() - camPos.x) * (pa.getX() - camPos.x)
                            + (pa.getY() - camPos.y) * (pa.getY() - camPos.y)
                            + (pa.getZ() - camPos.z) * (pa.getZ() - camPos.z);
                    double db = (pb.getX() - camPos.x) * (pb.getX() - camPos.x)
                            + (pb.getY() - camPos.y) * (pb.getY() - camPos.y)
                            + (pb.getZ() - camPos.z) * (pb.getZ() - camPos.z);
                    return Double.compare(da, db);
                });
                List<BlockPos> truncPos = new ArrayList<>();
                List<Color4f> truncCol = new ArrayList<>();
                for (int i = 0; i < maxPositions; i++) {
                    int idx = indices.get(i);
                    truncPos.add(positionsForSort.get(idx));
                    truncCol.add(colorsForSort.get(idx));
                }
                mismatchPositions = truncPos;
                mismatchColors = truncCol;
                if (lookedTarget != null && !mismatchPositions.contains(lookedTarget)) {
                    lookedTarget = null;
                    lookedColor = null;
                }
            }
        }

        // Mismatch 始终使用 NO_DEPTH_NO_CULL（始终穿墙，不可配置，与 Litematica 一致）
        RenderPipeline linePipeline = MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_NO_DEPTH_NO_CULL;
        RenderPipeline sidePipeline = MaLiLibPipelines.POSITION_COLOR_TRANSLUCENT_NO_DEPTH_NO_CULL;

        // ========== 层1: 边框线 + 层2: 注视方块加粗 ==========
        if (enableOutlines && !mismatchPositions.isEmpty()) {
            try (RenderContext ctx = new RenderContext(
                    () -> "wegui:mismatch/batched_lines",
                    linePipeline)) {
                BufferBuilder buffer = ctx.start(
                        () -> "wegui:mismatch/batched_lines",
                        linePipeline);

                BlockPos prevPos = null;
                for (int i = 0; i < mismatchPositions.size(); i++) {
                    BlockPos target = mismatchPositions.get(i);
                    Color4f color = mismatchColors.get(i);
                    if (target.equals(lookedTarget)) {
                        prevPos = target;
                        continue;
                    }
                    RenderUtils.drawBlockBoundingBoxOutlinesBatchedLinesSimple(target, color, OUTLINE_EXPAND, LINE_WIDTH_MISMATCH, buffer);
                    if (renderConnections && prevPos != null && !prevPos.equals(target)) {
                        RenderUtils.drawConnectingLineBatchedLines(prevPos, target, false, color, LINE_WIDTH_MISMATCH, buffer);
                    }
                    prevPos = target;
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

                // ========== 层2: 注视方块加粗 ==========
                if (lookedTarget != null && lookedColor != null) {
                    BufferBuilder boldBuffer = ctx.start(
                            () -> "wegui:mismatch/outlines",
                            linePipeline);
                    RenderUtils.drawBlockBoundingBoxOutlinesBatchedLinesSimple(lookedTarget, lookedColor, OUTLINE_EXPAND, LINE_WIDTH_MISMATCH_LOOKED, boldBuffer);
                    try {
                        MeshData meshData = boldBuffer.build();
                        if (meshData != null) {
                            ctx.draw(meshData, false, true);
                            meshData.close();
                        }
                        ctx.reset();
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // ========== 层3: mismatch 填充面（始终穿墙 NO_DEPTH_NO_CULL）==========
        if (renderSides && !mismatchPositions.isEmpty()) {
            try (RenderContext sideCtx = new RenderContext(
                    () -> "wegui:mismatch/side_quads",
                    sidePipeline)) {
                BufferBuilder sideBuffer = sideCtx.start(
                        () -> "wegui:mismatch/side_quads",
                        sidePipeline);
                for (int i = 0; i < mismatchPositions.size(); i++) {
                    BlockPos pos = mismatchPositions.get(i);
                    if (reducedInnerSides && !isBlockExposed(pos, level)) continue;
                    Color4f c = mismatchColors.get(i);
                    Color4f sideColor = new Color4f(c.r, c.g, c.b, sideAlpha);
                    RenderUtils.renderAreaSidesBatched(pos, pos, sideColor, OUTLINE_EXPAND, sideBuffer);
                }
                MeshData sideMesh = sideBuffer.build();
                if (sideMesh != null) {
                    sideCtx.draw(sideMesh, false, false);
                    sideMesh.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Schematic Overlay 渲染（复刻 Litematica WorldRendererSchematic.renderBlockOverlays）
     *
     * <p>严格复刻 Litematica {@code ChunkRendererSchematicVbo.getOverlayType} + {@code getOverlayColor}：
     * 始终按方块验证状态着色，CORRECT 不渲染（仅 ghost block 显示原纹理）。</p>
     *
     * <p>着色规则（与 Litematica 完全一致）：
     * <ul>
     *   <li>CORRECT（方块+状态相同）→ 不渲染 overlay</li>
     *   <li>WRONG_BLOCK（方块类型不同）→ 红色 #FF0000</li>
     *   <li>WRONG_STATE（方块相同，状态不同）→ 橙色 #FFAF00</li>
     *   <li>MISSING（期望非空，实际空气）→ 青色 #00FFFF</li>
     *   <li>EXTRA（期望空气，实际非空）→ 品红 #FF00CF</li>
     * </ul></p>
     *
     * <p>可配置穿墙（OVERLAY_RENDER_THROUGH），正常模式使用 OFFSET_2 管线防 z-fighting。</p>
     *
     * <p>层1: 边框线 (batched_lines) — OVERLAY_OUTLINE_WIDTH / OVERLAY_OUTLINE_WIDTH_THROUGH<br>
     * 层2: 填充面 (side_quads) — DEFAULT_SIDE_ALPHA，受 OVERLAY_ENABLE_SIDES 控制</p>
     */
    private void renderSchematicOverlay(Frustum frustum, Camera camera, BlockPos origin, Level level) {
        if (chunkCache.isEmpty()) return;
        if (!Configs.RenderStyles.ENABLE_OVERLAY.getBooleanValue()) return;

        int renderDistance = Configs.RenderStyles.GHOST_RENDER_DISTANCE.getIntegerValue();
        double renderDistSq = (double) renderDistance * renderDistance;
        Vec3 camPos = camera.position();

        // 透墙模式：OVERLAY_RENDER_THROUGH 控制
        boolean renderThrough = Configs.RenderStyles.OVERLAY_RENDER_THROUGH.getBooleanValue();
        float lineWidth = renderThrough
                ? (float) Configs.RenderStyles.OVERLAY_OUTLINE_WIDTH_THROUGH.getDoubleValue()
                : (float) Configs.RenderStyles.OVERLAY_OUTLINE_WIDTH.getDoubleValue();
        // 正常模式用 OFFSET_2（带 polygon offset 防 z-fighting），透墙模式用 NO_DEPTH_NO_CULL
        RenderPipeline linePipeline = renderThrough
                ? MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_NO_DEPTH_NO_CULL
                : MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_OFFSET_2;
        RenderPipeline sidePipeline = renderThrough
                ? MaLiLibPipelines.POSITION_COLOR_TRANSLUCENT_NO_DEPTH_NO_CULL
                : MaLiLibPipelines.POSITION_COLOR_TRANSLUCENT_LEQUAL_DEPTH_OFFSET_2;

        boolean enableCulling = Configs.RenderStyles.ENABLE_OVERLAY_CULLING.getBooleanValue();
        boolean reducedInnerSides = Configs.RenderStyles.OVERLAY_REDUCED_INNER_SIDES.getBooleanValue();
        boolean enableOutlines = Configs.RenderStyles.OVERLAY_ENABLE_OUTLINES.getBooleanValue();
        boolean enableSides = Configs.RenderStyles.OVERLAY_ENABLE_SIDES.getBooleanValue();
        float sideAlpha = DEFAULT_SIDE_ALPHA;

        // 严格复刻 Litematica ChunkRendererSchematicVbo.getOverlayType + getOverlayColor：
        // overlay 始终按方块验证状态着色，CORRECT 不渲染（仅 ghost block 显示原纹理），
        // WRONG_BLOCK=红 / WRONG_STATE=橙 / MISSING=青 / EXTRA=品红。
        // 不再使用单一颜色模式（BLOCK_OVERLAY_COLOR 已废弃）。

        // 收集需要渲染的方块
        List<BlockPos> overlayPositions = new ArrayList<>();
        List<Color4f> overlayColors = new ArrayList<>();

        for (ClipboardChunkCache.ChunkGroup group : chunkCache.getGroups()) {
            if (enableCulling && !frustum.isVisible(group.worldAabb)) continue;

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
                BlockState expected = cachedBlocks != null ? cachedBlocks.get(rel) : null;
                if (expected == null) continue;
                BlockState found = level.getBlockState(target);
                BlockMatchStatus status = verifyBlock(expected, found);
                // CORRECT 不渲染 overlay（Litematica getOverlayColor(NONE) 返回 null）
                if (!shouldRenderMismatchType(status)) continue;
                Color4f color = getVerifyColor(status);
                if (color == null) continue;
                overlayPositions.add(target);
                overlayColors.add(color);
            }
        }

        // ========== 层1: 边框线 ==========
        if (enableOutlines && !overlayPositions.isEmpty()) {
            try (RenderContext ctx = new RenderContext(
                    () -> "wegui:overlay/batched_lines",
                    linePipeline)) {
                BufferBuilder buffer = ctx.start(
                        () -> "wegui:overlay/batched_lines",
                        linePipeline);

                for (int i = 0; i < overlayPositions.size(); i++) {
                    BlockPos target = overlayPositions.get(i);
                    Color4f color = overlayColors.get(i);
                    RenderUtils.drawBlockBoundingBoxOutlinesBatchedLinesSimple(target, color, OUTLINE_EXPAND, lineWidth, buffer);
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
            } catch (Exception ignored) {
            }
        }

        // ========== 层2: 填充面 ==========
        if (enableSides && !overlayPositions.isEmpty()) {
            try (RenderContext sideCtx = new RenderContext(
                    () -> "wegui:overlay/side_quads",
                    sidePipeline)) {
                BufferBuilder sideBuffer = sideCtx.start(
                        () -> "wegui:overlay/side_quads",
                        sidePipeline);
                for (int i = 0; i < overlayPositions.size(); i++) {
                    BlockPos pos = overlayPositions.get(i);
                    if (reducedInnerSides && !isBlockExposed(pos, level)) continue;
                    Color4f c = overlayColors.get(i);
                    Color4f sideColor = new Color4f(c.r, c.g, c.b, sideAlpha);
                    RenderUtils.renderAreaSidesBatched(pos, pos, sideColor, OUTLINE_EXPAND, sideBuffer);
                }
                MeshData sideMesh = sideBuffer.build();
                if (sideMesh != null) {
                    sideCtx.draw(sideMesh, false, false);
                    sideMesh.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 检查方块是否有暴露面（任一邻居为空气或不同类型方块）。
     * 用于 OVERLAY_REDUCED_INNER_SIDES 优化，跳过完全被同类型方块包围的内部方块。
     */
    private boolean isBlockExposed(BlockPos pos, Level level) {
        BlockState self = level.getBlockState(pos);
        if (level.getBlockState(pos.above()).isAir()) return true;
        if (level.getBlockState(pos.below()).isAir()) return true;
        if (level.getBlockState(pos.north()).isAir()) return true;
        if (level.getBlockState(pos.south()).isAir()) return true;
        if (level.getBlockState(pos.east()).isAir()) return true;
        if (level.getBlockState(pos.west()).isAir()) return true;
        // 检查不同类型方块
        if (level.getBlockState(pos.above()).getBlock() != self.getBlock()) return true;
        if (level.getBlockState(pos.below()).getBlock() != self.getBlock()) return true;
        if (level.getBlockState(pos.north()).getBlock() != self.getBlock()) return true;
        if (level.getBlockState(pos.south()).getBlock() != self.getBlock()) return true;
        if (level.getBlockState(pos.east()).getBlock() != self.getBlock()) return true;
        if (level.getBlockState(pos.west()).getBlock() != self.getBlock()) return true;
        return false;
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
