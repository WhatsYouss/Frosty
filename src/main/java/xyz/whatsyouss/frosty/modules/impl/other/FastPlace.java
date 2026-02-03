package xyz.whatsyouss.frosty.modules.impl.other;

import meteordevelopment.orbit.EventHandler;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.mixin.accessor.MinecraftClientAccessor;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;

public class FastPlace extends Module {

    private SliderSetting delay;

    public FastPlace() {
        super("FastPlace", category.Other);

        this.registerSetting(delay = new SliderSetting("Delay", 1, 0, 3, 1));
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (((MinecraftClientAccessor)mc).frosty$getItemUseCooldown() > delay.getInput()) {
            ((MinecraftClientAccessor)mc).frosty$setItemUseCooldown((int) delay.getInput());
        }
    }
}
