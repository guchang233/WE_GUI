package com.sow.wegui.client;

import com.sow.wegui.ModConfig;
import com.sow.wegui.WeCommand;
import com.sow.wegui.WeCommandCategory;
import com.sow.wegui.WeCommandRegistry;
import com.sow.wegui.WheelProfile;
import com.sow.wegui.WheelSlot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 设置面板 — 轮盘管理 + 全局配置。
 */
public final class SettingsScreen extends Screen {
    private enum Tab { WHEELS, GLOBAL }

    private final Screen parent;
    private Tab tab = Tab.WHEELS;
    private int leftW, rightX, rightW;

    // 轮盘编辑状态
    private int selectedWheelIndex = 0;
    private int editingSlotIndex = -1;

    // 全局配置控件
    private EditBox line1Box, line2Box;

    public SettingsScreen(Screen parent) {
        super(Component.literal("WE GUI 设置"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        leftW = 140;
        rightX = leftW + 4;
        rightW = this.width - rightX - 4;
        if (tab == Tab.WHEELS) initWheelsTab();
        else initGlobalTab();
    }

    // ════════════════════ 轮盘标签 ════════════════════

    private void initWheelsTab() {
        // 标签
        addRenderableWidget(Button.builder(Component.literal("轮盘"), b -> { tab = Tab.WHEELS; init(); })
                .bounds(6, 6, leftW / 2 - 6, 18).build());
        addRenderableWidget(Button.builder(Component.literal("全局"), b -> { tab = Tab.GLOBAL; init(); })
                .bounds(leftW / 2 + 4, 6, leftW / 2 - 6, 18).build());

        List<WheelProfile> wheels = ModConfig.get().wheels;
        if (selectedWheelIndex >= wheels.size()) selectedWheelIndex = 0;
        if (editingSlotIndex >= ModConfig.get().wheelSlotCount) editingSlotIndex = -1;

        // 左侧轮盘列表
        int y = 32;
        for (int i = 0; i < wheels.size(); i++) {
            boolean sel = i == selectedWheelIndex;
            String name = (sel ? "§b▶ " : "§7  ") + wheels.get(i).name();
            int fi = i;
            addRenderableWidget(Button.builder(Component.literal(name), b -> {
                selectedWheelIndex = fi;
                editingSlotIndex = -1;
                init();
            }).bounds(6, y, leftW - 12, 16).build());
            y += 18;
        }
        y += 4;

        // 添加/删除
        addRenderableWidget(Button.builder(Component.literal("＋添加"), b -> {
            wheels.add(new WheelProfile("新轮盘"));
            selectedWheelIndex = wheels.size() - 1;
            ModConfig.get().rebuildSlots();
            ModConfig.save();
            init();
        }).bounds(6, y, (leftW - 16) / 2, 16).build());
        addRenderableWidget(Button.builder(Component.literal("－删除"), b -> {
            if (wheels.size() > 1) {
                wheels.remove(selectedWheelIndex);
                if (selectedWheelIndex >= wheels.size()) selectedWheelIndex = wheels.size() - 1;
                ModConfig.save();
                init();
            }
        }).bounds(8 + (leftW - 16) / 2, y, (leftW - 16) / 2, 16).build());

        // 右侧轮盘编辑
        if (selectedWheelIndex < wheels.size()) {
            renderWheelEditor(wheels.get(selectedWheelIndex));
        }
    }

    private void renderWheelEditor(WheelProfile wheel) {
        int y = 30;
        int ew = rightW - 10;

        // 轮盘名
        addRenderableWidget(Button.builder(Component.literal("§7名称:"), b -> {}).bounds(rightX, y, 50, 16).build());
        EditBox nameBox = new EditBox(this.font, rightX + 52, y, ew - 52, 16, Component.literal(""));
        nameBox.setValue(wheel.name());
        nameBox.setResponder(t -> { wheel.setName(t); ModConfig.save(); });
        addRenderableWidget(nameBox);
        y += 22;

        // 颜色
        int[] colors = {0x4A90D9, 0xE67E22, 0x27AE60, 0xE74C3C, 0x9B59B6, 0x2ECC71, 0xF1C40F, 0x95A5A6};
        for (int i = 0; i < colors.length; i++) {
            int c = colors[i];
            boolean sel = wheel.color() == c;
            int bx = rightX + i * 22;
            addRenderableWidget(Button.builder(Component.literal(sel ? "●" : "○"), b -> {
                wheel.setColor(c);
                ModConfig.save();
                init();
            }).bounds(bx, y, 20, 16).build());
        }
        y += 22;

        // 槽位按钮
        for (int i = 0; i < wheel.slotCount(); i++) {
            WheelSlot slot = wheel.getSlot(i);
            String label;
            if (slot == null || slot.commandId() == null || slot.commandId().isBlank()) {
                label = "§8空";
            } else {
                WeCommand cmd = WeCommandRegistry.getById(slot.commandId()).orElse(null);
                label = cmd != null ? cmd.displayName() : "§c?" + slot.commandId();
            }
            boolean edit = i == editingSlotIndex;
            int sx = rightX + (i % 4) * (ew / 4);
            int sy = y + (i / 4) * 18;
            int fi = i;
            addRenderableWidget(Button.builder(Component.literal((edit ? "§b▶ " : "") + label), b -> {
                editingSlotIndex = (editingSlotIndex == fi) ? -1 : fi;
                init();
            }).bounds(sx, sy, ew / 4 - 2, 16).build());
        }
        y += ((wheel.slotCount() + 3) / 4) * 18 + 4;

        // 命令选择器
        if (editingSlotIndex >= 0) {
            renderCommandSelector(wheel, editingSlotIndex, rightX, y, ew);
        }
    }

    private void renderCommandSelector(WheelProfile wheel, int slotIdx, int x, int y, int w) {
        int catY = y;
        for (WeCommandCategory cat : WeCommandCategory.values()) {
            List<WeCommand> cmds = WeCommandRegistry.getByCategory(cat);
            if (cmds.isEmpty()) continue;
            if (catY + 16 > this.height - 10) break;
            addRenderableWidget(Button.builder(Component.literal("§b" + cat.getDisplayName()), b -> {})
                    .bounds(x, catY, w, 14).build());
            catY += 16;

            int cols = Math.max(1, w / 100);
            int btnW = (w - (cols - 1) * 2) / cols;
            for (int i = 0; i < cmds.size(); i++) {
                if (catY + 14 > this.height - 10) break;
                int bx = x + (i % cols) * (btnW + 2);
                int by = catY + (i / cols) * 16;
                WeCommand cmd = cmds.get(i);
                addRenderableWidget(Button.builder(Component.literal(cmd.displayName()), b -> {
                    wheel.setSlot(slotIdx, new WheelSlot(cmd.id()));
                    ModConfig.save();
                    editingSlotIndex = -1;
                    init();
                }).bounds(bx, by, btnW, 14).build());
            }
            catY += ((cmds.size() + cols - 1) / cols) * 16 + 4;
        }
    }

    // ════════════════════ 全局标签 ════════════════════

    private void initGlobalTab() {
        int cx = this.width / 2;
        int y = 32;

        addRenderableWidget(Button.builder(Component.literal("轮盘"), b -> { tab = Tab.WHEELS; init(); })
                .bounds(6, 6, leftW / 2 - 6, 18).build());
        addRenderableWidget(Button.builder(Component.literal("全局"), b -> { tab = Tab.GLOBAL; init(); })
                .bounds(leftW / 2 + 4, 6, leftW / 2 - 6, 18).build());

        // 触发工具
        EditBox triggerBox = new EditBox(this.font, cx - 80, y, 160, 16, Component.literal(""));
        triggerBox.setHint(Component.literal("触发工具 ID"));
        triggerBox.setValue(ModConfig.get().triggerItem);
        triggerBox.setResponder(t -> {
            if (!t.isBlank()) { ModConfig.get().triggerItem = t.trim(); ModConfig.save(); }
        });
        addRenderableWidget(triggerBox);
        y += 30;

        // 槽位数
        addRenderableWidget(Button.builder(Component.literal("-"), b -> {
            ModConfig.get().setWheelSlotCount(ModConfig.get().wheelSlotCount - 1); init();
        }).bounds(cx - 80, y, 28, 16).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            ModConfig.get().setWheelSlotCount(ModConfig.get().wheelSlotCount + 1); init();
        }).bounds(cx - 48, y, 28, 16).build());
        y += 30;

        // 状态栏位置
        ModConfig.Anchor[] anchors = ModConfig.Anchor.values();
        ModConfig.Anchor cur = ModConfig.get().statusBarAnchor();
        for (int i = 0; i < anchors.length; i++) {
            ModConfig.Anchor a = anchors[i];
            boolean sel = a == cur;
            addRenderableWidget(Button.builder(
                    Component.literal((sel ? "§b" : "") + anchorLabel(a)), b -> {
                ModConfig.get().statusBarPosition = a.name();
                ModConfig.save(); init();
            }).bounds(cx - 80 + i * 42, y, 40, 16).build());
        }
        y += 30;

        // 偏移
        addRenderableWidget(Button.builder(Component.literal("X-"), b -> {
            ModConfig.get().statusBarOffsetX -= 1; ModConfig.save(); init();
        }).bounds(cx - 80, y, 24, 16).build());
        addRenderableWidget(Button.builder(Component.literal("X+"), b -> {
            ModConfig.get().statusBarOffsetX += 1; ModConfig.save(); init();
        }).bounds(cx - 54, y, 24, 16).build());
        addRenderableWidget(Button.builder(Component.literal("Y-"), b -> {
            ModConfig.get().statusBarOffsetY -= 1; ModConfig.save(); init();
        }).bounds(cx - 26, y, 24, 16).build());
        addRenderableWidget(Button.builder(Component.literal("Y+"), b -> {
            ModConfig.get().statusBarOffsetY += 1; ModConfig.save(); init();
        }).bounds(cx, y, 24, 16).build());
        y += 30;

        // 格式字符串
        line1Box = new EditBox(this.font, cx - 80, y, 160, 16, Component.literal(""));
        line1Box.setValue(ModConfig.get().statusBarLine1);
        line1Box.setResponder(t -> { ModConfig.get().statusBarLine1 = t; ModConfig.save(); });
        addRenderableWidget(line1Box);
        y += 22;
        line2Box = new EditBox(this.font, cx - 80, y, 160, 16, Component.literal(""));
        line2Box.setValue(ModConfig.get().statusBarLine2);
        line2Box.setResponder(t -> { ModConfig.get().statusBarLine2 = t; ModConfig.save(); });
        addRenderableWidget(line2Box);
        y += 32;

        // 恢复默认
        addRenderableWidget(Button.builder(Component.literal("恢复默认轮盘"), b -> {
            ModConfig.get().wheels = com.sow.wegui.WheelPresetsFactory.createDefaultWheels();
            ModConfig.get().rebuildSlots();
            selectedWheelIndex = 0;
            init();
        }).bounds(cx - 80, y, 160, 18).build());
        y += 26;

        addRenderableWidget(Button.builder(Component.literal("返回"), b -> this.minecraft.setScreen(parent))
                .bounds(cx - 40, y, 80, 18).build());
    }

    // ════════════════════ 绘制 ════════════════════

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        if (tab == Tab.GLOBAL) renderGlobalLabels(g);
        g.drawCenteredString(this.font, "§8WE GUI 设置", this.width / 2, 8, 0xFFFFFFFF);
    }

    private void renderGlobalLabels(GuiGraphics g) {
        int cx = this.width / 2, y = 22;
        g.drawString(this.font, "触发工具", cx - 90, y, 0xFF888888); y += 30;
        g.drawString(this.font, "槽位数: " + ModConfig.get().wheelSlotCount, cx - 90, y, 0xFF888888); y += 30;
        g.drawString(this.font, "状态栏位置", cx - 90, y, 0xFF888888); y += 30;
        g.drawString(this.font, "偏移 X:" + ModConfig.get().statusBarOffsetX
                + " Y:" + ModConfig.get().statusBarOffsetY, cx - 90, y, 0xFF888888); y += 30;
        g.drawString(this.font, "状态栏格式", cx - 90, y, 0xFF888888);
        g.drawString(this.font, "§7可用: {size} {count} {width} {height} {length} {clipboard} {version}",
                cx - 80, y + 42, 0xFF666666);
    }

    // ════════════════════ 按键捕获 ════════════════════

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        ModConfig.save();
        this.minecraft.setScreen(parent);
    }

    // ── 工具 ──
    private static String anchorLabel(ModConfig.Anchor a) {
        return switch (a) {
            case TOP_LEFT -> "左上";
            case TOP_RIGHT -> "右上";
            case BOTTOM_LEFT -> "左下";
            case BOTTOM_RIGHT -> "右下";
        };
    }
}