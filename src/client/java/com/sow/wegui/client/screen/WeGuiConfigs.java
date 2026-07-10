package com.sow.wegui.client.screen;

import com.google.common.collect.ImmutableList;
import com.sow.wegui.WeGuiMod;
import com.sow.wegui.config.Configs;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.IConfigGuiAllTab;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

public class WeGuiConfigs extends GuiConfigsBase implements IConfigGuiAllTab {
    public WeGuiConfigs() {
        super(10, 50, WeGuiMod.MOD_ID, null, "wegui.title.configs");
    }

    @Override
    public void initGui() {
        super.initGui();
        this.clearOptions();

        int x = 10;
        int y = 26;
        for (ConfigGuiTab tab : ConfigGuiTab.values()) {
            if (!this.useAllTab() && tab == ConfigGuiTab.ALL) continue;
            x += this.createButton(x, y, -1, tab);
        }

        ButtonGeneric panel = new ButtonGeneric(this.width - 90, y, 80, 20, StringUtils.translate("wegui.command.open_panel"));
        this.addButton(panel, (btn, mouseButton) -> this.mc.setScreen(new WeCommandScreen()));
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        List<? extends IConfigBase> configs;
        ConfigGuiTab tab = getActiveTab();

        if (tab == ConfigGuiTab.ALL && this.useAllTab()) {
            return this.getAllConfigs();
        } else if (tab == ConfigGuiTab.GENERIC) {
            configs = Configs.Generic.OPTIONS;
        } else if (tab == ConfigGuiTab.STATUS_BAR) {
            configs = Configs.StatusBar.OPTIONS;
        } else if (tab == ConfigGuiTab.PASTE_PREVIEW) {
            configs = Configs.PastePreview.OPTIONS;
        } else if (tab == ConfigGuiTab.MODE_INDICATOR) {
            configs = Configs.ModeIndicator.OPTIONS;
        } else if (tab == ConfigGuiTab.HOTKEYS) {
            configs = Configs.Hotkeys.OPTIONS;
        } else {
            configs = ImmutableList.of();
        }

        return ConfigOptionWrapper.createFor(configs);
    }

    @Override
    public boolean useAllTab() {
        return true;
    }

    @Override
    public List<ConfigOptionWrapper> getAllConfigs() {
        List<ConfigOptionWrapper> configs = new ArrayList<>();
        configs.addAll(ConfigOptionWrapper.createFor(Configs.Generic.OPTIONS));
        configs.addAll(ConfigOptionWrapper.createFor(Configs.StatusBar.OPTIONS));
        configs.addAll(ConfigOptionWrapper.createFor(Configs.PastePreview.OPTIONS));
        configs.addAll(ConfigOptionWrapper.createFor(Configs.ModeIndicator.OPTIONS));
        configs.addAll(ConfigOptionWrapper.createFor(Configs.Hotkeys.OPTIONS));
        return configs;
    }

    @Override
    protected int getConfigWidth() {
        ConfigGuiTab tab = getActiveTab();
        if (tab == ConfigGuiTab.STATUS_BAR || tab == ConfigGuiTab.PASTE_PREVIEW) {
            return 140;
        }
        return super.getConfigWidth();
    }

    @Override
    protected void onSettingsChanged() {
        super.onSettingsChanged();
        Configs.INSTANCE.save();
    }

    private int createButton(int x, int y, int width, ConfigGuiTab tab) {
        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, tab.getDisplayName());
        button.setEnabled(getActiveTab() != tab);
        this.addButton(button, new ButtonListener(tab, this));
        return button.getWidth() + 2;
    }

    private ConfigGuiTab getActiveTab() {
        String name = Configs.Internal.LAST_CONFIG_CATEGORY.getStringValue();
        try {
            return ConfigGuiTab.valueOf(name);
        } catch (IllegalArgumentException e) {
            return ConfigGuiTab.GENERIC;
        }
    }

    private void setActiveTab(ConfigGuiTab tab) {
        Configs.Internal.LAST_CONFIG_CATEGORY.setValueFromString(tab.name());
        this.reCreateListWidget();
        if (this.getListWidget() != null) {
            this.getListWidget().resetScrollbarPosition();
        }
        this.initGui();
    }

    private record ButtonListener(ConfigGuiTab tab, WeGuiConfigs parent) implements IButtonActionListener {
        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton) {
            parent.setActiveTab(tab);
        }
    }

    public enum ConfigGuiTab {
        ALL(IConfigGuiAllTab.getTranslationKey()),
        GENERIC("wegui.config.tab.generic"),
        STATUS_BAR("wegui.config.tab.status_bar"),
        PASTE_PREVIEW("wegui.config.tab.paste_preview"),
        MODE_INDICATOR("wegui.config.tab.mode_indicator"),
        HOTKEYS("wegui.config.tab.hotkeys");

        private final String translationKey;

        ConfigGuiTab(String translationKey) {
            this.translationKey = translationKey;
        }

        public String getDisplayName() {
            return StringUtils.translate(this.translationKey);
        }
    }
}
