package xyz.whatsyouss.frosty.mixin;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.events.impl.MouseButtonEvent;
import xyz.whatsyouss.frosty.events.impl.MouseScrollEvent;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.utility.Input;
import xyz.whatsyouss.frosty.utility.KeyAction;

import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Shadow
    public abstract double getScaledXPos(Window window);
    @Shadow
    public abstract double getScaledYPos(Window window);
    @Shadow @Final
    private Minecraft minecraft;

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long handle, MouseButtonInfo input, int action, CallbackInfo ci) {
        int button = input.button();
        Input.setButtonState(button, action != GLFW_RELEASE);

        var window = this.minecraft.getWindow();
        double xm = this.getScaledXPos(window);
        double ym = this.getScaledYPos(window);

        if (Frosty.EVENT_BUS.post(MouseButtonEvent.get(button, KeyAction.get(action), xm, ym)).isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (Frosty.EVENT_BUS.post(MouseScrollEvent.get(vertical)).isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "grabMouse", at = @At("HEAD"), cancellable = true)
    private void onLockCursor(CallbackInfo ci) {
        if ((ModuleManager.ungrabMouse != null && ModuleManager.ungrabMouse.isEnabled())
                || (ModuleManager.farmingMacro != null && ModuleManager.farmingMacro.isControllingMouse())) {
            ci.cancel();
        }
    }

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void onUpdateMouse(CallbackInfo ci) {
        if ((ModuleManager.ungrabMouse != null && ModuleManager.ungrabMouse.isEnabled())
                || (ModuleManager.farmingMacro != null && ModuleManager.farmingMacro.isControllingMouse())) {
            ci.cancel();
        }
    }
}
