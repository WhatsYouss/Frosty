package xyz.whatsyouss.frosty.modules.impl.farming;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.utility.Utils;
import xyz.whatsyouss.frosty.utility.pathfinding.FlyPathfinder;
import xyz.whatsyouss.frosty.utility.pathfinding.NavMeshPath;

import java.util.*;

public class PestCleaner extends Module {

    private static final Map<Integer, double[]> PLOT_CENTERS = new HashMap<>();
    private static final double BARN_MIN_X = -52, BARN_MAX_X = 51;
    private static final double BARN_MIN_Z = -52, BARN_MAX_Z = 51;
    private static final double BARN_SAFE_Y = 80.0;
    private static final int MAX_RANGE = 512;
    private static final double ENTITY_RANGE = 3.0;
    private static final double WAYPOINT_ARRIVE = 2.5;
    private static final int FLY_ACTIVATE_WAIT = 6;
    private static final int FLY_CONFIRM_WAIT = FLY_ACTIVATE_WAIT + 12;
    private static final int PATH_RECOMPUTE_INTERVAL = 5;
    private static final float CRUISE_YAW_SPEED = 0.35f;
    private static final int SCAN_GIVE_UP_TICKS = 100;
    private static final double PLOT_CLICK_RADIUS = 5.0;
    private static final double OBSTACLE_PROBE = 3.0;
    private static final int SIDESTEP_TICKS = 10;
    private static final int RISE_TICKS = 10;
    private static final int STUCK_CHECK_TICKS = 15;
    private static final double STUCK_MIN_MOVE = 0.25;
    private static final int UNSTUCK_TICKS = 8;
    private static final double RETURN_CRUISE_HEIGHT = 15.0;
    private static final double DECEL_START_XZ = 12.0;
    private static final double FREEFALL_XZ = 0.25;
    private static final double ALIGN_MAX_SPEED = 0.06;
    private static final double LAND_Y_THRESHOLD = 0.5;
    private static final int RETURN_TIMEOUT_TICKS = 400;
    private static final int REWARP_COOLDOWN_TICKS = 20;
    private static final int REWARP_TIMEOUT_TICKS = 200;
    private static final double REWARP_DETECT_DIST = 8.0;
    private static final int AVOID_COOLDOWN_TICKS = 8;
    private static final double DESCEND_CORRECT_XZ = 0.12;
    private static final int DESCEND_PULSE_PERIOD = 3;
    public static int tickCounter = 0;

    static {
        PLOT_CENTERS.put(21, new double[]{-192, 65, -192});
        PLOT_CENTERS.put(13, new double[]{-96, 65, -192});
        PLOT_CENTERS.put(9, new double[]{0, 65, -192});
        PLOT_CENTERS.put(14, new double[]{96, 65, -192});
        PLOT_CENTERS.put(22, new double[]{192, 65, -192});
        PLOT_CENTERS.put(15, new double[]{-192, 65, -96});
        PLOT_CENTERS.put(5, new double[]{-96, 65, -96});
        PLOT_CENTERS.put(1, new double[]{0, 65, -96});
        PLOT_CENTERS.put(6, new double[]{96, 65, -96});
        PLOT_CENTERS.put(16, new double[]{192, 65, -96});
        PLOT_CENTERS.put(10, new double[]{-192, 65, 0});
        PLOT_CENTERS.put(2, new double[]{-96, 65, 0});
        PLOT_CENTERS.put(3, new double[]{96, 65, 0});
        PLOT_CENTERS.put(11, new double[]{192, 65, 0});
        PLOT_CENTERS.put(17, new double[]{-192, 65, 96});
        PLOT_CENTERS.put(7, new double[]{-96, 65, 96});
        PLOT_CENTERS.put(4, new double[]{0, 65, 96});
        PLOT_CENTERS.put(8, new double[]{96, 65, 96});
        PLOT_CENTERS.put(18, new double[]{192, 65, 96});
        PLOT_CENTERS.put(23, new double[]{-192, 65, 192});
        PLOT_CENTERS.put(19, new double[]{-96, 65, 192});
        PLOT_CENTERS.put(12, new double[]{0, 65, 192});
        PLOT_CENTERS.put(20, new double[]{96, 65, 192});
        PLOT_CENTERS.put(24, new double[]{192, 65, 192});
    }

    private final FlyPathfinder flyPathfinder = new FlyPathfinder();
    private final Queue<Integer> plotQueue = new LinkedList<>();
    private final Set<Integer> plotFirstClickDone = new HashSet<>();
    private State state = State.IDLE;
    private List<Vec3d> currentPath = Collections.emptyList();
    private int pathIndex = 0;
    private int flyActivateTick = 0;
    private int vacuumSlot = -1;
    private Entity targetEntity = null;
    private int recomputeTick = 0;
    private int scanTick = 0;
    private Vec3d returnTarget = null;
    private boolean resumeFarming = false;
    private boolean rewarpOnly = false;
    private Vec3d preRewarpPos = null;
    private int rewarpCooldown = 0;
    private int rewarpTimeout = 0;
    private Vec3d lastProgressPos = null;
    private int progressCheckTick = 0;
    private int avoidMode = 0;
    private int avoidTicksLeft = 0;
    private double avoidSidestepX = 0;
    private double avoidSidestepZ = 0;
    private int avoidCooldown = 0;
    private Vec3d cruiseWaypoint = null;
    private int returnTick = 0;

    private float landingYaw = 0f;
    private int descentPulseTick = 0;

    public PestCleaner() {
        super("PestCleaner", category.Farming);
    }

    private static double xzDist(Vec3d a, Vec3d b) {
        double dx = a.x - b.x, dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public void requestPestClean(Vec3d resumeWaypoint, boolean rewarpOnly) {
        this.returnTarget = resumeWaypoint;
        this.resumeFarming = true;
        this.rewarpOnly = rewarpOnly;
        if (!isEnabled()) toggle();
        else beginClean();
    }

    public void requestPestClean(Vec3d resumeWaypoint) {
        requestPestClean(resumeWaypoint, false);
    }

    @Override
    public void onEnable() {
        if (!Utils.nullCheck()) {
            toggle();
            return;
        }
        beginClean();
    }

    @Override
    public void onDisable() {
        releaseAll();
        state = State.IDLE;
        currentPath = Collections.emptyList();
        pathIndex = 0;
        targetEntity = null;
        plotQueue.clear();
        plotFirstClickDone.clear();
        resetAvoid();
        returnTick = 0;
    }

    private void beginClean() {
        int alive = parseAlive();
        if (alive == 0) {
            Utils.addModuleMessage(this.getName(), "§aNo pests found (Alive: 0)");
            toggle();
            return;
        }

        List<Integer> infested = parseInfestedPlots();
        if (infested.isEmpty()) {
            Utils.addModuleMessage(this.getName(), "§cCould not parse infested plots from Tab");
            toggle();
            return;
        }

        Vec3d playerPos = mc.player.getEntityPos();
        infested.sort(Comparator.comparingDouble(p -> distToPlot(p, playerPos)));
        plotQueue.clear();
        plotFirstClickDone.clear();
        plotQueue.addAll(infested);

        vacuumSlot = findVacuumSlot();
        if (vacuumSlot == -1) {
            Utils.addModuleMessage(this.getName(), "§cNo vacuum found in hotbar");
            toggle();
            return;
        }

        tickCounter = 0;
        flyActivateTick = 0;
        resetAvoid();
        state = State.ACTIVATING_FLY;
        Utils.addModuleMessage(this.getName(), "§bActivating flight...");
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck() || state == State.IDLE) return;
        tickCounter++;

        switch (state) {
            case ACTIVATING_FLY -> tickActivatingFly();
            case PATHING_TO_PLOT -> tickPathingToPlot();
            case SCANNING -> tickScanning();
            case CHASING -> tickChasing();
            case VACUUMING -> tickVacuuming();
            case RETURNING -> tickReturning();
            case ALIGNING -> tickAligning();
            case DESCENDING -> tickDescending();
            case REWARP_WAIT -> tickRewarpWait();
        }
    }

    private void tickActivatingFly() {
        flyActivateTick++;

        if (flyActivateTick == 1) mc.options.jumpKey.setPressed(true);
        else if (flyActivateTick == 2) mc.options.jumpKey.setPressed(false);
        else if (flyActivateTick == FLY_ACTIVATE_WAIT) mc.options.jumpKey.setPressed(true);
        else if (flyActivateTick == FLY_ACTIVATE_WAIT + 1) mc.options.jumpKey.setPressed(false);
        else if (flyActivateTick >= FLY_CONFIRM_WAIT) {
            if (!mc.player.getAbilities().flying && mc.player.isOnGround()) {
                Utils.addModuleMessage(this.getName(),
                        "§cNo cookie buff or mushroom soup found, disabled PestCleaner");
                toggle();
                return;
            }
            vacuumSlot = findVacuumSlot();
            if (vacuumSlot == -1) {
                Utils.addModuleMessage(this.getName(), "§cNo vacuum in hotbar, disabled PestCleaner");
                toggle();
                return;
            }
            mc.player.getInventory().setSelectedSlot(vacuumSlot);
            flyToNextPlot();
        }
    }

    private void flyToNextPlot() {
        if (plotQueue.isEmpty()) {
            finishCleaning();
            return;
        }

        Integer plotNum = plotQueue.peek();
        double[] center = PLOT_CENTERS.get(plotNum);
        if (center == null) {
            plotQueue.poll();
            flyToNextPlot();
            return;
        }

        Vec3d goal = new Vec3d(center[0], center[1] + 10, center[2]);
        Vec3d start = mc.player.getEntityPos();

        NavMeshPath path = flyPathfinder.findPath(start, goal, mc.world, MAX_RANGE);
        if (!path.isFound() || path.getWaypoints().isEmpty()) {
            Utils.addModuleMessage(this.getName(), "§eCan't path to Plot " + plotNum + ", skipping");
            plotQueue.poll();
            flyToNextPlot();
            return;
        }

        currentPath = path.getWaypoints();
        pathIndex = 0;
        resetAvoid();
        state = State.PATHING_TO_PLOT;
        Utils.addModuleMessage(this.getName(), "§bFlying to Plot " + plotNum);
    }

    private void tickPathingToPlot() {
        if (pathIndex >= currentPath.size()) {
            releaseMovement();
            scanTick = 0;
            resetAvoid();
            state = State.SCANNING;
            return;
        }

        Vec3d target = currentPath.get(pathIndex);
        Vec3d pos = mc.player.getEntityPos();

        if (pos.distanceTo(target) < WAYPOINT_ARRIVE) {
            pathIndex++;
            return;
        }

        cruise(target);
    }

    private void tickScanning() {
        releaseAll();
        scanTick++;

        if (parseAlive() == 0) {
            finishCleaning();
            return;
        }


        Integer currentPlot = plotQueue.peek();
        if (currentPlot != null && !plotFirstClickDone.contains(currentPlot)) {
            double[] center = PLOT_CENTERS.get(currentPlot);
            if (center != null) {
                Vec3d pos = mc.player.getEntityPos();
                double d = Math.sqrt(Math.pow(pos.x - center[0], 2) + Math.pow(pos.z - center[2], 2));
                if (d <= PLOT_CLICK_RADIUS) {
                    plotFirstClickDone.add(currentPlot);
                    vacuumSlot = findVacuumSlot();
                    if (vacuumSlot != -1) {
                        mc.player.getInventory().setSelectedSlot(vacuumSlot);
                        mc.options.attackKey.setPressed(true);

                    }
                }
            }
        }

        Entity pest = findNearestPest();
        if (pest != null) {
            targetEntity = pest;
            recomputeTick = 0;
            resetAvoid();
            state = State.CHASING;
        } else if (scanTick >= SCAN_GIVE_UP_TICKS) {
            plotQueue.poll();
            flyToNextPlot();
        }
    }

    private void tickChasing() {
        if (targetEntity == null || !targetEntity.isAlive()) {
            targetEntity = null;
            scanTick = 0;
            state = State.SCANNING;
            return;
        }

        Vec3d pos = mc.player.getEntityPos();
        Vec3d entPos = targetEntity.getEntityPos();

        if (pos.distanceTo(entPos) <= ENTITY_RANGE) {
            releaseMovement();
            resetAvoid();
            state = State.VACUUMING;
            return;
        }

        recomputeTick++;
        if (recomputeTick >= PATH_RECOMPUTE_INTERVAL) {
            recomputeTick = 0;
            Entity nearest = findNearestPest();
            if (nearest != null) targetEntity = nearest;
        }

        cruise(entPos);
    }

    private void tickVacuuming() {
        if (targetEntity == null || !targetEntity.isAlive()) {
            targetEntity = null;
            mc.options.useKey.setPressed(false);
            if (parseAlive() == 0) finishCleaning();
            else {
                scanTick = 0;
                state = State.SCANNING;
            }
            return;
        }

        Vec3d entPos = targetEntity.getEntityPos().add(0, targetEntity.getHeight() / 2.0, 0);
        Vec3d pos = mc.player.getEyePos();

        if (pos.distanceTo(entPos) > ENTITY_RANGE + 1.5) {
            mc.options.useKey.setPressed(false);
            resetAvoid();
            state = State.CHASING;
            return;
        }

        if (mc.player.isOnGround() && !mc.player.getAbilities().flying) {
            mc.player.getAbilities().flying = true;
            mc.player.sendAbilitiesUpdate();
        }

        aimAt(entPos);

        vacuumSlot = findVacuumSlot();
        if (vacuumSlot == -1) {
            Utils.addModuleMessage(this.getName(), "§cVacuum gone from hotbar");
            toggle();
            return;
        }
        mc.player.getInventory().setSelectedSlot(vacuumSlot);
        mc.options.useKey.setPressed(true);
    }

    private void finishCleaning() {
        releaseAll();
        Utils.addModuleMessage(this.getName(), "§aPests cleared!");

        if (!resumeFarming) {
            finalDisable();
            return;
        }

        if (rewarpOnly) {
            preRewarpPos = mc.player.getEntityPos();
            rewarpCooldown = REWARP_COOLDOWN_TICKS;
            rewarpTimeout = 0;
            mc.player.networkHandler.sendChatCommand("warp garden");
            state = State.REWARP_WAIT;
            Utils.addModuleMessage(this.getName(), "§b/warp garden sent, waiting for teleport...");
            return;
        }

        if (returnTarget == null) {
            finalDisable();
            return;
        }

        ensureFlying();

        Vec3d pos = mc.player.getEntityPos();
        double cruiseY = Math.max(pos.y, returnTarget.y) + RETURN_CRUISE_HEIGHT;
        cruiseWaypoint = new Vec3d(returnTarget.x, cruiseY, returnTarget.z);
        returnTick = 0;
        resetAvoid();
        state = State.RETURNING;
    }

    private void tickRewarpWait() {
        releaseAll();
        rewarpCooldown = Math.max(0, rewarpCooldown - 1);
        rewarpTimeout++;

        if (rewarpTimeout >= REWARP_TIMEOUT_TICKS) {
            Utils.addModuleMessage(this.getName(), "§eWarp timed out, resuming farming...");
            finalDisable();
            return;
        }
        if (rewarpCooldown > 0) return;

        if (preRewarpPos != null) {
            Vec3d now = mc.player.getEntityPos();
            double d = xzDist(now, preRewarpPos);
            if (d >= REWARP_DETECT_DIST) {
                Utils.addModuleMessage(this.getName(), "§aTeleported — resuming farming...");
                finalDisable();
            }
        }
    }

    private void tickReturning() {
        returnTick++;
        if (returnTick >= RETURN_TIMEOUT_TICKS) {
            forceLand();
            return;
        }

        ensureFlying();

        Vec3d pos = mc.player.getEntityPos();
        double xzD = xzDist(pos, cruiseWaypoint);
        double dyUp = cruiseWaypoint.y - pos.y;


        if (pos.y < cruiseWaypoint.y - 1.5) {

            cruiseWithObstacleAvoid(cruiseWaypoint);
            return;
        }


        if (xzD > DECEL_START_XZ) {

            cruiseWithObstacleAvoid(cruiseWaypoint);
            return;
        }


        Vec3d vel = mc.player.getVelocity();
        double xzSpd = Math.sqrt(vel.x * vel.x + vel.z * vel.z);


        double targetSpd = Math.max(0, 0.25 * (xzD - FREEFALL_XZ) / (DECEL_START_XZ - FREEFALL_XZ));


        holdAltitude(cruiseWaypoint.y);


        double dxT = cruiseWaypoint.x - pos.x;
        double dzT = cruiseWaypoint.z - pos.z;
        float wantYaw = (float) Math.toDegrees(Math.atan2(-dxT, dzT));
        float cy = mc.player.getYaw();
        mc.player.setYaw(cy + MathHelper.wrapDegrees(wantYaw - cy) * CRUISE_YAW_SPEED);

        if (xzD <= FREEFALL_XZ && xzSpd <= ALIGN_MAX_SPEED) {

            releaseMovement();
            landingYaw = mc.player.getYaw();
            state = State.ALIGNING;
            returnTick = 0;
            return;
        }

        if (xzSpd < targetSpd - 0.02) {

            mc.options.forwardKey.setPressed(true);
            mc.options.backKey.setPressed(false);
        } else if (xzSpd > targetSpd + 0.02) {

            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(true);
        } else {
            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(false);
        }
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
    }

    private void tickAligning() {
        returnTick++;
        if (returnTick >= RETURN_TIMEOUT_TICKS) {
            forceLand();
            return;
        }

        ensureFlying();


        mc.player.setYaw(landingYaw);

        Vec3d pos = mc.player.getEntityPos();
        Vec3d vel = mc.player.getVelocity();
        double xzD = xzDist(pos, cruiseWaypoint);
        double xzSpd = Math.sqrt(vel.x * vel.x + vel.z * vel.z);


        holdAltitude(cruiseWaypoint.y);

        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);


        if (xzD > FREEFALL_XZ * 6) {
            state = State.RETURNING;
            returnTick = 0;
            return;
        }


        if (xzSpd > 0.008) {

            double invVx = (vel.x == 0 && vel.z == 0) ? 0 : -vel.x;
            double invVz = (vel.x == 0 && vel.z == 0) ? 0 : -vel.z;
            pressWorldKey(invVx, invVz, landingYaw);

            return;
        }


        releaseMovement();
        stopFlying();
        state = State.DESCENDING;
        returnTick = 0;
    }

    private void tickDescending() {
        returnTick++;
        if (returnTick >= RETURN_TIMEOUT_TICKS) {
            forceLand();
            return;
        }


        mc.player.setYaw(landingYaw);

        releaseMovement();

        Vec3d pos = mc.player.getEntityPos();
        double errX = returnTarget.x - pos.x;
        double errZ = returnTarget.z - pos.z;
        double xzErr = Math.sqrt(errX * errX + errZ * errZ);

        descentPulseTick++;
        if (descentPulseTick >= DESCEND_PULSE_PERIOD) {
            descentPulseTick = 0;

            if (xzErr > DESCEND_CORRECT_XZ) {

                double wantX, wantZ;
                if (Math.abs(errX) >= Math.abs(errZ)) {
                    wantX = errX > 0 ? 1.0 : -1.0;
                    wantZ = 0.0;
                } else {
                    wantX = 0.0;
                    wantZ = errZ > 0 ? 1.0 : -1.0;
                }
                pressWorldKey(wantX, wantZ, landingYaw);

            }
        }


        if (mc.player.isOnGround()) {
            double landErr = Math.sqrt(errX * errX + errZ * errZ);

            if (landErr > 0.4) {
                ensureFlying();
                precisionNudge();
            } else {
                forceLand();
            }
        }
    }


    private void precisionNudge() {
        Vec3d pos = mc.player.getEntityPos();
        double errX = returnTarget.x - pos.x;
        double errZ = returnTarget.z - pos.z;

        if (Math.abs(errX) >= Math.abs(errZ)) {
            pressWorldKey(errX > 0 ? 1 : -1, 0, landingYaw);
        } else {
            pressWorldKey(0, errZ > 0 ? 1 : -1, landingYaw);
        }


    }

    private void forceLand() {
        releaseAll();
        stopFlying();
        finalDisable();
    }

    private void finalDisable() {
        releaseAll();
        if (resumeFarming) {
            FarmingMacro fm = ModuleManager.farmingMacro;
            if (fm != null && fm.isEnabled()) fm.resumeFromPestClean();
            resumeFarming = false;
            returnTarget = null;
        }
        toggle();
    }


    private void cruise(Vec3d target) {
        cruiseWithObstacleAvoid(target);
    }

    private void cruiseWithObstacleAvoid(Vec3d target) {
        Vec3d pos = mc.player.getEntityPos();


        if (mc.player.isOnGround() && !mc.player.getAbilities().flying) {
            mc.player.getAbilities().flying = true;
            mc.player.sendAbilitiesUpdate();
            avoidMode = 0;
            avoidTicksLeft = 0;
        }


        if (pos.y < BARN_SAFE_Y) {
            Vec3d detour = barnDetour(pos, target);
            if (detour != null) {
                target = detour;
            }
        }


        if (avoidTicksLeft > 0) {
            avoidTicksLeft--;
            applyAvoidManoeuvre(pos, target);
            tickProgressCheck(pos);
            if (avoidTicksLeft == 0) avoidCooldown = AVOID_COOLDOWN_TICKS;
            return;
        }

        if (avoidCooldown > 0) {
            avoidCooldown--;
            flyTowardDirect(target);
            tickProgressCheck(pos);
            return;
        }


        double dx = target.x - pos.x;
        double dz = target.z - pos.z;
        double hDist = Math.sqrt(dx * dx + dz * dz);

        if (hDist > 0.5) {
            double ux = dx / hDist, uz = dz / hDist;


            if (probeBlocked(pos, ux, uz, OBSTACLE_PROBE)) {

                double lx = uz, lz = -ux;
                boolean leftClear = !probeBlocked(pos, lx, lz, OBSTACLE_PROBE);

                double rx = -uz, rz = ux;
                boolean rightClear = !probeBlocked(pos, rx, rz, OBSTACLE_PROBE);

                if (leftClear) {
                    avoidMode = 1;
                    avoidTicksLeft = SIDESTEP_TICKS;

                    avoidSidestepX = uz;
                    avoidSidestepZ = -ux;
                } else if (rightClear) {
                    avoidMode = 2;
                    avoidTicksLeft = SIDESTEP_TICKS;
                    avoidSidestepX = -uz;
                    avoidSidestepZ = ux;
                } else {

                    avoidMode = 3;
                    avoidTicksLeft = RISE_TICKS;
                    avoidSidestepX = 0;
                    avoidSidestepZ = 0;
                }
                applyAvoidManoeuvre(pos, target);
                tickProgressCheck(pos);
                return;
            }
        }


        progressCheckTick++;
        if (progressCheckTick >= STUCK_CHECK_TICKS) {
            progressCheckTick = 0;
            if (lastProgressPos != null) {
                double moved = xzDist(pos, lastProgressPos);
                if (moved < STUCK_MIN_MOVE) {
                    avoidMode = 4;
                    avoidTicksLeft = UNSTUCK_TICKS;
                    applyAvoidManoeuvre(pos, target);
                    lastProgressPos = pos;
                    return;
                }
            }
            lastProgressPos = pos;
        }

        flyTowardDirect(target);
    }

    private void applyAvoidManoeuvre(Vec3d pos, Vec3d target) {
        double dx = target.x - pos.x;
        double dz = target.z - pos.z;
        double hDist = Math.sqrt(dx * dx + dz * dz);
        double ux = hDist > 0.01 ? dx / hDist : 1;
        double uz = hDist > 0.01 ? dz / hDist : 0;

        switch (avoidMode) {
            case 1, 2 -> {
                float sideYaw = (float) Math.toDegrees(Math.atan2(-avoidSidestepX, avoidSidestepZ));
                mc.player.setYaw(sideYaw);
                mc.options.forwardKey.setPressed(true);
                mc.options.backKey.setPressed(false);
                mc.options.leftKey.setPressed(false);
                mc.options.rightKey.setPressed(false);
                mc.options.jumpKey.setPressed(false);
                mc.options.sneakKey.setPressed(false);
            }
            case 3 -> {
                flyTowardDirect(new Vec3d(target.x, pos.y + 3, target.z));
            }
            case 4 -> {
                float reverseYaw = (float) Math.toDegrees(Math.atan2(ux, -uz));
                mc.player.setYaw(reverseYaw);
                mc.options.forwardKey.setPressed(true);
                mc.options.backKey.setPressed(false);
                mc.options.leftKey.setPressed(false);
                mc.options.rightKey.setPressed(false);
                mc.options.jumpKey.setPressed(true);
                mc.options.sneakKey.setPressed(false);
            }
            default -> flyTowardDirect(target);
        }
    }

    private void faceDirection(double worldX, double riseFlag, double worldZ) {
        float wantYaw = (float) Math.toDegrees(Math.atan2(-worldX, worldZ));
        float cy = mc.player.getYaw();
        mc.player.setYaw(cy + MathHelper.wrapDegrees(wantYaw - cy) * CRUISE_YAW_SPEED);

        mc.options.forwardKey.setPressed(true);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);

        boolean rise = (riseFlag > 0.5);
        mc.options.jumpKey.setPressed(rise);
        mc.options.sneakKey.setPressed(false);
    }

    private void pressForward(double riseFlag) {
        mc.options.forwardKey.setPressed(true);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.jumpKey.setPressed(riseFlag > 0.5);
        mc.options.sneakKey.setPressed(false);
    }

    private boolean probeBlocked(Vec3d pos, double ux, double uz, double dist) {
        double px = pos.x + ux * dist;
        double pz = pos.z + uz * dist;
        BlockPos feet = BlockPos.ofFloored(px, pos.y + 0.1, pz);
        BlockPos head = BlockPos.ofFloored(px, pos.y + 0.1 + 1.0, pz);
        return isHardBlock(feet) || isHardBlock(head);
    }

    private Vec3d barnDetour(Vec3d pos, Vec3d target) {
        if (pos.x >= BARN_MIN_X && pos.x <= BARN_MAX_X &&
                pos.z >= BARN_MIN_Z && pos.z <= BARN_MAX_Z) return null;
        if (target.x >= BARN_MIN_X && target.x <= BARN_MAX_X &&
                target.z >= BARN_MIN_Z && target.z <= BARN_MAX_Z) return null;

        boolean crossesBarn = false;
        int STEPS = 10;
        for (int i = 1; i < STEPS; i++) {
            double t = (double) i / STEPS;
            double ix = pos.x + (target.x - pos.x) * t;
            double iz = pos.z + (target.z - pos.z) * t;
            if (ix >= BARN_MIN_X && ix <= BARN_MAX_X && iz >= BARN_MIN_Z && iz <= BARN_MAX_Z) {
                crossesBarn = true;
                break;
            }
        }
        if (!crossesBarn) return null;

        double[] corners = {
                BARN_MIN_X - 4, BARN_MIN_Z - 4,
                BARN_MAX_X + 4, BARN_MIN_Z - 4,
                BARN_MIN_X - 4, BARN_MAX_Z + 4,
                BARN_MAX_X + 4, BARN_MAX_Z + 4,
        };
        double bestDist = Double.MAX_VALUE;
        double bx = 0, bz = 0;
        for (int i = 0; i < corners.length; i += 2) {
            double cx = corners[i], cz = corners[i + 1];
            double d = Math.sqrt(Math.pow(cx - pos.x, 2) + Math.pow(cz - pos.z, 2))
                    + Math.sqrt(Math.pow(cx - target.x, 2) + Math.pow(cz - target.z, 2));
            if (d < bestDist) {
                bestDist = d;
                bx = cx;
                bz = cz;
            }
        }
        return new Vec3d(bx, target.y, bz);
    }

    private void flyTowardDirect(Vec3d target) {
        Vec3d pos = mc.player.getEntityPos();
        double dx = target.x - pos.x;
        double dy = target.y - pos.y;
        double dz = target.z - pos.z;
        double hDist = Math.sqrt(dx * dx + dz * dz);

        float wantYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float cy = mc.player.getYaw();
        mc.player.setYaw(cy + MathHelper.wrapDegrees(wantYaw - cy) * CRUISE_YAW_SPEED);

        mc.options.forwardKey.setPressed(true);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);

        if (dy > 1.5) {
            mc.options.jumpKey.setPressed(true);
            mc.options.sneakKey.setPressed(false);
        } else if (dy < -1.5) {
            mc.options.sneakKey.setPressed(true);
            mc.options.jumpKey.setPressed(false);
        } else {
            mc.options.jumpKey.setPressed(false);
            mc.options.sneakKey.setPressed(false);
        }
    }

    private void holdAltitude(double targetY) {
        double dy = targetY - mc.player.getEntityPos().y;
        if (dy > 0.3) {
            mc.options.jumpKey.setPressed(true);
            mc.options.sneakKey.setPressed(false);
        } else if (dy < -0.3) {
            mc.options.sneakKey.setPressed(true);
            mc.options.jumpKey.setPressed(false);
        } else {
            mc.options.jumpKey.setPressed(false);
            mc.options.sneakKey.setPressed(false);
        }
    }

    private void pressWorldKey(double wX, double wZ, float lockedYaw) {
        double mag = Math.sqrt(wX * wX + wZ * wZ);
        if (mag < 1e-6) return;
        wX /= mag;
        wZ /= mag;

        float yawRad = (float) Math.toRadians(lockedYaw);
        double fwdX = -Math.sin(yawRad), fwdZ = Math.cos(yawRad);
        double rgtX = Math.cos(yawRad), rgtZ = Math.sin(yawRad);

        double dotF = wX * fwdX + wZ * fwdZ;
        double dotB = -dotF;
        double dotR = wX * rgtX + wZ * rgtZ;
        double dotL = -dotR;

        double best = Math.max(Math.max(dotF, dotB), Math.max(dotR, dotL));
        if (best < 0.01) return;

        if (dotF >= best) mc.options.forwardKey.setPressed(true);
        else if (dotB >= best) mc.options.backKey.setPressed(true);
        else if (dotR >= best) mc.options.rightKey.setPressed(true);
        else if (dotL >= best) mc.options.leftKey.setPressed(true);
    }

    private void aimAt(Vec3d target) {
        Vec3d eye = mc.player.getEyePos();
        double dx = target.x - eye.x, dy = target.y - eye.y, dz = target.z - eye.z;
        double dist = Math.sqrt(dx * dx + dz * dz);

        float cy = mc.player.getYaw(), cp = mc.player.getPitch();
        mc.player.setYaw(cy + MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(-dx, dz)) - cy) * 0.4f);
        mc.player.setPitch(cp + ((float) Math.toDegrees(-Math.atan2(dy, Math.max(dist, 0.01))) - cp) * 0.4f);
    }

    private void ensureFlying() {
        if (!mc.player.getAbilities().flying) {
            mc.player.getAbilities().flying = true;
            mc.player.sendAbilitiesUpdate();
        }
    }

    private void stopFlying() {
        if (mc.player.getAbilities().flying) {
            mc.player.getAbilities().flying = false;
            mc.player.sendAbilitiesUpdate();
        }
    }


    private void tickProgressCheck(Vec3d pos) {
        progressCheckTick++;
        if (progressCheckTick >= STUCK_CHECK_TICKS) {
            progressCheckTick = 0;
            lastProgressPos = pos;
        }
    }

    private void resetAvoid() {
        lastProgressPos = null;
        progressCheckTick = 0;
        avoidMode = 0;
        avoidTicksLeft = 0;
        avoidCooldown = 0;
        avoidSidestepX = 0;
        avoidSidestepZ = 0;
    }


    private boolean isHardBlock(BlockPos bp) {
        if (!mc.world.isChunkLoaded(bp.getX() >> 4, bp.getZ() >> 4)) return false;
        var bs = mc.world.getBlockState(bp);
        if (!bs.getFluidState().isEmpty()) return false;
        return !bs.getCollisionShape(mc.world, bp).isEmpty();
    }

    private void releaseMovement() {
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        mc.options.sneakKey.setPressed(false);
    }

    private void releaseAll() {
        releaseMovement();
        mc.options.attackKey.setPressed(false);
        mc.options.useKey.setPressed(false);
    }

    private int parseAlive() {
        if (mc.getNetworkHandler() == null) return 0;
        for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
            if (entry.getDisplayName() == null) continue;
            String line = entry.getDisplayName().getString().trim();
            if (line.toLowerCase().contains("alive:")) {
                String after = line.substring(line.toLowerCase().indexOf("alive:") + 6).trim();
                String num = after.split("[^0-9]")[0];
                try {
                    return Integer.parseInt(num);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return 0;
    }

    private List<Integer> parseInfestedPlots() {
        if (mc.getNetworkHandler() == null) return Collections.emptyList();
        List<Integer> result = new ArrayList<>();
        for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
            if (entry.getDisplayName() == null) continue;
            String line = entry.getDisplayName().getString().trim();
            if (line.toLowerCase().contains("plot")) {
                for (String tok : line.split("[^0-9]+")) {
                    if (tok.isEmpty()) continue;
                    try {
                        int n = Integer.parseInt(tok);
                        if (PLOT_CENTERS.containsKey(n)) result.add(n);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return result;
    }

    private Entity findNearestPest() {
        Vec3d pos = mc.player.getEntityPos();
        Entity nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof BatEntity) && !(e instanceof SilverfishEntity)) continue;
            double d = e.getEntityPos().distanceTo(pos);
            if (d < minDist) {
                minDist = d;
                nearest = e;
            }
        }
        return nearest;
    }

    private int findVacuumSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            if (stack.getName().getString().toLowerCase().contains("vacuum")) return i;
        }
        return -1;
    }

    private double distToPlot(int plotNum, Vec3d from) {
        double[] c = PLOT_CENTERS.get(plotNum);
        if (c == null) return Double.MAX_VALUE;
        double dx = c[0] - from.x, dz = c[2] - from.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private enum State {
        IDLE, ACTIVATING_FLY, PATHING_TO_PLOT, SCANNING, CHASING, VACUUMING,
        RETURNING,
        ALIGNING,
        DESCENDING,
        REWARP_WAIT
    }
}