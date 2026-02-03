package xyz.whatsyouss.frosty.modules.impl.render;

import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;

public class NoOverlay extends Module {

    public ButtonSetting fire, water, inWall;

    public NoOverlay() {
        super("NoOverlay", category.Render);

        this.registerSetting(fire = new ButtonSetting("Fire", true));
        this.registerSetting(water = new ButtonSetting("Water", true));
        this.registerSetting(inWall = new ButtonSetting("In Wall", true));
    }
}
