package xyz.whatsyouss.frosty.events.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.culling.Frustum;

public class Render3DEvent {
    private final PoseStack matrix;
    private final float delta;
    public double offsetX, offsetY, offsetZ;

    public Render3DEvent(PoseStack matrix, float delta, double offsetX, double offsetY, double offsetZ) {
        this.matrix = matrix;
        this.delta = delta;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    public PoseStack getMatrix() {
        return matrix;
    }

    public float getDelta() {
        return delta;
    }

    public double getOffsetX() {
        return offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public double getOffsetZ() {
        return offsetZ;
    }
}