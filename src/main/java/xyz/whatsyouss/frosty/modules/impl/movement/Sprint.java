package xyz.whatsyouss.frosty.modules.impl.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.Utils;

public class Sprint extends Module {

    public ButtonSetting keep;
    public SliderSetting slow;

    public Sprint() {
        super("Sprint", category.Movement);

        this.registerSetting(keep = new ButtonSetting("Keep", false));
        this.registerSetting(slow = new SliderSetting("Slow", "%", 0, 0, 40, 1));

    }

    @Override
    public void guiUpdate() {
        this.slow.setVisibilityCondition(() -> keep.isToggled());
    }


    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck() && mc.isWindowFocused()) {
            return;
        }
        if (mc.player.forwardSpeed != 0) {
            if (!mc.options.getSprintToggled().getValue()) {
                mc.options.sprintKey.setPressed(true);
            } else if (mc.options.getSprintToggled().getValue() && !mc.player.isSprinting()) {
                mc.player.setSprinting(true);
            }
        }
    }
}
