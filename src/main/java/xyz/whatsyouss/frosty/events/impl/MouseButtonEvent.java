package xyz.whatsyouss.frosty.events.impl;

import xyz.whatsyouss.frosty.events.Cancellable;
import xyz.whatsyouss.frosty.utility.KeyAction;

public class MouseButtonEvent extends Cancellable {
    private static final MouseButtonEvent INSTANCE = new MouseButtonEvent();

    public int button;
    public KeyAction action;
    public double mouseX;
    public double mouseY;

    public static MouseButtonEvent get(int button, KeyAction action, double mouseX, double mouseY) {
        INSTANCE.setCancelled(false);
        INSTANCE.button = button;
        INSTANCE.action = action;
        INSTANCE.mouseX = mouseX;
        INSTANCE.mouseY = mouseY;
        return INSTANCE;
    }
}