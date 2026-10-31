package xyz.whatsyouss.frosty.utility.pathfinding;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class PestFlightMotion {
    public static final double BRAKING_LOOKAHEAD_TICKS = 2.0;
    private static final double HORIZONTAL_DRAG = 0.91;

    private PestFlightMotion() {}

    public static double coastTicks() {
        return 1.0 / (1.0 - HORIZONTAL_DRAG) + BRAKING_LOOKAHEAD_TICKS;
    }

    public static Vec3 approachVelocity(Vec3 offset, Vec3 targetVelocity, double distance, double maxSpeed) {
        double horizontal = offset.horizontalDistance();
        double speed = Math.min(maxSpeed, Math.max(0.0, horizontal - distance) / coastTicks());
        Vec3 closing = horizontal > 1.0e-6
                ? new Vec3(offset.x * speed / horizontal, 0.0, offset.z * speed / horizontal)
                : Vec3.ZERO;
        Vec3 desired = closing.add(targetVelocity.x, 0.0, targetVelocity.z);
        double length = desired.horizontalDistance();
        return length > maxSpeed ? desired.scale(maxSpeed / length) : desired;
    }

    public static void steer(Minecraft client, Vec3 desired, Vec3 velocity, float yaw) {
        double angle = Math.toRadians(yaw);
        double sin = Math.sin(angle), cos = Math.cos(angle);
        int forward = axis(-desired.x * sin + desired.z * cos, -velocity.x * sin + velocity.z * cos);
        int right = axis(-desired.x * cos - desired.z * sin, -velocity.x * cos - velocity.z * sin);
        client.options.keyUp.setDown(forward > 0);
        client.options.keyDown.setDown(forward < 0);
        client.options.keyRight.setDown(right > 0);
        client.options.keyLeft.setDown(right < 0);
    }

    private static int axis(double desired, double current) {
        double error = desired - current;
        double deadband = desired * error <= 0.0 ? 0.18 : 0.04;
        return Math.abs(error) <= deadband ? 0 : error > 0 ? 1 : -1;
    }

    public static int verticalInput(double heightError, double velocity) {
        double projected = heightError - velocity / (1.0 - 0.6);
        if (heightError > 0.5 && projected > 0.5) return 1;
        if (heightError < -0.5 && projected < -0.5) return -1;
        return 0;
    }
}
