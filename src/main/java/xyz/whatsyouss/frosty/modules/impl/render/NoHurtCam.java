package xyz.whatsyouss.frosty.modules.impl.render;

import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;

public class NoHurtCam extends Module {

    public SliderSetting multiplier;

    public NoHurtCam() {
        super("NoHurtCam", category.Render);

        this.registerSetting(multiplier = new SliderSetting("Multiplier", "x", 0, 0, 14, 1));
    }
}
