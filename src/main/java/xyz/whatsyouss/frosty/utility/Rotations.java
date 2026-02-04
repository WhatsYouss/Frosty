package xyz.whatsyouss.frosty.utility;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import xyz.whatsyouss.frosty.events.impl.*;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.impl.other.MoveFix;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Rotations {

    public static float serverYaw, lastServerYaw;
    public static float serverPitch, lastServerPitch;
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

    public static void setRotate(Module module, float yaw, float pitch, int priority) {
        if (module == null || !module.isEnabled()) {
            return;
        }
        rotationRequests.put(module, new RotationRequest(yaw, pitch, priority, false, 0));
        updateRotation();
    }

    public static void setSmoothRotate(Module module, float yaw, float pitch, int priority, float smooth) {
        if (module == null || !module.isEnabled()) {
            return;
        }
        rotationRequests.put(module, new RotationRequest(yaw, pitch, priority, true, smooth));
        updateRotation();
    }

    private static void updateRotation() {
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
            currentRotationOwner = highestPriorityModule;

            if (highestPriorityRequest.smooth) {
                targetYaw = highestPriorityRequest.yaw;
                targetPitch = highestPriorityRequest.pitch;
                smoothFactor = Math.max(0.01f, highestPriorityRequest.smoothFactor);
                smoothProgress = 0f;
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
            updateRotation();
        }
    }

    public static void cancelRotate() {
        rotationRequests.clear();
        currentRotationOwner = null;
        rotating = false;
        smoothRotating = false;
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

    @EventHandler
    public void onPreMotion(PreMotionEvent e) {
        if (!rotating) {
            return;
        }

        if (smoothRotating) {
            smoothProgress += smoothFactor;
            if (smoothProgress >= 1.0f) {
                smoothProgress = 1.0f;
                serverYaw = targetYaw;
                serverPitch = targetPitch;
            } else {
                serverYaw = preYaw + (targetYaw - preYaw) * smoothProgress;
                serverPitch = prePitch + (targetPitch - prePitch) * smoothProgress;
            }
        }

        e.setYaw(serverYaw);
        e.setPitch(serverPitch);
    }

    @EventHandler
    public void onStrafe(StrafeEvent e) {
        if (MoveFix.shouldApply()) {
            e.setYaw(serverYaw);
        }
    }

    @EventHandler
    public void onJump(JumpEvent e) {
        if (MoveFix.shouldApply()) {
            e.setYaw(serverYaw);
        }
    }

    public static DirectionalInput calculateCorrectedInput(DirectionalInput originalInput, float originalYaw, float serverYaw) {
        if (!originalInput.isMoving()) {
            return DirectionalInput.NONE;
        }

        float yawDifference = MathHelper.wrapDegrees(serverYaw - originalYaw);

        Vec2f originalMovement = getMovementVectorFromInput(originalInput);

        Vec2f correctedMovement = rotateMovementVector(originalMovement, yawDifference);

        return getInputFromMovementVector(correctedMovement);
    }

    private static Vec2f getMovementVectorFromInput(DirectionalInput input) {
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

        return new Vec2f(sideways, forward);
    }

    private static Vec2f rotateMovementVector(Vec2f movement, float yawDifference) {
        double radians = Math.toRadians(yawDifference);
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);

        float newX = movement.x * cos - movement.y * sin;
        float newY = movement.x * sin + movement.y * cos;

        return new Vec2f(newX, newY);
    }

    private static DirectionalInput getInputFromMovementVector(Vec2f movement) {
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