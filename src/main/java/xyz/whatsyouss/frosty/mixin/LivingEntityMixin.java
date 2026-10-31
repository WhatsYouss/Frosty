package xyz.whatsyouss.frosty.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.whatsyouss.frosty.events.impl.JumpEvent;
import xyz.whatsyouss.frosty.modules.impl.other.MoveFix;
import xyz.whatsyouss.frosty.utility.Rotations;

@Mixin(value = LivingEntity.class, priority = 999)
public abstract class LivingEntityMixin extends EntityMixin {

    @Shadow public abstract float getJumpPower();

    @Inject(
            method = "jumpFromGround",
            at = @At("HEAD"),
            cancellable = true
    )
    private void hookJumpEvent(CallbackInfo ci) {
        if ((Object) this != Minecraft.getInstance().player) return;

        JumpEvent jumpEvent = new JumpEvent(this.getJumpPower(), this.getYRot(), this.isSprinting());
        xyz.whatsyouss.frosty.Frosty.EVENT_BUS.post(jumpEvent);

        if (jumpEvent.isCancelled()) {
            ci.cancel();
            return;
        }

        if (jumpEvent.getJumpVelocity() != this.getJumpPower()) {
            float f = jumpEvent.getJumpVelocity();
            if (f > 1.0E-5F) {
                Vec3 vec3d = this.getDeltaMovement();
                this.setDeltaMovement(new Vec3(vec3d.x, Math.max((double) f, vec3d.y), vec3d.z));

                if (this.isSprinting()) {
                    float yaw = (MoveFix.shouldApply() && (Object) this == Minecraft.getInstance().player)
                            ? Rotations.serverYaw
                            : jumpEvent.getYaw();
                    float g = yaw * ((float) Math.PI / 180.0F);
                    this.addDeltaMovement(new Vec3(
                            (double)(-net.minecraft.util.Mth.sin(g)) * 0.2,
                            0.0,
                            (double) net.minecraft.util.Mth.cos(g) * 0.2));
                }

                this.needsSync = true;
            }
            ci.cancel();
        }
    }

    @ModifyExpressionValue(
            method = "jumpFromGround",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F")
    )
    private float hookJumpYaw(float original) {
        if ((Object) this != Minecraft.getInstance().player) return original;
        if (MoveFix.shouldApply()) return Rotations.serverYaw;
        return original;
    }

    @ModifyExpressionValue(
            method = "updateFallFlyingMovement",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getXRot()F")
    )
    private float hookModifyFallFlyingPitch(float original) {
        if ((Object) this != Minecraft.getInstance().player) return original;
        if (MoveFix.shouldApply()) return Rotations.serverPitch;
        return original;
    }
}
