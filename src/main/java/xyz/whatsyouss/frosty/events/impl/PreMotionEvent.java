package xyz.whatsyouss.frosty.events.impl;

import net.minecraft.util.math.Vec3d;
import xyz.whatsyouss.frosty.events.Cancellable;

public class PreMotionEvent extends Cancellable {
    private double posX;
    public double posY;
    private double posZ;
    private float yaw;
    private float pitch;
    private boolean onGround;
    private static boolean setRenderYaw;
    private boolean isSprinting;
    private boolean isSneaking;
    public static boolean setRotations;
    public static float preMotionYaw;

    public PreMotionEvent(double posX, double posY, double posZ, float yaw, float pitch, boolean onGround, boolean isSprinting, boolean isSneaking) {
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
        this.isSprinting = isSprinting;
        this.isSneaking = isSneaking;
    }

    public double getPosX() {
        return posX;
    }

    public double getPosY() {
        return posY;
    }

    public double getPosZ() {
        return posZ;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public boolean isOnGround() {
        return onGround;
    }

    public void setPosX(double posX) {
        this.posX = posX;
    }

    public void setPosY(double posY) {
        this.posY = posY;
    }

    public void setPosZ(double posZ) {
        this.posZ = posZ;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
        this.setRenderYaw = true;
        setRotations = true;
        preMotionYaw = yaw;
    }

    public void setRotations(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.setRenderYaw = true;
        setRotations = true;
        preMotionYaw = yaw;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
        setRotations = true;
    }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }

    public static boolean setRenderYaw() {
        return setRenderYaw;
    }

    public static void setRenderYaw(boolean setYaw) {
        setRenderYaw = setYaw;
    }

    public boolean isSprinting() {
        return isSprinting;
    }

    public void setSprinting(boolean sprinting) {
        this.isSprinting = sprinting;
    }

    public boolean isSneaking() {
        return isSneaking;
    }

    public void setSneaking(boolean sneaking) {
        this.isSneaking = sneaking;
    }

    public Vec3d getPos() {
        return new Vec3d(posX, posY, posZ);
    }
}
