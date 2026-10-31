package xyz.whatsyouss.frosty.mixin;

import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.whatsyouss.frosty.modules.ModuleManager;

@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {
    @Inject(method = "submitFire", at = @At("HEAD"), cancellable = true)
    private static void onRenderFireOverlay(PoseStack matrices, SubmitNodeCollector vertexConsumers, TextureAtlasSprite sprite, CallbackInfo ci) {
        if (ModuleManager.noOverlay.isEnabled() && ModuleManager.noOverlay.fire.isToggled())
            ci.cancel();
    }

    @Inject(method = "submitWater", at = @At("HEAD"), cancellable = true)
    private static void renderUnderwaterOverlayHook(PlayerRenderState.WaterOverlay overlay, PoseStack matrices, SubmitNodeCollector vertexConsumers, CallbackInfo ci) {
        if (ModuleManager.noOverlay.isEnabled() && ModuleManager.noOverlay.water.isToggled())
            ci.cancel();
    }

    @Inject(method = "submitBlockSprite", at = @At("HEAD"), cancellable = true)
    private static void renderInWallOverlayHook(Identifier sprite, float minX, float minY, float maxX, float maxY, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int color, CallbackInfo ci) {
        if (ModuleManager.noOverlay.isEnabled() && ModuleManager.noOverlay.inWall.isToggled())
            ci.cancel();
    }
}
