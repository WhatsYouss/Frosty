package xyz.whatsyouss.frosty.mixin;

import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.whatsyouss.frosty.modules.ModuleManager;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Inject(method = "extractTransparentBackground", at = @At("HEAD"), cancellable = true)
    private void onExtractTransparentBackground(CallbackInfo ci) {
        if (ModuleManager.noBlur.isEnabled()) {
            ci.cancel();
        }
    }
}