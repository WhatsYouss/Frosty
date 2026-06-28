package xyz.whatsyouss.frosty.modules.impl.client;

import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.InputSetting;

public class Title extends Module {

    public ButtonSetting keepOriginal;

    public Title() {
        super("Title", "游戏标题", category.Client);

        this.registerSetting(keepOriginal = new ButtonSetting("Keep original", true));
    }
}
