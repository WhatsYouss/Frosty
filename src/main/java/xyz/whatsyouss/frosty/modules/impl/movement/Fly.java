package xyz.whatsyouss.frosty.modules.impl.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.Utils;

public class Fly extends Module {

    private final SliderSetting hs;
    private final SliderSetting vs;

    public Fly() {
        super("Fly", "飞行", category.Movement);

        this.registerSetting(hs = new SliderSetting("Horizontal speed", 1, 0.1, 15, 0.1));
        this.registerSetting(vs = new SliderSetting("Vertical speed", 1, 0.1, 15, 0.1));
    }

    @Override
    public void onDisable() {
        if (Utils.nullCheck()) {
            mc.player.setDeltaMovement(0, 0, 0);
        }
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }

        double motionY = 0.0;

        if (mc.options.keyJump.isDown()) {
            motionY = vs.getInput();
        } else if (mc.options.keyShift.isDown()) {
            motionY = -vs.getInput();
        }

        double movementForward = mc.player.zza;
        double movementSideways = mc.player.xxa;

        double motionX = 0.0;
        double motionZ = 0.0;

        if (movementForward != 0 || movementSideways != 0) {
            float yaw = mc.player.getYRot();
            double rad = Math.toRadians(yaw);

            double cos = Math.cos(rad);
            double sin = Math.sin(rad);

            double forwardX = -sin * movementForward;
            double forwardZ = cos * movementForward;
            double sideX = cos * movementSideways;
            double sideZ = sin * movementSideways;

            double dirX = forwardX + sideX;
            double dirZ = forwardZ + sideZ;

            double length = Math.sqrt(dirX * dirX + dirZ * dirZ);
            if (length > 0) {
                dirX /= length;
                dirZ /= length;
            }

            double speed = hs.getInput();
            motionX = dirX * speed;
            motionZ = dirZ * speed;
        }

        mc.player.setDeltaMovement(motionX, motionY, motionZ);
    }
}