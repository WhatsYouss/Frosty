package xyz.whatsyouss.frosty.mixin;

import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.events.impl.MouseButtonEvent;
import xyz.whatsyouss.frosty.events.impl.MouseScrollEvent;
import xyz.whatsyouss.frosty.utility.Input;
import xyz.whatsyouss.frosty.utility.KeyAction;

import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, MouseInput input, int action, CallbackInfo ci) {
        int button = input.button();
        Input.setButtonState(button, action != GLFW_RELEASE);

        if (Frosty.EVENT_BUS.post(MouseButtonEvent.get(button, KeyAction.get(action))).isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (Frosty.EVENT_BUS.post(MouseScrollEvent.get(vertical)).isCancelled()) {
            ci.cancel();
        }
    }
}