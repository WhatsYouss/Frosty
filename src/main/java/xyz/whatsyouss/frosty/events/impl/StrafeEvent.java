package xyz.whatsyouss.frosty.events.impl;

import net.fabricmc.loader.impl.lib.sat4j.core.Vec;
import net.minecraft.util.math.Vec3d;

public class StrafeEvent {
    private Vec3d input;
    private float friction;
    private float yaw;

    public StrafeEvent(Vec3d input, float friction, float yaw) {
        this.input = input;
        this.friction = friction;
        this.yaw = yaw;
    }

    public Vec3d getInput() {
        return input;
    }

    public void setInput(Vec3d input) {
        this.input = input;
    }

    public float getFriction() {
        return this.friction;
    }

    public void setFriction(float friction) {
        this.friction = friction;
    }

    public float getYaw() {
        return this.yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

}
