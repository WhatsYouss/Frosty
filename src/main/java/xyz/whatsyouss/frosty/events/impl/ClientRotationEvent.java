package xyz.whatsyouss.frosty.events.impl;

public class ClientRotationEvent {
    public Float yaw;
    public Float pitch;
    private float tYaw;
    private float tPitch;

    public ClientRotationEvent(Float yaw, Float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public float getYaw() {
        return this.tYaw;
    }

    public float getPitch() {
        return this.tPitch;
    }

    public void setYaw(Float yaw) {
        this.yaw = yaw;
    }

    public void setPitch(Float pitch) {
        this.pitch = pitch;
    }

    public void setRotations(Float yaw, Float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }
}
