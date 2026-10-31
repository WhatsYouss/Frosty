package xyz.whatsyouss.frosty.interfaces;

import net.minecraft.client.KeyMapping;

public interface IKeyMapping {

    public default boolean frosty$isActuallyDown()
    {
        return frosty$isActuallyDown();
    }

    public default void frosty$resetPressedState()
    {
        frosty$resetPressedState();
    }

    public default void frosty$simulatePress(boolean pressed)
    {
        frosty$simulatePress(pressed);
    }

    public default void setDown(boolean down)
    {
        asVanilla().setDown(down);
    }

    public default KeyMapping asVanilla()
    {
        return (KeyMapping)this;
    }

    public static IKeyMapping get(KeyMapping kb)
    {
        return (IKeyMapping)kb;
    }
}