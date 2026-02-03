package xyz.whatsyouss.frosty.modules.impl.render;

import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;

public class NoHudElement extends Module {

    public ButtonSetting scoreboard, bossBar, title;

    public NoHudElement() {
        super("NoHudElement", category.Render);

        this.registerSetting(scoreboard = new ButtonSetting("Scoreboard", true));
        this.registerSetting(bossBar = new ButtonSetting("Boss bar", true));
        this.registerSetting(title = new ButtonSetting("Title", true));
    }
}
