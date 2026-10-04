package xyz.whatsyouss.frosty.modules.impl.farming;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.events.impl.ReceiveMessageEvent;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.mixin.accessor.KeyMappingAccessor;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.RenderUtils;
import xyz.whatsyouss.frosty.utility.Utils;
import xyz.whatsyouss.frosty.utility.pathfinding.FlyPathfinder;
import xyz.whatsyouss.frosty.utility.pathfinding.NavMeshPath;
import xyz.whatsyouss.frosty.utility.pathfinding.PestFlightMotion;

import java.awt.Color;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PestCleaner extends Module {

    private static final Map<Integer, double[]> PLOT_CENTERS = new HashMap<>();
    private static final Color PLOT_PATH_COLOR = new Color(0, 210, 255);
    private static final Color CHASE_PATH_COLOR = new Color(255, 130, 55);
    private static final Color RETURN_PATH_COLOR = new Color(180, 90, 255);
    private static final Color AVOID_PATH_COLOR = new Color(255, 225, 45);
    private static final Color PATH_END_COLOR = new Color(255, 70, 90);
    private static final double BARN_MIN_X = -52, BARN_MAX_X = 51;
    private static final double BARN_MIN_Z = -52, BARN_MAX_Z = 51;
    private static final double BARN_SAFE_Y = 80.0;
    private double vacuumRange = 3.0;
    private static final double APPROACH_SPEED = 0.35;
    private static final int PLOT_TP_WAIT_TICKS = 50;
    private static final long TRUSTED_PLOT_TTL_MS = 120_000L;
    private static final double PLOT_ARRIVAL_MARGIN = 3.0;
    private static final int MAX_PLOT_SWEEPS = 2;
    private static final int MAX_SCAN_WAYPOINTS = 10;
    private static final long STARTUP_FINISH_GRACE_MS = 5_000L;
    private static final int TAB_FINISH_CONFIRM_TICKS = 10;
    private static final long CLEAN_TIMEOUT_MS = 300_000L;
    private static final double[][] SCAN_OFFSETS = {{30,30},{-30,30},{-30,-30},{30,-30}};
    private static final Pattern PESTS_ALIVE_PATTERN =
            Pattern.compile("(?i)(?:Pests|Alive):?\\s*\\(?(\\d+)\\)?");
    private static final Pattern INFESTED_PLOTS_PATTERN =
            Pattern.compile("(?i)Plots?:\\s*(.+)");
    private static final Pattern PLOT_TELEPORT_PATTERN =
            Pattern.compile("(?i)Teleported you to Plot\\s*-\\s*(\\d+)");
    private static final Pattern SIDEBAR_PLOT_PATTERN =
            Pattern.compile("(?i)Plot\\s*[:\\-#]\\s*(\\d+)");
    private static final double WAYPOINT_ARRIVE = 0.8;
    private static final int FLY_ACTIVATE_WAIT = 6;
    private static final int FLY_CONFIRM_WAIT = FLY_ACTIVATE_WAIT + 12;
    private static final int CHASE_REPATH_INTERVAL = 12;
    private static final int CHASE_PATH_RANGE = 96;
    private static final double CHASE_GOAL_TOLERANCE = 2.5;
    private static final double CHASE_GOAL_MOVE_THRESHOLD = 1.25;
    private static final double CHASE_WAYPOINT_ARRIVE = 0.9;
    private static final float CRUISE_YAW_SPEED = 0.35f;
    private static final double OBSTACLE_PROBE = 3.0;
    private static final double LOCAL_ESCAPE_MIN_DISTANCE = 1.25;
    private static final double LOCAL_ESCAPE_MAX_DISTANCE = 3.5;
    private static final int STUCK_CHECK_TICKS = 15;
    private static final double STUCK_MIN_MOVE = 0.25;
    private static final double RETURN_CRUISE_HEIGHT = 15.0;
    private static final double DECEL_START_XZ = 12.0;
    private static final double FREEFALL_XZ = 0.25;
    private static final double ALIGN_MAX_SPEED = 0.06;
    private static final int RETURN_TIMEOUT_TICKS = 400;
    private static final int REWARP_COOLDOWN_TICKS = 20;
    private static final int REWARP_TIMEOUT_TICKS = 200;
    private static final double REWARP_DETECT_DIST = 8.0;
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
    private final SliderSetting followDistance;
    private final SliderSetting hoverHeight;
    private final Queue<Integer> plotQueue = new LinkedList<>();
    private final Set<Integer> exhaustedPlots = new HashSet<>();
    private final Set<Integer> deferredTargets = new HashSet<>();
    private final Set<Integer> abandonedTargets = new HashSet<>();
    private final Map<Integer, Integer> targetFailures = new HashMap<>();
    private int plotTeleportTick;
    private int plotTeleportAttempts;
    private Vec3 plotTeleportOrigin;
    private long plotTeleportSentAt;
    private boolean plotTeleportChatConfirmed;
    private int trustedPlot = -1;
    private long trustedPlotExpiresAt;
    private int scanPointIndex;
    private int scanSweepCount;
    private int scanWaypointCount;
    private int emptyPlotTabTicks;
    private Vec3 scanWaypoint;
    private long stateEnteredAt;
    private long runStartedAt;
    private int zeroAliveTicks;
    private int lastTargetId = -1;
    private Vec3 lastTargetPosition;
    private Vec3 targetVelocity = Vec3.ZERO;
    private final PestTrackerTrail trackerTrail = new PestTrackerTrail();
    private long trackerRequestedAt;
    private long trackerAwaitingAttackAt;
    private long trackerNextUseAt;
    private int trackerReadyTick;
    private int guidedWaypoints;
    private Vec3 previousGuidedWaypoint;
    private boolean trackerFinishedForScan;
    private State state = State.IDLE;
    private List<Vec3> currentPath = Collections.emptyList();
    private int pathIndex = 0;
    private int flyActivateTick = 0;
    private int vacuumSlot = -1;
    private Entity targetEntity = null;
    private List<Vec3> chasePath = Collections.emptyList();
    private int chasePathIndex = 0;
    private int chaseRepathTick = 0;
    private Vec3 chasePathGoal = null;
    private int scanTick = 0;
    private Vec3 returnTarget = null;
    private boolean resumeFarming = false;
    private boolean rewarpOnly = false;
    private Vec3 preRewarpPos = null;
    private int rewarpCooldown = 0;
    private int rewarpTimeout = 0;
    private Vec3 lastProgressPos = null;
    private int progressCheckTick = 0;
    private Vec3 localAvoidTarget = null;
    private Vec3 lastEscapeDirection = null;
    private int stuckLevel = 0;
    private Vec3 cruiseWaypoint = null;
    private int returnTick = 0;

    private float landingYaw = 0f;
    private int descentPulseTick = 0;

    public PestCleaner() {
        super("PestCleaner", "害虫清理", category.Farming);
        this.registerSetting(followDistance = new SliderSetting("Vacuum follow distance", 2.0, 1.0, 5.0, 0.1, "吸虫跟随距离"));
        this.registerSetting(hoverHeight = new SliderSetting("Vacuum hover height", 1.8, 1.0, 3.0, 0.1, "吸虫悬停高度"));
    }

    private static double xzDist(Vec3 a, Vec3 b) {
        double dx = a.x - b.x, dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public void requestPestClean(Vec3 resumeWaypoint, boolean rewarpOnly) {
        this.returnTarget = resumeWaypoint;
        this.resumeFarming = true;
        this.rewarpOnly = rewarpOnly;
        if (!isEnabled()) toggle();
        else beginClean();
    }

    public void requestPestClean(Vec3 resumeWaypoint) {
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
        if (resumeFarming) {
            FarmingMacro macro = ModuleManager.farmingMacro;
            if (macro != null && macro.isEnabled()) macro.resumeFromPestClean();
            resumeFarming = false;
            returnTarget = null;
        }
        state = State.IDLE;
        currentPath = Collections.emptyList();
        pathIndex = 0;
        targetEntity = null;
        resetChasePath();
        plotQueue.clear();
        trustedPlot = -1;
        trustedPlotExpiresAt = 0L;
        plotTeleportChatConfirmed = false;
        exhaustedPlots.clear();
        deferredTargets.clear();
        abandonedTargets.clear();
        targetFailures.clear();
        trackerTrail.reset();
        trackerAwaitingAttackAt = 0;
        resetAvoid();
        returnTick = 0;
    }

    private void beginClean() {
        int alive = parseAlive();
        if (alive == 0) {
            Utils.addModuleMessage(this.getName(), "§aNo pests found (Alive: 0)");
            finalDisable();
            return;
        }

        List<Integer> infested = parseInfestedPlots();
        if (infested.isEmpty()) {
            Utils.addModuleMessage(this.getName(), "§cCould not parse infested plots from Tab");
            finalDisable();
            return;
        }

        Vec3 playerPos = mc.player.position();
        infested.sort(Comparator.comparingInt(p -> isOnPlot(p, playerPos) ? 0 : 1));
        plotQueue.clear();
        trustedPlot = -1;
        trustedPlotExpiresAt = 0L;
        plotTeleportChatConfirmed = false;
        exhaustedPlots.clear();
        deferredTargets.clear();
        abandonedTargets.clear();
        targetFailures.clear();
        trackerTrail.reset();
        guidedWaypoints = 0;
        trackerRequestedAt = 0;
        trackerAwaitingAttackAt = 0;
        trackerReadyTick = 0;
        trackerFinishedForScan = false;
        plotQueue.addAll(infested);

        vacuumSlot = findVacuumSlot();
        if (vacuumSlot == -1) {
            Utils.addModuleMessage(this.getName(), "§cNo vacuum found in hotbar");
            finalDisable();
            return;
        }
        vacuumRange = detectVacuumRange(vacuumSlot);

        tickCounter = 0;
        runStartedAt = System.currentTimeMillis();
        zeroAliveTicks = 0;
        flyActivateTick = 0;
        targetEntity = null;
        resetChasePath();
        resetAvoid();
        state = State.ACTIVATING_FLY;
        stateEnteredAt = System.currentTimeMillis();
        Utils.addModuleMessage(this.getName(), "§bActivating flight...");
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck() || state == State.IDLE) return;
        if (mc.screen != null) {
            releaseAll();
            return;
        }
        tickCounter++;
        if (state != State.RETURNING && state != State.ALIGNING
                && state != State.DESCENDING && state != State.REWARP_WAIT) {
            long now = System.currentTimeMillis();
            zeroAliveTicks = parseAlive() == 0 && now - runStartedAt >= STARTUP_FINISH_GRACE_MS
                    ? zeroAliveTicks + 1 : 0;
            if (zeroAliveTicks >= TAB_FINISH_CONFIRM_TICKS
                    || now - runStartedAt > CLEAN_TIMEOUT_MS) {
                finishCleaning();
                return;
            }
        }

        switch (state) {
            case ACTIVATING_FLY -> tickActivatingFly();
            case TELEPORTING -> tickTeleporting();
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

    @EventHandler
    public void onReceiveMessage(ReceiveMessageEvent event) {
        if (state != State.TELEPORTING || event.getMessage() == null
                || System.currentTimeMillis() < plotTeleportSentAt) return;
        Matcher matcher = PLOT_TELEPORT_PATTERN.matcher(event.getMessage().getString());
        if (!matcher.find()) return;
        Integer target = plotQueue.peek();
        if (target != null && Integer.parseInt(matcher.group(1)) == target) {
            plotTeleportChatConfirmed = true;
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (!Utils.nullCheck() || state == State.IDLE) return;

        switch (state) {
            case PATHING_TO_PLOT -> renderPath(
                    event, currentPath, pathIndex, PLOT_PATH_COLOR);
            case CHASING -> renderChaseRoute(event);
            case VACUUMING -> renderTargetEntity(event, CHASE_PATH_COLOR);
            case RETURNING, ALIGNING -> renderDirectRoute(
                    event, cruiseWaypoint, RETURN_PATH_COLOR);
            case DESCENDING -> renderDirectRoute(
                    event, returnTarget, RETURN_PATH_COLOR);
            default -> {
            }
        }

        if (localAvoidTarget != null) {
            renderDirectRoute(event, localAvoidTarget, AVOID_PATH_COLOR);
        }
    }

    private void renderChaseRoute(Render3DEvent event) {
        if (targetEntity == null || !targetEntity.isAlive()) return;

        renderTargetEntity(event, CHASE_PATH_COLOR);
        if (chasePathIndex < chasePath.size()) {
            renderPath(event, chasePath, chasePathIndex, CHASE_PATH_COLOR);
            return;
        }

        Vec3 pos = mc.player.position();
        Vec3 target = approachPoint(pos, targetEntity.position(), CHASE_GOAL_TOLERANCE);
        renderDirectRoute(event, target, CHASE_PATH_COLOR);
    }

    private void renderPath(Render3DEvent event, List<Vec3> path, int firstPoint, Color color) {
        if (path == null || path.isEmpty()) return;

        List<Vec3> snapshot = List.copyOf(path);
        int start = Math.max(0, Math.min(firstPoint, snapshot.size() - 1));
        Vec3 previous = mc.player.position().add(0, 0.2, 0);
        for (int i = start; i < snapshot.size(); i++) {
            Vec3 point = snapshot.get(i);
            Vec3 raisedPoint = point.add(0, 0.2, 0);
            RenderUtils.drawLine3D(
                    event.getMatrix(), previous, raisedPoint, color, 2.5f, false);

            Color pointColor = i == snapshot.size() - 1
                    ? PATH_END_COLOR
                    : i == start ? Color.WHITE : color;
            RenderUtils.drawBox(
                    event.getMatrix(), pathPointBox(point, i == snapshot.size() - 1),
                    pointColor, 2.0f, false);
            previous = raisedPoint;
        }
    }

    private void renderDirectRoute(Render3DEvent event, Vec3 target, Color color) {
        if (target == null) return;
        Vec3 from = mc.player.position().add(0, 0.2, 0);
        Vec3 to = target.add(0, 0.2, 0);
        RenderUtils.drawLine3D(event.getMatrix(), from, to, color, 2.5f, false);
        RenderUtils.drawBox(event.getMatrix(), pathPointBox(target, true), PATH_END_COLOR, 2.0f, false);
    }

    private void renderTargetEntity(Render3DEvent event, Color color) {
        if (targetEntity == null || !targetEntity.isAlive()) return;
        RenderUtils.drawBox(event.getMatrix(), targetEntity.getBoundingBox(), color, 2.0f, false);
    }

    private AABB pathPointBox(Vec3 point, boolean endpoint) {
        double radius = endpoint ? 0.24 : 0.16;
        return new AABB(
                point.x - radius, point.y, point.z - radius,
                point.x + radius, point.y + radius * 2.0, point.z + radius
        );
    }

    private void tickActivatingFly() {
        flyActivateTick++;

        if (flyActivateTick == 1) mc.options.keyJump.setDown(true);
        else if (flyActivateTick == 2) mc.options.keyJump.setDown(false);
        else if (flyActivateTick == FLY_ACTIVATE_WAIT) mc.options.keyJump.setDown(true);
        else if (flyActivateTick == FLY_ACTIVATE_WAIT + 1) mc.options.keyJump.setDown(false);
        else if (flyActivateTick >= FLY_CONFIRM_WAIT) {
            if (!mc.player.getAbilities().flying && mc.player.onGround()) {
                Utils.addModuleMessage(this.getName(),
                        "§cNo cookie buff or mushroom soup found, disabled PestCleaner");
                finalDisable();
                return;
            }
            vacuumSlot = findVacuumSlot();
            if (vacuumSlot == -1) {
                Utils.addModuleMessage(this.getName(), "§cNo vacuum in hotbar, disabled PestCleaner");
                finalDisable();
                return;
            }
            mc.player.getInventory().setSelectedSlot(vacuumSlot);
            flyToNextPlot();
        }
    }

    private void flyToNextPlot() {
        releaseAll();
        if (parseAlive() == 0) {
            state = State.SCANNING;
            return;
        }

        List<Integer> infested = parseInfestedPlots();
        Vec3 here = mc.player.position();
        infested.sort(Comparator.comparingInt(plot -> isPlayerOnPlot(plot, here) ? 0 : 1));
        plotQueue.clear();
        for (int plot : infested) {
            if (!exhaustedPlots.contains(plot)) plotQueue.add(plot);
        }
        if (plotQueue.isEmpty()) {
            finishCleaning();
            return;
        }
        Integer plotNum = plotQueue.peek();
        if (plotNum == null) return;
        if (isPlayerOnPlot(plotNum, mc.player.position())) {
            arriveAtPlot();
            return;
        }
        plotTeleportAttempts = 0;
        sendPlotTeleport(plotNum);
    }

    private void sendPlotTeleport(int plotNum) {
        releaseAll();
        trustedPlot = -1;
        trustedPlotExpiresAt = 0L;
        plotTeleportOrigin = mc.player.position();
        plotTeleportTick = 0;
        plotTeleportAttempts++;
        plotTeleportSentAt = System.currentTimeMillis();
        plotTeleportChatConfirmed = false;
        state = State.TELEPORTING;
        mc.player.connection.sendCommand("plottp " + plotNum);
        Utils.addModuleMessage(this.getName(), "§bTeleporting to Plot " + plotNum);
    }

    private void tickTeleporting() {
        releaseAll();
        Integer plotNum = plotQueue.peek();
        if (plotNum == null) { finishCleaning(); return; }
        plotTeleportTick++;
        Vec3 current = mc.player.position();
        boolean moved = plotTeleportOrigin.distanceToSqr(current) > 16.0;
        boolean nearTarget = isOnPlot(plotNum, current, PLOT_ARRIVAL_MARGIN);
        if (plotTeleportChatConfirmed && plotTeleportTick > 1
                || moved && nearTarget
                || plotTeleportTick >= PLOT_TP_WAIT_TICKS
                && currentSidebarPlot() == plotNum && nearTarget) {
            arriveAtPlot();
            return;
        }
        if (plotTeleportTick < PLOT_TP_WAIT_TICKS) return;
        if (plotTeleportAttempts < 3) {
            sendPlotTeleport(plotNum);
        } else {
            Utils.addModuleMessage(this.getName(), "§ePlot teleport timed out: " + plotNum);
            exhaustedPlots.add(plotNum);
            plotQueue.poll();
            flyToNextPlot();
        }
    }

    private void arriveAtPlot() {
        releaseAll();
        Integer plot = plotQueue.peek();
        if (plot != null) {
            trustedPlot = plot;
            trustedPlotExpiresAt = System.currentTimeMillis() + TRUSTED_PLOT_TTL_MS;
        }
        scanPointIndex = 0;
        scanSweepCount = 0;
        scanWaypointCount = 0;
        emptyPlotTabTicks = 0;
        scanTick = 0;
        scanWaypoint = null;
        trackerTrail.reset();
        trackerRequestedAt = 0;
        trackerAwaitingAttackAt = 0;
        trackerReadyTick = 0;
        trackerFinishedForScan = false;
        guidedWaypoints = 0;
        previousGuidedWaypoint = null;
        resetAvoid();
        if (!mc.player.getAbilities().flying && mc.player.getAbilities().mayfly) {
            flyActivateTick = 0;
            state = State.ACTIVATING_FLY;
        } else {
            state = State.SCANNING;
        }
    }

    private void tickPathingToPlot() {
        if (checkPlotCleared()) return;
        Entity pest = findNearestPest();
        if (pest != null) { engagePest(pest); return; }
        if (scanWaypoint == null || mc.player.position().distanceTo(scanWaypoint) < 15.0
                || System.currentTimeMillis() - stateEnteredAt > 30_000) {
            releaseMovement();
            state = State.SCANNING;
            scanWaypoint = null;
            scanTick = 0;
            trackerRequestedAt = 0;
            trackerAwaitingAttackAt = 0;
            trackerFinishedForScan = false;
            return;
        }
        if (pathIndex < currentPath.size()) {
            Vec3 point = currentPath.get(pathIndex);
            if (mc.player.position().distanceTo(point) < WAYPOINT_ARRIVE) pathIndex++;
            else cruise(point);
        } else {
            cruise(scanWaypoint);
        }
    }

    private void tickScanning() {
        releaseAll();
        scanTick++;

        if (checkPlotCleared()) return;


        Entity pest = findNearestPest();
        if (pest != null) {
            engagePest(pest);
            return;
        }
        Integer plotNum = plotQueue.peek();
        if (plotNum == null) { finishCleaning(); return; }
        if (!isPlayerOnPlot(plotNum, mc.player.position())) { sendPlotTeleport(plotNum); return; }
        if (!trackerFinishedForScan && guidedWaypoints < 2) {
            if (tickTrackerSearch(plotNum)) return;
        }
        if (scanPointIndex >= SCAN_OFFSETS.length) {
            scanPointIndex = 0;
            scanSweepCount++;
            if (!deferredTargets.isEmpty()) {
                deferredTargets.clear();
                scanSweepCount = Math.max(0, scanSweepCount - 1);
            }
        }
        if (scanSweepCount >= MAX_PLOT_SWEEPS || scanWaypointCount >= MAX_SCAN_WAYPOINTS) {
            exhaustedPlots.add(plotNum);
            plotQueue.poll();
            flyToNextPlot();
            return;
        }
        if (scanTick < 5) return;
        double[] center = PLOT_CENTERS.get(plotNum);
        double[] offset = SCAN_OFFSETS[scanPointIndex++];
        scanWaypointCount++;
        scanWaypoint = new Vec3(center[0] + offset[0], mc.player.getY(), center[2] + offset[1]);
        guidedWaypoints = 0;
        startScanWaypoint();
    }

    private boolean checkPlotCleared() {
        Integer current = plotQueue.peek();
        if (current == null) {
            flyToNextPlot();
            return true;
        }
        List<Integer> infested = parseInfestedPlots();
        if (infested.isEmpty()) {
            releaseAll();
            if (parseAlive() == 0 || ++emptyPlotTabTicks < 40) return true;
            finishCleaning();
            return true;
        }
        emptyPlotTabTicks = 0;
        if (infested.contains(current)) return false;
        releaseAll();
        trackerTrail.reset();
        trackerRequestedAt = 0;
        trackerAwaitingAttackAt = 0;
        targetEntity = null;
        resetChasePath();
        scanWaypoint = null;
        currentPath = Collections.emptyList();
        Utils.addModuleMessage(getName(), "§bPlot " + current + " cleared; selecting the next infested plot");
        flyToNextPlot();
        return true;
    }

    private boolean tickTrackerSearch(int plotNum) {
        long now = System.currentTimeMillis();
        if (trackerAwaitingAttackAt != 0) {
            if (now - trackerAwaitingAttackAt < 250) return true;
            trackerAwaitingAttackAt = 0;
            trackerRequestedAt = 0;
            trackerFinishedForScan = true;
            return false;
        }
        if (trackerRequestedAt == 0) {
            if (vacuumSlot < 0 || vacuumSlot > 8) return false;
            if (scanTick > 40) {
                trackerFinishedForScan = true;
                return false;
            }
            if (mc.player.isUsingItem() && mc.gameMode != null) {
                mc.gameMode.releaseUsingItem(mc.player);
                trackerReadyTick = mc.player.tickCount + 2;
                return true;
            }
            if (mc.player.getInventory().getSelectedSlot() != vacuumSlot) {
                mc.player.getInventory().setSelectedSlot(vacuumSlot);
                trackerReadyTick = mc.player.tickCount + 2;
                return true;
            }
            if (mc.player.tickCount < trackerReadyTick || mc.player.isShiftKeyDown()) return true;
            if (now < trackerNextUseAt) return true;
            mc.options.keyUse.setDown(false);
            mc.options.keyAttack.setDown(false);
            trackerRequestedAt = now;
            trackerAwaitingAttackAt = now;
            trackerNextUseAt = now + 1_100;
            KeyMapping.click(((KeyMappingAccessor) mc.options.keyAttack).getKey());
            return true;
        }
        if (!trackerTrail.belongsTo(trackerRequestedAt)) return true;
        PestTrackerTrail.Prediction current = trackerTrail.prediction(now);
        if (current != null) aimAtTracker(current.target());
        if (!trackerTrail.isComplete(now)) return true;
        PestTrackerTrail.Prediction prediction = trackerTrail.prediction(now);
        trackerRequestedAt = 0;
        trackerFinishedForScan = true;
        if (prediction == null) return false;
        double[] center = PLOT_CENTERS.get(plotNum);
        Vec3 estimate = prediction.target();
        Vec3 candidate = new Vec3(
                Mth.clamp(estimate.x, center[0] - 40.0, center[0] + 40.0),
                mc.player.getY(),
                Mth.clamp(estimate.z, center[2] - 40.0, center[2] + 40.0));
        List<Vec3> observed = prediction.observed();
        Vec3 visibleTravel = observed.getLast().subtract(observed.getFirst()).multiply(1, 0, 1);
        Vec3 guidedTravel = candidate.subtract(mc.player.position()).multiply(1, 0, 1);
        if (visibleTravel.lengthSqr() > 1.0 && guidedTravel.lengthSqr() > 1.0
                && visibleTravel.normalize().dot(guidedTravel.normalize()) < 0.35) {
            return false;
        }
        if (mc.player.position().distanceTo(candidate) < 16.0
                || previousGuidedWaypoint != null && previousGuidedWaypoint.distanceTo(candidate) < 12.0) {
            return false;
        }
        previousGuidedWaypoint = candidate;
        guidedWaypoints++;
        scanWaypointCount++;
        scanWaypoint = candidate;
        startScanWaypoint();
        return true;
    }

    public void onTrackerAttack() {
        if (state != State.SCANNING || trackerAwaitingAttackAt == 0
                || mc.player == null || mc.level == null) return;
        long now = System.currentTimeMillis();
        trackerTrail.begin(mc.player.position(), now);
        trackerRequestedAt = now;
        trackerAwaitingAttackAt = 0;
    }

    public void onTrackerParticle(ClientboundLevelParticlesPacket packet) {
        if (!mc.isSameThread() || trackerRequestedAt == 0 || mc.level == null || packet == null
                || packet.getParticle().getType() != ParticleTypes.ANGRY_VILLAGER
                || packet.getCount() != 1 || packet.getMaxSpeed() != 0
                || packet.getXDist() != 0 || packet.getYDist() != 0 || packet.getZDist() != 0) return;
        trackerTrail.add(new Vec3(packet.getX(), packet.getY(), packet.getZ()),
                System.currentTimeMillis());
    }

    private void startScanWaypoint() {
        Vec3 start = mc.player.position();
        if (flyPathfinder.hasClearPath(start, scanWaypoint, mc.level)) {
            currentPath = List.of(scanWaypoint);
        } else {
            NavMeshPath route = flyPathfinder.findPath(start, scanWaypoint, mc.level, 128);
            currentPath = route.isFound() ? route.getWaypoints() : Collections.emptyList();
        }
        pathIndex = 0;
        stateEnteredAt = System.currentTimeMillis();
        state = State.PATHING_TO_PLOT;
    }

    private void engagePest(Entity pest) {
        targetEntity = pest;
        scanPointIndex = 0;
        scanSweepCount = 0;
        scanWaypointCount = 0;
        lastTargetId = -1;
        targetVelocity = Vec3.ZERO;
        resetChasePath();
        resetAvoid();
        stateEnteredAt = System.currentTimeMillis();
        state = State.CHASING;
    }

    private void tickChasing() {
        if (targetEntity == null || !targetEntity.isAlive()) {
            targetEntity = null;
            resetChasePath();
            scanTick = 0;
            state = State.SCANNING;
            return;
        }
        Integer plot = plotQueue.peek();
        if (plot != null && !isOnPlot(plot, targetEntity.position(), 3.0)) {
            targetEntity = null;
            state = State.SCANNING;
            scanTick = 0;
            return;
        }

        if (System.currentTimeMillis() - stateEnteredAt > 30_000) {
            deferCurrentTarget();
            targetEntity = null;
            state = State.SCANNING;
            scanTick = 0;
            return;
        }

        Vec3 pos = mc.player.position();
        Vec3 entPos = targetEntity.position();

        if (pos.distanceTo(entPos) <= Math.max(12.0,
                followDistance.getInput() + mc.player.getDeltaMovement().horizontalDistance()
                        * PestFlightMotion.coastTicks() + 2.0)
                && flyPathfinder.hasClearPath(pos,
                entPos.add(0, hoverHeight.getInput(), 0), mc.level)) {
            releaseMovement();
            resetChasePath();
            resetAvoid();
            state = State.VACUUMING;
            stateEnteredAt = System.currentTimeMillis();
            return;
        }

        Vec3 elevatedTarget = entPos.add(0, hoverHeight.getInput(), 0);
        Vec3 directApproach = approachPoint(pos, elevatedTarget, followDistance.getInput());
        if (flyPathfinder.hasClearPath(pos, directApproach, mc.level)) {
            resetChasePath();
            cruise(directApproach);
            return;
        }

        chaseRepathTick = Math.max(0, chaseRepathTick - 1);
        boolean goalMoved = chasePathGoal == null
                || chasePathGoal.distanceToSqr(entPos)
                > CHASE_GOAL_MOVE_THRESHOLD * CHASE_GOAL_MOVE_THRESHOLD;
        boolean pathFinished = chasePathIndex >= chasePath.size();
        boolean nextSegmentBlocked = !pathFinished
                && !flyPathfinder.hasClearPath(pos, chasePath.get(chasePathIndex), mc.level);

        if (chaseRepathTick == 0 && (goalMoved || pathFinished || nextSegmentBlocked)) {
            NavMeshPath route = flyPathfinder.findPathToRange(
                    pos, elevatedTarget, mc.level, CHASE_PATH_RANGE, CHASE_GOAL_TOLERANCE);
            chaseRepathTick = CHASE_REPATH_INTERVAL;
            chasePathGoal = entPos;
            if (route.isFound() && !route.getWaypoints().isEmpty()) {
                chasePath = route.getWaypoints();
                chasePathIndex = 0;
                advanceChaseWaypoint(pos);
                localAvoidTarget = null;
            } else {
                chasePath = Collections.emptyList();
                chasePathIndex = 0;
            }
        }

        advanceChaseWaypoint(pos);
        if (chasePathIndex < chasePath.size()) {
            cruise(chasePath.get(chasePathIndex));
        } else {
            cruise(directApproach);
        }
    }

    private void tickVacuuming() {
        if (targetEntity == null || !targetEntity.isAlive()) {
            targetEntity = null;
            mc.options.keyUse.setDown(false);
            scanTick = 0;
            state = State.SCANNING;
            return;
        }

        Vec3 entPos = targetEntity.position().add(0, targetEntity.getBbHeight() / 2.0, 0);
        Vec3 pos = mc.player.getEyePosition();

        if (System.currentTimeMillis() - stateEnteredAt > 30_000) {
            deferCurrentTarget();
            mc.options.keyUse.setDown(false);
            targetEntity = null;
            state = State.SCANNING;
            scanTick = 0;
            return;
        }
        Vec3 targetFeet = targetEntity.position();
        if (mc.player.position().distanceTo(targetFeet) > vacuumRange + 6.0
                || !flyPathfinder.hasClearPath(mc.player.position(),
                targetFeet.add(0, hoverHeight.getInput(), 0), mc.level)) {
            mc.options.keyUse.setDown(false);
            resetChasePath();
            resetAvoid();
            state = State.CHASING;
            stateEnteredAt = System.currentTimeMillis();
            return;
        }

        if (mc.player.onGround() && !mc.player.getAbilities().flying) {
            mc.player.getAbilities().flying = true;
            mc.player.onUpdateAbilities();
        }

        int targetId = targetEntity.getId();
        if (targetId == lastTargetId && lastTargetPosition != null
                && targetFeet.distanceToSqr(lastTargetPosition) <= 9.0) {
            targetVelocity = targetVelocity.scale(0.65)
                    .add(targetFeet.subtract(lastTargetPosition).scale(0.35));
        } else {
            targetVelocity = Vec3.ZERO;
        }
        lastTargetId = targetId;
        lastTargetPosition = targetFeet;
        Vec3 offset = targetFeet.subtract(mc.player.position());
        double hover = hoverHeight.getInput();
        double verticalEyeGap = Math.abs(hover + mc.player.getEyeHeight()
                - targetEntity.getEyeHeight(targetEntity.getPose()));
        double safeRange = Math.max(0.0, vacuumRange - 0.75);
        double follow = Math.min(followDistance.getInput(),
                Math.sqrt(Math.max(0.0, safeRange * safeRange - verticalEyeGap * verticalEyeGap)));
        Vec3 desired = PestFlightMotion.approachVelocity(offset, targetVelocity, follow, APPROACH_SPEED);
        Vec3 ledAim = entPos.add(targetVelocity.scale(4.4));
        aimAt(ledAim);
        boolean facingTarget = facesTarget(offset, mc.player.getYRot());
        if (!facingTarget) desired = Vec3.ZERO;
        PestFlightMotion.steer(mc, desired, mc.player.getDeltaMovement(), mc.player.getYRot());
        int vertical = PestFlightMotion.verticalInput(offset.y + hover, mc.player.getDeltaMovement().y);
        mc.options.keyJump.setDown(vertical > 0);
        mc.options.keyShift.setDown(vertical < 0);

        vacuumSlot = findVacuumSlot();
        if (vacuumSlot == -1) {
            Utils.addModuleMessage(this.getName(), "§cVacuum gone from hotbar");
            finalDisable();
            return;
        }
        vacuumRange = detectVacuumRange(vacuumSlot);
        mc.player.getInventory().setSelectedSlot(vacuumSlot);
        mc.options.keyUse.setDown((facingTarget || offset.horizontalDistance() < 0.25)
                && mc.player.getEyePosition().distanceTo(entPos) <= safeRange);
    }

    private void finishCleaning() {
        releaseAll();
        Utils.addModuleMessage(this.getName(), "§aPests cleared!");

        if (!resumeFarming) {
            finalDisable();
            return;
        }

        if (rewarpOnly) {
            preRewarpPos = mc.player.position();
            rewarpCooldown = REWARP_COOLDOWN_TICKS;
            rewarpTimeout = 0;
            mc.player.connection.sendCommand("warp garden");
            state = State.REWARP_WAIT;
            Utils.addModuleMessage(this.getName(), "§b/warp garden sent, waiting for teleport...");
            return;
        }

        if (returnTarget == null) {
            finalDisable();
            return;
        }

        ensureFlying();

        Vec3 pos = mc.player.position();
        double cruiseY = Math.max(pos.y, returnTarget.y) + RETURN_CRUISE_HEIGHT;
        cruiseWaypoint = new Vec3(returnTarget.x, cruiseY, returnTarget.z);
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
            Vec3 now = mc.player.position();
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

        Vec3 pos = mc.player.position();
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


        Vec3 vel = mc.player.getDeltaMovement();
        double xzSpd = Math.sqrt(vel.x * vel.x + vel.z * vel.z);


        double targetSpd = Math.max(0, 0.25 * (xzD - FREEFALL_XZ) / (DECEL_START_XZ - FREEFALL_XZ));


        holdAltitude(cruiseWaypoint.y);


        double dxT = cruiseWaypoint.x - pos.x;
        double dzT = cruiseWaypoint.z - pos.z;
        float wantYaw = (float) Math.toDegrees(Math.atan2(-dxT, dzT));
        float cy = mc.player.getYRot();
        mc.player.setYRot(cy + Mth.wrapDegrees(wantYaw - cy) * CRUISE_YAW_SPEED);

        if (xzD <= FREEFALL_XZ && xzSpd <= ALIGN_MAX_SPEED) {

            releaseMovement();
            landingYaw = mc.player.getYRot();
            state = State.ALIGNING;
            returnTick = 0;
            return;
        }

        if (xzSpd < targetSpd - 0.02) {

            mc.options.keyUp.setDown(true);
            mc.options.keyDown.setDown(false);
        } else if (xzSpd > targetSpd + 0.02) {

            mc.options.keyUp.setDown(false);
            mc.options.keyDown.setDown(true);
        } else {
            mc.options.keyUp.setDown(false);
            mc.options.keyDown.setDown(false);
        }
        mc.options.keyLeft.setDown(false);
        mc.options.keyRight.setDown(false);
    }

    private void tickAligning() {
        returnTick++;
        if (returnTick >= RETURN_TIMEOUT_TICKS) {
            forceLand();
            return;
        }

        ensureFlying();


        mc.player.setYRot(landingYaw);

        Vec3 pos = mc.player.position();
        Vec3 vel = mc.player.getDeltaMovement();
        double xzD = xzDist(pos, cruiseWaypoint);
        double xzSpd = Math.sqrt(vel.x * vel.x + vel.z * vel.z);


        holdAltitude(cruiseWaypoint.y);

        mc.options.keyUp.setDown(false);
        mc.options.keyDown.setDown(false);
        mc.options.keyLeft.setDown(false);
        mc.options.keyRight.setDown(false);


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


        mc.player.setYRot(landingYaw);

        releaseMovement();

        Vec3 pos = mc.player.position();
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


        if (mc.player.onGround()) {
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
        Vec3 pos = mc.player.position();
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


    private void cruise(Vec3 target) {
        cruiseWithObstacleAvoid(target);
    }

    private void cruiseWithObstacleAvoid(Vec3 target) {
        Vec3 pos = mc.player.position();

        if (mc.player.onGround() && !mc.player.getAbilities().flying) {
            mc.player.getAbilities().flying = true;
            mc.player.onUpdateAbilities();
            localAvoidTarget = null;
        }

        if (pos.y < BARN_SAFE_Y) {
            Vec3 detour = barnDetour(pos, target);
            if (detour != null) target = detour;
        }

        updateProgress(pos);

        if (localAvoidTarget != null) {
            if (pos.distanceTo(localAvoidTarget) <= 0.55
                    || !flyPathfinder.hasClearPath(pos, localAvoidTarget, mc.level)) {
                localAvoidTarget = null;
            } else {
                flyTowardDirect(localAvoidTarget, true);
                return;
            }
        }

        Vec3 probeEnd = pointAlong(pos, target, OBSTACLE_PROBE);
        boolean routeBlocked = !flyPathfinder.hasClearPath(pos, probeEnd, mc.level);
        if (routeBlocked || stuckLevel > 0) {
            Vec3 escape = findLocalEscape(pos, target);
            if (escape != null) {
                localAvoidTarget = escape;
                lastEscapeDirection = escape.subtract(pos).normalize();
                flyTowardDirect(escape, true);
                return;
            }
        }

        flyTowardDirect(target, false);
    }

    private Vec3 findLocalEscape(Vec3 pos, Vec3 target) {
        Vec3 toTarget = target.subtract(pos);
        double targetDistance = toTarget.length();
        Vec3 targetDirection = targetDistance > 1.0E-6 ? toTarget.scale(1.0 / targetDistance) : Vec3.ZERO;

        Vec3 best = null;
        double bestScore = -Double.MAX_VALUE;
        double wantedDistance = Math.min(LOCAL_ESCAPE_MAX_DISTANCE,
                LOCAL_ESCAPE_MIN_DISTANCE + stuckLevel * 0.55);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    Vec3 direction = new Vec3(dx, dy, dz).normalize();
                    Vec3 candidate = farthestClearPoint(pos, direction, wantedDistance);
                    if (candidate == null) continue;

                    double travel = candidate.distanceTo(pos);
                    double progress = targetDistance - candidate.distanceTo(target);
                    double alignment = direction.dot(targetDirection);
                    double score = progress * 2.5 + alignment * 1.5 + travel * 0.35;

                    Vec3 onward = pointAlong(candidate, target, 2.0);
                    if (flyPathfinder.hasClearPath(candidate, onward, mc.level)) score += 2.5;
                    if (lastEscapeDirection != null) {
                        double continuity = direction.dot(lastEscapeDirection);
                        score += continuity * (stuckLevel >= 3 ? 0.25 : 1.25);
                    }
                    if (dx == 0 && dz == 0) score -= 0.35;

                    if (score > bestScore) {
                        bestScore = score;
                        best = candidate;
                    }
                }
            }
        }
        return best;
    }

    private Vec3 farthestClearPoint(Vec3 pos, Vec3 direction, double wantedDistance) {
        for (double distance = wantedDistance; distance >= LOCAL_ESCAPE_MIN_DISTANCE; distance -= 0.5) {
            Vec3 candidate = pos.add(direction.scale(distance));
            if (flyPathfinder.hasClearPath(pos, candidate, mc.level)) return candidate;
        }
        return null;
    }

    private void updateProgress(Vec3 pos) {
        progressCheckTick++;
        if (progressCheckTick < STUCK_CHECK_TICKS) return;

        progressCheckTick = 0;
        if (lastProgressPos != null && pos.distanceTo(lastProgressPos) < STUCK_MIN_MOVE) {
            stuckLevel = Math.min(stuckLevel + 1, 6);
            localAvoidTarget = null;
        } else {
            stuckLevel = Math.max(0, stuckLevel - 1);
        }
        lastProgressPos = pos;
    }

    private Vec3 barnDetour(Vec3 pos, Vec3 target) {
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
        return new Vec3(bx, target.y, bz);
    }

    private void flyTowardDirect(Vec3 target, boolean immediateYaw) {
        Vec3 pos = mc.player.position();
        double dx = target.x - pos.x;
        double dy = target.y - pos.y;
        double dz = target.z - pos.z;
        double hDist = Math.sqrt(dx * dx + dz * dz);

        if (hDist > 0.05) {
            float wantYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float cy = mc.player.getYRot();
            mc.player.setYRot(immediateYaw
                    ? wantYaw
                    : cy + Mth.wrapDegrees(wantYaw - cy) * CRUISE_YAW_SPEED);
        }

        mc.options.keyUp.setDown(hDist > 0.18);
        mc.options.keyDown.setDown(false);
        mc.options.keyLeft.setDown(false);
        mc.options.keyRight.setDown(false);

        if (dy > 0.35) {
            mc.options.keyJump.setDown(true);
            mc.options.keyShift.setDown(false);
        } else if (dy < -0.35) {
            mc.options.keyShift.setDown(true);
            mc.options.keyJump.setDown(false);
        } else {
            mc.options.keyJump.setDown(false);
            mc.options.keyShift.setDown(false);
        }
    }

    private void holdAltitude(double targetY) {
        double dy = targetY - mc.player.position().y;
        if (dy > 0.3) {
            mc.options.keyJump.setDown(true);
            mc.options.keyShift.setDown(false);
        } else if (dy < -0.3) {
            mc.options.keyShift.setDown(true);
            mc.options.keyJump.setDown(false);
        } else {
            mc.options.keyJump.setDown(false);
            mc.options.keyShift.setDown(false);
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

        if (dotF >= best) mc.options.keyUp.setDown(true);
        else if (dotB >= best) mc.options.keyDown.setDown(true);
        else if (dotR >= best) mc.options.keyRight.setDown(true);
        else if (dotL >= best) mc.options.keyLeft.setDown(true);
    }

    private void aimAt(Vec3 target) {
        rotateToward(target, 220.0, 15.0f);
    }

    private void aimAtTracker(Vec3 target) {
        rotateToward(target, 450.0, 6.0f);
    }

    private void rotateToward(Vec3 target, double smoothingMs, float maxDegreesPerTick) {
        Vec3 eye = mc.player.getEyePosition();
        double dx = target.x - eye.x, dy = target.y - eye.y, dz = target.z - eye.z;
        double dist = Math.sqrt(dx * dx + dz * dz);

        float cy = mc.player.getYRot(), cp = mc.player.getXRot();
        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float targetPitch = (float) Math.toDegrees(-Math.atan2(dy, Math.max(dist, 0.01)));
        float smoothing = (float) (1.0 - Math.exp(-50.0 / smoothingMs));
        mc.player.setYRot(cy + Mth.clamp(Mth.wrapDegrees(targetYaw - cy) * smoothing,
                -maxDegreesPerTick, maxDegreesPerTick));
        mc.player.setXRot(cp + Mth.clamp((targetPitch - cp) * smoothing,
                -maxDegreesPerTick, maxDegreesPerTick));
    }

    private void ensureFlying() {
        if (!mc.player.getAbilities().flying) {
            mc.player.getAbilities().flying = true;
            mc.player.onUpdateAbilities();
        }
    }

    private void stopFlying() {
        if (mc.player.getAbilities().flying) {
            mc.player.getAbilities().flying = false;
            mc.player.onUpdateAbilities();
        }
    }

    private static boolean facesTarget(Vec3 offset, float yaw) {
        double horizontal = offset.horizontalDistance();
        if (horizontal < 0.25) return false;
        double angle = Math.toRadians(yaw);
        return (-offset.x * Math.sin(angle) + offset.z * Math.cos(angle)) / horizontal >= 0.5;
    }

    private Vec3 pointAlong(Vec3 from, Vec3 to, double maxTravel) {
        Vec3 delta = to.subtract(from);
        double distance = delta.length();
        if (distance <= maxTravel || distance < 1.0E-6) return to;
        return from.add(delta.scale(maxTravel / distance));
    }

    private Vec3 approachPoint(Vec3 from, Vec3 target, double stopDistance) {
        Vec3 delta = target.subtract(from);
        double distance = delta.length();
        if (distance <= stopDistance || distance < 1.0E-6) return from;
        return from.add(delta.scale((distance - stopDistance) / distance));
    }

    private void advanceChaseWaypoint(Vec3 pos) {
        while (chasePathIndex < chasePath.size()
                && pos.distanceTo(chasePath.get(chasePathIndex)) <= CHASE_WAYPOINT_ARRIVE) {
            chasePathIndex++;
        }

        for (int i = chasePath.size() - 1; i > chasePathIndex; i--) {
            if (flyPathfinder.hasClearPath(pos, chasePath.get(i), mc.level)) {
                chasePathIndex = i;
                break;
            }
        }
    }

    private void resetChasePath() {
        chasePath = Collections.emptyList();
        chasePathIndex = 0;
        chaseRepathTick = 0;
        chasePathGoal = null;
    }

    private void resetAvoid() {
        lastProgressPos = null;
        progressCheckTick = 0;
        localAvoidTarget = null;
        lastEscapeDirection = null;
        stuckLevel = 0;
    }

    private void releaseMovement() {
        mc.options.keyUp.setDown(false);
        mc.options.keyDown.setDown(false);
        mc.options.keyLeft.setDown(false);
        mc.options.keyRight.setDown(false);
        mc.options.keyJump.setDown(false);
        mc.options.keyShift.setDown(false);
    }

    private void releaseAll() {
        releaseMovement();
        mc.options.keyAttack.setDown(false);
        mc.options.keyUse.setDown(false);
    }

    private int parseAlive() {
        if (mc.getConnection() == null) return -1;
        int alive = -1;
        for (PlayerInfo entry : mc.getConnection().getOnlinePlayers()) {
            if (entry.getTabListDisplayName() == null) continue;
            String line = entry.getTabListDisplayName().getString().trim();
            Matcher matcher = PESTS_ALIVE_PATTERN.matcher(line);
            if (matcher.find()) alive = Math.max(alive, Integer.parseInt(matcher.group(1)));
        }
        return alive;
    }

    private List<Integer> parseInfestedPlots() {
        if (mc.getConnection() == null) return Collections.emptyList();
        List<Integer> result = new ArrayList<>();
        for (PlayerInfo entry : mc.getConnection().getOnlinePlayers()) {
            if (entry.getTabListDisplayName() == null) continue;
            String line = entry.getTabListDisplayName().getString().trim();
            Matcher matcher = INFESTED_PLOTS_PATTERN.matcher(line);
            if (matcher.find()) {
                for (String part : matcher.group(1).split(",")) {
                    String token = part.trim().replaceAll("\\D", "");
                    if (token.isEmpty()) continue;
                    int n = Integer.parseInt(token);
                    if (PLOT_CENTERS.containsKey(n) && !result.contains(n)) result.add(n);
                }
            }
        }
        return result;
    }

    private Entity findNearestPest() {
        Vec3 pos = mc.player.position();
        Entity nearest = null;
        double minDist = Double.MAX_VALUE;
        Integer plot = plotQueue.peek();
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof Bat) && !(e instanceof Silverfish)
                    && !(e instanceof ArmorStand marker
                    && PestMarkerDetector.isPestMarker(marker, mc.level.entitiesForRendering()))) continue;
            if (!e.isAlive() || e.getY() < 50 || deferredTargets.contains(e.getId())
                    || abandonedTargets.contains(e.getId())) continue;
            if (plot != null && !isOnPlot(plot, e.position(), 3.0)) continue;
            double d = e.position().distanceTo(pos);
            if (d < minDist) {
                minDist = d;
                nearest = e;
            }
        }
        return nearest;
    }

    private void deferCurrentTarget() {
        if (targetEntity == null) return;
        int id = targetEntity.getId();
        int failures = targetFailures.merge(id, 1, Integer::sum);
        if (failures < 2) deferredTargets.add(id);
        else abandonedTargets.add(id);
    }

    private double detectVacuumRange(int slot) {
        if (slot < 0 || slot >= 9) return 4.5;
        ItemStack stack = mc.player.getInventory().getItem(slot);
        if (stack.isEmpty()) return 4.5;
        var lore = stack.get(DataComponents.LORE);
        var custom = stack.get(DataComponents.CUSTOM_DATA);
        boolean recombobulated = custom != null
                && custom.copyTag().getInt("rarity_upgrades").orElse(0) > 0;
        return VacuumProfile.parse(stack.getHoverName().getString(),
                lore == null ? List.of() : lore.lines().stream().map(line -> line.getString()).toList(),
                recombobulated).range();
    }

    private boolean isOnPlot(int plot, Vec3 position) { return isOnPlot(plot, position, 0.0); }

    private boolean isPlayerOnPlot(int plot, Vec3 position) {
        if (isOnPlot(plot, position)) return true;
        if (trustedPlot != plot || System.currentTimeMillis() >= trustedPlotExpiresAt) return false;
        if (isOnPlot(plot, position, PLOT_ARRIVAL_MARGIN)) return true;
        return System.currentTimeMillis() < trustedPlotExpiresAt
                - TRUSTED_PLOT_TTL_MS + PLOT_TP_WAIT_TICKS * 50L;
    }

    private int currentSidebarPlot() {
        for (String raw : Utils.getScoreboardSidebarLines()) {
            Matcher matcher = SIDEBAR_PLOT_PATTERN.matcher(Utils.stripColor(raw));
            if (matcher.find()) return Integer.parseInt(matcher.group(1));
        }
        return -1;
    }

    private boolean isOnPlot(int plot, Vec3 position, double margin) {
        double[] center = PLOT_CENTERS.get(plot);
        return center != null && position.x >= center[0] - 48.0 - margin
                && position.x < center[0] + 48.0 + margin
                && position.z >= center[2] - 48.0 - margin
                && position.z < center[2] + 48.0 + margin;
    }

    private int findVacuumSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getHoverName().getString().toLowerCase().contains("vacuum")) return i;
        }
        return -1;
    }

    private enum State {
        IDLE, ACTIVATING_FLY, TELEPORTING, PATHING_TO_PLOT, SCANNING, CHASING, VACUUMING,
        RETURNING,
        ALIGNING,
        DESCENDING,
        REWARP_WAIT
    }
}
