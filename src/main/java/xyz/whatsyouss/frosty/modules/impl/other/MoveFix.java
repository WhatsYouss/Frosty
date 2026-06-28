package xyz.whatsyouss.frosty.modules.impl.other;

import meteordevelopment.orbit.EventHandler;
import xyz.whatsyouss.frosty.events.impl.PostSendMovementPacketsEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.utility.Rotations;

public class MoveFix extends Module {

    private final String[] modes = new String[]{"Strict", "Silent"};
    public SelectSetting mode;
    public ButtonSetting foragingOnly;

    public MoveFix() {
        super("MoveFix", "移动修正", category.Other);

        this.registerSetting(mode = new SelectSetting("Mode", 0, modes));
    }

    @Override
    public String getDesc() {
        return "Correct movement while silent rotating";
    }

    public static boolean shouldApply() {
        return ModuleManager.moveFix.isEnabled() && Rotations.rotating;
    }

    public static boolean isSilent() {
        return ModuleManager.moveFix != null
                && ModuleManager.moveFix.mode != null
                && ModuleManager.moveFix.mode.getValue() == 1;
    }

    @EventHandler
    public void onPostSendMovementPacket(PostSendMovementPacketsEvent event) {
        if (shouldApply()) {
        }
    }
}
