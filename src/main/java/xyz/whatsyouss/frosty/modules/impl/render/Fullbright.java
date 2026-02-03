package xyz.whatsyouss.frosty.modules.impl.render;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.option.GameOptions;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.Registries;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.events.impl.SettingUpdateEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.utility.Utils;

public class Fullbright extends Module {

    private SelectSetting mode;

    private String[] modes = new String[]{"Gamma", "Night Vision"};

    private int selectedMode;
    private double originalGamma;
    private boolean modeChanged;

    public Fullbright() {
        super("Fullbright", category.Render);

        this.registerSetting(mode = new SelectSetting("Mode", 0, modes));
    }

    @Override
    public void onEnable() {
        selectedMode = (int) mode.getValue();
        if (selectedMode == 0) {
            originalGamma = mc.options.getGamma().getValue();
            mc.options.getGamma().setValue(100d);
        } else {
            mc.player.addStatusEffect(new StatusEffectInstance(Registries.STATUS_EFFECT.getEntry(StatusEffects.NIGHT_VISION.value()), 999999, 0));
        }
    }

    @Override
    public void onDisable() {
        if (selectedMode == 0) {
            mc.options.getGamma().setValue(originalGamma);
        } else {
            mc.player.removeStatusEffect(Registries.STATUS_EFFECT.getEntry(StatusEffects.NIGHT_VISION.value()));
        }
    }
}
