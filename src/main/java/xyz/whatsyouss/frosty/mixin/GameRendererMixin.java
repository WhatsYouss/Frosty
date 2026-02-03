package xyz.whatsyouss.frosty.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.events.impl.RenderAfterWorldEvent;
import xyz.whatsyouss.frosty.interfaces.IVec3d;
import xyz.whatsyouss.frosty.modules.ModuleManager;

@Mixin(value = GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    public abstract void updateCrosshairTarget(float tickDelta);

    @Shadow
    public abstract void reset();

    @Shadow
    @Final
    private Camera camera;

    @Redirect(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerp(FFF)F"))
    private float renderWorldHook(float delta, float first, float second) {
        if (ModuleManager.antiDebuff.isEnabled() && ModuleManager.antiDebuff.nausea.isToggled()) {
            return 0.0f;
        }
        return MathHelper.lerp(delta, first, second);
    }

    @Redirect(method = "tiltViewWhenHurt", at = @At(value = "INVOKE", target = "Ljava/lang/Double;doubleValue()D"))
    private double modifyTiltStrength(Double original) {
        if (ModuleManager.noHurtCam.isEnabled()) {
            return original * ModuleManager.noHurtCam.multiplier.getInput();
        }
        return original;
    }

    @Inject(method = "renderWorld", at = @At("TAIL"))
    private void onRenderWorldTail(CallbackInfo info) {
        Frosty.EVENT_BUS.post(RenderAfterWorldEvent.get());
    }
}
