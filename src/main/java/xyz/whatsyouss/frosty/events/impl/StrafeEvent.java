package xyz.whatsyouss.frosty.events.impl;

import net.fabricmc.loader.impl.lib.sat4j.core.Vec;
import net.minecraft.world.phys.Vec3;

public class StrafeEvent {
    private Vec3 input;
    private float friction;
    private float yaw;

    public StrafeEvent(Vec3 input, float friction, float yaw) {
        this.input = input;
        this.friction = friction;
        this.yaw = yaw;
    }

    public Vec3 getInput() {
        return input;
    }

    public void setInput(Vec3 input) {
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
