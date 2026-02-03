package xyz.whatsyouss.frosty.mixin;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.events.impl.JumpEvent;
import xyz.whatsyouss.frosty.events.impl.StrafeEvent;
import xyz.whatsyouss.frosty.utility.Rotations;

@Mixin(value = LivingEntity.class, priority = 999)
public abstract class LivingEntityMixin extends EntityMixin {

    @Shadow
    protected abstract float getJumpVelocity();

    /**
     * @author You
     * @reason MoveFix
     */
    @Overwrite
    @VisibleForTesting
    public void jump() {
        JumpEvent jumpEvent = new JumpEvent(this.getJumpVelocity(), this.getYaw(), this.isSprinting());
        Frosty.EVENT_BUS.post(jumpEvent);
        if (jumpEvent.isCancelled()) {
            return;
        }
        float f = jumpEvent.getJumpVelocity();
        if (!(f <= 1.0E-5F)) {
            Vec3d vec3d = this.getVelocity();
            this.setVelocity(vec3d.x, Math.max((double)f, vec3d.y), vec3d.z);
            if (this.isSprinting()) {
                float g = jumpEvent.getYaw() * ((float)Math.PI / 180F);
                this.addVelocityInternal(new Vec3d((double)(-MathHelper.sin(g)) * 0.2, (double)0.0F, (double)MathHelper.cos(g) * 0.2));
            }

            this.velocityDirty = true;
        }
    }
}
