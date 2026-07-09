package com.sow.wegui.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * 小木斧双模式控制：
 * - 正常模式：保持 WorldEdit 默认行为（左键 pos1，右键 pos2）。
 * - 编辑选区模式：按住 Alt 并滚动滚轮，可向玩家朝向方向挪动选区角点；
 *   若准星未指向任何角点，则整体平移选区。
 */
public final class AxeModeHandler {
    private AxeModeHandler() {}

    public enum AxeMode {
        NORMAL("正常模式"),
        EDIT_SELECTION("编辑选区");

        private final String displayName;

        AxeMode(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private static AxeMode currentMode = AxeMode.NORMAL;
    private static long lastScrollTime = 0;
    private static final long SCROLL_COOLDOWN_MS = 80;

    public static void register() {
        // 滚动事件由 Mixin 注入后调用 handleMouseScroll。
    }

    public static AxeMode getMode() {
        return currentMode;
    }

    public static void toggleMode() {
        currentMode = currentMode == AxeMode.NORMAL ? AxeMode.EDIT_SELECTION : AxeMode.NORMAL;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("§a[WE GUI] 小木斧模式: §f" + currentMode.getDisplayName()), true);
        }
    }

    public static boolean handleMouseScroll(double scrollDelta) {
        if (currentMode != AxeMode.EDIT_SELECTION) return false;
        if (scrollDelta == 0) return false;

        long now = System.currentTimeMillis();
        if (now - lastScrollTime < SCROLL_COOLDOWN_MS) return false;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null) return false;
        if (!isHoldingWoodenAxe(player)) return false;
        if (!isAltHeld()) return false;

        WorldEditBridge.CornerPositions corners = WorldEditBridge.getSelectionCorners(mc);
        if (corners == null) {
            player.displayClientMessage(Component.literal("§c[WE GUI] 没有可用的立方体选区"), true);
            return true;
        }

        int amount = scrollDelta > 0 ? 1 : -1;
        Direction direction = getLookDirection(player);
        if (direction == null) return true;

        BlockPos target = getTargetCorner(mc, corners);
        if (target != null) {
            // 移动单个角点
            BlockPos moved = target.relative(direction, amount);
            if (target.equals(corners.pos1())) {
                CommandSender.send("//pos1 " + formatPos(moved));
                player.displayClientMessage(Component.literal("§a[WE GUI] 移动 pos1: §f" + formatPos(moved)), true);
            } else {
                CommandSender.send("//pos2 " + formatPos(moved));
                player.displayClientMessage(Component.literal("§a[WE GUI] 移动 pos2: §f" + formatPos(moved)), true);
            }
        } else {
            // 整体平移选区
            BlockPos moved1 = corners.pos1().relative(direction, amount);
            BlockPos moved2 = corners.pos2().relative(direction, amount);
            CommandSender.send("//pos1 " + formatPos(moved1));
            CommandSender.send("//pos2 " + formatPos(moved2));
            player.displayClientMessage(Component.literal("§a[WE GUI] 平移选区: §f" + formatPos(moved1) + " -> " + formatPos(moved2)), true);
        }

        lastScrollTime = now;
        return true;
    }

    private static boolean isHoldingWoodenAxe(LocalPlayer player) {
        return player.getMainHandItem().is(Items.WOODEN_AXE)
                || player.getOffhandItem().is(Items.WOODEN_AXE);
    }

    private static boolean isAltHeld() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
    }

    private static Direction getLookDirection(LocalPlayer player) {
        Vec3 view = player.getViewVector(1.0f);
        return Direction.getNearest(view.x, view.y, view.z);
    }

    @Nullable
    private static BlockPos getTargetCorner(Minecraft mc, WorldEditBridge.CornerPositions corners) {
        LocalPlayer player = mc.player;
        if (player == null) return null;

        double reach = 64.0;
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 view = player.getViewVector(1.0f);
        Vec3 end = eye.add(view.x * reach, view.y * reach, view.z * reach);

        AABB box1 = cornerBox(corners.pos1());
        AABB box2 = cornerBox(corners.pos2());

        Vec3 hit1 = clipBox(box1, eye, end);
        Vec3 hit2 = clipBox(box2, eye, end);

        if (hit1 == null && hit2 == null) return null;
        if (hit1 == null) return corners.pos2();
        if (hit2 == null) return corners.pos1();

        double d1 = eye.distanceToSqr(hit1);
        double d2 = eye.distanceToSqr(hit2);
        return d1 <= d2 ? corners.pos1() : corners.pos2();
    }

    private static AABB cornerBox(BlockPos pos) {
        return new AABB(
                pos.getX() - 0.125, pos.getY() - 0.125, pos.getZ() - 0.125,
                pos.getX() + 1.125, pos.getY() + 1.125, pos.getZ() + 1.125);
    }

    @Nullable
    private static Vec3 clipBox(AABB box, Vec3 start, Vec3 end) {
        double[] tMin = {0.0};
        double[] tMax = {1.0};
        if (!clipLine(box.minX, box.maxX, start.x, end.x, tMin, tMax)) return null;
        if (!clipLine(box.minY, box.maxY, start.y, end.y, tMin, tMax)) return null;
        if (!clipLine(box.minZ, box.maxZ, start.z, end.z, tMin, tMax)) return null;
        if (tMin[0] < 0 || tMin[0] > 1) return null;
        return start.lerp(tMin[0], end);
    }

    private static boolean clipLine(double min, double max, double start, double end, double[] tMin, double[] tMax) {
        double dir = end - start;
        if (Math.abs(dir) < 1e-6) {
            return start >= min && start <= max;
        }
        double t1 = (min - start) / dir;
        double t2 = (max - start) / dir;
        if (t1 > t2) {
            double tmp = t1;
            t1 = t2;
            t2 = tmp;
        }
        if (t1 > tMin[0]) tMin[0] = t1;
        if (t2 < tMax[0]) tMax[0] = t2;
        return tMin[0] <= tMax[0];
    }

    private static String formatPos(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}
