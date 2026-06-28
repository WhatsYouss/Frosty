package xyz.whatsyouss.frosty.modules.impl.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.Utils;

public class Sprint extends Module {

    public ButtonSetting keep;
    public SliderSetting slow;

    public Sprint() {
        super("Sprint", "疾跑", category.Movement);

        this.registerSetting(keep = new ButtonSetting("Keep", false));
        this.registerSetting(slow = new SliderSetting("Slow", "%", 0, 0, 40, 1));

    }

    @Override
    public void guiUpdate() {
        this.slow.setVisibilityCondition(() -> keep.isToggled());
    }


    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck() && mc.gui.screen().isFocused()) {
            return;
        }
        if (mc.player.zza != 0) {
            if (!mc.options.toggleSprint().get()) {
                mc.options.keySprint.setDown(true);
            } else if (mc.options.toggleSprint().get() && !mc.player.isSprinting()) {
                mc.player.setSprinting(true);
            }
        }
    }
}
