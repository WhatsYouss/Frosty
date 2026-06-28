package xyz.whatsyouss.frosty.modules.impl.render;

import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;

public class Fullbright extends Module {

    private SelectSetting mode;

    private String[] modes = new String[]{"Gamma"};

    public int selectedMode;

    public Fullbright() {
        super("Fullbright", "夜视", category.Render);

        this.registerSetting(mode = new SelectSetting("Mode", 0, modes));
    }

    @Override
    public void onEnable() {
        selectedMode = (int) mode.getValue();
    }
}
