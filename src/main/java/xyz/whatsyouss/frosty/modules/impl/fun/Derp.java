package xyz.whatsyouss.frosty.modules.impl.fun;

import meteordevelopment.orbit.EventHandler;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.Rotations;
import xyz.whatsyouss.frosty.utility.Utils;

public class Derp extends Module {

    private final String[] dir = new String[]{"Left", "Right", "Switch"};

    private final SelectSetting direction;
    private final SliderSetting speed;

    private float targetYaw = 0.0f;

    private boolean isTurningLeft = true;

    private float accumulatedDegrees = 0.0f;

    public Derp() {
        super("Derp", "自身旋转", category.Fun);

        this.registerSetting(speed = new SliderSetting("Speed", 3, 1, 7, 1));
        this.registerSetting(direction = new SelectSetting("Direction", 2, dir));
    }

    @Override
    public void onEnable() {
        if (Utils.nullCheck()) {
            this.targetYaw = mc.player.getYRot();
        }
        this.isTurningLeft = true;
        this.accumulatedDegrees = 0.0f;
    }

    @Override
    public void onDisable() {
        Rotations.cancelRotate(this);
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck()) return;

        float step = (float) speed.getInput() * 5.0f;
        if (step <= 0) return;

        int mode = (int) direction.getValue();

        switch (mode) {
            case 0:
                targetYaw -= step;
                break;

            case 1:
                targetYaw += step;
                break;

            case 2:
                if (isTurningLeft) {
                    targetYaw -= step;
                } else {
                    targetYaw += step;
                }

                accumulatedDegrees += step;

                if (accumulatedDegrees >= 360.0f) {
                    isTurningLeft = !isTurningLeft;
                    accumulatedDegrees = 0.0f;
                }
                break;
        }

        targetYaw %= 360.0F;

        float smoothness = 1.0f;
        Rotations.setRotate(this, targetYaw, mc.player.getXRot(), 10, smoothness);
    }
}
