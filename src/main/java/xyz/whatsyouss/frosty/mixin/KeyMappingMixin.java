package xyz.whatsyouss.frosty.mixin;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonInfo;
import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.interfaces.IKeyMapping;

import static xyz.whatsyouss.frosty.Frosty.mc;

@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin implements IKeyMapping
{
    @Shadow
    private InputConstants.Key key;

    @Override
    @Unique
    @Deprecated
    public boolean frosty$isActuallyDown()
    {
        Window window = mc.getWindow();
        int code = key.getValue();

        if(key.getType() == InputConstants.Type.MOUSE)
            return GLFW.glfwGetMouseButton(window.handle(), code) == 1;

        return InputConstants.isKeyDown(window, code);
    }

    @Override
    @Unique
    @Deprecated
    public void frosty$resetPressedState()
    {
        setDown(frosty$isActuallyDown());
    }

    @Override
    @Unique
    @Deprecated
    public void frosty$simulatePress(boolean pressed)
    {
        Minecraft mc = Frosty.mc;
        Window window = mc.getWindow();
        int action = pressed ? 1 : 0;

        switch(key.getType())
        {
            case KEYSYM:
                mc.keyboardHandler.keyPress(window.handle(), action,
                        new KeyEvent(key.getValue(), 0, 0));
                break;

            case SCANCODE:
                mc.keyboardHandler.keyPress(window.handle(), action,
                        new KeyEvent(GLFW.GLFW_KEY_UNKNOWN, key.getValue(), 0));
                break;

            case MOUSE:
                mc.mouseHandler.onButton(window.handle(),
                        new MouseButtonInfo(key.getValue(), 0), action);
                break;

            default:
                System.out.println("Unknown key mapping type: " + key.getType());
                break;
        }
    }

    @Override
    @Shadow
    public abstract void setDown(boolean pressed);
}