package com.sow.wegui.client;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import com.sow.wegui.config.Configs;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * WE 绑定工具双模式控制：
 * - 正常模式：保持 WorldEdit 默认行为（左键 pos1，右键 pos2）。
 * - 编辑选区模式：按住 Alt 并滚动滚轮，可向玩家朝向方向挪动上一次修改的选点。
 *
 * 模式切换：手持配置的工具时按住 Ctrl 并滚动滚轮循环切换所有模式。
 * 手持配置的工具时只要按下了 Ctrl 或 Alt，滚轮事件都会被消费，避免触发物品栏切换。
 */
public final class AxeModeHandler {
    private AxeModeHandler() {}

    public enum AxeMode {
        NORMAL("wegui.mode.normal"),
        EDIT_SELECTION("wegui.mode.edit_selection"),
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

    private static AxeMode currentMode = AxeMode.NORMAL;
    private static LastModifiedCorner lastModified = LastModifiedCorner.NONE;
    private static BlockPos pastePreviewOffset = BlockPos.ZERO;

    public static void register() {
        // 左键记录 pos1 为最后修改点，并根据开关显示提示
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (level.isClientSide() && hand == InteractionHand.MAIN_HAND && isHoldingConfiguredWand(player)) {
                lastModified = LastModifiedCorner.POS1;
                if (Configs.Generic.SELECTION_MESSAGE_ENABLED.getBooleanValue()) {
                    player.displayClientMessage(Component.translatable("wegui.message.pos1_set", formatPos(pos)).withStyle(ChatFormatting.DARK_PURPLE), false);
                }
            }
            return InteractionResult.PASS;
        });

        // 右键方块：移动 paste 预览模式下禁用 WorldEdit 默认 pos2 行为
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (!level.isClientSide() || hand != InteractionHand.MAIN_HAND || !isHoldingConfiguredWand(player)) {
                return InteractionResult.PASS;
            }

            if (currentMode == AxeMode.MOVE_PASTE_PREVIEW) {
                return InteractionResult.SUCCESS;
            }

            lastModified = LastModifiedCorner.POS2;
            if (Configs.Generic.SELECTION_MESSAGE_ENABLED.getBooleanValue()) {
                BlockPos target = hitResult.getBlockPos();
                player.displayClientMessage(Component.translatable("wegui.message.pos2_set", formatPos(target)).withStyle(ChatFormatting.DARK_PURPLE), false);
            }
            return InteractionResult.PASS;
        });

        // 右键物品/空气：移动 paste 预览模式下禁用 WorldEdit 默认 pos2 行为
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (!level.isClientSide() || hand != InteractionHand.MAIN_HAND || !isHoldingConfiguredWand(player)) {
                return InteractionResult.PASS;
            }

            if (currentMode == AxeMode.MOVE_PASTE_PREVIEW) {
                return InteractionResult.SUCCESS;
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
     * 若存在非零粘贴偏移，则在预览原点执行 //paste；否则正常发送 //paste。
     */
    public static void executePasteAtPreview(LocalPlayer player) {
        pasteAtPreview(player);
    }

    /**
     * 处理鼠标滚动事件。只要手持木斧且按下了 Ctrl 或 Alt，就返回 true 以消费事件，
     * 防止滚轮继续触发物品栏切换等默认行为。
     */
    public static boolean handleMouseScroll(double scrollDelta) {
        if (scrollDelta == 0) return false;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null) return false;
        if (!isHoldingConfiguredWand(player)) return false;

        boolean ctrl = isModifierHeld(GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL);
        boolean alt = isModifierHeld(GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT);
        if (!ctrl && !alt) return false;

        if (ctrl) {
            cycleMode(player, scrollDelta > 0);
            return true;
        }

        if (currentMode == AxeMode.EDIT_SELECTION) {
            return handleEditSelectionScroll(player, mc, scrollDelta);
        }

        if (currentMode == AxeMode.MOVE_PASTE_PREVIEW) {
            return handleMovePastePreviewScroll(player, scrollDelta);
        }

        player.displayClientMessage(Component.translatable("wegui.message.need_edit_or_move_mode").withStyle(ChatFormatting.RED), true);
        return true;
    }

    private static boolean handleEditSelectionScroll(LocalPlayer player, Minecraft mc, double scrollDelta) {
        WorldEditBridge.PartialCornerPositions corners = WorldEditBridge.getPartialSelectionCorners(mc);
        if (corners == null) {
            player.displayClientMessage(Component.translatable("wegui.message.no_selection").withStyle(ChatFormatting.RED), true);
            return true;
        }

        if (lastModified == LastModifiedCorner.NONE) {
            player.displayClientMessage(Component.translatable("wegui.message.no_corner").withStyle(ChatFormatting.RED), true);
            return true;
        }

        BlockPos target = lastModified == LastModifiedCorner.POS1 ? corners.pos1() : corners.pos2();
        if (target == null) {
            player.displayClientMessage(Component.translatable("wegui.message.corner_not_set").withStyle(ChatFormatting.RED), true);
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
            player.displayClientMessage(Component.translatable("wegui.message.moved_pos1", formatPos(moved)).withStyle(ChatFormatting.GREEN), true);
        } else {
            CommandSender.send("//pos2 " + formatPos(moved));
            player.displayClientMessage(Component.translatable("wegui.message.moved_pos2", formatPos(moved)).withStyle(ChatFormatting.GREEN), true);
        }

        return true;
    }

    private static boolean handleMovePastePreviewScroll(LocalPlayer player, double scrollDelta) {
        if (!hasClipboard() || !WorldEditBridge.canUseDirectPaste()) {
            player.displayClientMessage(Component.translatable("wegui.message.no_clipboard_or_multiplayer").withStyle(ChatFormatting.RED), true);
            resetPastePreviewOffset();
            return true;
        }

        int amount = scrollDelta > 0 ? 1 : -1;
        Direction direction = getLookDirection(player);
        if (direction == null) {
            return true;
        }

        pastePreviewOffset = pastePreviewOffset.relative(direction, amount);
        player.displayClientMessage(Component.translatable("wegui.message.moved_paste_preview",
                formatPos(pastePreviewOffset)).withStyle(ChatFormatting.GREEN), true);
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

        player.displayClientMessage(Component.translatable("wegui.message.mode_changed",
                Component.literal("[" + (currentMode.ordinal() + 1) + "/" + count + "] "),
                currentMode.getDisplayName()).withStyle(ChatFormatting.GREEN), true);
    }

    public static boolean isHoldingConfiguredWand(Player player) {
        Item wandItem = getConfiguredWandItem();
        if (wandItem == null) return false;
        return player.getMainHandItem().is(wandItem)
                || player.getOffhandItem().is(wandItem);
    }

    @Nullable
    private static Item getConfiguredWandItem() {
        String id = Configs.Generic.WAND_ITEM.getStringValue();
        try {
            net.minecraft.resources.Identifier identifier = net.minecraft.resources.Identifier.parse(id);
            return net.minecraft.core.registries.BuiltInRegistries.ITEM.get(identifier)
                    .map(ref -> ref.value())
                    .orElse(null);
        } catch (Exception e) {
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

    @Nullable
    private static BlockPos getLookingAtPos(Player player, Level level) {
        double reach = player.blockInteractionRange();
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 view = player.getViewVector(1.0f);
        Vec3 end = eye.add(view.x * reach, view.y * reach, view.z * reach);
        BlockHitResult result = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (result.getType() == HitResult.Type.BLOCK) {
            return result.getBlockPos();
        }
        return null;
    }

    private static boolean hasClipboard() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && WorldEditBridge.getClipboardBounds(mc) != null;
    }

    /**
     * 在预览原点执行粘贴。单人世界且存在非零偏移时直接调用 WorldEdit API，避免命令异步导致位置错误；
     * 无偏移或无法直接粘贴时退回到普通 //paste 命令。
     */
    private static void pasteAtPreview(LocalPlayer player) {
        WeGuiMod.LOGGER.info("[WE GUI] pasteAtPreview 被调用, offset={}, canDirect={}",
                pastePreviewOffset, WorldEditBridge.canUseDirectPaste());

        if (pastePreviewOffset.equals(BlockPos.ZERO)) {
            WeGuiMod.LOGGER.info("[WE GUI] offset 为零，走普通 //paste");
            sendNormalPasteCommand(player);
            return;
        }

        if (!WorldEditBridge.canUseDirectPaste()) {
            WeGuiMod.LOGGER.info("[WE GUI] 无法直接粘贴，提示多人服务器");
            player.displayClientMessage(Component.translatable("wegui.message.move_paste_multiplayer_disabled").withStyle(ChatFormatting.RED), true);
            return;
        }

        BlockPos origin = getPasteOrigin(player);
        WeGuiMod.LOGGER.info("[WE GUI] 调用 API 粘贴到 {}", origin);
        boolean success = WorldEditBridge.pasteClipboardAt(player, origin);
        if (success) {
            player.displayClientMessage(Component.translatable("wegui.message.paste_success", formatPos(origin)).withStyle(ChatFormatting.GREEN), true);
        } else {
            player.displayClientMessage(Component.translatable("wegui.message.paste_failed").withStyle(ChatFormatting.RED), true);
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
