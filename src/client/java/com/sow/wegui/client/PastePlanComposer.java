package com.sow.wegui.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;

/**
 * 多人服务器精确偏移粘贴：把目标偏移量编排为一串合法的 WorldEdit 命令。
 *
 * <p>原理：</p>
 * <ol>
 *   <li>{@code /paste -so}：{@code -o} 把剪贴板粘贴回复制时的原位置（绝对坐标，与玩家站位无关），
 *       {@code -s} 粘贴后自动选中新区域，供后续 {@code /move} 操作；</li>
 *   <li>{@code /move <n> <方向>}：按轴平移选区内容，整数方块级精确。</li>
 * </ol>
 *
 * <p>平移与旋转/翻转可交换（先变换再平移 ≡ 先平移再变换），因此无需感知剪贴板的
 * 变换状态，偏移量直接在世界坐标系下生效。最终结果与目标偏移零误差（整数格），
 * 不受玩家站位亚格坐标影响——这是普通 {@code //paste} 相对粘贴做不到的。</p>
 *
 * <p>限制：实体不参与编排（与普通 {@code //paste} 默认行为一致）；"只替换空气"等
 * 掩码型变体无法用命令表达，由调用方负责拒绝。</p>
 */
public final class PastePlanComposer {
    private PastePlanComposer() {}

    /**
     * 一个可执行的粘贴计划。
     *
     * @param commands 单前导斜杠的命令序列（可直接交给 CommandSender.sendRawCommand 按序发送）
     */
    public record Plan(List<String> commands) {
        /** 撤销整个计划需要的 //undo 次数 */
        public int undoSteps() {
            return commands.size();
        }
    }

    public static Plan compose(BlockPos delta) {
        List<String> commands = new ArrayList<>();
        if (delta.equals(BlockPos.ZERO)) {
            // 零偏移保持旧行为：普通相对粘贴
            commands.add("/paste");
            return new Plan(commands);
        }
        // 先原位落地并让选区跟随，然后逐轴平移到目标
        commands.add("/paste -so");
        appendMove(commands, delta.getX(), "east", "west");
        appendMove(commands, delta.getZ(), "south", "north");
        appendMove(commands, delta.getY(), "up", "down");
        return new Plan(commands);
    }

    private static void appendMove(List<String> commands, int count, String positive, String negative) {
        if (count == 0) return;
        String dir = count > 0 ? positive : negative;
        commands.add("/move " + Math.abs(count) + " " + dir);
    }
}
