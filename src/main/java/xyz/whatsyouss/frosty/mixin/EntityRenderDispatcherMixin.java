package xyz.whatsyouss.frosty.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.whatsyouss.frosty.interfaces.IEntityRenderState;
import xyz.whatsyouss.frosty.utility.EntityUtils;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Shadow
    public Camera camera;

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private <S extends EntityRenderState> void render(S renderState, CameraRenderState cameraRenderState, double d, double e, double f, PoseStack matrixStack, SubmitNodeCollector orderedRenderCommandQueue, CallbackInfo info) {
        var entity = ((IEntityRenderState) renderState).frosty$getEntity();

        if (entity instanceof EntityUtils player && player.hideWhenInsideCamera) {
            int cX = Mth.floor(this.camera.position().x);
            int cY = Mth.floor(this.camera.position().y);
            int cZ = Mth.floor(this.camera.position().z);

            if (cX == entity.getBlockX() && cZ == entity.getBlockZ() && (cY == entity.getBlockY() || cY == entity.getBlockY() + 1)) info.cancel();
        }
    }

    @ModifyExpressionValue(
            method = "extractEntity(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;")
    )
    private <E extends Entity> EntityRenderState getAndUpdateRenderState$setEntity(EntityRenderState state, E entity, float tickProgress) {
        ((IEntityRenderState) state).frosty$setEntity(entity);
        return state;
    }

    @Inject(method = "distanceToSqr(Lnet/minecraft/world/entity/Entity;)D", at = @At("HEAD"), cancellable = true)
    private void onGetSquaredDistanceToCameraEntity(Entity entity, CallbackInfoReturnable<Double> info) {
        if (camera == null) info.setReturnValue(0.0);
    }
}
