package com.sow.wegui.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import com.sow.wegui.config.Configs;
import com.sow.wegui.config.PastePlacementMode;
import com.sow.wegui.WeGuiMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Map;
import net.minecraft.world.level.block.state.BlockState;

/**
 * WE 绑定工具多模式控制：
 * - 编辑选区模式：保留 WorldEdit 默认行为（左键 pos1，右键 pos2），Alt+滚轮挪动选点
 * - 放置模式：禁用 WE 工具的左右键选区行为；左键不破坏方块也不选 pos1；右键方块 = 移动 Litematica 预览原点（仅预览，用户手动 //paste 才真正粘贴）
 * - 移动粘贴预览模式：禁用 WE 工具的左右键选区行为；Alt+滚轮 移动 Litematica 同步预览的原点
 *
 * 模式切换：Ctrl+滚轮 循环切换；按下 Ctrl 或 Alt 时滚轮事件都会被消费避免触发物品栏切换。
 */
public final class AxeModeHandler {
    private AxeModeHandler() {}

    public enum AxeMode {
        EDIT_SELECTION("wegui.mode.edit_selection"),
        PLACE("wegui.mode.place"),
        MOVE_PASTE_PREVIEW("wegui.mode.move_paste_preview");

        private final String translationKey;

        AxeMode(String translationKey) {
            this.translationKey = translationKey;
        }

        public String getTranslationKey() {
            return translationKey;
        }

        public String getDisplayName() {
            return Component.translatable(translationKey).getString();
        }
    }

    private enum LastModifiedCorner {
        NONE, POS1, POS2
    }

    private static AxeMode currentMode = AxeMode.EDIT_SELECTION;
    private static LastModifiedCorner lastModified = LastModifiedCorner.NONE;
    private static BlockPos pastePreviewOffset = BlockPos.ZERO;
    private static Item cachedWandItem;
    private static String cachedWandItemId;

    @Nullable
    private static BlockPos fixedOrigin = null;
    private static PastePlacementMode lastMode = PastePlacementMode.FIXED;

    public static void register() {
        // 监听 PASTE_PLACEMENT_MODE 变化，触发 handlePlacementModeChange
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null) return;
            PastePlacementMode currentModeCfg = (PastePlacementMode) Configs.PastePreview.PASTE_PLACEMENT_MODE.getOptionListValue();
            if (currentModeCfg != lastMode) {
                handlePlacementModeChange(currentModeCfg, mc);
                lastMode = currentModeCfg;
            }
        });

        // 左键：
        // - 编辑选区模式：记录 pos1 为最后修改点，保留 WE 默认 pos1 行为
        // - 放置模式：返回 FAIL 阻止 START_DESTROY_BLOCK 包发送到服务端，
        //   使服务端 WE 的 AttackBlockCallback 不会被触发，从而不设置 pos1。
        //   副作用：客户端不会破坏方块（无法实现“只破坏不选区”，因同一包触发两者）
        // - 移动 paste 预览模式：同上，阻止 pos1 选区
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (level.isClientSide() && hand == InteractionHand.MAIN_HAND && isHoldingConfiguredWand(player)) {
                WeGuiMod.LOGGER.info("[WE GUI] AttackBlockCallback mode={} pos={}", currentMode, pos);
                if (currentMode == AxeMode.MOVE_PASTE_PREVIEW || currentMode == AxeMode.PLACE) {
                    // FAIL: 取消 vanilla 处理且不发送 START_DESTROY_BLOCK 包到服务端
                    // (SUCCESS/CONSUME 会触发 Fabric Mixin 主动补发包，仍会触发服务端 WE 选区)
                    return InteractionResult.FAIL;
                }
                lastModified = LastModifiedCorner.POS1;
                if (Configs.Generic.SELECTION_MESSAGE_ENABLED.getBooleanValue()) {
                    player.sendSystemMessage(Component.translatable("wegui.message.pos1_set", formatPos(pos)).withStyle(ChatFormatting.DARK_PURPLE));
                }
            }
            return InteractionResult.PASS;
        });

        // 右键方块：
        // - 放置模式：把粘贴预览移动到右键方块（仅预览，不执行 //paste），返回 FAIL 阻止服务端 WE 选 pos2
        // - 移动 paste 预览模式：同上，阻止 pos2 选区
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (!level.isClientSide() || hand != InteractionHand.MAIN_HAND || !isHoldingConfiguredWand(player)) {
                return InteractionResult.PASS;
            }
            WeGuiMod.LOGGER.info("[WE GUI] UseBlockCallback mode={} target={}", currentMode, hitResult.getBlockPos());

            if (currentMode == AxeMode.MOVE_PASTE_PREVIEW) {
                return InteractionResult.FAIL;
            }

            if (currentMode == AxeMode.PLACE) {
                BlockPos target = hitResult.getBlockPos();
                movePreviewTo((LocalPlayer) player, target);
                // FAIL: 阻止服务端收到 UseItemOnPacket，避免 WE 在服务端 UseBlockCallback 中设置 pos2
                return InteractionResult.FAIL;
            }

            lastModified = LastModifiedCorner.POS2;
            if (Configs.Generic.SELECTION_MESSAGE_ENABLED.getBooleanValue()) {
                BlockPos target = hitResult.getBlockPos();
                player.sendSystemMessage(Component.translatable("wegui.message.pos2_set", formatPos(target)).withStyle(ChatFormatting.DARK_PURPLE));
            }
            return InteractionResult.PASS;
        });

        // 右键物品/空气：放置模式 / 移动 paste 预览模式下阻止服务端 WE 默认 pos2 行为
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (!level.isClientSide() || hand != InteractionHand.MAIN_HAND || !isHoldingConfiguredWand(player)) {
                return InteractionResult.PASS;
            }
            WeGuiMod.LOGGER.info("[WE GUI] UseItemCallback mode={}", currentMode);

            if (currentMode == AxeMode.PLACE || currentMode == AxeMode.MOVE_PASTE_PREVIEW) {
                // FAIL: 阻止服务端收到 UseItemPacket
                return InteractionResult.FAIL;
            }

            return InteractionResult.PASS;
        });
    }

    public static AxeMode getMode() {
        return currentMode;
    }

    public static BlockPos getPastePreviewOffset() {
        return pastePreviewOffset;
    }

    public static void setPastePreviewOffset(BlockPos offset) {
        pastePreviewOffset = offset;
    }

    public static void resetPastePreviewOffset() {
        pastePreviewOffset = BlockPos.ZERO;
    }

    /**
     * 获取当前 paste 预览的实际原点位置（玩家位置 + 手动偏移）。
     */
    public static BlockPos getPasteOrigin(LocalPlayer player) {
        return BlockPos.containing(player.getX(), player.getY(), player.getZ()).offset(pastePreviewOffset);
    }

    /**
     * 获取当前生效的预览原点。
     * - FIXED 模式：首次调用时自动把 fixedOrigin 固定到当前玩家位置，之后保持不变
     * - FOLLOW_PLAYER 模式：返回玩家位置 + 手动偏移
     */
    public static BlockPos getEffectiveOrigin(Minecraft mc) {
        if (lastMode == PastePlacementMode.FIXED) {
            if (fixedOrigin == null && mc.player != null) {
                fixedOrigin = getPasteOrigin(mc.player);
            }
            if (fixedOrigin != null) {
                return fixedOrigin;
            }
        }
        if (mc.player == null) return BlockPos.ZERO;
        return getPasteOrigin(mc.player);
    }

    /**
     * 若存在非零粘贴偏移，则在预览原点执行 //paste；否则正常发送 //paste。
     */
    public static void executePasteAtPreview(LocalPlayer player) {
        pasteAtPreview(player);
    }

    /**
     * 放置模式下：把粘贴预览同步到 target 方块（仅移动预览，不执行 //paste）。
     * - 同步预览原点到 target（让 Litematica 渲染立即移动到该方块）
     * - 用户需要手动 //paste 才会真正粘贴
     */
    private static void movePreviewTo(LocalPlayer player, BlockPos target) {
        if (!hasClipboard()) {
            player.sendOverlayMessage(Component.translatable("wegui.message.fixed_mode_no_clipboard").withStyle(ChatFormatting.RED));
            return;
        }

        // 同步预览原点到 target，让 Litematica 渲染立即移动到该方块
        setFixedOrigin(target);

        WeGuiMod.LOGGER.info("[WE GUI] 放置模式移动预览到 {}", target);
        player.sendOverlayMessage(Component.translatable("wegui.message.preview_moved", formatPos(target)).withStyle(ChatFormatting.GREEN));
    }

    // ---- 固定放置模式状态访问 ----

    /** 当前是否处于固定模式（直接读配置，不依赖 lastMode） */
    public static boolean isFixedMode() {
        PastePlacementMode mode = (PastePlacementMode) Configs.PastePreview.PASTE_PLACEMENT_MODE.getOptionListValue();
        return mode == PastePlacementMode.FIXED && fixedOrigin != null;
    }

    /** 获取固定位置（固定模式下），否则返回 null */
    @Nullable
    public static BlockPos getFixedOrigin() {
        return isFixedMode() ? fixedOrigin : null;
    }

    /** 设置固定位置（用于 pasteAtPreview 在 fixedOrigin 未初始化时回填） */
    public static void setFixedOrigin(BlockPos origin) {
        fixedOrigin = origin;
    }

    /**
     * 处理放置模式切换：
     * - 切换到 FIXED：记录当前预览位置作为固定原点，用户通过 //paste 在此位置执行真正的 paste
     * - 切换到 FOLLOW_PLAYER：清除固定位置，预览恢复跟随玩家
     */
    private static void handlePlacementModeChange(PastePlacementMode newMode, Minecraft mc) {
        if (mc.player == null) return;

        if (newMode == PastePlacementMode.FIXED) {
            // 仅单人世界
            if (!WorldEditBridge.canUseDirectPaste()) {
                mc.player.sendOverlayMessage(
                        Component.translatable("wegui.message.fixed_mode_multiplayer_disabled")
                                .withStyle(ChatFormatting.RED));
                Configs.PastePreview.PASTE_PLACEMENT_MODE.setOptionListValue(PastePlacementMode.FOLLOW_PLAYER);
                return;
            }

            // 需有剪贴板
            BlockPos currentOrigin = getPasteOrigin(mc.player);
            Map<BlockPos, BlockState> blocks = WorldEditBridge.getClipboardBlocks(mc);
            if (blocks == null || blocks.isEmpty()) {
                mc.player.sendOverlayMessage(
                        Component.translatable("wegui.message.fixed_mode_no_clipboard")
                                .withStyle(ChatFormatting.RED));
                Configs.PastePreview.PASTE_PLACEMENT_MODE.setOptionListValue(PastePlacementMode.FOLLOW_PLAYER);
                return;
            }

            fixedOrigin = currentOrigin;
            mc.player.sendOverlayMessage(
                    Component.translatable("wegui.message.fixed_mode_enabled")
                            .withStyle(ChatFormatting.GREEN));
        } else {
            fixedOrigin = null;
        }
    }

    /**
     * 处理鼠标滚动事件。只要手持木斧且按下了 Ctrl 或 Alt，就返回 true 以消费事件，
     * 防止滚轮继续触发物品栏切换等默认行为。
     */
    public static boolean handleMouseScroll(double scrollDelta) {
        if (scrollDelta == 0) return false;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.gui.screen() != null) return false;
        if (!isHoldingConfiguredWand(player)) return false;

        boolean ctrl = isModifierHeld(GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL);
        boolean alt = isModifierHeld(GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT);
        if (!ctrl && !alt) return false;

        if (ctrl) {
            cycleMode(player, scrollDelta < 0);
            return true;
        }

        if (currentMode == AxeMode.EDIT_SELECTION) {
            return handleEditSelectionScroll(player, mc, scrollDelta);
        }

        if (currentMode == AxeMode.MOVE_PASTE_PREVIEW) {
            return handleMovePastePreviewScroll(player, scrollDelta);
        }

        player.sendOverlayMessage(Component.translatable("wegui.message.need_edit_or_move_mode").withStyle(ChatFormatting.RED));
        return true;
    }

    private static boolean handleEditSelectionScroll(LocalPlayer player, Minecraft mc, double scrollDelta) {
        WorldEditBridge.PartialCornerPositions corners = WorldEditBridge.getPartialSelectionCorners(mc);
        if (corners == null) {
            player.sendOverlayMessage(Component.translatable("wegui.message.no_selection").withStyle(ChatFormatting.RED));
            return true;
        }

        if (lastModified == LastModifiedCorner.NONE) {
            player.sendOverlayMessage(Component.translatable("wegui.message.no_corner").withStyle(ChatFormatting.RED));
            return true;
        }

        BlockPos target = lastModified == LastModifiedCorner.POS1 ? corners.pos1() : corners.pos2();
        if (target == null) {
            player.sendOverlayMessage(Component.translatable("wegui.message.corner_not_set").withStyle(ChatFormatting.RED));
            return true;
        }

        int amount = scrollDelta > 0 ? 1 : -1;
        Direction direction = getLookDirection(player);
        if (direction == null) {
            return true;
        }

        BlockPos moved = target.relative(direction, amount);

        if (lastModified == LastModifiedCorner.POS1) {
            CommandSender.send("//pos1 " + formatPos(moved));
            player.sendOverlayMessage(Component.translatable("wegui.message.moved_pos1", formatPos(moved)).withStyle(ChatFormatting.GREEN));
        } else {
            CommandSender.send("//pos2 " + formatPos(moved));
            player.sendOverlayMessage(Component.translatable("wegui.message.moved_pos2", formatPos(moved)).withStyle(ChatFormatting.GREEN));
        }

        return true;
    }

    private static boolean handleMovePastePreviewScroll(LocalPlayer player, double scrollDelta) {
        if (!hasClipboard() || !WorldEditBridge.canUseDirectPaste()) {
            player.sendOverlayMessage(Component.translatable("wegui.message.no_clipboard_or_multiplayer").withStyle(ChatFormatting.RED));
            resetPastePreviewOffset();
            return true;
        }

        int amount = scrollDelta > 0 ? 1 : -1;
        Direction direction = getLookDirection(player);
        if (direction == null) {
            return true;
        }

        // 固定模式下移动 fixedOrigin（不随玩家移动，但可手动移动）
        // 随玩家移动模式下移动 pastePreviewOffset
        if (isFixedMode()) {
            BlockPos fixedOriginPos = getFixedOrigin();
            if (fixedOriginPos != null) {
                BlockPos moved = fixedOriginPos.relative(direction, amount);
                setFixedOrigin(moved);
                player.sendOverlayMessage(Component.translatable("wegui.message.moved_paste_preview",
                        formatPos(moved)).withStyle(ChatFormatting.GREEN));
            }
        } else {
            pastePreviewOffset = pastePreviewOffset.relative(direction, amount);
            player.sendOverlayMessage(Component.translatable("wegui.message.moved_paste_preview",
                    formatPos(pastePreviewOffset)).withStyle(ChatFormatting.GREEN));
        }
        return true;
    }

    private static void cycleMode(LocalPlayer player, boolean forward) {
        AxeMode[] values = AxeMode.values();
        int count = values.length;
        int dir = forward ? 1 : -1;
        int nextOrdinal = currentMode.ordinal();

        // 最多循环 count 次，跳过不可用的模式
        for (int i = 0; i < count; i++) {
            nextOrdinal = (nextOrdinal + dir + count) % count;
            AxeMode candidate = values[nextOrdinal];
            if (candidate == AxeMode.MOVE_PASTE_PREVIEW && !WorldEditBridge.canUseDirectPaste()) {
                continue;
            }
            currentMode = candidate;
            break;
        }

        player.sendOverlayMessage(Component.translatable("wegui.message.mode_changed",
                Component.literal("[" + (currentMode.ordinal() + 1) + "/" + count + "] "),
                currentMode.getDisplayName()).withStyle(ChatFormatting.GREEN));
    }

    public static boolean isHoldingConfiguredWand(Player player) {
        Item wandItem = getConfiguredWandItem();
        if (wandItem == null) {
            WeGuiMod.LOGGER.warn("[WE GUI] isHoldingConfiguredWand: wandItem=null (id={})", Configs.Generic.WAND_ITEM.getStringValue());
            return false;
        }
        boolean main = player.getMainHandItem().is(wandItem);
        boolean off = player.getOffhandItem().is(wandItem);
        if (!main && !off) {
            WeGuiMod.LOGGER.warn("[WE GUI] isHoldingConfiguredWand: main={}, off={}, mainItem={}, offItem={}",
                    main, off, player.getMainHandItem().getItem(), player.getOffhandItem().getItem());
        }
        return main || off;
    }

    @Nullable
    private static Item getConfiguredWandItem() {
        String id = Configs.Generic.WAND_ITEM.getStringValue();
        if (id.equals(cachedWandItemId) && cachedWandItem != null) {
            return cachedWandItem;
        }
        try {
            net.minecraft.resources.Identifier identifier = net.minecraft.resources.Identifier.parse(id);
            Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(identifier)
                    .map(ref -> ref.value())
                    .orElse(null);
            cachedWandItemId = id;
            cachedWandItem = item;
            return item;
        } catch (Exception e) {
            cachedWandItemId = id;
            cachedWandItem = null;
            return null;
        }
    }

    private static boolean isModifierHeld(int leftKey, int rightKey) {
        long window = GLFW.glfwGetCurrentContext();
        if (window == 0) return false;
        return GLFW.glfwGetKey(window, leftKey) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, rightKey) == GLFW.GLFW_PRESS;
    }

    private static Direction getLookDirection(LocalPlayer player) {
        Vec3 view = player.getViewVector(1.0f);
        double ax = Math.abs(view.x);
        double ay = Math.abs(view.y);
        double az = Math.abs(view.z);
        if (ax >= ay && ax >= az) {
            return view.x > 0 ? Direction.EAST : Direction.WEST;
        } else if (ay >= ax && ay >= az) {
            return view.y > 0 ? Direction.UP : Direction.DOWN;
        } else {
            return view.z > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }

    private static boolean hasClipboard() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && WorldEditBridge.getClipboardBounds(mc) != null;
    }

    /**
     * 在预览原点执行粘贴。
     * - 固定模式：始终在 fixedOrigin 执行 paste，与 Litematica 同步渲染位置一致
     * - 随玩家移动模式：单人世界且存在非零偏移时直接调用 WorldEdit API；
     *   无偏移或无法直接粘贴时退回到普通 //paste 命令。
     */
    private static void pasteAtPreview(LocalPlayer player) {
        PastePlacementMode mode = (PastePlacementMode) Configs.PastePreview.PASTE_PLACEMENT_MODE.getOptionListValue();

        if (mode == PastePlacementMode.FIXED) {
            // 仅单人世界
            if (!WorldEditBridge.canUseDirectPaste()) {
                WeGuiMod.LOGGER.warn("[WE GUI] 固定模式 paste 失败：非单人世界");
                player.sendOverlayMessage(Component.translatable("wegui.message.fixed_mode_multiplayer_disabled").withStyle(ChatFormatting.RED));
                Configs.PastePreview.PASTE_PLACEMENT_MODE.setOptionListValue(PastePlacementMode.FOLLOW_PLAYER);
                return;
            }

            BlockPos fixedOriginPos = getFixedOrigin();
            if (fixedOriginPos == null) {
                fixedOriginPos = getPasteOrigin(player);
                setFixedOrigin(fixedOriginPos);
                WeGuiMod.LOGGER.info("[WE GUI] fixedOrigin 未初始化，回填为当前预览位置 {}", fixedOriginPos);
            }

            WeGuiMod.LOGGER.info("[WE GUI] 固定模式 paste 到 {}", fixedOriginPos);
            boolean success = WorldEditBridge.pasteClipboardAt(player, fixedOriginPos);
            if (success) {
                player.sendOverlayMessage(Component.translatable("wegui.message.paste_success", formatPos(fixedOriginPos)).withStyle(ChatFormatting.GREEN));
            } else {
                player.sendOverlayMessage(Component.translatable("wegui.message.paste_failed").withStyle(ChatFormatting.RED));
            }
            return;
        }

        // FOLLOW_PLAYER 模式
        if (pastePreviewOffset.equals(BlockPos.ZERO)) {
            sendNormalPasteCommand(player);
            return;
        }

        if (!WorldEditBridge.canUseDirectPaste()) {
            player.sendOverlayMessage(Component.translatable("wegui.message.move_paste_multiplayer_disabled").withStyle(ChatFormatting.RED));
            return;
        }

        BlockPos origin = getPasteOrigin(player);
        boolean success = WorldEditBridge.pasteClipboardAt(player, origin);
        if (success) {
            player.sendOverlayMessage(Component.translatable("wegui.message.paste_success", formatPos(origin)).withStyle(ChatFormatting.GREEN));
        } else {
            player.sendOverlayMessage(Component.translatable("wegui.message.paste_failed").withStyle(ChatFormatting.RED));
        }
    }

    private static void sendNormalPasteCommand(LocalPlayer player) {
        // WorldEdit 的 //paste 在 Brigadier 中注册为 /paste，需要保留前导 /
        CommandSender.sendRawCommand("/paste");
    }

    private static String formatPos(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}
