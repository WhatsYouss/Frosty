package xyz.whatsyouss.frosty.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.whatsyouss.frosty.interfaces.ICameraOverriddenEntity;
import xyz.whatsyouss.frosty.modules.impl.render.FreeLook;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Unique
    boolean firstTime = true;

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V", ordinal = 1, shift = At.Shift.AFTER))
    public void lockRotation(BlockView focusedBlock, Entity cameraEntity, boolean isThirdPerson, boolean isFrontFacing, float tickDelta, CallbackInfo ci) {
        if (FreeLook.freelooking && cameraEntity instanceof ClientPlayerEntity) {
            ICameraOverriddenEntity cameraOverriddenEntity = (ICameraOverriddenEntity) cameraEntity;

            if (firstTime && MinecraftClient.getInstance().player != null) {
                cameraOverriddenEntity.frosty$setCameraPitch(MinecraftClient.getInstance().player.getPitch());
                cameraOverriddenEntity.frosty$setCameraYaw(MinecraftClient.getInstance().player.getYaw());
                firstTime = false;
            }
            this.setRotation(cameraOverriddenEntity.frosty$getCameraYaw(), cameraOverriddenEntity.frosty$getCameraPitch());

        }
        if (!FreeLook.freelooking && cameraEntity instanceof ClientPlayerEntity) {
            firstTime = true;
        }
    }

}
