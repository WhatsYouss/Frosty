package xyz.whatsyouss.frosty.modules.impl.render;

import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;

public class ArmorHider extends Module {

    public ButtonSetting head, selfOnly;

    public ArmorHider() {
        super("ArmorHider", "装备隐形", category.Render);

        this.registerSetting(head = new ButtonSetting("Head", true));
        this.registerSetting(selfOnly = new ButtonSetting("Self only", true));
    }
}