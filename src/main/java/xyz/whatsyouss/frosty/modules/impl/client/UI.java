package xyz.whatsyouss.frosty.modules.impl.client;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.InputUtil;
import xyz.whatsyouss.frosty.gui.ClickGui;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.InputSetting;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.Utils;

public class UI extends Module {

    public SelectSetting clickGuiColor;
    private String[] clickGuiColors = new String[] {"Light", "Dark"};

    public UI() {
        super("ClickGui", category.Client, InputUtil.GLFW_KEY_RIGHT_SHIFT);

        this.registerSetting(clickGuiColor = new SelectSetting("Background Color", 0, clickGuiColors));
    }

    @Override
    public String getDesc() {
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
        if (mc.currentScreen instanceof ClickGui) {
            mc.setScreen(null);
        }
    }

    @Override
    public void onUpdate() {
        if (!(mc.currentScreen instanceof ClickGui)) {
            this.disable();
        }
    }
}
