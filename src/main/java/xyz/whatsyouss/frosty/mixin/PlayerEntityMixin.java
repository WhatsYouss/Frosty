package xyz.whatsyouss.frosty.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.modules.impl.other.MoveFix;
import xyz.whatsyouss.frosty.utility.Rotations;

import static xyz.whatsyouss.frosty.Frosty.mc;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @ModifyExpressionValue(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getYaw()F"))
    private float hookFixRotation(float original) {
        if ((Object) this != mc.player) {
            return original;
        }

        if (!MoveFix.shouldApply()) {
            return original;
        }

        return Rotations.serverYaw;
    }

    @SuppressWarnings({"UnreachableCode", "ConstantValue"})
    @Redirect(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec3d;multiply(DDD)Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d hookSlowVelocity(Vec3d instance, double x, double y, double z) {
        if ((Object) this == mc.player && ModuleManager.sprint.isEnabled() && ModuleManager.sprint.keep.isToggled()) {
            x = z = (100 - ModuleManager.sprint.slow.getInput()) / 100;
        }

        return instance.multiply(x, y, z);
    }
}
