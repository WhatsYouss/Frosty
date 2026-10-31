package xyz.whatsyouss.frosty.modules.impl.render;

import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.impl.client.UI;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;

public class NoHurtCam extends Module {

    public SliderSetting multiplier;

    public NoHurtCam() {
        super("NoHurtCam", "无受伤抖动", category.Render);

        this.registerSetting(multiplier = new SliderSetting("Multiplier", "x", 0, 0, 14, 1, "倍率"));
    }

    @Override
    public String getDesc() {
        if (UI.lang.getValue() == 1) {
            return "初始值为 14x";
        }
        return "Original is 14x";
    }
}
