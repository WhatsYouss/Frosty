package xyz.whatsyouss.frosty.modules.impl.render;

import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;

public class AntiDebuff extends Module {

    public ButtonSetting nausea;

    public AntiDebuff() {
        super("AntiDebuff", "防负面效果", category.Render);

        this.registerSetting(nausea = new ButtonSetting("Nausea", true));
    }
}
