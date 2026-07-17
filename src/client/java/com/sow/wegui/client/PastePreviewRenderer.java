package com.sow.wegui.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
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
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;

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
 *   <li>层④ Mismatch 渲染（验证模式，边框线有遮挡）— 对齐 {@code OverlayRenderer.renderSchematicMismatches}：
 *     DEBUG_LINES_MASA_SIMPLE_OFFSET_2 管线（有深度测试），lineWidth=2.0f，注视方块加粗 lineWidth=6.0f</li>
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
    // 基于 RenderPipelines.TRANSLUCENT_MOVING_BLOCK，关键调整：
    // 1. 禁用背面剔除（cull=false）：确保半透明 ghost block 的所有面都被渲染
    // 2. 显式开启深度写入（withDepthWrite=true）：让 ghost block 之间正确遮挡（解决透视问题）
    // 3. 极小深度偏移（withDepthBias=0.0f, -1.0f）：offset = 0*m + r*(-1) = -r（r 是最小可分辨深度差）
    //    - 对所有 ghost block 施加相同的极小偏移，保留它们之间的相对深度差异 → ghost block 之间正确遮挡
    //    - 让 ghost block 比同位置世界方块"近一点点"（-r），避免被同位置世界方块完全遮挡 → 纹理可见
    //    - 注意：不能用 withDepthBias(-1.0f, -10.0f)（原版 CRUMBLING/TEXT 用的值），
    //      那会让 offset 达到 -1，把所有 ghost block 深度推到 near plane，导致相互之间无法区分深度（透视）
    //    Litematica 原版用独立 schematic world 隔离深度，WeGui 用极小深度偏移近似。
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
            .withDepthWrite(true)
            .withDepthBias(0.0f, -1.0f)
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

    // ---- 验证结果跨帧缓存（ghost blocks / mismatch / overlay 三处共享，避免每帧 N 次 getBlockState + verifyBlock）----
    // 世界方块变化频率低（玩家放置/破坏方块），每 VERIFICATION_UPDATE_INTERVAL tick 重新验证一次即可。
    private Map<BlockPos, BlockMatchStatus> verificationCache;
    private BlockPos verificationCacheOrigin;
    private long verificationCacheTick = -1;
    private static final int VERIFICATION_UPDATE_INTERVAL = 10;  // 0.5 秒

    // ---- mismatch 位置列表跨帧缓存（对齐 Litematica SchematicVerifier.getSelectedMismatchPositionsForRender）----
    // mismatch 渲染和 overlay 渲染共享此列表，避免每帧重复遍历所有方块收集 mismatch 位置。
    // 缓存 key: (origin, verificationCacheTick)。verificationCache 重建或 chunkCache 重建时自动失效。
    private List<BlockPos> mismatchPositionsCache;
    private List<BlockMatchStatus> mismatchStatusesCache;
    private BlockPos mismatchCacheOrigin;
    private long mismatchCacheKeyTick = -1;

    // ---- ghost block 持久化 GpuBuffer 缓存（避免每帧 upload vertex buffer 的 GPU 传输开销）----
    // 顶点用 relPos（相对 origin 的偏移，不依赖 origin），缓存可跨 origin 变化复用。
    // 每帧渲染时 modelViewStack 已含相机旋转 R（Minecraft 在 BEFORE_TRANSLUCENT 前设置），
    // 我们只需 pushMatrix + translate(origin - cam)：最终顶点位置 = R × (relPos + origin - cam) = R × (worldPos - cam)。
    //
    // 关键性能优化：直接用 GpuBuffer + RenderPass API 绘制（对齐 Litematica ChunkRenderGpuUploader），
    // 而非走 RenderType.draw —— RenderType.draw 内部 uploadImmediateVertexBuffer 每帧上传 vertex buffer 到 GPU
    // （~10ms for 67MB），且 meshData.close() 每次绘制后关闭 MeshData，导致缓存根本无法复用。
    // 持久化方案：数据上传一次到 GpuBuffer，每帧只创建 RenderPass + drawIndexed，零数据传输。
    //
    // 失效条件：solid/alpha/packedLight/verifTick 变化（不含 origin，因顶点用 relPos）。
    private GpuBuffer ghostVertexGpuBuffer;
    private int ghostIndexCount;
    private boolean ghostGpuBufferDirty = true;
    private boolean cachedMeshSolid;
    private float cachedMeshAlpha = -1.0f;
    private int cachedMeshPackedLight;
    private long cachedMeshVerifTick = -1;

    // ---- 固定放置模式 ----
    @Nullable
    private BlockPos fixedOrigin = null;
    private PastePlacementMode lastMode = PastePlacementMode.FOLLOW_PLAYER;

    // ---- 渲染风格预设 ----
    private RenderStylePreset lastPreset = RenderStylePreset.DEFAULT;
    /** 标记预设是否已初始化应用过，确保启动时强制应用一次以覆盖配置文件中的旧值 */
    private boolean presetInitialized = false;

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
     * 在每帧渲染时调用。启动时强制应用一次，覆盖配置文件中的旧值。
     */
    private void checkPresetChange() {
        RenderStylePreset current = (RenderStylePreset) Configs.PastePreview.RENDER_STYLE_PRESET.getOptionListValue();
        if (current != lastPreset || !presetInitialized) {
            applyPreset(current);
            lastPreset = current;
            presetInitialized = true;
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
                // Litematica 投影模组风格：半透明 schematic blocks（不写深度，可见纹理）+ 半透明 overlay 填充面 + 实线边框
                // 角点颜色对齐 Litematica colorPos1/colorPos2
                Configs.RenderStyles.SELECTION_POS1_COLOR.setValueFromString("#FF1010FF");
                Configs.RenderStyles.SELECTION_POS2_COLOR.setValueFromString("#1010FFFF");
                // 粘贴外框对齐 Litematica colorArea（白色）
                Configs.RenderStyles.PASTE_BOX_COLOR.setValueFromString("#FFFFFFFF");
                // 关键：RENDER_BLOCKS_AS_TRANSLUCENT=true，让 ghost block 通过 AlphaMultiBufferSource 渲染到
                // GHOST_BLOCK_TYPE（基于 TRANSLUCENT_MOVING_BLOCK，不写深度），这样 ghost block 不会被世界
                // 方块遮挡，纹理始终可见。GHOST_BLOCK_ALPHA=1.0 保持视觉上接近实心。
                // Litematica 原版用独立 schematic world 隔离深度，WeGui 无此架构，用半透明不写深度等效复现。
                Configs.RenderStyles.RENDER_BLOCKS_AS_TRANSLUCENT.setBooleanValue(true);
                Configs.RenderStyles.GHOST_BLOCK_ALPHA.setDoubleValue(1.0);
                // 仿 Litematica: RENDER_PASTE_BOX_SIDES=false（仅线框，无半透明面）
                Configs.RenderStyles.RENDER_PASTE_BOX_SIDES.setBooleanValue(false);
                Configs.RenderStyles.PASTE_BOX_SIDE_ALPHA.setDoubleValue(0.2);
                Configs.Generic.BLOCK_OUTLINE_ENABLED.setBooleanValue(true);
                // Litematica 方块验证配色（严格对齐 Configs.Colors.SCHEMATIC_OVERLAY_COLOR_*，#AARRGGBB）：
                // MISSING=#2C33B3E6（青）、WRONG_BLOCK=#4CFF3333（红）、WRONG_STATE=#4CFF9010（橙）、EXTRA=#4CFF4CE6（品红）
                Configs.RenderStyles.VERIFY_MISSING_COLOR.setValueFromString("#2C33B3E6");
                Configs.RenderStyles.VERIFY_WRONG_BLOCK_COLOR.setValueFromString("#4CFF3333");
                Configs.RenderStyles.VERIFY_WRONG_STATE_COLOR.setValueFromString("#4CFF9010");
                Configs.RenderStyles.VERIFY_EXTRA_COLOR.setValueFromString("#4CFF4CE6");
                Configs.Generic.BLOCK_VERIFICATION_ENABLED.setBooleanValue(true);
                // 禁用 mismatch 填充面，避免与 overlay 填充面叠加导致颜色变深遮挡纹理。
                // mismatch 只保留边框线（始终穿墙 + 注视方块加粗），overlay 负责半透明填充面。
                Configs.RenderStyles.RENDER_ERROR_MARKER_SIDES.setBooleanValue(false);
                Configs.RenderStyles.VERIFY_HILIGHT_ALPHA.setDoubleValue(0.2);
                // 关键：OVERLAY_ENABLE_SIDES=true，渲染半透明填充面（alpha 来自 Litematica 颜色 #AARRGGBB），
                // 透过半透明填充面可以看到 ghost block 纹理，完全复现 Litematica 的视觉效果
                Configs.RenderStyles.OVERLAY_ENABLE_SIDES.setBooleanValue(true);
            }
            case DEFAULT -> {
                // WeGui 默认风格：黄色粘贴外框 + Litematica 渲染机制
                // 保留 WeGui 特色的黄色外框和角点颜色，但渲染机制完全对齐 Litematica
                Configs.RenderStyles.SELECTION_POS1_COLOR.setValueFromString("#FF0F0FFF");
                Configs.RenderStyles.SELECTION_POS2_COLOR.setValueFromString("#0F0FFFFF");
                Configs.RenderStyles.PASTE_BOX_COLOR.setValueFromString("#FFD100FF");
                // 渲染机制对齐 Litematica（与 LITEMATICA 预设一致）：
                // 半透明 ghost block（不写深度，纹理可见）+ 验证状态着色 + 半透明 overlay 填充面
                Configs.RenderStyles.RENDER_BLOCKS_AS_TRANSLUCENT.setBooleanValue(true);
                Configs.RenderStyles.GHOST_BLOCK_ALPHA.setDoubleValue(1.0);
                Configs.RenderStyles.RENDER_PASTE_BOX_SIDES.setBooleanValue(false);
                Configs.RenderStyles.PASTE_BOX_SIDE_ALPHA.setDoubleValue(0.2);
                Configs.Generic.BLOCK_OUTLINE_ENABLED.setBooleanValue(true);
                // Litematica 方块验证配色（#AARRGGBB）
                Configs.RenderStyles.VERIFY_MISSING_COLOR.setValueFromString("#2C33B3E6");
                Configs.RenderStyles.VERIFY_WRONG_BLOCK_COLOR.setValueFromString("#4CFF3333");
                Configs.RenderStyles.VERIFY_WRONG_STATE_COLOR.setValueFromString("#4CFF9010");
                Configs.RenderStyles.VERIFY_EXTRA_COLOR.setValueFromString("#4CFF4CE6");
                Configs.Generic.BLOCK_VERIFICATION_ENABLED.setBooleanValue(true);
                Configs.RenderStyles.RENDER_ERROR_MARKER_SIDES.setBooleanValue(false);
                Configs.RenderStyles.VERIFY_HILIGHT_ALPHA.setDoubleValue(0.2);
                Configs.RenderStyles.OVERLAY_ENABLE_SIDES.setBooleanValue(true);
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

        // LITEMATICA 预设使用半透明模式（RENDER_BLOCKS_AS_TRANSLUCENT=true）：
        // 通过 putBulkData 的 alpha 参数把 alpha 写入顶点颜色 alpha 通道。
        // 实心模式 alpha=1.0（不透明），半透明模式 alpha<1.0（半透明，由 BlendFunction.TRANSLUCENT 混合）。
        boolean solid = !Configs.RenderStyles.RENDER_BLOCKS_AS_TRANSLUCENT.getBooleanValue();
        float alpha = solid ? 1.0f : (float) Configs.RenderStyles.GHOST_BLOCK_ALPHA.getDoubleValue();
        if (alpha <= 0.0f) return;

        // 光照：默认全亮度；启用假光照时使用固定光照等级（仿 Litematica FAKE_LIGHTING）
        boolean fakeLighting = Configs.RenderStyles.ENABLE_FAKE_LIGHTING.getBooleanValue();
        int lightLevel = fakeLighting ? Configs.RenderStyles.RENDER_FAKE_LIGHTING_LEVEL.getIntegerValue() : 15;
        int blockLight = fakeLighting ? lightLevel : 15;
        int skyLight = fakeLighting ? lightLevel : 15;
        int packedLight = LightTexture.pack(blockLight, skyLight);

        // 验证模式：方块种类错误（WRONG_BLOCK）或状态错误（WRONG_STATE）时，隐藏 ghost block 贴图。
        boolean verificationEnabled = Configs.Generic.BLOCK_VERIFICATION_ENABLED.getBooleanValue();
        Level level = mc.level;

        // 构建验证缓存（验证模式开启时，ghost blocks / mismatch / overlay 三处共享，跨帧缓存避免每帧 getBlockState）
        Map<BlockPos, BlockMatchStatus> verifCache = null;
        if (verificationEnabled) {
            verifCache = getVerificationCache(origin, level);
        }

        // 确保 ghost block BakedQuad 缓存已填充（避免每帧 renderSingleBlock 的模型查找/颜色计算开销）
        ensureGhostBlockRenderCache();

        // 检查 GpuBuffer 是否需要重建（不含 origin，因为顶点用 relPos 不依赖 origin）
        boolean needRebuild = ghostGpuBufferDirty
                || solid != cachedMeshSolid
                || alpha != cachedMeshAlpha
                || packedLight != cachedMeshPackedLight
                || (verificationEnabled && verificationCacheTick != cachedMeshVerifTick);
        if (needRebuild) {
            uploadGhostBlockGpuBuffer(solid, alpha, verifCache, verificationEnabled, packedLight);
        }
        if (ghostVertexGpuBuffer == null || ghostIndexCount == 0) return;

        // 每帧渲染：modelViewStack 已含相机旋转 R（由 Minecraft LevelRenderer 在 BEFORE_TRANSLUCENT 前设置）。
        // 只需 pushMatrix + translate(origin - cam)，让顶点 relPos 变换到相机空间：
        //   最终顶点位置 = R × (relPos + origin - cam) = R × (worldPos - cam)。
        // 对齐 Litematica WorldRendererSchematic.renderBlockOverlays 的 pushMatrix + translate(chunkOrigin - cam) 方案。
        Camera camera = context.gameRenderer().getMainCamera();
        Vec3 cam = camera.position();
        Matrix4fStack stack = RenderSystem.getModelViewStack();
        stack.pushMatrix();
        stack.translate(origin.getX() - (float) cam.x,
                        origin.getY() - (float) cam.y,
                        origin.getZ() - (float) cam.z);
        try {
            drawGhostBlocksFromGpu();
        } catch (Throwable e) {
            WeGuiMod.LOGGER.debug("WeGui ghost blocks draw 出错: {}", e.toString());
        } finally {
            stack.popMatrix();
        }
    }

    /**
     * 上传 ghost block 顶点到持久化 GpuBuffer（对齐 Litematica ChunkRenderGpuUploader.uploadBuffersByLayer）。
     *
     * <p>顶点用 <b>relPos</b>（方块相对 origin 的偏移），不依赖 origin，因此缓存可跨 origin 变化复用：
     * FOLLOW_PLAYER 模式下玩家每跨方块边界时 origin 变化，但缓存仍然有效，避免每帧 putBulkData。
     *
     * <p>每帧渲染时 modelViewStack 已含相机旋转 R（由 Minecraft 在 BEFORE_TRANSLUCENT 前设置），
     * 我们只需 pushMatrix + translate(origin - cam)，最终顶点位置 = R × (relPos + origin - cam) = R × (worldPos - cam)。
     *
     * <p>alpha 调制直接写入顶点颜色 alpha 通道（putBulkData 的 alpha 参数），无需 uniform。
     *
     * <p>关键优化：上传一次到 GpuBuffer 后立即 close MeshData 释放 CPU 端内存，
     * 之后每帧渲染直接用 GpuBuffer + RenderPass API 绘制，零数据传输（对比 RenderType.draw 每帧 upload）。
     *
     * <p>失效条件：solid/alpha/packedLight/verificationCacheTick 变化。
     * 不含 origin（顶点用 relPos），origin 变化由 modelViewStack 实时处理。
     */
    @SuppressWarnings("unchecked")
    private void uploadGhostBlockGpuBuffer(boolean solid, float alpha,
            Map<BlockPos, BlockMatchStatus> verifCache, boolean verificationEnabled, int packedLight) {
        // 关闭旧 GpuBuffer
        if (ghostVertexGpuBuffer != null) {
            ghostVertexGpuBuffer.close();
            ghostVertexGpuBuffer = null;
        }
        ghostIndexCount = 0;

        ByteBufferBuilder alloc = new ByteBufferBuilder(2 * 1024 * 1024);
        BufferBuilder builder = new BufferBuilder(alloc, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
        // 顶点用 relPos（不依赖 origin），缓存可跨 origin 变化复用
        PoseStack pose = new PoseStack();
        PoseStack.Pose poseEntry = pose.last();
        Matrix4f poseMatrix = poseEntry.pose();
        boolean anyVertex = false;

        for (ClipboardChunkCache.ChunkGroup group : chunkCache.getGroups()) {
            List<CachedGhostBlock> cache = (List<CachedGhostBlock>) group.renderCache;
            if (cache == null) continue;
            for (CachedGhostBlock cached : cache) {
                try {
                    if (verificationEnabled && verifCache != null) {
                        BlockMatchStatus status = verifCache.get(cached.relPos);
                        if (status == BlockMatchStatus.WRONG_BLOCK
                                || status == BlockMatchStatus.WRONG_STATE) {
                            continue;
                        }
                    }
                    poseMatrix.setTranslation(cached.relPos.getX(), cached.relPos.getY(), cached.relPos.getZ());
                    for (BakedQuad quad : cached.allQuads) {
                        float r2 = quad.isTinted() ? cached.r : 1.0f;
                        float g2 = quad.isTinted() ? cached.g : 1.0f;
                        float b2 = quad.isTinted() ? cached.b : 1.0f;
                        builder.putBulkData(poseEntry, quad, r2, g2, b2, alpha, packedLight, OverlayTexture.NO_OVERLAY);
                        anyVertex = true;
                    }
                } catch (Throwable e) {
                    WeGuiMod.LOGGER.debug("跳过无法渲染的预览方块: {}", e.toString());
                }
            }
        }

        if (anyVertex) {
            MeshData meshData = null;
            try {
                meshData = builder.build();
                if (meshData != null) {
                    // 上传到持久化 GpuBuffer（usage=40 表示 VERTEX | COPY_DST，对齐 Litematica）
                    ghostVertexGpuBuffer = RenderSystem.getDevice().createBuffer(
                            () -> "wegui:ghost_block_vertices",
                            40,
                            meshData.vertexBuffer()
                    );
                    ghostIndexCount = meshData.drawState().indexCount();
                }
            } catch (Throwable e) {
                WeGuiMod.LOGGER.debug("ghost block GpuBuffer 构建失败: {}", e.toString());
            } finally {
                // 数据已在 GpuBuffer 中，立即关闭 MeshData 释放 CPU 端内存
                if (meshData != null) {
                    meshData.close();
                }
            }
        }
        cachedMeshSolid = solid;
        cachedMeshAlpha = alpha;
        cachedMeshPackedLight = packedLight;
        cachedMeshVerifTick = verificationCacheTick;
        ghostGpuBufferDirty = false;
    }

    /**
     * 直接用 RenderPass API 绘制持久化 GpuBuffer（对齐 Litematica WorldRendererSchematic.drawOverlayInternal）。
     *
     * <p>关键性能点：跳过 RenderType.draw 的 uploadImmediateVertexBuffer（每帧上传 ~10ms for 67MB）
     * 和 meshData.close()，使用已在 GPU 上的持久化 GpuBuffer，每帧只创建 RenderPass + drawIndexed。
     *
     * <p>实现参照 RenderType.draw 源码（decompile_out/RenderType.java line 55-130）：
     * <ol>
     *   <li>writeTransform 写 DynamicTransforms uniform（含 modelViewMatrix、color、offset、textureMatrix）</li>
     *   <li>获取 RenderTarget 的 color/depth view</li>
     *   <li>createRenderPass → setPipeline → bindDefaultUniforms → setUniform → setVertexBuffer →
     *       bindTexture(Sampler0=block atlas, Sampler2=lightmap) → setIndexBuffer → drawIndexed</li>
     * </ol>
     *
     * <p>纹理解析对齐 RenderSetup.getTextures()：
     * <ul>
     *   <li>Sampler0（block atlas）：mc.getTextureManager().getTexture(LOCATION_BLOCKS).getTextureView() + .getSampler()</li>
     *   <li>Sampler2（lightmap）：mc.gameRenderer.lightTexture().getTextureView() + getClampToEdge(LINEAR)</li>
     * </ul>
     */
    private void drawGhostBlocksFromGpu() {
        if (ghostVertexGpuBuffer == null || ghostIndexCount == 0) return;

        Minecraft mc = Minecraft.getInstance();

        // 1. 写 DynamicTransforms uniform（color=白色，offset=零，textureMatrix=identity）
        // GHOST_BLOCK_TYPE 没设 textureTransform，默认 TextureTransform.DEFAULT_TEXTURING 是 identity
        GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms().writeTransform(
                RenderSystem.getModelViewMatrix(),
                new Vector4f(1.0f, 1.0f, 1.0f, 1.0f),
                new Vector3f(),
                new Matrix4f()  // identity textureMatrix
        );

        // 2. 解析纹理（对齐 RenderSetup.getTextures()，但通过 AbstractTexture API 绕过 RenderContext checkcast）
        AbstractTexture blockAtlas = mc.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
        GpuTextureView atlasView = blockAtlas.getTextureView();
        GpuSampler atlasSampler = blockAtlas.getSampler();
        GpuTextureView lightmapView = mc.gameRenderer.lightTexture().getTextureView();
        GpuSampler lightmapSampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);

        // 3. 获取 RenderTarget（OutputTarget.MAIN_TARGET 等同于 mc.getMainRenderTarget()）
        RenderTarget renderTarget = mc.getMainRenderTarget();
        GpuTextureView colorView = renderTarget.getColorTextureView();
        GpuTextureView depthView = renderTarget.useDepth ? renderTarget.getDepthTextureView() : null;

        // 4. 获取 index buffer（QUADS 模式 BufferBuilder 不产生 indexBuffer，用 AutoStorageIndexBuffer）
        RenderSystem.AutoStorageIndexBuffer autoStorageIndexBuffer =
                RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        GpuBuffer indexBuffer = autoStorageIndexBuffer.getBuffer(ghostIndexCount);
        VertexFormat.IndexType indexType = autoStorageIndexBuffer.type();

        // 5. 创建 RenderPass 并绘制
        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(
                        () -> "wegui:ghost_block_immediate",
                        colorView, OptionalInt.empty(),
                        depthView, OptionalDouble.empty())) {
            renderPass.setPipeline(GHOST_BLOCK_PIPELINE);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
            renderPass.setVertexBuffer(0, ghostVertexGpuBuffer);
            renderPass.bindTexture("Sampler0", atlasView, atlasSampler);
            renderPass.bindTexture("Sampler2", lightmapView, lightmapSampler);
            renderPass.setIndexBuffer(indexBuffer, indexType);
            renderPass.drawIndexed(0, 0, ghostIndexCount, 1);
        } catch (Throwable e) {
            WeGuiMod.LOGGER.debug("WeGui ghost blocks GpuBuffer draw 出错: {}", e.toString());
        }
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

                // 预构建 mismatch 位置列表缓存（层 ④⑤ 共享，跨帧缓存避免每帧重复遍历所有方块）
                boolean verificationEnabled = Configs.Generic.BLOCK_VERIFICATION_ENABLED.getBooleanValue();
                boolean overlayEnabled = Configs.RenderStyles.ENABLE_OVERLAY.getBooleanValue();
                if (verificationEnabled || overlayEnabled) {
                    ensureMismatchCache(origin, mc.level);
                }

                // 层 ④：Mismatch 渲染（始终穿墙，仅验证模式，复刻 Litematica renderSchematicMismatches）
                if (verificationEnabled) {
                    profiler.push("mismatch");
                    renderSchematicMismatches(frustum, camera, origin, mc.level);
                    profiler.pop();
                }

                // 层 ⑤：Schematic Overlay（可配置穿墙，复刻 Litematica renderBlockOverlays）
                // 始终按验证状态着色（CORRECT 跳过，WRONG_BLOCK/WRONG_STATE/MISSING/EXTRA 各自颜色）
                if (overlayEnabled) {
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
                // 使用 renderAreaOutline（不跳过角点），确保 8 个角点处三条线都交汇。
                // 不用 renderAreaOutlineNoCorners，因为它跳过 min/max 对角点的线段，
                // 而角点框只在 WorldEdit pos1/pos2 位置渲染，若 pos1/pos2 不是 min/max 角
                // 会导致跳过的角点无框填补，出现"三条线不交汇"的缺口。
                RenderUtils.renderAreaOutline(pos1, pos2, LINE_WIDTH_AREA, COLOR_X, COLOR_Y, COLOR_Z);
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
        // 使用 renderAreaOutline（不跳过角点），确保 8 个角点处三条线都交汇。
        // 不用 renderAreaOutlineNoCorners，因为它跳过 min/max 对角点的线段，导致角点处三条线不交汇。
        RenderUtils.renderAreaOutline(pos1, pos2, LINE_WIDTH_AREA, outlineColor, outlineColor, outlineColor);

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
     * 获取验证结果缓存：遍历所有剪贴板方块，对每个位置调用一次 level.getBlockState + verifyBlock。
     * ghost blocks / mismatch / overlay 三处渲染共享此缓存。
     *
     * <p>缓存 key 用 <b>relPos</b>（相对 origin 的偏移），不依赖 origin：
     * 这样 FOLLOW_PLAYER 模式下 origin 变化时缓存仍然有效，避免每帧重建（重建会触发 ghost block MeshData 重建）。
     * origin 变化时只需要重新查 worldPos = origin + relPos 的世界方块，但这是按超时周期重新执行的。
     *
     * <p>跨帧缓存：每 VERIFICATION_UPDATE_INTERVAL tick 重新验证一次（世界方块变化频率低）。
     */
    private Map<BlockPos, BlockMatchStatus> getVerificationCache(BlockPos origin, Level level) {
        long tick = level.getGameTime();
        // 缓存有效：未超时（不依赖 origin，因为 key 用 relPos）
        if (verificationCache != null
                && verificationCacheTick >= 0
                && tick - verificationCacheTick < VERIFICATION_UPDATE_INTERVAL) {
            return verificationCache;
        }
        // 重建缓存：key 用 relPos（不依赖 origin）
        Map<BlockPos, BlockMatchStatus> cache = new HashMap<>();
        if (cachedBlocks != null) {
            for (ClipboardChunkCache.ChunkGroup group : chunkCache.getGroups()) {
                List<BlockPos> positions = group.relPositions;
                List<BlockState> states = group.states;
                for (int i = 0; i < positions.size(); i++) {
                    BlockPos rel = positions.get(i);
                    BlockPos target = origin.offset(rel);
                    BlockState found = level.getBlockState(target);
                    cache.put(rel, verifyBlock(states.get(i), found));
                }
                // EXTRA 检测：原理图空气位置 + 世界非空 = 多余方块
                for (BlockPos rel : group.airPositions) {
                    BlockPos target = origin.offset(rel);
                    BlockState found = level.getBlockState(target);
                    BlockState expected = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
                    cache.put(rel, verifyBlock(expected, found));
                }
            }
        }
        verificationCache = cache;
        verificationCacheOrigin = origin;
        verificationCacheTick = tick;
        return cache;
    }

    /**
     * 填充 mismatch 位置列表跨帧缓存（对齐 Litematica {@code SchematicVerifier.getSelectedMismatchPositionsForRender}）。
     *
     * <p>mismatch 渲染（{@link #renderSchematicMismatches}）和 overlay 渲染（{@link #renderSchematicOverlay}）
     * 共享此缓存，避免每帧重复遍历所有方块收集 mismatch 位置。</p>
     *
     * <p>缓存失效条件（与 verificationCache 同步）：
     * <ul>
     *   <li>tick 超时 → verificationCache 重建 → verificationCacheTick 变化 → 此缓存失效</li>
     *   <li>chunkCache 重建（剪贴板变化）→ {@link #invalidateMismatchCache} 显式清空</li>
     * </ul>
     * 距离剔除、frustum 剔除不在收集阶段做（否则缓存无法跨帧复用），由调用方在渲染时按需过滤。</p>
     *
     * <p>注意：positions 列表存的是 worldPos（origin + relPos），因为 mismatch 渲染需要按世界坐标绘制边框。
     * 但 verifCache 的 key 是 relPos（不依赖 origin），所以这里查 verifCache 用 rel，存 positions 用 target。
     * 这意味着 origin 变化时 positions 列表也需重建（即 origin 也参与缓存 key）。
     */
    private void ensureMismatchCache(BlockPos origin, Level level) {
        // 先确保 verificationCache 已构建（同步更新 verificationCacheTick）
        getVerificationCache(origin, level);

        // 检查缓存有效性：origin 不变 + verificationCacheTick 不变
        if (mismatchPositionsCache != null
                && origin.equals(mismatchCacheOrigin)
                && verificationCacheTick == mismatchCacheKeyTick) {
            return;
        }

        // 重建：遍历所有方块，收集需要渲染的 mismatch 位置和状态
        List<BlockPos> positions = new ArrayList<>();
        List<BlockMatchStatus> statuses = new ArrayList<>();
        Map<BlockPos, BlockMatchStatus> verifCache = verificationCache;
        if (verifCache != null) {
            for (ClipboardChunkCache.ChunkGroup group : chunkCache.getGroups()) {
                for (BlockPos rel : group.relPositions) {
                    BlockMatchStatus status = verifCache.get(rel);
                    if (status == null || status == BlockMatchStatus.CORRECT) continue;
                    if (!shouldRenderMismatchType(status)) continue;
                    positions.add(origin.offset(rel));
                    statuses.add(status);
                }
                for (BlockPos rel : group.airPositions) {
                    BlockMatchStatus status = verifCache.get(rel);
                    if (status == null || status == BlockMatchStatus.CORRECT) continue;
                    if (!shouldRenderMismatchType(status)) continue;
                    positions.add(origin.offset(rel));
                    statuses.add(status);
                }
            }
        }

        mismatchPositionsCache = positions;
        mismatchStatusesCache = statuses;
        mismatchCacheOrigin = origin;
        mismatchCacheKeyTick = verificationCacheTick;
    }

    /** 失效 mismatch 位置缓存（在 chunkCache 重建时调用） */
    private void invalidateMismatchCache() {
        mismatchPositionsCache = null;
        mismatchStatusesCache = null;
        mismatchCacheKeyTick = -1;
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
     * <p>边框线使用 OFFSET_2（有深度测试，被前方方块/ghost block 遮挡），仅渲染 mismatch 方块，使用 VERIFY_*_COLOR。
     * 与 Litematica 原版不同：原版用 NO_DEPTH_NO_CULL（始终穿墙），但用户要求边框线有正确遮挡。
     * 填充面（层3）仍用 NO_DEPTH_NO_CULL（穿墙可见，方便定位错误），但 DEFAULT 预设下禁用填充面。</p>
     *
     * <p>层1: 边框线 (batched_lines) — 所有 mismatch 方块（排除注视方块），lineWidth=2.0f<br>
     * 层2: 注视方块加粗 (outlines) — 仅注视的 mismatch 方块，lineWidth=6.0f<br>
     * 层3: 半透明填充面 (side_quads) — 所有 mismatch 方块，alpha=VERIFY_HILIGHT_ALPHA</p>
     */
    private void renderSchematicMismatches(Frustum frustum, Camera camera, BlockPos origin, Level level) {
        if (chunkCache.isEmpty()) return;
        if (!Configs.RenderStyles.ENABLE_OVERLAY.getBooleanValue()) return;
        if (!Configs.Generic.BLOCK_VERIFICATION_ENABLED.getBooleanValue()) return;
        if (mismatchPositionsCache == null || mismatchStatusesCache == null) return;

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
        BlockMatchStatus lookedStatus = null;

        // 从跨帧缓存中收集可见的 mismatch 方块（带距离剔除）
        // 缓存已做类型过滤（shouldRenderMismatchType），此处只需距离剔除 + 注视方块检测
        List<BlockPos> mismatchPositions = new ArrayList<>();
        List<BlockMatchStatus> mismatchStatuses = new ArrayList<>();

        for (int i = 0; i < mismatchPositionsCache.size(); i++) {
            BlockPos target = mismatchPositionsCache.get(i);
            if (enableCulling) {
                double dx = target.getX() + 0.5 - camPos.x;
                double dy = target.getY() + 0.5 - camPos.y;
                double dz = target.getZ() + 0.5 - camPos.z;
                if (dx * dx + dy * dy + dz * dz > renderDistSq) continue;
            }
            BlockMatchStatus status = mismatchStatusesCache.get(i);
            mismatchPositions.add(target);
            mismatchStatuses.add(status);
            if (lookPos != null && lookPos.equals(target)) {
                lookedTarget = target;
                lookedStatus = status;
            }
        }

        if (mismatchPositions.isEmpty()) return;

        // 位置限制（按距离截断，对齐 Litematica VERIFIER_ERROR_HILIGHT_MAX_POSITIONS）
        int maxPositions = Configs.RenderStyles.VERIFIER_ERROR_HILIGHT_MAX_POSITIONS.getIntegerValue();
        if (mismatchPositions.size() > maxPositions) {
            final List<BlockPos> positionsForSort = mismatchPositions;
            final Vec3 camSort = camPos;
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < positionsForSort.size(); i++) indices.add(i);
            indices.sort((a, b) -> {
                BlockPos pa = positionsForSort.get(a);
                BlockPos pb = positionsForSort.get(b);
                double da = (pa.getX() - camSort.x) * (pa.getX() - camSort.x)
                        + (pa.getY() - camSort.y) * (pa.getY() - camSort.y)
                        + (pa.getZ() - camSort.z) * (pa.getZ() - camSort.z);
                double db = (pb.getX() - camSort.x) * (pb.getX() - camSort.x)
                        + (pb.getY() - camSort.y) * (pb.getY() - camSort.y)
                        + (pb.getZ() - camSort.z) * (pb.getZ() - camSort.z);
                return Double.compare(da, db);
            });
            List<BlockPos> truncPos = new ArrayList<>();
            List<BlockMatchStatus> truncStatus = new ArrayList<>();
            for (int i = 0; i < maxPositions; i++) {
                int idx = indices.get(i);
                truncPos.add(positionsForSort.get(idx));
                truncStatus.add(mismatchStatuses.get(idx));
            }
            mismatchPositions = truncPos;
            mismatchStatuses = truncStatus;
            if (lookedTarget != null && !mismatchPositions.contains(lookedTarget)) {
                lookedTarget = null;
                lookedStatus = null;
            }
        }

        // Mismatch 边框线使用 OFFSET_2（有深度测试 + polygon offset 防 z-fighting）。
        RenderPipeline linePipeline = MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_OFFSET_2;
        RenderPipeline sidePipeline = MaLiLibPipelines.POSITION_COLOR_TRANSLUCENT_NO_DEPTH_NO_CULL;

        // ========== 层1: 边框线 + 层2: 注视方块加粗 ==========
        if (enableOutlines) {
            try (RenderContext ctx = new RenderContext(
                    () -> "wegui:mismatch/batched_lines",
                    linePipeline)) {
                BufferBuilder buffer = ctx.start(
                        () -> "wegui:mismatch/batched_lines",
                        linePipeline);

                BlockPos prevPos = null;
                for (int i = 0; i < mismatchPositions.size(); i++) {
                    BlockPos target = mismatchPositions.get(i);
                    if (target.equals(lookedTarget)) {
                        prevPos = target;
                        continue;
                    }
                    Color4f c = getVerifyColor(mismatchStatuses.get(i));
                    // 边框线强制 alpha=1.0（对齐 Litematica OverlayRenderer.renderSchematicMismatches:390）
                    Color4f lineColor = new Color4f(c.r, c.g, c.b, 1.0f);
                    RenderUtils.drawBlockBoundingBoxOutlinesBatchedLinesSimple(target, lineColor, OUTLINE_EXPAND, LINE_WIDTH_MISMATCH, buffer);
                    if (renderConnections && prevPos != null && !prevPos.equals(target)) {
                        RenderUtils.drawConnectingLineBatchedLines(prevPos, target, false, lineColor, LINE_WIDTH_MISMATCH, buffer);
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
                if (lookedTarget != null && lookedStatus != null) {
                    BufferBuilder boldBuffer = ctx.start(
                            () -> "wegui:mismatch/outlines",
                            linePipeline);
                    Color4f lookedC = getVerifyColor(lookedStatus);
                    Color4f lookedBold = new Color4f(lookedC.r, lookedC.g, lookedC.b, 1.0f);
                    RenderUtils.drawBlockBoundingBoxOutlinesBatchedLinesSimple(lookedTarget, lookedBold, OUTLINE_EXPAND, LINE_WIDTH_MISMATCH_LOOKED, boldBuffer);
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
        if (renderSides) {
            try (RenderContext sideCtx = new RenderContext(
                    () -> "wegui:mismatch/side_quads",
                    sidePipeline)) {
                BufferBuilder sideBuffer = sideCtx.start(
                        () -> "wegui:mismatch/side_quads",
                        sidePipeline);
                for (int i = 0; i < mismatchPositions.size(); i++) {
                    BlockPos pos = mismatchPositions.get(i);
                    if (reducedInnerSides && !isBlockExposed(pos, level)) continue;
                    Color4f c = getVerifyColor(mismatchStatuses.get(i));
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
     * <p>着色规则（与 Litematica SCHEMATIC_OVERLAY_COLOR_* 完全一致）：
     * <ul>
     *   <li>CORRECT（方块+状态相同）→ 不渲染 overlay</li>
     *   <li>WRONG_BLOCK（方块类型不同）→ #4CFF3333（红色半透明）</li>
     *   <li>WRONG_STATE（方块相同，状态不同）→ #4CFF9010（橙色半透明）</li>
     *   <li>MISSING（期望非空，实际空气）→ #2C33B3E6（青色半透明）</li>
     *   <li>EXTRA（期望空气，实际非空）→ #4CFF4CE6（品红半透明）</li>
     * </ul></p>
     *
     * <p>可配置穿墙（OVERLAY_RENDER_THROUGH），正常模式使用 OFFSET_2 管线防 z-fighting。</p>
     *
     * <p>层1: 边框线 (batched_lines) — 强制 alpha=1.0，OVERLAY_OUTLINE_WIDTH / OVERLAY_OUTLINE_WIDTH_THROUGH<br>
     * 层2: 填充面 (side_quads) — 使用颜色自带 alpha，受 OVERLAY_ENABLE_SIDES 控制</p>
     */
    private void renderSchematicOverlay(Frustum frustum, Camera camera, BlockPos origin, Level level) {
        if (chunkCache.isEmpty()) return;
        if (!Configs.RenderStyles.ENABLE_OVERLAY.getBooleanValue()) return;
        if (mismatchPositionsCache == null || mismatchStatusesCache == null) return;

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

        // 从跨帧缓存中收集可见的 overlay 方块（带距离剔除）
        // 缓存已做类型过滤，此处只需距离剔除
        List<BlockPos> overlayPositions = new ArrayList<>();
        List<BlockMatchStatus> overlayStatuses = new ArrayList<>();

        for (int i = 0; i < mismatchPositionsCache.size(); i++) {
            BlockPos target = mismatchPositionsCache.get(i);
            if (enableCulling) {
                double dx = target.getX() + 0.5 - camPos.x;
                double dy = target.getY() + 0.5 - camPos.y;
                double dz = target.getZ() + 0.5 - camPos.z;
                if (dx * dx + dy * dy + dz * dz > renderDistSq) continue;
            }
            overlayPositions.add(target);
            overlayStatuses.add(mismatchStatusesCache.get(i));
        }

        // ========== 层1: 边框线 ==========
        // Litematica ChunkRendererSchematicVbo.renderOverlay: 边框线强制 alpha=1.0
        if (enableOutlines && !overlayPositions.isEmpty()) {
            try (RenderContext ctx = new RenderContext(
                    () -> "wegui:overlay/batched_lines",
                    linePipeline)) {
                BufferBuilder buffer = ctx.start(
                        () -> "wegui:overlay/batched_lines",
                        linePipeline);

                for (int i = 0; i < overlayPositions.size(); i++) {
                    BlockPos target = overlayPositions.get(i);
                    Color4f c = getVerifyColor(overlayStatuses.get(i));
                    // 边框线强制 alpha=1.0（对齐 Litematica ChunkRendererSchematicVbo:813）
                    Color4f lineColor = new Color4f(c.r, c.g, c.b, 1.0f);
                    RenderUtils.drawBlockBoundingBoxOutlinesBatchedLinesSimple(target, lineColor, OUTLINE_EXPAND, lineWidth, buffer);
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
        // Litematica ChunkRendererSchematicVbo.renderOverlay: 填充面使用颜色自带的 alpha
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
                    Color4f c = getVerifyColor(overlayStatuses.get(i));
                    // 填充面使用颜色自带的 alpha（对齐 Litematica，SCHEMATIC_OVERLAY_COLOR_* 已含 alpha）
                    RenderUtils.renderAreaSidesBatched(pos, pos, c, OUTLINE_EXPAND, sideBuffer);
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
            // 先通过 getClipboardBounds 检查剪贴板是否存在（带无剪贴板状态缓存，避免每帧抛 EmptyClipboardException）
            if (WorldEditBridge.getClipboardBounds(mc) == null) {
                clearCache();
                return null;
            }
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
        // chunkCache 重建意味着方块集合变化，ghost block GpuBuffer 也需要重建
        // 立即关闭旧 GpuBuffer 释放 GPU 内存
        if (ghostVertexGpuBuffer != null) {
            ghostVertexGpuBuffer.close();
            ghostVertexGpuBuffer = null;
        }
        ghostIndexCount = 0;
        ghostGpuBufferDirty = true;
        invalidateMismatchCache();
    }

    private void ensureChunkCache(Map<BlockPos, BlockState> blocks, BlockPos origin) {
        if (chunkCacheDirty) {
            chunkCache.rebuild(blocks, origin);
            chunkCacheDirty = false;
            // chunkCache 重建意味着方块集合变化，mismatch 位置列表需要重新收集
            invalidateMismatchCache();
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

    // ========================================================================
    // ghost block BakedQuad 渲染缓存
    // ========================================================================

    /**
     * ghost block 渲染缓存条目：预计算单个方块的 BakedQuad 列表和着色信息，
     * 避免每帧调用 {@code renderSingleBlock} 重复执行模型查找、颜色计算和 quad 收集。
     *
     * <p>缓存存储在 {@link ClipboardChunkCache.ChunkGroup#renderCache} 中，
     * 当 {@code chunkCache.rebuild()} 创建新 {@code ChunkGroup} 时自动失效（renderCache=null）。</p>
     */
    private static final class CachedGhostBlock {
        final BlockPos relPos;
        final RenderType renderType;
        /** tint 颜色（getTintIndex >= 0 的 quad 用此颜色着色） */
        final float r, g, b;
        /** 所有方向（含 null 方向）的 BakedQuad 合并列表 */
        final List<BakedQuad> allQuads;

        CachedGhostBlock(BlockPos relPos, RenderType renderType, float r, float g, float b, List<BakedQuad> allQuads) {
            this.relPos = relPos;
            this.renderType = renderType;
            this.r = r;
            this.g = g;
            this.b = b;
            this.allQuads = allQuads;
        }
    }

    /**
     * 为所有 {@link ClipboardChunkCache.ChunkGroup} 填充 {@code renderCache}（若尚未填充）。
     *
     * <p>对每个非空气方块预计算：
     * <ul>
     *   <li>{@link BakedModel#getQuads} 收集所有方向的 BakedQuad（固定 seed=42L，与 {@code renderSingleBlock} 一致）</li>
     *   <li>{@link BlockColors#getColor} 计算 tint 颜色</li>
     *   <li>{@link ItemBlockRenderTypes#getRenderType} 获取 RenderType</li>
     * </ul>
     * 缓存按 chunk section 组织，支持 section 级距离剔除。</p>
     */
    @SuppressWarnings("unchecked")
    private void ensureGhostBlockRenderCache() {
        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
        BlockColors blockColors = mc.getBlockColors();
        RandomSource random = RandomSource.create(42L);
        Direction[] directions = Direction.values();

        for (ClipboardChunkCache.ChunkGroup group : chunkCache.getGroups()) {
            if (group.renderCache != null) continue;  // 已缓存

            List<BlockPos> positions = group.relPositions;
            List<BlockState> states = group.states;
            List<CachedGhostBlock> cache = new ArrayList<>(positions.size());

            for (int i = 0; i < positions.size(); i++) {
                try {
                    BlockState state = states.get(i);
                    // renderSingleBlock 只处理 RenderShape.MODEL，其他形状跳过
                    if (state.getRenderShape() != RenderShape.MODEL) continue;

                    BlockStateModel model = dispatcher.getBlockModel(state);
                    RenderType renderType = ItemBlockRenderTypes.getRenderType(state);

                    int color = blockColors.getColor(state, null, null, 0);
                    float r = (color >> 16 & 255) / 255f;
                    float g = (color >> 8 & 255) / 255f;
                    float b = (color & 255) / 255f;

                    // 1.21.11 API: BlockStateModel.collectParts(RandomSource) → List<BlockModelPart>
                    // BlockModelPart.getQuads(Direction) → List<BakedQuad>（1 参数，无 BlockState/RandomSource）
                    List<BakedQuad> allQuads = new ArrayList<>();
                    for (BlockModelPart part : model.collectParts(random)) {
                        for (Direction dir : directions) {
                            allQuads.addAll(part.getQuads(dir));
                        }
                        allQuads.addAll(part.getQuads(null));
                    }
                    if (allQuads.isEmpty()) continue;

                    cache.add(new CachedGhostBlock(positions.get(i), renderType, r, g, b, allQuads));
                } catch (Throwable ignored) {
                    // 跳过无法获取模型的方块
                }
            }

            group.renderCache = cache;
        }
    }
}
