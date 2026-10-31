package xyz.whatsyouss.frosty.mixin;

import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.whatsyouss.frosty.modules.ModuleManager;

@Mixin(BossHealthOverlay.class)
public class BossHealthOverlayMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void onRender(CallbackInfo ci) {
        if (ModuleManager.noHudElement.isEnabled() && ModuleManager.noHudElement.bossBar.isToggled()) {
            ci.cancel();
        }
    }
}
