package xyz.whatsyouss.frosty.utility;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;

public class RotationUtils {

    private static final Minecraft mc = Minecraft.getInstance();

    private static float getGCDFixedRotation(float targetRotation, float currentRotation) {
        if (mc.options == null || mc.player == null) return targetRotation;
        float sensitivity = mc.options.sensitivity().get().floatValue();
        float f = sensitivity * 0.6F + 0.2F;
        float gcd = f * f * f * 8.0F * 0.15F;

        float delta = Mth.wrapDegrees(targetRotation - currentRotation);
        int pixels = Math.round(delta / gcd);
        return currentRotation + (pixels * gcd);
    }

    public static void setYawTo(float targetYaw, float smoothness) {
        float currentYaw = mc.player.getYRot();
        float delta = wrapDegrees(targetYaw - currentYaw);
        float nextYaw = currentYaw + delta / smoothness;
        mc.player.setYRot(getGCDFixedRotation(nextYaw, currentYaw));
    }

    public static void setPitchTo(float targetPitch, float smoothness) {
        float currentPitch = mc.player.getXRot();
        float delta = wrapDegrees(targetPitch - currentPitch);
        float nextPitch = currentPitch + delta / smoothness;
        mc.player.setXRot(getGCDFixedRotation(nextPitch, currentPitch));
    }

    public static void aim(LocalPlayer target, float smoothness) {
        if (mc.player != null && target != null) {
            Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2.0, 0);
            Vec3 playerPos = mc.player.getEyePosition();

            float[] angles = getYawPitchTo(playerPos, targetPos);
            setYawTo(angles[0], smoothness);
            setPitchTo(angles[1], smoothness);
        }
    }

    public static void aimByPos(Vec3 pos, float smoothness) {
        if (mc.player != null && pos != null) {
            Vec3 playerPos = mc.player.getEyePosition();

            float[] angles = getYawPitchTo(playerPos, pos);
            setYawTo(angles[0], smoothness);
            setPitchTo(angles[1], smoothness);
        }
    }

    public static float[] getYawPitchTo(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;

        double distanceXZ = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90F;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, distanceXZ));

        return new float[]{wrapDegrees(yaw), wrapDegrees(pitch)};
    }

    private static float wrapDegrees(float degrees) {
        degrees %= 360.0F;
        if (degrees >= 180.0F) degrees -= 360.0F;
        if (degrees < -180.0F) degrees += 360.0F;
        return degrees;
    }

    public static boolean isPossibleToHit(Entity target, double reach, float[] rotations) {
        if (mc.player == null || target == null) return false;

        Vec3 eyePos = mc.player.getEyePosition();

        float yaw = rotations[0];
        float pitch = rotations[1];
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);

        double dx = -Math.sin(yawRad) * Math.cos(pitchRad);
        double dz =  Math.cos(yawRad) * Math.cos(pitchRad);
        double dy = -Math.sin(pitchRad);

        Vec3 direction = new Vec3(dx, dy, dz);

        Vec3 endPos = eyePos.add(direction.scale(reach));

        BlockHitResult blockHit = mc.level.clip(new ClipContext(eyePos, endPos, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, mc.player));

        if (blockHit.getType() != HitResult.Type.MISS) {
            double blockDistSq = blockHit.getLocation().distanceToSqr(eyePos);
            double targetDistSq = target.getBoundingBox().getCenter().distanceToSqr(eyePos);
            if (blockDistSq < targetDistSq) {
                return false;
            }
        }

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(mc.player, eyePos, endPos,
                target.getBoundingBox().inflate(0.3),
                e -> e == target,
                reach
        );

        return entityHit != null && entityHit.getEntity() == target;
    }

    public static void aimAtEntity(Entity target, float heightFraction, float smoothness) {
        if (mc.player == null || target == null) return;
        Vec3 aimPos    = target.position().add(0,
                Math.max(0.1, target.getBbHeight() * heightFraction), 0);
        Vec3 playerPos = mc.player.getEyePosition();
        float[] angles  = getYawPitchTo(playerPos, aimPos);
        setYawTo(angles[0], smoothness);
        setPitchTo(angles[1], smoothness);
    }
}
