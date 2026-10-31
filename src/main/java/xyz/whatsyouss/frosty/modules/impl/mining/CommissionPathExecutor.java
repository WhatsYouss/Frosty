package xyz.whatsyouss.frosty.modules.impl.mining;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frosty.utility.commission.pathfinder.Path;
import xyz.whatsyouss.frosty.interfaces.IKeyMapping;
import xyz.whatsyouss.frosty.utility.RotationUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class CommissionPathExecutor {
    private static final double NODE_REACHED_HORIZONTAL_DISTANCE = 0.7;
    private static final double NODE_REACHED_VERTICAL_TOLERANCE = 1.35;
    private static final double SEGMENT_PROGRESS_SWITCH_THRESHOLD = 0.65;
    private static final double STUCK_SPEED_THRESHOLD = 0.05;
    private static final long STUCK_DETECTION_MS = 1_000L;
    private static final long STUCK_RECOVERY_TIMEOUT_MS = 700L;
    private static final long SEGMENT_TIMEOUT_MS = 30_000L;

    private final Minecraft mc = Minecraft.getInstance();
    private List<BlockPos> blockPath = Collections.emptyList();
    private final Map<Long, List<NodeIndex>> nodesByColumn = new HashMap<>();
    private int targetIndex;
    private int previousIndex;
    private Status status = Status.IDLE;
    private long segmentStartedAt;
    private long stuckSince;
    private long recoveryJumpAt;
    private boolean attemptedStuckRecovery;
    private long nextNodeSwitchAt;

    void start(Path path) {
        stop();
        if (path == null) {
            status = Status.FAILED;
            return;
        }
        blockPath = path.getSmoothedPath();
        if (blockPath.isEmpty()) {
            status = Status.FAILED;
            return;
        }
        targetIndex = blockPath.size() > 1 ? 1 : 0;
        previousIndex = Math.max(-1, targetIndex - 1);
        for (int index = 0; index < blockPath.size(); index++) {
            BlockPos position = blockPath.get(index);
            nodesByColumn.computeIfAbsent(pack(position.getX(), position.getZ()), ignored -> new java.util.ArrayList<>())
                    .add(new NodeIndex(position.getY(), index));
        }
        segmentStartedAt = System.currentTimeMillis();
        status = Status.RUNNING;
    }

    boolean tick() {
        if (status != Status.RUNNING || mc.player == null) return false;
        if (System.currentTimeMillis() - segmentStartedAt > SEGMENT_TIMEOUT_MS) {
            fail();
            return false;
        }
        if (targetIndex >= blockPath.size()) {
            succeed();
            return true;
        }

        BlockPos target = blockPath.get(targetIndex);
        BlockPos standing = blockStandingOn();
        advanceToNodeUnderPlayer(standing);
        if (targetIndex >= blockPath.size()) {
            succeed();
            return true;
        }
        target = blockPath.get(targetIndex);
        if (targetIndex == blockPath.size() - 1 && standing.equals(target)) {
            succeed();
            return true;
        }

        Vec3 playerPos = mc.player.position();
        double horizontalDistance = horizontalDistance(playerPos, target);
        double verticalDistance = Math.abs(mc.player.getY() - target.getY());
        boolean closeToCurrentNode = horizontalDistance <= NODE_REACHED_HORIZONTAL_DISTANCE
                && verticalDistance <= NODE_REACHED_VERTICAL_TOLERANCE;
        boolean passedCurrentNode = hasPassedCurrentNode(playerPos);

        if (targetIndex < blockPath.size() - 1 && (closeToCurrentNode || passedCurrentNode)
                && System.currentTimeMillis() >= nextNodeSwitchAt) {
            previousIndex = targetIndex++;
            nextNodeSwitchAt = System.currentTimeMillis() + 50L;
            target = blockPath.get(targetIndex);
            horizontalDistance = horizontalDistance(playerPos, target);
        }

        updateStuckState();
        if (status != Status.RUNNING) return false;
        moveToward(target, standing, horizontalDistance);
        return false;
    }

    void stop() {
        releaseMovement();
        blockPath = Collections.emptyList();
        nodesByColumn.clear();
        targetIndex = 0;
        previousIndex = -1;
        segmentStartedAt = 0L;
        stuckSince = 0L;
        recoveryJumpAt = 0L;
        attemptedStuckRecovery = false;
        nextNodeSwitchAt = 0L;
        status = Status.IDLE;
    }

    boolean isRunning() { return status == Status.RUNNING; }
    boolean failed() { return status == Status.FAILED; }
    BlockPos getTarget() { return targetIndex < blockPath.size() ? blockPath.get(targetIndex) : null; }

    private boolean hasPassedCurrentNode(Vec3 playerPos) {
        if (targetIndex >= blockPath.size() - 1) return false;
        BlockPos current = blockPath.get(targetIndex);
        BlockPos next = blockPath.get(targetIndex + 1);
        double segmentX = next.getX() - current.getX();
        double segmentZ = next.getZ() - current.getZ();
        double lengthSquared = segmentX * segmentX + segmentZ * segmentZ;
        if (lengthSquared <= 1.0E-6) return false;
        double fromCurrentX = playerPos.x - (current.getX() + 0.5);
        double fromCurrentZ = playerPos.z - (current.getZ() + 0.5);
        double progress = (fromCurrentX * segmentX + fromCurrentZ * segmentZ) / lengthSquared;
        return progress >= SEGMENT_PROGRESS_SWITCH_THRESHOLD;
    }

    private void advanceToNodeUnderPlayer(BlockPos standing) {
        List<NodeIndex> entries = nodesByColumn.get(pack(standing.getX(), standing.getZ()));
        if (entries == null || entries.isEmpty()) return;

        NodeIndex best = null;
        for (NodeIndex entry : entries) {
            if (entry.index() <= previousIndex) continue;
            if (best == null || Math.abs(entry.y() - standing.getY()) < Math.abs(best.y() - standing.getY())) {
                best = entry;
            }
        }
        if (best != null && best.index() >= targetIndex) {
            previousIndex = best.index();
            targetIndex = best.index() + 1;
            nextNodeSwitchAt = System.currentTimeMillis() + 50L;
        }
    }

    private void updateStuckState() {
        double horizontalSpeed = Math.hypot(mc.player.getDeltaMovement().x, mc.player.getDeltaMovement().z);
        long now = System.currentTimeMillis();
        if (horizontalSpeed >= STUCK_SPEED_THRESHOLD) {
            stuckSince = 0L;
            attemptedStuckRecovery = false;
            recoveryJumpAt = 0L;
            return;
        }
        if (attemptedStuckRecovery) {
            if (now - recoveryJumpAt >= STUCK_RECOVERY_TIMEOUT_MS) fail();
            return;
        }
        if (stuckSince == 0L) {
            stuckSince = now;
        } else if (now - stuckSince >= STUCK_DETECTION_MS) {
            attemptedStuckRecovery = true;
            recoveryJumpAt = now;
        }
    }

    private void moveToward(BlockPos target, BlockPos standing, double horizontalDistance) {
        Vec3 position = mc.player.position();
        float targetYaw = (float) Math.toDegrees(Math.atan2(
                -(target.getX() + 0.5 - position.x), target.getZ() + 0.5 - position.z));
        RotationUtils.setYawTo(targetYaw, 3.0F);

        float relativeYaw = Mth.wrapDegrees(targetYaw - mc.player.getYRot());
        setKey(mc.options.keyUp, Math.abs(relativeYaw) < 112.5F);
        setKey(mc.options.keyDown, Math.abs(relativeYaw) > 67.5F);
        setKey(mc.options.keyLeft, relativeYaw < -22.5F && relativeYaw > -157.5F);
        setKey(mc.options.keyRight, relativeYaw > 22.5F && relativeYaw < 157.5F);

        boolean shouldJump = target.getY() > standing.getY()
                && horizontalDistance <= 2.2
                && mc.player.onGround();
        if (attemptedStuckRecovery && mc.player.onGround()) shouldJump = true;
        setKey(mc.options.keyJump, shouldJump);
        setKey(mc.options.keySprint, false);
    }

    private BlockPos blockStandingOn() {
        return BlockPos.containing(mc.player.getX(), Math.ceil(mc.player.getY() - 0.25) - 1, mc.player.getZ());
    }

    private double horizontalDistance(Vec3 position, BlockPos target) {
        return Math.hypot(position.x - target.getX() - 0.5, position.z - target.getZ() - 0.5);
    }

    private void succeed() {
        releaseMovement();
        status = Status.SUCCEEDED;
    }

    private void fail() {
        releaseMovement();
        status = Status.FAILED;
    }

    private void releaseMovement() {
        if (mc.options == null) return;
        setKey(mc.options.keyUp, false);
        setKey(mc.options.keyDown, false);
        setKey(mc.options.keyLeft, false);
        setKey(mc.options.keyRight, false);
        setKey(mc.options.keyJump, false);
        setKey(mc.options.keySprint, false);
    }

    private void setKey(net.minecraft.client.KeyMapping key, boolean down) {
        IKeyMapping.get(key).setDown(down);
    }

    private long pack(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private enum Status { IDLE, RUNNING, SUCCEEDED, FAILED }
    private record NodeIndex(int y, int index) { }
}
