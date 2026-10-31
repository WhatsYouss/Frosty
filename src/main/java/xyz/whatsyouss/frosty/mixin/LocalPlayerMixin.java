package xyz.whatsyouss.frosty.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.events.impl.*;
import xyz.whatsyouss.frosty.interfaces.ICameraOverriddenEntity;
import xyz.whatsyouss.frosty.modules.impl.render.FreeLook;
import xyz.whatsyouss.frosty.utility.DirectionalInput;
import xyz.whatsyouss.frosty.utility.Rotations;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends net.minecraft.client.player.AbstractClientPlayer implements ICameraOverriddenEntity {

    public LocalPlayerMixin(net.minecraft.client.multiplayer.ClientLevel level, com.mojang.authlib.GameProfile profile) {
        super(level, profile);
    }

    @Unique private float cameraPitch;
    @Unique private float cameraYaw;
    @Shadow public net.minecraft.client.player.ClientInput input;


    @ModifyExpressionValue(
            method = {"sendPosition", "tick"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F")
    )
    private float hookSilentRotationYaw(float original) {
        if (Rotations.rotating) {
            return Rotations.lastServerYaw = Rotations.serverYaw;
        }
        return original;
    }

    @ModifyExpressionValue(
            method = {"sendPosition", "tick"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F")
    )
    private float hookSilentRotationPitch(float original) {
        if (Rotations.rotating) {
            Rotations.lastServerPitch = Rotations.serverPitch;
            return Rotations.serverPitch;
        }
        return original;
    }


    @Inject(method = "sendPosition", at = @At("HEAD"))
    private void hookPreSendMovementPackets(CallbackInfo ci) {
        Frosty.EVENT_BUS.post(PreSendMovementPacketsEvent.get());

        PreMotionEvent preMotionEvent = new PreMotionEvent(
                this.getX(), this.getY(), this.getZ(),
                this.getYRot(), this.getXRot(),
                this.onGround(), this.isSprinting(), this.isCrouching()
        );
        Frosty.EVENT_BUS.post(preMotionEvent);
    }

    @Inject(method = "sendPosition", at = @At("TAIL"))
    private void hookPostSendMovementPackets(CallbackInfo ci) {
        Frosty.EVENT_BUS.post(PostSendMovementPacketsEvent.get());
    }

    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/ClientInput;tick()V", shift = At.Shift.AFTER))
    private void hookSprintInput(CallbackInfo ci) {
        var event = new SprintEvent(new DirectionalInput(this.input.keyPresses), this.input.hasForwardImpulse(), SprintEvent.Source.INPUT);
        Frosty.EVENT_BUS.post(event);
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;canStartSprinting()Z"))
    private boolean modifyCanSprint(boolean original) {
        var event = new SprintEvent(new DirectionalInput(this.input.keyPresses), original, SprintEvent.Source.MOVEMENT_TICK);
        Frosty.EVENT_BUS.post(event);
        return event.getSprint();
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;shouldStopRunSprinting()Z"))
    private boolean hookSprintStop(boolean original) {
        var event = new SprintEvent(new DirectionalInput(this.input.keyPresses), !original, SprintEvent.Source.MOVEMENT_TICK);
        Frosty.EVENT_BUS.post(event);
        return !event.getSprint();
    }

    @Override @Unique public float frosty$getCameraPitch() { return this.cameraPitch; }
    @Override @Unique public float frosty$getCameraYaw() { return this.cameraYaw; }
    @Override @Unique public void frosty$setCameraPitch(float pitch) { this.cameraPitch = pitch; }
    @Override @Unique public void frosty$setCameraYaw(float yaw) { this.cameraYaw = yaw; }
}