package xyz.whatsyouss.frosty.modules.impl.fishing;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frosty.events.impl.EntityJoinEvent;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.events.impl.SprintEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.impl.other.AntiBot;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.RenderUtils;
import xyz.whatsyouss.frosty.utility.RotationUtils;
import xyz.whatsyouss.frosty.utility.Utils;
import xyz.whatsyouss.frosty.utility.pathfinding.NavMeshGenerator;
import xyz.whatsyouss.frosty.utility.pathfinding.NavMeshPath;
import xyz.whatsyouss.frosty.utility.pathfinding.PathfindingService;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class AutoFish extends Module {

    private static final int REEL_SCAN_TICKS = 3;
    private static final double SEA_CREATURE_SPAWN_RANGE = 1.0;
    private static final double SEA_CREATURE_KEEP_RANGE = 32.0;
    private static final double ATTACK_RANGE = 3.5;
    private static final double SMALL_TARGET_ATTACK_RANGE = 2.35;
    private static final double PATH_GOAL_RECOMPUTE_DIST = 2.0;
    private static final double PATH_REACHED_XZ = 0.5;
    private static final double RETURN_REACHED_XZ = 0.45;
    private static final int PATH_RECOMPUTE_TICKS = 30;
    private static final int PATH_RANGE = 96;
    private static final int RESTORE_TIMEOUT_TICKS = 80;
    private static final int SLIME_SPLIT_SCAN_TICKS = 20;

    private String[] faces = new String[]{"Mob", "Down"};
    private String[] CNfaces = new String[]{"海怪", "下方"};

    private ButtonSetting autoThrow, autoKill, antiAFK, useAbility;
    private SliderSetting maxWait, triggerAmount, weaponSlot;
    private SelectSetting face;

    private int currentMode = 0;

    private long biteTime = 0;
    private long randomDelayMs = 0;
    private Thread antiAFKThread;
    private boolean hookBiten = false;
    private long throwTick = 0;
    private long waitStartTick = 0;
    private int rodSlot = 0;
    private InteractionHand rodHand = InteractionHand.MAIN_HAND;
    private boolean failed = false;

    private Vec3 startPos = null;
    private float startYaw = 0f;
    private float startPitch = 0f;

    private float preAbilityYaw = 0f;
    private float preAbilityPitch = 0f;
    private boolean rotationSaved = false;
    private int itemSwitchTicks = 0;

    private Vec3 lastReelPos = null;
    private long reelScanUntil = 0;
    private KillState killState = KillState.IDLE;
    private final List<Entity> targets = new ArrayList<>();
    private Entity currentTarget = null;
    private List<Vec3> currentPath = new ArrayList<>();
    private int pathIndex = 0;
    private int pathRecomputeTick = 0;
    private Vec3 lastPathGoal = null;
    private int restoreTicks = 0;
    private int attackCooldown = 0;
    private Vec3 lastKilledSlimePos = null;
    private long slimeSplitScanUntil = 0;

    public AutoFish() {
        super("AutoFish", "自动钓鱼", category.Fishing);

        this.registerSetting(autoThrow = new ButtonSetting("Auto Throw", "自动抛竿", true));
        this.registerSetting(antiAFK = new ButtonSetting("Anti AFK", "防挂机", true));
        this.registerSetting(maxWait = new SliderSetting("Max Wait", "s", 30, 5, 60, 1, "最长等待时间"));
        this.registerSetting(autoKill = new ButtonSetting("Auto kill", "击杀海怪", true));
        this.registerSetting(triggerAmount = new SliderSetting("Trigger amount", 3, 1, 15, 1, "触发数量"));
        this.registerSetting(useAbility = new ButtonSetting("Use ability", "使用技能", false));
        this.registerSetting(face = new SelectSetting("Face", "朝向", 0, faces, CNfaces));
        this.registerSetting(weaponSlot = new SliderSetting("Weapon slot", "武器槽位", 1, 1, 9, 1));
    }

    @Override
    public void guiUpdate() {
        this.face.setVisibilityCondition(() -> autoKill.isToggled() && useAbility.isToggled());
        this.triggerAmount.setVisibilityCondition(() -> autoKill.isToggled());
        this.useAbility.setVisibilityCondition(() -> autoKill.isToggled());
        this.weaponSlot.setVisibilityCondition(() -> autoKill.isToggled());
    }

    @EventHandler
    public void onSprint(SprintEvent event) {
        event.setSprint(false);
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }

        PathfindingService.getInstance().tick();
        checkRod();
        pruneTargets();

        if (killState != KillState.IDLE) {
            tickAutoKill();
            return;
        }

        long currentTick = mc.level.getGameTime();

        switch (currentMode) {
            case 0 -> {
                if (autoThrow.isToggled()) {
                    currentMode = 1;
                }
            }
            case 1 -> {
                if (mc.player.fishing != null || rightClick()) {
                    currentMode = 2;
                    waitStartTick = currentTick;
                }
                throwTick = currentTick;
            }
            case 2 -> {
                checkForArmorStand();

                if (hookBiten) {
                    if (biteTime == 0) {
                        biteTime = System.currentTimeMillis();
                        randomDelayMs = 10 + new Random().nextInt(291);
                    }
                    if (System.currentTimeMillis() - biteTime >= randomDelayMs) {
                        currentMode = 3;
                        biteTime = 0;
                    }
                    return;
                }

                if (mc.player.fishing == null) {
                    if (currentTick - throwTick >= 20) {
                        currentMode = 1;
                    }
                    return;
                }
                if (currentTick - waitStartTick >= maxWait.getInput() * 20) {
                    currentMode = 3;
                }
            }
            case 3 -> {
                rightClick();
                beginReelScan();
                hookBiten = false;
                currentMode = 0;
                if (autoKill.isToggled()) {
                    killState = KillState.COLLECTING;
                }
            }
        }
    }

    @EventHandler
    public void onEntityJoin(EntityJoinEvent event) {
        if (!Utils.nullCheck() || !autoKill.isToggled()) return;
        if (isSlimeSplitCandidate(event.entity)) {
            addTarget(event.entity);
            return;
        }
        if (reelScanUntil <= 0 || mc.level.getGameTime() > reelScanUntil) return;
        if (isSeaCreatureCandidate(event.entity)) {
            addTarget(event.entity);
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (!Utils.nullCheck()) return;

        Color renderColor = Color.CYAN;
        if (currentTarget != null && isValidLiveTarget(currentTarget)) {
            AABB box = currentTarget.getBoundingBox();
            RenderUtils.drawBoxFilled(
                    event.getMatrix(),
                    box,
                    new Color(renderColor.getRed(), renderColor.getGreen(), renderColor.getBlue(), 100),
                    false
            );
            RenderUtils.drawBox(event.getMatrix(), box, renderColor, 2f, false);
        }

        if (currentPath.isEmpty()) return;
        for (int i = 0; i < currentPath.size(); i++) {
            Vec3 point = currentPath.get(i);
            Color pointColor = i < pathIndex ? new Color(80, 220, 120) : renderColor;
            RenderUtils.drawBox(event.getMatrix(), pointBox(point), pointColor, 1.5f, false);

            if (i > 0) {
                Vec3 prev = currentPath.get(i - 1);
                RenderUtils.drawLine3D(
                        event.getMatrix(),
                        prev.add(0, 0.2, 0),
                        point.add(0, 0.2, 0),
                        renderColor,
                        2f,
                        false
                );
            }
        }
    }

    @Override
    public void onEnable() {
        if (!Utils.nullCheck()) return;

        startPos = mc.player.position();
        startYaw = mc.player.getYRot();
        startPitch = mc.player.getXRot();
        killState = KillState.IDLE;
        targets.clear();
        currentTarget = null;
        currentPath.clear();
        reelScanUntil = 0;
        lastReelPos = null;
        lastKilledSlimePos = null;
        slimeSplitScanUntil = 0;
        rotationSaved = false;
        itemSwitchTicks = 0;

        this.rodHand = null;
        ItemStack mainHand = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = mc.player.getItemInHand(InteractionHand.OFF_HAND);

        if (mainHand.getItem() instanceof FishingRodItem) {
            rodHand = InteractionHand.MAIN_HAND;
            rodSlot = mc.player.getInventory().getSelectedSlot();
        } else if (offHand.getItem() instanceof FishingRodItem) {
            rodHand = InteractionHand.OFF_HAND;
        }

        if (rodHand == null) {
            Utils.addModuleMessage(this.getTransName(), "You must holding your fishing rod!");
            this.disable();
            return;
        }

        if (antiAFK.isToggled()) {
            antiAFKThread = new Thread(() -> {
                while (isEnabled()) {
                    try {
                        Thread.sleep(15000 + (long) (new Random().nextDouble() * 10000 - 5000));
                        boolean direction = new Random().nextBoolean();
                        int ms = new Random().nextInt(0, 25) * 25;
                        float yawChange = 1.0f;

                        if (direction) {
                            mc.player.setYRot(mc.player.getYRot() + yawChange);
                            Thread.sleep(5000 + ms);
                            mc.player.setYRot(mc.player.getYRot() - yawChange);
                        } else {
                            mc.player.setYRot(mc.player.getYRot() - yawChange);
                            Thread.sleep(5000 + ms);
                            mc.player.setYRot(mc.player.getYRot() + yawChange);
                        }
                    } catch (Throwable e) {
                        if (e instanceof InterruptedException) break;
                        e.printStackTrace();
                    }
                }
            }, "antiAFK");
            antiAFKThread.start();
        }
    }

    @Override
    public void onDisable() {
        if (!failed && mc.player != null && mc.player.fishing != null && rodHand != null) {
            mc.gameMode.useItem(mc.player, rodHand);
            mc.player.swing(rodHand, mc.player.getItemInHand(rodHand).getAttackAnimation(), true);
        }
        releaseMovement();
        failed = false;
        currentMode = 0;
        killState = KillState.IDLE;
        targets.clear();
        currentTarget = null;
        currentPath.clear();
        if (antiAFKThread != null) {
            antiAFKThread.interrupt();
        }
    }

    private void checkRod() {
        if (killState != KillState.IDLE) return;
        if (rodHand == null) {
            failed = true;
            this.disable();
            return;
        }
        if (rodHand == InteractionHand.MAIN_HAND) {
            if (rodSlot == mc.player.getInventory().getSelectedSlot()) {
                return;
            }
            Utils.addModuleMessage(this.getTransName(), "§cItem changed, disable module!");
            failed = true;
            this.disable();
        }
    }

    private boolean rightClick() {
        if (mc.gameMode == null || rodHand == null) return false;
        mc.gameMode.useItem(mc.player, rodHand);
        mc.player.swing(rodHand, mc.player.getItemInHand(rodHand).getAttackAnimation(), true);
        return true;
    }

    private void beginReelScan() {
        lastReelPos = mc.player.position();
        reelScanUntil = mc.level.getGameTime() + REEL_SCAN_TICKS;
        scanExistingSeaCreatures();
    }

    private void tickAutoKill() {
        if (attackCooldown > 0) attackCooldown--;

        switch (killState) {
            case COLLECTING -> tickCollecting();
            case ABILITY -> tickAbilityKill();
            case CHASING -> tickMeleeKill();
            case RETURNING -> tickReturning();
            case RESTORE_ROTATION -> tickRestoreRotation();
            default -> {
            }
        }
    }

    private void tickCollecting() {
        releaseMovement();
        scanExistingSeaCreatures();
        if (mc.level.getGameTime() < reelScanUntil) return;
        reelScanUntil = 0;

        if (targets.size() < (int) triggerAmount.getInput()) {
            restoreRodAndThrow();
            return;
        }

        selectNextTarget();
        mc.player.getInventory().setSelectedSlot((int) weaponSlot.getInput() - 1);

        itemSwitchTicks = 3;
        rotationSaved = false;

        killState = useAbility.isToggled() ? KillState.ABILITY : KillState.CHASING;
    }

    private void tickAbilityKill() {
        if (!ensureTarget()) return;

        if (itemSwitchTicks > 0) {
            itemSwitchTicks--;
            return;
        }

        if (!rotationSaved) {
            preAbilityYaw = mc.player.getYRot();
            preAbilityPitch = mc.player.getXRot();
            rotationSaved = true;
        }

        mc.player.getInventory().setSelectedSlot((int) weaponSlot.getInput() - 1);

        if ((int) face.getValue() == 0) {
            Vec3 aim = currentTarget.position().add(0, Math.max(0.1, currentTarget.getBbHeight() / 3.0), 0);
            RotationUtils.aimByPos(aim, 4);
            float[] angles = RotationUtils.getYawPitchTo(mc.player.getEyePosition(), aim);
            if (rotationClose(angles[0], angles[1], 1.5f)) {
                if (isLookingAtBlock()) {
                    return;
                }
                useWeaponAbility();
                targets.remove(currentTarget);
                currentTarget = null;
            }
        } else {
            RotationUtils.setPitchTo(90f, 4);
            if (Math.abs(Mth.wrapDegrees(90f - mc.player.getXRot())) < 1.5f) {
                if (isLookingAtBlock()) {
                    return;
                }
                useWeaponAbility();
                targets.remove(currentTarget);
                currentTarget = null;
            }
        }
    }

    private void tickMeleeKill() {
        if (!ensureTarget()) return;

        Vec3 goal = findStandableBeside(currentTarget);
        if (goal == null) {
            targets.remove(currentTarget);
            currentTarget = null;
            return;
        }

        if (mc.player.distanceTo(currentTarget) > desiredAttackRange(currentTarget) - 0.2) {
            pathRecomputeTick++;
            if (currentPath.isEmpty() || pathRecomputeTick >= PATH_RECOMPUTE_TICKS
                    || lastPathGoal == null || lastPathGoal.distanceTo(goal) > PATH_GOAL_RECOMPUTE_DIST) {
                computePath(goal);
            }
            followPath(goal);
            return;
        }

        releaseMovement();
        Vec3 aim = currentTarget.position().add(0, currentTarget.getBbHeight() / 2.0, 0);
        RotationUtils.aimByPos(aim, 4);
        if (attackCooldown == 0 && canAttackCurrentTarget(aim)) {
            mc.player.attack(currentTarget);
            mc.player.connection.send(new ServerboundAttackPacket(currentTarget.getId()));
            mc.player.swing(InteractionHand.MAIN_HAND, mc.player.getItemInHand(InteractionHand.MAIN_HAND).getAttackAnimation(), true);
            attackCooldown = 5;
        }
    }

    private void tickReturning() {
        if (startPos == null) {
            killState = KillState.RESTORE_ROTATION;
            return;
        }

        if (isAtPos(startPos, RETURN_REACHED_XZ)) {
            releaseMovement();
            killState = KillState.RESTORE_ROTATION;
            return;
        }

        pathRecomputeTick++;
        if (currentPath.isEmpty() || pathRecomputeTick >= PATH_RECOMPUTE_TICKS
                || lastPathGoal == null || lastPathGoal.distanceTo(startPos) > PATH_GOAL_RECOMPUTE_DIST) {
            computePath(startPos);
        }
        followPath(startPos);
    }

    private void tickRestoreRotation() {
        releaseMovement();
        restoreTicks++;

        float targetYaw = useAbility.isToggled() ? preAbilityYaw : startYaw;
        float targetPitch = useAbility.isToggled() ? preAbilityPitch : startPitch;

        RotationUtils.setYawTo(targetYaw, 4);
        RotationUtils.setPitchTo(targetPitch, 4);
        if (rotationClose(targetYaw, targetPitch, 1.0f) || restoreTicks >= RESTORE_TIMEOUT_TICKS) {
            restoreRodAndThrow();
        }
    }

    private boolean ensureTarget() {
        scanSlimeSplits();
        pruneTargets();
        if (currentTarget == null || !isValidLiveTarget(currentTarget)) {
            selectNextTarget();
        }
        if (currentTarget == null) {
            beginRestore();
            return false;
        }
        return true;
    }

    private void beginRestore() {
        currentTarget = null;
        currentPath.clear();
        pathIndex = 0;
        pathRecomputeTick = 0;
        restoreTicks = 0;
        killState = useAbility.isToggled() ? KillState.RESTORE_ROTATION : KillState.RETURNING;
    }

    private void restoreRodAndThrow() {
        if (rodHand == InteractionHand.MAIN_HAND) {
            mc.player.getInventory().setSelectedSlot(rodSlot);
        }
        killState = KillState.IDLE;
        currentMode = autoThrow.isToggled() ? 1 : 0;
        rotationSaved = false;
    }

    private void selectNextTarget() {
        pruneTargets();
        currentTarget = targets.isEmpty() ? null : targets.get(0);
        currentPath.clear();
        pathIndex = 0;
        lastPathGoal = null;
    }

    private void scanExistingSeaCreatures() {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (isSeaCreatureCandidate(entity)) addTarget(entity);
        }
    }

    private boolean isSeaCreatureCandidate(Entity entity) {
        if (!(entity instanceof LivingEntity)) return false;
        if (entity == mc.player || entity instanceof ArmorStand) return false;
        if (!entity.isAlive()) return false;
        if (mc.player.distanceTo(entity) > SEA_CREATURE_KEEP_RANGE) return false;
        if (lastReelPos != null && entity.position().distanceTo(lastReelPos) > SEA_CREATURE_SPAWN_RANGE) return false;
        return true;
    }

    private boolean isValidLiveTarget(Entity entity) {
        return entity != null && entity.isAlive() && !entity.isRemoved()
                && mc.player.distanceTo(entity) <= SEA_CREATURE_KEEP_RANGE;
    }

    private void addTarget(Entity entity) {
        if (!targets.contains(entity)) targets.add(entity);
    }

    private void pruneTargets() {
        Iterator<Entity> it = targets.iterator();
        while (it.hasNext()) {
            Entity e = it.next();
            if (!isValidLiveTarget(e)) {
                rememberKilledSlime(e);
                it.remove();
            }
        }
    }

    private Vec3 findStandableBeside(Entity target) {
        BlockPos base = target.blockPosition();
        Vec3 best = null;
        double bestDist = Double.MAX_VALUE;
        for (int y = -2; y <= 2; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) continue;
                    BlockPos pos = base.offset(x, y, z);
                    if (!NavMeshGenerator.isWalkable(pos, mc.level)) continue;
                    Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                    double wantedRange = desiredAttackRange(target);
                    if (center.distanceTo(target.position()) > wantedRange - 0.2) continue;
                    double playerDist = center.distanceTo(mc.player.position());
                    if (playerDist < bestDist) {
                        bestDist = playerDist;
                        best = center;
                    }
                }
            }
        }
        return best;
    }

    private void computePath(Vec3 goal) {
        pathRecomputeTick = 0;
        lastPathGoal = goal;
        NavMeshPath path = PathfindingService.getInstance().findNavMeshPathSync(mc.player.position(), goal, mc.level, PATH_RANGE);
        currentPath = path.isFound() ? new ArrayList<>(path.getWaypoints()) : new ArrayList<>();
        pathIndex = currentPath.size() > 1 ? 1 : 0;
    }

    private void followPath(Vec3 fallbackGoal) {
        if (currentPath.isEmpty() || pathIndex >= currentPath.size()) {
            walkToward(fallbackGoal);
            return;
        }

        Vec3 target = currentPath.get(pathIndex);
        if (isAtPos(target, PATH_REACHED_XZ)) {
            pathIndex++;
            releaseMovement();
            return;
        }

        walkToward(target);
    }

    private void walkToward(Vec3 target) {
        Vec3 pos = mc.player.position();
        double dx = target.x - pos.x;
        double dz = target.z - pos.z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        mc.player.setYRot(yaw);
        mc.options.keyUp.setDown(true);
        mc.options.keyDown.setDown(false);
        mc.options.keyLeft.setDown(false);
        mc.options.keyRight.setDown(false);
        mc.options.keyJump.setDown(target.y - pos.y > 0.45);
        mc.options.keyShift.setDown(false);
    }

    private boolean isAtPos(Vec3 target, double xzThreshold) {
        Vec3 pos = mc.player.position();
        return xzDist(pos, target) <= xzThreshold
                && Math.abs(pos.y - target.y) <= 0.85;
    }

    private double xzDist(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private boolean rotationClose(float yaw, float pitch, float threshold) {
        return Math.abs(Mth.wrapDegrees(yaw - mc.player.getYRot())) <= threshold
                && Math.abs(Mth.wrapDegrees(pitch - mc.player.getXRot())) <= threshold;
    }

    private boolean canAttackCurrentTarget(Vec3 aim) {
        double range = desiredAttackRange(currentTarget);
        float[] rotations = new float[]{mc.player.getYRot(), mc.player.getXRot()};
        if (RotationUtils.isPossibleToHit(currentTarget, range, rotations)) return true;

        if (!isSmallTarget(currentTarget) || mc.player.distanceTo(currentTarget) > SMALL_TARGET_ATTACK_RANGE) {
            return false;
        }

        float[] targetRotations = RotationUtils.getYawPitchTo(mc.player.getEyePosition(), aim);
        return rotationClose(targetRotations[0], targetRotations[1], 4.0f);
    }

    private double desiredAttackRange(Entity target) {
        return isSmallTarget(target) ? SMALL_TARGET_ATTACK_RANGE : ATTACK_RANGE;
    }

    private boolean isSmallTarget(Entity target) {
        return target != null && (target.getBbHeight() <= 0.55 || target.getBbWidth() <= 0.55);
    }

    private void rememberKilledSlime(Entity entity) {
        if (entity instanceof Slime) {
            lastKilledSlimePos = entity.position();
            slimeSplitScanUntil = mc.level.getGameTime() + SLIME_SPLIT_SCAN_TICKS;
        }
    }

    private boolean isSlimeSplitCandidate(Entity entity) {
        return entity instanceof Slime
                && lastKilledSlimePos != null
                && mc.level.getGameTime() <= slimeSplitScanUntil
                && entity.isAlive()
                && entity.position().distanceTo(lastKilledSlimePos) <= 4.0
                && mc.player.distanceTo(entity) <= SEA_CREATURE_KEEP_RANGE;
    }

    private void scanSlimeSplits() {
        if (lastKilledSlimePos == null || mc.level.getGameTime() > slimeSplitScanUntil) return;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (isSlimeSplitCandidate(entity)) addTarget(entity);
        }
    }

    private boolean isLookingAtBlock() {
        return mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK;
    }

    private void useWeaponAbility() {
        if (mc.gameMode == null) return;
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        mc.player.swing(InteractionHand.MAIN_HAND, mc.player.getItemInHand(InteractionHand.MAIN_HAND).getAttackAnimation(), true);
    }

    private void releaseMovement() {
        mc.options.keyUp.setDown(false);
        mc.options.keyDown.setDown(false);
        mc.options.keyLeft.setDown(false);
        mc.options.keyRight.setDown(false);
        mc.options.keyJump.setDown(false);
        mc.options.keyShift.setDown(false);
    }

    private AABB pointBox(Vec3 point) {
        double r = 0.16;
        return new AABB(point.x - r, point.y + 0.02, point.z - r,
                point.x + r, point.y + 0.34, point.z + r);
    }

    private void checkForArmorStand() {
        if (!Utils.nullCheck() || mc.player.fishing == null) return;

        List<ArmorStand> armorStands = mc.level.getEntitiesOfClass(
                ArmorStand.class,
                mc.player.fishing.getBoundingBox().inflate(2.0),
                stand -> true
        );

        for (ArmorStand stand : armorStands) {
            if (stand.getCustomName() != null) {
                if (stand.getCustomName().getString().contains("!!!")) {
                    hookBiten = true;
                    break;
                }
            }
        }
    }

    private enum KillState {
        IDLE,
        COLLECTING,
        ABILITY,
        CHASING,
        RETURNING,
        RESTORE_ROTATION
    }
}