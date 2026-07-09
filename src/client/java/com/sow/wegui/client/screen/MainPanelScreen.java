package com.sow.wegui.client.screen;

import com.sow.wegui.commands.WeCommands;
import com.sow.wegui.commands.WeCommands.Category;
import com.sow.wegui.commands.WeCommands.Command;
import com.sow.wegui.commands.WeCommands.Usage;
import com.sow.wegui.client.CommandSender;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 主面板：按分类展示所有 WorldEdit 命令。
 */
public class MainPanelScreen extends BaseScreen {
    private static final int TOP = 38;
    private static final int BOTTOM_PAD = 10;
    private static final int BUTTON_W = 120;
    private static final int BUTTON_H = 18;
    private static final int GAP = 4;

    private Category selectedCategory = Category.GENERAL;
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private final List<Button> commandButtons = new ArrayList<>();

    public MainPanelScreen() {
        super(Component.literal("WorldEdit GUI"));
    }

    @Override
    protected void init() {
        super.init();
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        commandButtons.clear();

        // 分类标签
        Category[] categories = Category.values();
        int tabW = Math.max(52, (width - 20) / categories.length);
        for (int i = 0; i < categories.length; i++) {
            Category cat = categories[i];
            int x = 10 + i * (tabW + 2);
            addRenderableWidget(Button.builder(
                            Component.literal(cat.getDisplayName()),
                            b -> selectCategory(cat))
                    .bounds(x, 5, tabW, 18)
                    .build());
        }

        // 设置按钮
        addRenderableWidget(Button.builder(
                        Component.literal("设置"),
                        b -> minecraft.setScreen(new SettingsScreen(this)))
                .bounds(width - 60, 5, 50, 18)
                .build());

        // 命令按钮
        List<Command> commands = WeCommands.byCategory(selectedCategory);
        int cols = Math.max(1, (width - 20) / (BUTTON_W + GAP));
        int rows = (commands.size() + cols - 1) / cols;
        int contentH = rows * (BUTTON_H + GAP);
        int viewH = height - TOP - BOTTOM_PAD;
        maxScroll = Math.max(0, contentH - viewH);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        for (int i = 0; i < commands.size(); i++) {
            Command cmd = commands.get(i);
            int col = i % cols;
            int row = i / cols;
            int x = 10 + col * (BUTTON_W + GAP);
            int y = TOP + row * (BUTTON_H + GAP) - scrollOffset;
            Button btn = Button.builder(
                            Component.literal(cmd.displayName()),
                            b -> onCommand(cmd))
                    .bounds(x, y, BUTTON_W, BUTTON_H)
                    .build();
            btn.setTooltip(Tooltip.create(Component.literal(cmd.description())));
            addRenderableWidget(btn);
            commandButtons.add(btn);
        }
    }

    private void selectCategory(Category cat) {
        selectedCategory = cat;
        scrollOffset = 0;
        rebuild();
    }

    private void onCommand(Command cmd) {
        if (cmd.usages().size() == 1) {
            Usage usage = cmd.usages().get(0);
            if (usage.params().isEmpty()) {
                CommandSender.send(usage.buildCommand(List.of()));
                onClose();
            } else {
                minecraft.setScreen(new ParamInputScreen(this, cmd, usage));
            }
        } else {
            minecraft.setScreen(new UsageSelectScreen(this, cmd));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int old = scrollOffset;
        scrollOffset = (int) Math.clamp(scrollOffset - scrollY * (BUTTON_H + GAP), 0, maxScroll);
        if (old != scrollOffset) {
            rebuild();
        }
        return true;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(null);
    }
}
