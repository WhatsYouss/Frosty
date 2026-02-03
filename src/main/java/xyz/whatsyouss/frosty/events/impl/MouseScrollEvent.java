package xyz.whatsyouss.frosty.events.impl;

import xyz.whatsyouss.frosty.events.Cancellable;

public class MouseScrollEvent extends Cancellable {
    private static final MouseScrollEvent INSTANCE = new MouseScrollEvent();
    private static double currentScrollDelta = 0;

    public double value;

    public static MouseScrollEvent get(double value) {
        INSTANCE.setCancelled(false);
        INSTANCE.value = value;
        return INSTANCE;
    }

    public static double getCurrentScrollDelta() {
        return currentScrollDelta;
    }

    public static void resetScrollDelta() {
        currentScrollDelta = 0;
    }
}