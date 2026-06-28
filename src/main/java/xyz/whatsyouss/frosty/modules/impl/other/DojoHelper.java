package xyz.whatsyouss.frosty.modules.impl.other;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frosty.events.impl.*;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.utility.RotationUtils;
import xyz.whatsyouss.frosty.utility.Utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DojoHelper extends Module {
    private static final Pattern CHALLENGE_PATTERN = Pattern.compile("Challenge:\\s*([^,\\]\\n\\r]+)");
    private static final long MASTERY_WAIT_MS = 6520L;
    private static final long MASTERY_SHOOT_WINDOW_MS = 320L;
    private static final float SWIFTNESS_IDLE_YAW = 180.0f;
    private static final float SWIFTNESS_YAW_EPSILON = 6.0f;

    private static final double CONTROL_PREDICTION_TICKS = 5.0;

    private final List<MasteryTarget> masteryTargets = new ArrayList<>();
    private final Queue<SwiftnessTarget> swiftnessTargets = new ArrayDeque<>();

    private Challenge challenge = Challenge.NONE;
    private MasteryTarget currentMasteryTarget;
    private SwiftnessTarget currentSwiftnessTarget;
    private BlockPos lastSwiftnessPos;
    private boolean drawingBow;

    private Entity targetSkeleton;
    private Vec3 lastSkeletonPos;
    private Vec3 skeletonVel = Vec3.ZERO;
    private long controlLookCooldown = 0L;

    public DojoHelper() {
        super("DojoHelper", "道场助手", category.Other);
    }

    @Override
    public void onDisable() {
        resetMastery();
        resetSwiftness();
        resetControl();
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck()) {
            resetMastery();
            resetSwiftness();
            resetControl();
            challenge = Challenge.NONE;
            return;
        }

        Challenge newChallenge = getChallenge();
        if (newChallenge != challenge) {
            resetMastery();
            resetSwiftness();
            resetControl();
            challenge = newChallenge;
        }

        if (challenge == Challenge.MASTERY) {
            handleMastery();
        } else if (challenge == Challenge.SWIFTNESS) {
            handleSwiftness();
        } else if (challenge == Challenge.CONTROL) {
            handleControl();
        }
    }

    @EventHandler
    public void onBlockUpdate(BlockUpdateEvent event)  {
        if (!Utils.nullCheck()) {
            return;
        }

        Challenge current = getChallenge();
        if (!isNewLimeWool(event)) {
            return;
        }

        if (current == Challenge.MASTERY && isWithinPlayerCube(event.pos, 32)) {
            addMasteryTarget(event.pos);
        } else if (current == Challenge.SWIFTNESS) {
            addSwiftnessTarget(event.pos);
        }
    }

    @EventHandler
    public void onSendPacket(SendPacketEvent event) {
        if (!Utils.nullCheck() || !(event.getPacket() instanceof ServerboundAttackPacket packet)) {
            return;
        }

        Challenge current = getChallenge();

        if (current == Challenge.FORCE) {
            Entity entity = mc.level.getEntity(packet.entityId());
            if (entity instanceof Zombie zombie && zombie.getItemBySlot(EquipmentSlot.HEAD).getItem() == Items.LEATHER_HELMET) {
                event.setCancelled(true);
            }
        }
        else if (current == Challenge.DISCIPLINE) {
            Entity entity = mc.level.getEntity(packet.entityId());
            if (entity instanceof Zombie zombie) {
                int requiredSlot = slotForHelmet(zombie.getItemBySlot(EquipmentSlot.HEAD).getItem());

                if (requiredSlot != -1 && mc.player.getInventory().getSelectedSlot() != requiredSlot) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onSprint(SprintEvent event) {
        if (challenge == Challenge.SWIFTNESS && currentSwiftnessTarget != null && !currentSwiftnessTarget.sprintJumping) {
            event.setSprint(false);
        }
    }

    private Challenge getChallenge() {
        String sidebar = Utils.stripColor(Utils.getScoreboardSidebarLines().toString());
        Matcher matcher = CHALLENGE_PATTERN.matcher(sidebar);
        if (!matcher.find()) {
            return Challenge.NONE;
        }

        String value = matcher.group(1).trim().toLowerCase(Locale.ROOT);
        if (value.startsWith("force")) return Challenge.FORCE;
        if (value.startsWith("mastery")) return Challenge.MASTERY;
        if (value.startsWith("discipline")) return Challenge.DISCIPLINE;
        if (value.startsWith("swiftness")) return Challenge.SWIFTNESS;
        if (value.startsWith("control")) return Challenge.CONTROL;
        return Challenge.NONE;
    }

    private void handleControl() {
        Entity closestSkeleton = null;
        double minDist = 25.0;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof WitherSkeleton) {
                if (((WitherSkeleton) entity).getItemBySlot(EquipmentSlot.HEAD).getItem() == Items.REDSTONE_BLOCK) {
                    continue;
                }

                double dist = mc.player.position().distanceTo(entity.position());
                if (dist < minDist) {
                    minDist = dist;
                    closestSkeleton = entity;
                }
            }
        }

        targetSkeleton = closestSkeleton;
        if (targetSkeleton == null) {
            lastSkeletonPos = null;
            skeletonVel = Vec3.ZERO;
            return;
        }

        Vec3 currentPos = targetSkeleton.position();
        if (lastSkeletonPos != null) {
            skeletonVel = currentPos.subtract(lastSkeletonPos);
        }
        lastSkeletonPos = currentPos;

        long now = System.currentTimeMillis();
        if (now - controlLookCooldown <= 40) {
            return;
        }
        controlLookCooldown = now;

        double predX = currentPos.x + (skeletonVel.x * CONTROL_PREDICTION_TICKS);
        double predY = currentPos.y + (skeletonVel.y * 2) + 2.5;
        double predZ = currentPos.z + (skeletonVel.z * CONTROL_PREDICTION_TICKS);

        RotationUtils.aimByPos(new Vec3(predX, predY, predZ), 3);
    }

    private boolean isNewLimeWool(BlockUpdateEvent event) {
        return event.pos != null
                && event.oldState != null
                && event.newState != null
                && event.oldState.isAir()
                && event.newState.getBlock() == Blocks.WOOL.lime();
    }

    private boolean isWithinPlayerCube(BlockPos pos, int range) {
        BlockPos playerPos = mc.player.blockPosition();
        return Math.abs(pos.getX() - playerPos.getX()) <= range
                && Math.abs(pos.getY() - playerPos.getY()) <= range
                && Math.abs(pos.getZ() - playerPos.getZ()) <= range;
    }

    private void addMasteryTarget(BlockPos pos) {
        for (MasteryTarget target : masteryTargets) {
            if (target.pos.equals(pos)) {
                return;
            }
        }
        masteryTargets.add(new MasteryTarget(pos.immutable(), System.currentTimeMillis()));
    }

    private void handleMastery() {
        long now = System.currentTimeMillis();
        masteryTargets.removeIf(target -> now > target.shootAt + MASTERY_SHOOT_WINDOW_MS);

        if (masteryTargets.isEmpty()) {
            stopDrawingBow();
            currentMasteryTarget = null;
            return;
        }

        MasteryTarget target = chooseMasteryTarget(now);
        if (target == null) {
            stopDrawingBow();
            currentMasteryTarget = null;
            return;
        }

        if (currentMasteryTarget != target) {
            stopDrawingBow();
            currentMasteryTarget = target;
        }

        switchHotbarSlot(0);
        Vec3 aimPos = getMasteryAimPos(target.pos);
        RotationUtils.aimByPos(aimPos, 3);

        if (!drawingBow) {
            if (!mc.options.keyUse.isDown()) {
                mc.options.keyUse.setDown(true);
            }
            drawingBow = true;
        }

        if (now >= target.shootAt) {
            mc.options.keyUse.setDown(false);
            drawingBow = false;
            masteryTargets.remove(target);
            currentMasteryTarget = null;
        }
    }

    private MasteryTarget chooseMasteryTarget(long now) {
        return masteryTargets.stream()
                .filter(target -> now <= target.shootAt + MASTERY_SHOOT_WINDOW_MS)
                .filter(target -> {
                    long requiredDraw = getRequiredBowDrawMs(target.pos);
                    return now + requiredDraw <= target.shootAt + MASTERY_SHOOT_WINDOW_MS;
                })
                .min(Comparator.comparingLong(target -> target.shootAt))
                .orElse(null);
    }

    private long getRequiredBowDrawMs(BlockPos pos) {
        double distance = mc.player.getEyePosition().distanceTo(Vec3.atCenterOf(pos));
        return Math.max(450L, Math.min(1000L, Math.round(350.0 + distance * 25.0)));
    }

    private Vec3 getMasteryAimPos(BlockPos pos) {
        Vec3 woolCenter = Vec3.atCenterOf(pos);
        Vec3 aboveCenter = Vec3.atCenterOf(pos.above());
        double distance = mc.player.getEyePosition().distanceTo(woolCenter);
        double t = Math.max(0.0, Math.min(1.0, (distance - 10.0) / 10.0));
        return woolCenter.lerp(aboveCenter, t);
    }

    private void resetMastery() {
        masteryTargets.clear();
        currentMasteryTarget = null;
        stopDrawingBow();
    }

    private void stopDrawingBow() {
        if (!drawingBow || mc.player == null || mc.gameMode == null) {
            drawingBow = false;
            return;
        }
        mc.gameMode.releaseUsingItem(mc.player);
        mc.player.stopUsingItem();
        drawingBow = false;
    }

    private void handleSwiftness() {
        mc.options.keySprint.setDown(false);

        if (currentSwiftnessTarget == null) {
            currentSwiftnessTarget = swiftnessTargets.poll();
        }

        if (currentSwiftnessTarget == null) {
            releaseMovementKeys();
            RotationUtils.setYawTo(SWIFTNESS_IDLE_YAW, 3);
            return;
        }

        Vec3 targetCenter = Vec3.atCenterOf(currentSwiftnessTarget.pos);
        Vec3 playerPos = mc.player.position();
        double dx = targetCenter.x - playerPos.x;
        double dz = targetCenter.z - playerPos.z;
        double horizontalDistance = Math.hypot(dx, dz);

        if (horizontalDistance < 0.35) {
            releaseMovementKeys();
            currentSwiftnessTarget = null;
            return;
        }

        Direction direction = Math.abs(dx) >= Math.abs(dz)
                ? (dx > 0 ? Direction.EAST : Direction.WEST)
                : (dz > 0 ? Direction.SOUTH : Direction.NORTH);

        boolean sprintJump = currentSwiftnessTarget.needSprint && horizontalDistance > 0.5;
        currentSwiftnessTarget.sprintJumping = sprintJump;

        if (sprintJump) {
            float targetYaw = yawFor(direction);
            RotationUtils.setYawTo(targetYaw, 3);

            if (!isYawClose(targetYaw)) {
                releaseMovementKeys();
                return;
            }

            pressOnly(mc.options.keyUp);
            mc.options.keySprint.setDown(true);
            mc.options.keyJump.setDown(horizontalDistance > 0.8);
            return;
        }

        RotationUtils.setYawTo(SWIFTNESS_IDLE_YAW, 3);
        pressOnly(keyForDirectionWithNorthYaw(direction));
        mc.options.keySprint.setDown(false);
        mc.options.keyJump.setDown(currentSwiftnessTarget.jump && horizontalDistance > 0.8);
    }

    private void addSwiftnessTarget(BlockPos pos) {
        BlockPos immutablePos = pos.immutable();
        boolean jump = lastSwiftnessPos != null && hasAirGap(lastSwiftnessPos, immutablePos);

        int blockDist = lastSwiftnessPos != null ?
                Math.abs(immutablePos.getX() - lastSwiftnessPos.getX()) + Math.abs(immutablePos.getZ() - lastSwiftnessPos.getZ()) : 0;

        boolean needSprint = jump && blockDist >= 3;

        swiftnessTargets.add(new SwiftnessTarget(immutablePos, jump, needSprint));
        lastSwiftnessPos = immutablePos;
    }

    private boolean hasAirGap(BlockPos from, BlockPos to) {
        int dx = Integer.compare(to.getX(), from.getX());
        int dz = Integer.compare(to.getZ(), from.getZ());
        if ((dx != 0 && dz != 0) || (dx == 0 && dz == 0)) {
            return false;
        }

        BlockPos check = from.offset(dx, 0, dz);
        boolean hasMiddle = false;
        while (!check.equals(to)) {
            hasMiddle = true;
            if (!mc.level.getBlockState(check).isAir()) {
                return false;
            }
            check = check.offset(dx, 0, dz);
        }
        return hasMiddle;
    }

    private float yawFor(Direction direction) {
        return switch (direction) {
            case SOUTH -> 0.0f;
            case WEST -> 90.0f;
            case EAST -> -90.0f;
            default -> 180.0f;
        };
    }

    private KeyMapping keyForDirectionWithNorthYaw(Direction direction) {
        return switch (direction) {
            case SOUTH -> mc.options.keyDown;
            case WEST -> mc.options.keyLeft;
            case EAST -> mc.options.keyRight;
            default -> mc.options.keyUp;
        };
    }

    private boolean isYawClose(float targetYaw) {
        return Math.abs(wrapDegrees(mc.player.getYRot() - targetYaw)) <= SWIFTNESS_YAW_EPSILON;
    }

    private float wrapDegrees(float degrees) {
        degrees %= 360.0F;
        if (degrees >= 180.0F) degrees -= 360.0F;
        if (degrees < -180.0F) degrees += 360.0F;
        return degrees;
    }

    private void resetSwiftness() {
        swiftnessTargets.clear();
        currentSwiftnessTarget = null;
        lastSwiftnessPos = null;
        releaseMovementKeys();
    }

    private void resetControl() {
        targetSkeleton = null;
        lastSkeletonPos = null;
        skeletonVel = Vec3.ZERO;
        controlLookCooldown = 0L;
    }

    private void pressOnly(KeyMapping key) {
        mc.options.keyUp.setDown(key == mc.options.keyUp);
        mc.options.keyDown.setDown(key == mc.options.keyDown);
        mc.options.keyLeft.setDown(key == mc.options.keyLeft);
        mc.options.keyRight.setDown(key == mc.options.keyRight);
    }

    private void releaseMovementKeys() {
        mc.options.keyUp.setDown(false);
        mc.options.keyDown.setDown(false);
        mc.options.keyLeft.setDown(false);
        mc.options.keyRight.setDown(false);
        mc.options.keyJump.setDown(false);
        mc.options.keySprint.setDown(false);
    }

    private int slotForHelmet(Item helmet) {
        if (helmet == Items.LEATHER_HELMET) return 0;
        if (helmet == Items.IRON_HELMET) return 1;
        if (helmet == Items.GOLDEN_HELMET) return 2;
        if (helmet == Items.DIAMOND_HELMET) return 3;
        return -1;
    }

    private void switchHotbarSlot(int slot) {
        if (slot < 0 || slot > 8 || mc.player.getInventory().getSelectedSlot() == slot) {
            return;
        }
        mc.player.getInventory().setSelectedSlot(slot);
        mc.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
    }

    private enum Challenge {
        NONE,
        FORCE,
        MASTERY,
        DISCIPLINE,
        SWIFTNESS,
        CONTROL
    }

    private static class MasteryTarget {
        private final BlockPos pos;
        private final long shootAt;

        private MasteryTarget(BlockPos pos, long addedAt) {
            this.pos = pos;
            this.shootAt = addedAt + MASTERY_WAIT_MS;
        }
    }

    private static class SwiftnessTarget {
        private final BlockPos pos;
        private final boolean jump;
        private final boolean needSprint;
        private boolean sprintJumping;

        private SwiftnessTarget(BlockPos pos, boolean jump, boolean needSprint) {
            this.pos = pos;
            this.jump = jump;
            this.needSprint = needSprint;
        }
    }
}