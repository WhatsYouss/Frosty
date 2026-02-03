package xyz.whatsyouss.frosty.settings.impl;

import net.minecraft.client.util.InputUtil;
import xyz.whatsyouss.frosty.modules.Module;

public class KeyBindSetting extends ButtonSetting {
    private final Module module;

    public KeyBindSetting(Module module) {
        super("KeyBind", false);
        this.module = module;
    }

    public String getKeyText() {
        int keycode = module.getKeycode();
        if (keycode == 0) return "None";
        if (keycode >= 1000) {
            if (keycode == 1069) return "Scroll Up";
            if (keycode == 1070) return "Scroll Down";
            return "Mouse " + (keycode - 1000);
        }

        if (keycode < 0 || keycode > 348) {
            return "Invalid Key";
        }

        try {
            InputUtil.Key key = InputUtil.Type.KEYSYM.createFromCode(keycode);
            return key.getLocalizedText().getString();
        } catch (Exception e) {
            return "Error";
        }
    }

    public Module getModule() {
        return module;
    }
}