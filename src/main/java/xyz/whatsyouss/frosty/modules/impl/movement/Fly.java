package xyz.whatsyouss.frosty.modules.impl.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.Vec3d;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.Utils;

public class Fly extends Module {

    private final SliderSetting hs; // 水平速度 (Horizontal speed)
    private final SliderSetting vs; // 垂直速度 (Vertical speed)

    public Fly() {
        super("Fly", category.Movement);

        this.registerSetting(hs = new SliderSetting("Horizontal speed", 1, 0.1, 15, 0.1));
        this.registerSetting(vs = new SliderSetting("Vertical speed", 1, 0.1, 15, 0.1));
    }

    @Override
    public void onDisable() {
        if (Utils.nullCheck()) {
            mc.player.setVelocity(0, 0, 0);
        }
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }

        double motionY = 0.0;

        if (mc.options.jumpKey.isPressed()) {
            motionY = vs.getInput();
        } else if (mc.options.sneakKey.isPressed()) {
            motionY = -vs.getInput();
        }

        double movementForward = mc.player.forwardSpeed;
        double movementSideways = mc.player.sidewaysSpeed;

        double motionX = 0.0;
        double motionZ = 0.0;

        if (movementForward != 0 || movementSideways != 0) {
            float yaw = mc.player.getYaw();
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

        mc.player.setVelocity(motionX, motionY, motionZ);
    }
}