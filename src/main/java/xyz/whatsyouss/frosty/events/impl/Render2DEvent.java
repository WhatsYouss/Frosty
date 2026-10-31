package xyz.whatsyouss.frosty.events.impl;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public class Render2DEvent {
    private static final Render2DEvent INSTANCE = new Render2DEvent();

    public GuiGraphicsExtractor drawContext;
    public int screenWidth, screenHeight;
    public double frameTime;
    public float tickDelta;

    public static Render2DEvent get(GuiGraphicsExtractor drawContext, int screenWidth, int screenHeight, float tickDelta) {
        INSTANCE.drawContext = drawContext;
        INSTANCE.screenWidth = screenWidth;
        INSTANCE.screenHeight = screenHeight;
        INSTANCE.tickDelta = tickDelta;
        return INSTANCE;
    }
}