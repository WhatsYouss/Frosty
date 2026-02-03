package xyz.whatsyouss.frosty.utility;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class RotationUtils {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void setYawTo(float targetYaw) {
        mc.player.setYaw(targetYaw);
    }

    public static void setYawTo(float targetYaw, float smoothness) {
        float currentYaw = mc.player.getYaw();
        float delta = wrapDegrees(targetYaw - currentYaw);
        mc.player.setYaw(currentYaw + delta / smoothness);
    }

    public static void setPitchTo(float targetPitch) {
        mc.player.setPitch(targetPitch);
    }

    public static void setPitchTo(float targetPitch, float smoothness) {
        float currentPitch = mc.player.getPitch();
        float delta = wrapDegrees(targetPitch - currentPitch);
        mc.player.setPitch(currentPitch + delta / smoothness);
    }

    public static void aim(Entity target) {
        if (mc.player != null && target != null) {
            Vec3d targetPos = target.getEntityPos().add(0, target.getHeight() / 2.0, 0);
            Vec3d playerPos = mc.player.getEyePos();

            float[] angles = getYawPitchTo(playerPos, targetPos);
            setYawTo(angles[0]);
            setPitchTo(angles[1]);
        }
    }

    public static void aimByPos(Vec3d pos) {
        if (mc.player != null && pos != null) {
            Vec3d playerPos = mc.player.getEyePos();

            float[] angles = getYawPitchTo(playerPos, pos);
            setYawTo(angles[0]);
            setPitchTo(angles[1]);
        }
    }

    public static void aim(Entity target, float smoothness) {
        if (mc.player != null && target != null) {
            Vec3d targetPos = target.getEntityPos().add(0, target.getHeight() / 2.0, 0);
            Vec3d playerPos = mc.player.getEyePos();

            float[] angles = getYawPitchTo(playerPos, targetPos);
            setYawTo(angles[0], smoothness);
            setPitchTo(angles[1], smoothness);
        }
    }

    public static void aimByPos(Vec3d pos, float smoothness) {
        if (mc.player != null && pos != null) {
            Vec3d playerPos = mc.player.getEyePos();

            float[] angles = getYawPitchTo(playerPos, pos);
            setYawTo(angles[0], smoothness);
            setPitchTo(angles[1], smoothness);
        }
    }

    public static float[] getYawPitchTo(Vec3d from, Vec3d to) {
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

        Vec3d eyePos = mc.player.getEyePos();

        float yaw = rotations[0];
        float pitch = rotations[1];
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);

        double dx = -Math.sin(yawRad) * Math.cos(pitchRad);
        double dz =  Math.cos(yawRad) * Math.cos(pitchRad);
        double dy = -Math.sin(pitchRad);

        Vec3d direction = new Vec3d(dx, dy, dz);

        Vec3d endPos = eyePos.add(direction.multiply(reach));

        HitResult blockHit = mc.world.raycast(new RaycastContext(
                eyePos,
                endPos,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));
        if (blockHit.getType() != HitResult.Type.MISS) {
            double blockDistSq = blockHit.getPos().squaredDistanceTo(eyePos);
            double targetDistSq = target.getBoundingBox().getCenter().squaredDistanceTo(eyePos);
            if (blockDistSq < targetDistSq) {
                return false;
            }
        }

        EntityHitResult entityHit = ProjectileUtil.raycast(
                mc.player,
                eyePos,
                endPos,
                target.getBoundingBox().expand(0.3),
                e -> e == target,
                reach * reach
        );

        return entityHit != null && entityHit.getEntity() == target;
    }
}
