package xyz.whatsyouss.frosty.modules.impl.render;

import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;

import java.util.HashSet;
import java.util.Set;

public class AntiTexture extends Module {

    public ButtonSetting skipDownload;

    public static final Set<String> whitelistedItems = new HashSet<>();
    public static boolean vanillaTooltip = true;

    public AntiTexture() {
        super("AntiTexture", "无纹理包", category.Render);

        this.registerSetting(skipDownload = new ButtonSetting("Skip download", "跳过下载", true));
    }
}