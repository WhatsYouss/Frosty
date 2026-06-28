package xyz.whatsyouss.frosty.utility;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import xyz.whatsyouss.frosty.events.impl.*;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.impl.other.MoveFix;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static xyz.whatsyouss.frosty.Frosty.mc;

public class Rotations {

    public static float serverYaw;
    public static float serverPitch;
    public static float lastServerYaw;
    public static float lastServerPitch;
    public static float preYaw;
    public static float prePitch;

    public static boolean rotating, packetSent = false;

    private static Map<Module, RotationRequest> rotationRequests = new HashMap<>();
    private static Module currentRotationOwner = null;

    private static boolean smoothRotating = false;
    private static float targetYaw;
    private static float targetPitch;
    private static float smoothFactor;
    private static float smoothProgress;
    private static float smoothStartYaw;
    private static float smoothStartPitch;
    private static int lastRotationUpdateTick = -1;
    private static double silentSidewaysError;
    private static double silentForwardError;

    private static class RotationRequest {
        public final float yaw;
        public final float pitch;
        public final int priority;
        public final boolean smooth;
        public final float smoothFactor;

        public RotationRequest(float yaw, float pitch, int priority, boolean smooth, float smoothFactor) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.priority = priority;
            this.smooth = smooth;
            this.smoothFactor = smoothFactor;
        }
    }

    public Rotations() {
    }

    public static void setRotate(Module module, float yaw, float pitch, int priority, float smooth) {
        if (module == null || !module.isEnabled()) {
            return;
        }
        RotationRequest previous = rotationRequests.get(module);
        rotationRequests.put(module, new RotationRequest(yaw, pitch, priority, true, smooth));
        updateRotation(previous == null || previous.priority != priority || !previous.smooth);
    }

    private static void updateRotation() {
        updateRotation(true);
    }

    private static void updateRotation(boolean resetSmooth) {
        if (rotationRequests.isEmpty()) {
            cancelRotate();
            return;
        }

        Module highestPriorityModule = null;
        RotationRequest highestPriorityRequest = null;

        for (Map.Entry<Module, RotationRequest> entry : rotationRequests.entrySet()) {
            if (highestPriorityRequest == null || entry.getValue().priority > highestPriorityRequest.priority) {
                highestPriorityModule = entry.getKey();
                highestPriorityRequest = entry.getValue();
            }
        }

        if (highestPriorityRequest != null) {
            boolean ownerChanged = currentRotationOwner != highestPriorityModule;
            boolean completedTargetChanged = smoothRotating
                    && smoothProgress >= 1.0f
                    && (Math.abs(Mth.wrapDegrees(highestPriorityRequest.yaw - targetYaw)) > 0.1f
                    || Math.abs(highestPriorityRequest.pitch - targetPitch) > 0.1f);
            currentRotationOwner = highestPriorityModule;

            if (highestPriorityRequest.smooth) {
                targetYaw = highestPriorityRequest.yaw;
                targetPitch = highestPriorityRequest.pitch;
                smoothFactor = Math.max(0.01f, highestPriorityRequest.smoothFactor);
                if (resetSmooth || !smoothRotating || ownerChanged || completedTargetChanged) {
                    boolean wasRotating = rotating;
                    smoothProgress = 0f;
                    smoothStartYaw = rotating ? serverYaw : mc.player.getYRot();
                    smoothStartPitch = rotating ? serverPitch : mc.player.getXRot();
                    if (!wasRotating) {
                        serverYaw = smoothStartYaw;
                        serverPitch = smoothStartPitch;
                    }
                }
                smoothRotating = true;
                rotating = true;
            } else {
                serverYaw = highestPriorityRequest.yaw;
                serverPitch = highestPriorityRequest.pitch;
                rotating = true;
                smoothRotating = false;

                if (!packetSent) {
                    packetSent = true;
                }
            }
        }
    }

    public static void cancelRotate(Module module) {
        rotationRequests.remove(module);

        if (rotationRequests.isEmpty()) {
            cancelRotate();
        } else {
            updateRotation(true);
        }
    }

    public static void cancelRotate() {
        rotationRequests.clear();
        currentRotationOwner = null;
        rotating = false;
        smoothRotating = false;
        smoothProgress = 0f;
        lastRotationUpdateTick = -1;
        resetSilentMoveFix();
        if (packetSent) {
            packetSent = false;
        }
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (rotationRequests.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<Module, RotationRequest>> iterator = rotationRequests.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Module, RotationRequest> entry = iterator.next();
            Module module = entry.getKey();

            if (!module.isEnabled()) {
                iterator.remove();
            }
        }
    }

    private static float getServerGCDFixedRotation(float targetRotation, float currentRotation) {
        if (mc.options == null) return targetRotation;
        float sensitivity = mc.options.sensitivity().get().floatValue();
        float f = sensitivity * 0.6F + 0.2F;
        float gcd = f * f * f * 8.0F * 0.15F;

        float delta = Mth.wrapDegrees(targetRotation - currentRotation);
        int pixels = Math.round(delta / gcd);
        return currentRotation + (pixels * gcd);
    }

    public static void updateServerRotation() {
        if (!rotating || !smoothRotating || mc.player == null) {
            return;
        }

        int tick = mc.player.tickCount;
        if (lastRotationUpdateTick == tick) {
            return;
        }
        lastRotationUpdateTick = tick;

        float progressStep = 1.0f / Math.max(1.0f, smoothFactor);
        smoothProgress += progressStep;
        if (smoothProgress >= 1.0f) {
            smoothProgress = 1.0f;
            serverYaw = getServerGCDFixedRotation(targetYaw, serverYaw);
            serverPitch = getServerGCDFixedRotation(targetPitch, serverPitch);
            return;
        }

        float t = smoothProgress < 0.5f
                ? 2.0f * smoothProgress * smoothProgress
                : 1.0f - (float) Math.pow(-2.0f * smoothProgress + 2.0f, 2.0f) / 2.0f;

        float lerpYaw = smoothStartYaw + Mth.wrapDegrees(targetYaw - smoothStartYaw) * t;
        float lerpPitch = smoothStartPitch + (targetPitch - smoothStartPitch) * t;

        serverYaw = getServerGCDFixedRotation(lerpYaw, serverYaw);
        serverPitch = getServerGCDFixedRotation(lerpPitch, serverPitch);
    }

    @EventHandler
    public void onPreMotion(PreMotionEvent e) {
        if (!rotating) {
            return;
        }

        updateServerRotation();

        e.setYaw(serverYaw);
        e.setPitch(serverPitch);
    }
    @EventHandler
    public void onStrafe(StrafeEvent e) {
        if (MoveFix.shouldApply()) {
            updateServerRotation();
            e.setYaw(serverYaw);
        }
    }

    @EventHandler
    public void onJump(JumpEvent e) {
        if (MoveFix.shouldApply()) {
            updateServerRotation();
            e.setYaw(serverYaw);
        }
    }

    public static DirectionalInput calculateCorrectedInput(DirectionalInput originalInput, float originalYaw, float serverYaw) {
        if (!originalInput.isMoving()) {
            resetSilentMoveFix();
            return DirectionalInput.NONE;
        }

        float yawDifference = Mth.wrapDegrees(serverYaw - originalYaw);

        Vec2 originalMovement = getMovementVectorFromInput(originalInput);

        Vec2 correctedMovement = rotateMovementVector(originalMovement, yawDifference);

        if (MoveFix.isSilent()) {
            return getSilentInputFromMovementVector(correctedMovement);
        }

        return getInputFromMovementVector(correctedMovement);
    }

    private static DirectionalInput getSilentInputFromMovementVector(Vec2 movement) {
        float max = Math.max(Math.abs(movement.x), Math.abs(movement.y));
        if (max < 0.1f) {
            resetSilentMoveFix();
            return DirectionalInput.NONE;
        }

        double desiredSideways = Mth.clamp(movement.x / max, -1.0f, 1.0f);
        double desiredForward = Mth.clamp(movement.y / max, -1.0f, 1.0f);

        int sideways = getDitheredAxis(desiredSideways, true);
        int forward = getDitheredAxis(desiredForward, false);

        return new DirectionalInput(forward > 0, forward < 0, sideways < 0, sideways > 0);
    }

    private static int getDitheredAxis(double desired, boolean sideways) {
        double error = sideways ? silentSidewaysError : silentForwardError;
        error += desired;

        int output = 0;
        if (error >= 0.5) {
            output = 1;
            error -= 1.0;
        } else if (error <= -0.5) {
            output = -1;
            error += 1.0;
        }

        if (sideways) {
            silentSidewaysError = Mth.clamp(error, -1.0, 1.0);
        } else {
            silentForwardError = Mth.clamp(error, -1.0, 1.0);
        }

        return output;
    }

    private static void resetSilentMoveFix() {
        silentSidewaysError = 0.0;
        silentForwardError = 0.0;
    }

    private static Vec2 getMovementVectorFromInput(DirectionalInput input) {
        float forward = 0;
        float sideways = 0;

        if (input.getForwards() && !input.getBackwards()) {
            forward = 1;
        } else if (input.getBackwards() && !input.getForwards()) {
            forward = -1;
        }

        if (input.getLeft() && !input.getRight()) {
            sideways = -1;
        } else if (input.getRight() && !input.getLeft()) {
            sideways = 1;
        }

        return new Vec2(sideways, forward);
    }

    private static Vec2 rotateMovementVector(Vec2 movement, float yawDifference) {
        double radians = Math.toRadians(yawDifference);
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);

        float newX = movement.x * cos - movement.y * sin;
        float newY = movement.x * sin + movement.y * cos;

        return new Vec2(newX, newY);
    }

    private static DirectionalInput getInputFromMovementVector(Vec2 movement) {
        final float THRESHOLD = 0.1f;

        boolean forward = movement.y > THRESHOLD;
        boolean backward = movement.y < -THRESHOLD;
        boolean left = movement.x < -THRESHOLD;
        boolean right = movement.x > THRESHOLD;

        if (forward && backward) {
            forward = Math.abs(movement.y) > Math.abs(-movement.y);
            backward = !forward;
        }

        if (left && right) {
            left = Math.abs(movement.x) > Math.abs(-movement.x);
            right = !left;
        }

        return new DirectionalInput(forward, backward, left, right);
    }

}
