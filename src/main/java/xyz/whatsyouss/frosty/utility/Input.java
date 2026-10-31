package xyz.whatsyouss.frosty.utility;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import xyz.whatsyouss.frosty.events.impl.GuiKeyEvents;
import xyz.whatsyouss.frosty.mixin.accessor.KeyMappingAccessor;

public class Input {
    private static final boolean[] keys = new boolean[512];
    private static final boolean[] buttons = new boolean[16];

    private Input() {
    }

    public static void setKeyState(int key, boolean pressed) {
        if (key >= 0 && key < keys.length) keys[key] = pressed;
    }

    public static void setButtonState(int button, boolean pressed) {
        if (button >= 0 && button < buttons.length) buttons[button] = pressed;
    }

    public static int getKey(KeyMapping bind) {
        return ((KeyMappingAccessor) bind).getKey().getValue();
    }

    public static KeyMapping getKeyBindingFromCode(int keyCode) {
        for (KeyMapping keyBinding : Minecraft.getInstance().options.keyMappings) {
            if (keyBinding.getDefaultKey().getValue() == keyCode) {
                return keyBinding;
            }
        }
        return null;
    }

    public static void setKeyState(KeyMapping bind, boolean pressed) {
        setKeyState(getKey(bind), pressed);
    }

    public static boolean isPressed(KeyMapping bind) {
        return isKeyPressed(getKey(bind));
    }

    public static boolean isKeyPressed(int key) {
        if (!GuiKeyEvents.canUseKeys) return false;

        if (key < 0) return false;
        return key < keys.length && keys[key];
    }

    public static boolean isButtonPressed(int button) {
        if (button < 0) return false;
        return button < buttons.length && buttons[button];
    }

    public static int getModifier(int key) {
        return switch (key) {
            case InputConstants.KEY_LSHIFT, InputConstants.KEY_RSHIFT -> InputConstants.MOD_SHIFT;
            case InputConstants.KEY_LCONTROL, InputConstants.KEY_RCONTROL -> InputConstants.MOD_CONTROL;
            case InputConstants.KEY_LALT, InputConstants.KEY_RALT -> InputConstants.MOD_ALT;
            case InputConstants.KEY_LGUI, InputConstants.KEY_RGUI -> InputConstants.MOD_SUPER;
            default -> 0;
        };
    }

    public static boolean isKeyDown(int keyCode) {
        return InputConstants.isKeyDown(keyCode);
    }
}
