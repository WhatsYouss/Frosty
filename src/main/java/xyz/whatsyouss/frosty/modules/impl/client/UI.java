package xyz.whatsyouss.frosty.modules.impl.client;

import com.mojang.blaze3d.platform.InputConstants;
import xyz.whatsyouss.frosty.gui.ClickGui;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.utility.Utils;

public class UI extends Module {

    private String[] langs = new String[]{"English", "简体中文"};
    private String[] clickGuiColors = new String[] {"Light", "Dark"};
    private String[] CNclickGuiColors = new String[] {"浅色", "深色"};
    private String[] liquidGlassModes = new String[] {"Clear", "Dense"};
    private String[] CNliquidGlassModes = new String[] {"透明", "实色"};

    public static ButtonSetting liquidGlass;
    public static SelectSetting lang, clickGuiColor, liquidGlassMode;

    public UI() {
        super("ClickGui","用户界面", category.Client, InputConstants.KEY_RSHIFT);

        this.registerSetting(lang = new SelectSetting("Language", "语言", 0, langs, langs));
        this.registerSetting(clickGuiColor = new SelectSetting("Background Color", "背景颜色", 0, clickGuiColors, CNclickGuiColors));
        this.registerSetting(liquidGlass = new ButtonSetting("Liquid Glass", "液态玻璃", true));
        this.registerSetting(liquidGlassMode = new SelectSetting("Liquid Glass Mode", "液态玻璃效果", 0, liquidGlassModes, CNliquidGlassModes));
    }

    @Override
    public void guiUpdate() {
        this.liquidGlassMode.setVisibilityCondition(() -> liquidGlass.isToggled());
    }

    @Override
    public String getDesc() {
        if (UI.lang.getValue() == 1) {
            return "这个模块包含全局设置";
        }
        return "This module is a global setting";
    }

    public void onEnable() {
        if (!Utils.nullCheck()) {
            return;
        }
        mc.setScreen(new ClickGui());
    }

    public void onDisable() {
        if (!Utils.nullCheck()) {
            return;
        }
        if (mc.screen instanceof ClickGui) {
            mc.setScreen(null);
        }
    }

    @Override
    public void onUpdate() {
        if (!(mc.screen instanceof ClickGui)) {
            this.disable();
        }
    }
}
