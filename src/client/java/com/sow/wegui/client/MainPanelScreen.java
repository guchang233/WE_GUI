package com.sow.wegui.client;

import com.sow.wegui.WeCommandCategory;
import com.sow.wegui.WeCommandRegistry;
import com.sow.wegui.config.WeGuiConfigs;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.render.GuiContext;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/**
 * WorldEdit 命令主面板 —— 使用 MaLiLib 的 GuiBase。
 * 显示所有分类，点击分类进入命令列表。
 */
public final class MainPanelScreen extends GuiBase {
    private static final int CAT_W = 130;
    private static final int BTN_H = 18;
    private static final int PAD = 6;

    public MainPanelScreen() {
        super();
        this.setTitle("WorldEdit GUI");
    }

    @Override
    public void initGui() {
        super.initGui();

        int cx = this.getScreenWidth() / 2;
        int startY = 40;
        int x = cx - CAT_W / 2;

        // 分类按钮
        List<WeCommandCategory> categories = List.of(WeCommandCategory.values());
        for (int i = 0; i < categories.size(); i++) {
            WeCommandCategory cat = categories.get(i);
            int count = WeCommandRegistry.getByCategory(cat).size();
            if (count == 0) continue;

            int y = startY + i * (BTN_H + PAD);
            String label = cat.getDisplayName() + " §8(" + count + ")";
            ButtonGeneric btn = new ButtonNoScroll(x, y, CAT_W, BTN_H, label);
            this.addButton(btn, new CategoryButtonListener(cat));
        }

        // 底部按钮
        int bottomY = this.getScreenHeight() - 30;
        ButtonGeneric closeBtn = new ButtonNoScroll(cx - 50, bottomY, 100, BTN_H, "关闭");
        this.addButton(closeBtn, (button, mouseButton) -> {
            if (this.getParent() != null) {
                this.mc.setScreen(this.getParent());
            } else {
                this.mc.setScreen(null);
            }
        });
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        super.render(g, mouseX, mouseY, partialTicks);

        GuiContext ctx = GuiContext.fromGuiGraphics(g);
        int cx = this.getScreenWidth() / 2;
        this.drawStringWithShadow(ctx, "§b§lWorldEdit GUI", cx - this.getStringWidth("WorldEdit GUI") / 2, 18, 0xFFFFFF);
        this.drawString(ctx, "§7触发物品: §f" + WeGuiConfigs.getTriggerItem(), cx - 80, this.getScreenHeight() - 48, 0xAAAAAA);
    }

    private record CategoryButtonListener(WeCommandCategory category) implements IButtonActionListener {
        @Override
        public void actionPerformedWithButton(fi.dy.masa.malilib.gui.button.ButtonBase button, int mouseButton) {
            GuiBase.openGui(new CommandCategoryScreen(category));
        }
    }
}
