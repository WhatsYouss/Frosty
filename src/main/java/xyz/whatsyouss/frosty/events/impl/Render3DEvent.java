package xyz.whatsyouss.frosty.events.impl;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.Frustum;

public class Render3DEvent {
    private final MatrixStack matrix;
    private final float delta;

    public Render3DEvent(MatrixStack matrix, float delta) {
        this.matrix = matrix;
        this.delta = delta;
    }

    public MatrixStack getMatrix() {
        return matrix;
    }

    public float getDelta() {
        return delta;
    }
}