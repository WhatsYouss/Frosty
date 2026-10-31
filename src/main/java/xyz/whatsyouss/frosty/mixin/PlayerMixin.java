package xyz.whatsyouss.frosty.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.modules.impl.other.MoveFix;
import xyz.whatsyouss.frosty.utility.Rotations;

@Mixin(Player.class)
public abstract class PlayerMixin {

    @Unique
    private float frosty$originalYaw;

    @Inject(
            method = "causeExtraKnockback(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/damagesource/DamageSource;FZ)V",
            at = @At("HEAD")
    )
    private void hookFixRotationPre(Entity entity, float knockbackAmount, Vec3 oldMovement, DamageSource damageSource, float damage, boolean comesFromEffect, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (self == Minecraft.getInstance().player && MoveFix.shouldApply()) {
            frosty$originalYaw = self.getYRot();
            self.setYRot(Rotations.serverYaw);
        }
    }

    @Inject(
            method = "causeExtraKnockback(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/damagesource/DamageSource;FZ)V",
            at = @At("TAIL")
    )
    private void hookFixRotationPost(Entity entity, float knockbackAmount, Vec3 oldMovement, DamageSource damageSource, float damage, boolean comesFromEffect, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (self == Minecraft.getInstance().player && MoveFix.shouldApply()) {
            self.setYRot(frosty$originalYaw);
        }
    }

    @Redirect(
            method = "causeExtraKnockback(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/damagesource/DamageSource;FZ)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;multiply(DDD)Lnet/minecraft/world/phys/Vec3;")
    )
    private Vec3 hookSlowVelocity(Vec3 instance, double x, double y, double z) {
        Player self = (Player) (Object) this;

        if (self == Minecraft.getInstance().player && ModuleManager.sprint.isEnabled() && ModuleManager.sprint.keep.isToggled()) {
            double customSlow = (100.0 - ModuleManager.sprint.slow.getInput()) / 100.0;
            return instance.multiply(customSlow, y, customSlow);
        }

        return instance.multiply(x, y, z);
    }

    @Redirect(
            method = "causeExtraKnockback(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/damagesource/DamageSource;FZ)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setSprinting(Z)V")
    )
    private void hookStopCancelSprint(Player instance, boolean sprinting) {
        if (instance == Minecraft.getInstance().player && ModuleManager.sprint.isEnabled() && ModuleManager.sprint.keep.isToggled()) {
            instance.setSprinting(true);
        } else {
            instance.setSprinting(sprinting);
        }
    }

    @ModifyExpressionValue(
            method = {"causeExtraKnockback(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/damagesource/DamageSource;FZ)V", "doSweepAttack"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getYRot()F")
    )
    private float hookBattleRotation(float original) {
        Player self = (Player) (Object) this;

        if (self != Minecraft.getInstance().player) {
            return original;
        }

        if (!MoveFix.shouldApply()) {
            return original;
        }

        return Rotations.serverYaw;
    }
}