package xyz.whatsyouss.frosty.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.events.impl.StrafeEvent;
import xyz.whatsyouss.frosty.interfaces.ICameraOverriddenEntity;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.modules.impl.render.FreeLook;

@Mixin(Entity.class)
public abstract class EntityMixin implements ICameraOverriddenEntity {
    @Unique
    private float cameraPitch;
    @Unique
    private float cameraYaw;
    @Shadow
    public boolean velocityDirty;
    @Shadow
    public abstract float getYaw();
    @Shadow
    public abstract void setVelocity(Vec3d velocity);
    @Shadow
    public abstract void setVelocity(double x, double y, double z);
    @Shadow
    public abstract void addVelocityInternal(Vec3d velocity);
    @Shadow
    public abstract Vec3d getVelocity();
    @Shadow
    public abstract boolean isSprinting();

    @Shadow
    public float speed;

    @Shadow
    protected static Vec3d movementInputToVelocity(Vec3d movementInput, float speed, float yaw) {
        double d = movementInput.lengthSquared();
        if (d < 1.0E-7) {
            return Vec3d.ZERO;
        } else {
            Vec3d vec3d = (d > (double)1.0F ? movementInput.normalize() : movementInput).multiply((double)speed);
            float f = MathHelper.sin(yaw * ((float)Math.PI / 180F));
            float g = MathHelper.cos(yaw * ((float)Math.PI / 180F));
            return new Vec3d(vec3d.x * (double)g - vec3d.z * (double)f, vec3d.y, vec3d.z * (double)g + vec3d.x * (double)f);
        }
    }

    @Inject(method={"changeLookDirection"}, at={@At(value="HEAD")}, cancellable=true)
    public void changeCameraLookDirection(double xDelta, double yDelta, CallbackInfo ci) {
        if (FreeLook.freelooking && (Object) this instanceof ClientPlayerEntity) {
            double pitchDelta = yDelta * 0.15;
            double yawDelta = xDelta * 0.15;
            this.cameraPitch = MathHelper.clamp(this.cameraPitch + (float)pitchDelta, -90.0f, 90.0f);
            this.cameraYaw += (float)yawDelta;
            ci.cancel();
        }
    }

    /**
     * @author You
     * @reason MoveFix
     */
    @Overwrite
    public void updateVelocity(float speed, Vec3d movementInput) {
        StrafeEvent strafeEvent = new StrafeEvent(movementInput, speed, this.getYaw());
        Frosty.EVENT_BUS.post(strafeEvent);
        Vec3d vec3d = movementInputToVelocity(strafeEvent.getInput(), strafeEvent.getFriction(), strafeEvent.getYaw());
        this.setVelocity(this.getVelocity().add(vec3d));
    }

    @Override
    @Unique
    public float frosty$getCameraPitch() {
        return this.cameraPitch;
    }

    @Override
    @Unique
    public float frosty$getCameraYaw() {
        return this.cameraYaw;
    }

    @Override
    @Unique
    public void frosty$setCameraPitch(float pitch) {
        this.cameraPitch = pitch;
    }

    @Override
    @Unique
    public void frosty$setCameraYaw(float yaw) {
        this.cameraYaw = yaw;
    }
}