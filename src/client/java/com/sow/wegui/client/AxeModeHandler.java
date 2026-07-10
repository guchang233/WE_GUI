package com.sow.wegui.client;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import com.sow.wegui.config.Configs;
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
        EDIT_SELECTION("wegui.mode.edit_selection");

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

        // 右键记录 pos2 为最后修改点，并根据开关显示提示
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (level.isClientSide() && hand == InteractionHand.MAIN_HAND && isHoldingConfiguredWand(player)) {
                lastModified = LastModifiedCorner.POS2;
                if (Configs.Generic.SELECTION_MESSAGE_ENABLED.getBooleanValue()) {
                    BlockPos target = getLookingAtPos(player, level);
                    if (target != null) {
                        player.displayClientMessage(Component.translatable("wegui.message.pos2_set", formatPos(target)).withStyle(ChatFormatting.DARK_PURPLE), false);
                    }
                }
            }
            return InteractionResult.PASS;
        });
    }

    public static AxeMode getMode() {
        return currentMode;
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
            cycleMode(player);
            return true;
        }

        // alt：仅在编辑选区模式下移动选点
        if (currentMode != AxeMode.EDIT_SELECTION) {
            player.displayClientMessage(Component.translatable("wegui.message.need_edit_mode").withStyle(ChatFormatting.RED), true);
            return true;
        }

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

    private static void cycleMode(LocalPlayer player) {
        AxeMode[] values = AxeMode.values();
        currentMode = values[(currentMode.ordinal() + 1) % values.length];
        player.displayClientMessage(Component.translatable("wegui.message.mode_changed", currentMode.getDisplayName()).withStyle(ChatFormatting.GREEN), true);
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

    private static String formatPos(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}
