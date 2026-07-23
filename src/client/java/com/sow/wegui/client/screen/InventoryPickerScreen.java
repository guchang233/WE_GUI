package com.sow.wegui.client.screen;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * 物品选择界面：显示玩家背包 + 快捷栏，支持搜索、Tooltip、高亮快捷栏，左键选择物品 ID。
 */
public class InventoryPickerScreen extends GuiBase {
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_GAP = 2;
    private static final int COLS = 9;
    private static final int ROWS = 4;

    private final Screen parent;
    private final Consumer<String> onSelected;

    private GuiTextFieldGeneric searchField;
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
        this.gridX = (this.width - gridW) / 2;
        this.gridY = 58;

        int fieldW = Math.min(260, this.width - 40);
        searchField = new GuiTextFieldGeneric((this.width - fieldW) / 2, 28, fieldW, 18, this.font);
        searchField.setHint(Component.literal(StringUtils.translate("wegui.picker.search")));
        searchField.setMaxLength(128);
        this.addTextField(searchField, (textField) -> true);

        ButtonGeneric back = new ButtonGeneric(this.width / 2 - 35, this.height - 32, 70, 20, StringUtils.translate("wegui.command.back"));
        this.addButton(back, (btn, mouseButton) -> this.mc.setScreen(parent));
    }

    @Override
    public void drawContents(GuiGraphics ctx, int mouseX, int mouseY, float partialTick) {
        Inventory inv = this.mc.player.getInventory();
        String query = getSearchQuery();

        Integer hoveredSlot = null;

        for (int slot = 0; slot < 36; slot++) {
            if (!matches(inv.getItem(slot), query)) continue;

            int col = slot % COLS;
            int row = slot / COLS;
            int x = gridX + col * (SLOT_SIZE + SLOT_GAP);
            int y = gridY + row * (SLOT_SIZE + SLOT_GAP);

            boolean hovered = mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE;
            if (hovered) hoveredSlot = slot;

            boolean empty = inv.getItem(slot).isEmpty();
            int bgColor;
            if (empty) {
                bgColor = hovered ? 0xFF666666 : 0xFF3A3A3A;
            } else if (row == 0) {
                bgColor = hovered ? 0xFFCCCC66 : 0xFF888844;
            } else {
                bgColor = hovered ? 0xFFAAAAAA : 0xFF555555;
            }
            ctx.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, bgColor);

            ItemStack stack = inv.getItem(slot);
            if (!stack.isEmpty()) {
                ctx.renderItem(stack, x + 1, y + 1);
                ctx.renderItemDecorations(this.font, stack, x + 1, y + 1);
            }
        }

        if (hoveredSlot != null) {
            ItemStack stack = inv.getItem(hoveredSlot);
            if (!stack.isEmpty()) {
                ctx.renderTooltip(this.font, stack, mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean onMouseClicked(int mouseX, int mouseY, int button) {
        if (super.onMouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (button != 0) return false;

        int slot = getSlotAt(mouseX, mouseY);
        if (slot < 0) return false;

        Inventory inv = this.mc.player.getInventory();
        ItemStack stack = inv.getItem(slot);
        if (stack.isEmpty()) return false;

        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        onSelected.accept(id);
        this.mc.setScreen(parent);
        return true;
    }

    private String getSearchQuery() {
        return searchField == null ? "" : searchField.getValue().trim().toLowerCase(Locale.ROOT);
    }

    private boolean matches(ItemStack stack, String query) {
        if (query.isBlank()) return true;
        if (stack.isEmpty()) return false;
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
        String q = query.toLowerCase(Locale.ROOT);
        return id.contains(q) || name.contains(q);
    }

    private int getSlotAt(int mouseX, int mouseY) {
        Inventory inv = this.mc.player.getInventory();
        String query = getSearchQuery();
        for (int slot = 0; slot < 36; slot++) {
            if (!matches(inv.getItem(slot), query)) continue;
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
