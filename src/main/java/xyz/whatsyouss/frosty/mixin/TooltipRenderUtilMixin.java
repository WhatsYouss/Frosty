package xyz.whatsyouss.frosty.mixin;

import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.whatsyouss.frosty.modules.impl.render.AntiTexture;

@Mixin(TooltipRenderUtil.class)
public class TooltipRenderUtilMixin {
    @Shadow @Final private static Identifier BACKGROUND_SPRITE;
    @Shadow @Final private static Identifier FRAME_SPRITE;

    @Inject(method = "getBackgroundSprite", at = @At("HEAD"), cancellable = true)
    private static void onGetBackgroundSprite(Identifier style, CallbackInfoReturnable<Identifier> cir) {
        if (!AntiTexture.vanillaTooltip || style == null) return;
        if (style.getNamespace().startsWith("hypixel_skyblock")) {
            cir.setReturnValue(BACKGROUND_SPRITE);
            cir.cancel();
        }
    }

    @Inject(method = "getFrameSprite", at = @At("HEAD"), cancellable = true)
    private static void onGetFrameSprite(Identifier style, CallbackInfoReturnable<Identifier> cir) {
        if (!AntiTexture.vanillaTooltip || style == null) return;
        if (style.getNamespace().startsWith("hypixel_skyblock")) {
            cir.setReturnValue(FRAME_SPRITE);
            cir.cancel();
        }
    }
}