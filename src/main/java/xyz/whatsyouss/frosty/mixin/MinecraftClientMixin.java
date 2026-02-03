package xyz.whatsyouss.frosty.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.whatsyouss.frosty.modules.ModuleManager;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Inject(method = "getWindowTitle", at = @At("RETURN"), cancellable = true)
    private void onGetWindowTitle(CallbackInfoReturnable<String> cir) {
        if (ModuleManager.title.isEnabled()) {
            String original = ModuleManager.title.keepOriginal.isToggled() ? cir.getReturnValue() + " " : "";
            cir.setReturnValue(original + "Frosty");
        }
    }
}

