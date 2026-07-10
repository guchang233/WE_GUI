package com.sow.wegui.client.screen;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

/**
 * 物品选择界面：显示玩家背包 + 快捷栏，左键物品即将其 ID 回填到调用方。
 */
public class InventoryPickerScreen extends GuiBase {
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_GAP = 2;
    private static final int COLS = 9;
    private static final int ROWS = 4;

    private final Screen parent;
    private final Consumer<String> onSelected;

    private int gridX;
    private int gridY;

    public InventoryPickerScreen(Screen parent, Consumer<String> onSelected) {
        this.setParent(parent);
        this.parent = parent;
        this.onSelected = onSelected;
        this.setTitle(StringUtils.translate("wegui.picker.title"));
    }

    @Override
    public void initGui() {
        super.initGui();

        int gridW = COLS * SLOT_SIZE + (COLS - 1) * SLOT_GAP;
        int gridH = ROWS * SLOT_SIZE + (ROWS - 1) * SLOT_GAP;
        this.gridX = (this.width - gridW) / 2;
        this.gridY = (this.height - gridH) / 2 + 10;

        ButtonGeneric back = new ButtonGeneric(this.width / 2 - 35, this.height - 32, 70, 20, StringUtils.translate("wegui.command.back"));
        this.addButton(back, (btn, mouseButton) -> this.mc.setScreen(parent));
    }

    @Override
    public void drawContents(GuiContext ctx, int mouseX, int mouseY, float partialTick) {
        Inventory inv = this.mc.player.getInventory();

        for (int slot = 0; slot < 36; slot++) {
            int col = slot % COLS;
            int row = slot / COLS;
            int x = gridX + col * (SLOT_SIZE + SLOT_GAP);
            int y = gridY + row * (SLOT_SIZE + SLOT_GAP);

            // 高亮悬停槽位
            boolean hovered = mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE;
            int bgColor = hovered ? 0xFFAAAAAA : 0xFF555555;
            ctx.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, bgColor);

            ItemStack stack = inv.getItem(slot);
            if (!stack.isEmpty()) {
                ctx.renderItem(stack, x + 1, y + 1);
                ctx.renderItemDecorations(this.font, stack, x + 1, y + 1);
            }
        }
    }

    @Override
    public boolean onMouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.onMouseClicked(event, doubleClick)) {
            return true;
        }

        if (event.button() != 0) return false;

        int slot = getSlotAt((int) event.x(), (int) event.y());
        if (slot < 0) return false;

        Inventory inv = this.mc.player.getInventory();
        ItemStack stack = inv.getItem(slot);
        if (stack.isEmpty()) return false;

        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        onSelected.accept(id);
        this.mc.setScreen(parent);
        return true;
    }

    private int getSlotAt(int mouseX, int mouseY) {
        for (int slot = 0; slot < 36; slot++) {
            int col = slot % COLS;
            int row = slot / COLS;
            int x = gridX + col * (SLOT_SIZE + SLOT_GAP);
            int y = gridY + row * (SLOT_SIZE + SLOT_GAP);
            if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                return slot;
            }
        }
        return -1;
    }
}
