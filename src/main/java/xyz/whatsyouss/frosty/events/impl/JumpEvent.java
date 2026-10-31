package xyz.whatsyouss.frosty.events.impl;

import xyz.whatsyouss.frosty.events.Cancellable;

public class JumpEvent extends Cancellable {
    private float jumpVelocity;
    private float yaw;
    private boolean applySprint;

    public JumpEvent(float jumpVelocity, float yaw, boolean applySprint) {
        this.jumpVelocity = jumpVelocity;
        this.yaw = yaw;
        this.applySprint = applySprint;
    }

    public float getJumpVelocity() {
        return this.jumpVelocity;
    }

    public void setJumpVelocity(float jumpVelocity) {
        this.jumpVelocity = jumpVelocity;
    }

    public float getYaw() {
        return this.yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public boolean applySprint() {
        return this.applySprint;
    }

    public void setSprint(boolean applySprint) {
        this.applySprint = applySprint;
    }
}
