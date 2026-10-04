package xyz.whatsyouss.frosty.modules.impl.farming;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.HoeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.mixin.accessor.MinecraftAccessor;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.RenderUtils;
import xyz.whatsyouss.frosty.utility.Utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class FarmingMacro extends Module {

    private static final double ARRIVAL_XZ = 0.45;
    private static final double ROUTE_CORRIDOR = 0.75;
    private static final double MAX_GATE_OVERSHOOT = 1.25;
    private static final double RECOVERY_EXIT_CORRIDOR = 0.35;
    private static final int STALLED_MOVE_TICKS = 14;
    private static final float YAW_SNAP_DEG = 2.0f;
    private static final int TURN_MAX_TICKS = 40;
    private static final int PRE_WARP_TICKS = 20;
    private static final int WARP_COOLDOWN_TICKS = 20;
    private static final int WARP_TIMEOUT_TICKS = 200;
    private static final double WARP_DETECT_DIST = 8.0;
    private static final int PENDING_RESUME_TIMEOUT = 200;
    private final List<double[]> waypoints = new ArrayList<>();

    private SelectSetting face, hold;
    private String[] FACES = new String[]{"North", "South", "East", "West", "Custom"};
    private String[] FACESCN = new String[]{"北", "南", "东", "西", "自定义"};
    private String[] HOLDS = new String[]{"NONE", "W", "S"};
    private String[] HOLDSCN = new String[]{"无", "W", "S"};
    private SliderSetting yaw, pitch, stopTime, triggerAmount;
    private ButtonSetting rotateOnFinish, pestCleaner, rewarpOnly;

    private State state = State.IDLE;
    private int targetIndex = 1;
    private int lapCount = 0;
    private int dwellTicks = 0;
    private int dwellDurationTicks = 1;
    private int stalledMoveTicks = 0;
    private double previousRemaining = Double.POSITIVE_INFINITY;
    private boolean recoveringWaypoint = false;
    private int turnTicks = 0;
    private int preWarpTicks = 0;
    private int warpCooldown = 0;
    private int warpTimeout = 0;

    private boolean awaitingStart = false;
    private boolean pestPaused = false;
    private int pestResumePt = 1;
    private boolean pestCleanCompletedLap = false;

    private KeyMapping activeKey = null;
    private int hoeSlot = -1;
    public boolean running = false;

    private Vec3 preWarpPos = null;

    private boolean pendingResume = false;
    private int pendingResumeTick = 0;
    public FarmingMacro() {
        super("FarmingMacro", "农业宏", category.Farming);

        this.registerSetting(face = new SelectSetting("Face", "朝向", 0, FACES, FACESCN));
        this.registerSetting(yaw = new SliderSetting("Yaw", 0, -180, 180, 1, "偏航角"));
        this.registerSetting(pitch = new SliderSetting("Pitch", 0, -90, 90, 1, "俯仰角"));
        this.registerSetting(hold = new SelectSetting("Hold", "长按", 0, HOLDS, HOLDSCN));
        this.registerSetting(stopTime = new SliderSetting("Stop time", 500, 1000, 100, 6000, 50, "刹车时长"));
        this.registerSetting(rotateOnFinish = new ButtonSetting("Rotate on finish", "结束后反向", false));
        this.registerSetting(pestCleaner = new ButtonSetting("Pest cleaner", "害虫清理", true));
        this.registerSetting(triggerAmount = new SliderSetting("Trigger amount", 4, 1, 8, 1, "触发数量"));
        this.registerSetting(rewarpOnly = new ButtonSetting("Rewarp only", "只在本轮结束清理", true));
    }

    private static WorldDir yawToWorldDir(float yaw) {
        float y = Mth.wrapDegrees(yaw);
        if (y >= -45f && y < 45f) return WorldDir.SOUTH;
        if (y >= 45f && y < 135f) return WorldDir.WEST;
        if (y >= 135f || y < -135f) return WorldDir.NORTH;
        return WorldDir.EAST;
    }

    @Override
    public void guiUpdate() {
        this.yaw.setVisibilityCondition(() -> face.getValue() == 4);
        this.triggerAmount.setVisibilityCondition(() -> pestCleaner.isToggled());
        this.rewarpOnly.setVisibilityCondition(() -> pestCleaner.isToggled());
    }

    public List<double[]> getWaypoints() {
        return waypoints;
    }

    public void setWaypoints(List<double[]> loaded) {
        waypoints.clear();
        waypoints.addAll(loaded);
    }

    @Override
    public void onEnable() {
        running = false;
        awaitingStart = false;
        state = State.IDLE;
        if (ModuleManager.farmingProtector != null) ModuleManager.farmingProtector.resetRotationMonitor();
        releaseAll();
    }

    @Override
    public void onDisable() {
        if (ModuleManager.pestCleaner.isEnabled()) {
            ModuleManager.pestCleaner.disable();
        }
        pestPaused = false;
        stopMacro();
    }

    public void startMacro(int startIndex) {
        if (!Utils.nullCheck()) return;
        if (waypoints.size() < 2) {
            Utils.addModuleMessage(this.getName(), "§cNeed at least 2 waypoints");
            return;
        }

        hoeSlot = findHoeSlot();
        if (hoeSlot == -1) {
            Utils.addModuleMessage(this.getName(), "§cNo hoe found in hotbar");
            return;
        }
        mc.player.getInventory().setSelectedSlot(hoeSlot);
        FarmingStats.reset();
        if (ModuleManager.farmingProtector != null) ModuleManager.farmingProtector.resetRotationMonitor();

        FarmingProtector.stopped = false;
        lapCount = 0;
        targetIndex = Math.min(startIndex + 1, waypoints.size() - 1);
        dwellTicks = 0;
        dwellDurationTicks = 1;
        stalledMoveTicks = 0;
        previousRemaining = Double.POSITIVE_INFINITY;
        recoveringWaypoint = false;
        turnTicks = 0;
        preWarpTicks = 0;
        warpCooldown = 0;
        warpTimeout = 0;
        preWarpPos = null;
        activeKey = null;
        awaitingStart = true;
        pestCleanCompletedLap = false;
        pendingResume = false;
        pendingResumeTick = 0;

        releaseAll();
        prepareMouseForMacroStart();

        if (pestCleaner.isToggled() && rewarpOnly.isToggled() && checkAliveThreshold() && startIndex == 0) {
            pauseForPestClean();
            return;
        }
    }

    public void stopMacro() {
        boolean wasControllingMouse = running || awaitingStart;
        FarmingStats.freezeSession();
        running = false;
        awaitingStart = false;
        state = State.IDLE;
        if (ModuleManager.farmingProtector != null) ModuleManager.farmingProtector.resetRotationMonitor();
        activeKey = null;
        dwellTicks = 0;
        dwellDurationTicks = 1;
        stalledMoveTicks = 0;
        previousRemaining = Double.POSITIVE_INFINITY;
        recoveringWaypoint = false;
        preWarpTicks = 0;
        warpCooldown = 0;
        preWarpPos = null;
        pendingResume = false;
        pendingResumeTick = 0;
        releaseAll();
        if (wasControllingMouse
                && mc.screen == null
                && mc.getWindow().isFocused()
                && (ModuleManager.ungrabMouse == null || !ModuleManager.ungrabMouse.isEnabled())) {
            mc.mouseHandler.grabMouse();
        }
    }

    public boolean isControllingMouse() {
        return isEnabled() && (running || awaitingStart);
    }

    public boolean isFarmingState() {
        return isEnabled() && running && !pestPaused && !pendingResume
                && mc.screen == null
                && (state == State.TURNING || state == State.MOVING || state == State.DWELLING);
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck()) return;

        FarmingProtector protector = ModuleManager.farmingProtector;
        if (protector != null) protector.beforeMacroRotation(this);

        if (running || awaitingStart) FarmingStats.tick(mc, isFarmingState());

        if (awaitingStart) {
            if (mc.screen == null) {
                awaitingStart = false;
                running = true;
                beginTurning();
            }
            return;
        }
        if (!running) {
            return;
        }
        if (mc.mouseHandler.isMouseGrabbed()) {
            mc.mouseHandler.releaseMouse();
        }
        if (mc.screen != null) {
            releaseAll();
            return;
        }
        if (pestPaused) {
            releaseAll();
            return;
        }
        if (pendingResume) {
            releaseAll();
            pendingResumeTick++;
            if (!mc.player.onGround()) {
                if (mc.player.getAbilities().flying) {
                    mc.player.getAbilities().flying = false;
                    mc.player.onUpdateAbilities();
                }
                mc.options.keyShift.setDown(true);
            } else {
                mc.options.keyShift.setDown(false);
            }
            boolean grounded = mc.player.onGround();
            boolean timedOut = pendingResumeTick >= PENDING_RESUME_TIMEOUT;
            if (grounded || timedOut) {
                pendingResume = false;
                pendingResumeTick = 0;
                beginTurning();
            }
            return;
        }

        boolean farming = state == State.TURNING || state == State.MOVING || state == State.DWELLING;
        if (farming && ((MinecraftAccessor) mc).frosty$getMissTime() == 10000) {
            ((MinecraftAccessor) mc).frosty$setMissTime(0);
        }
        setKeyPressed(mc.options.keyAttack, farming);

        int slot = findHoeSlot();
        if (slot != -1) {
            hoeSlot = slot;
            mc.player.getInventory().setSelectedSlot(hoeSlot);
        }

        switch (state) {
            case TURNING -> tickTurning();
            case MOVING -> tickMoving();
            case DWELLING -> tickDwelling();
            case PRE_WARP -> tickPreWarp();
            case WARPING -> tickWarping();
            default -> {
            }
        }

        if (protector != null) protector.afterMacroRotation(this);

        if (pestCleaner.isToggled() && running && !pestPaused && !rewarpOnly.isToggled()) {
            checkPestTrigger();
        }
    }

    private void beginTurning() {
        releaseKeys();
        turnTicks = 0;
        state = State.TURNING;
    }

    private void tickTurning() {
        float tYaw = faceYaw();
        float tPitch = (float) pitch.getInput();
        snapYaw(tYaw, tPitch);
        turnTicks++;

        float delta = Math.abs(Mth.wrapDegrees(tYaw - mc.player.getYRot()));
        if (delta < YAW_SNAP_DEG || turnTicks >= TURN_MAX_TICKS) {
            beginMovingToTarget();
        }
    }

    private void beginMovingToTarget() {
        if (targetIndex >= waypoints.size()) {
            beginPreWarp();
            return;
        }

        double[] from = waypoints.get(targetIndex - 1);
        double[] to = waypoints.get(targetIndex);

        KeyMapping test = moveKeyFor(from, to);
        if (test == null) {
            Utils.addModuleMessage(this.getName(), "§eWaypoints #" + targetIndex
                    + " identical; skipping.");
            targetIndex++;
            beginMovingToTarget();
            return;
        }

        activeKey = null;
        stalledMoveTicks = 0;
        previousRemaining = Double.POSITIVE_INFINITY;
        recoveringWaypoint = false;
        state = State.MOVING;
    }

    private void tickMoving() {
        if (targetIndex >= waypoints.size()) {
            beginPreWarp();
            return;
        }

        lockYaw();

        double[] target = waypoints.get(targetIndex);
        Vec3 pos = mc.player.position();

        double[] from = waypoints.get(targetIndex - 1);
        boolean routeAlongX = Math.abs(target[0] - from[0]) >= Math.abs(target[2] - from[2]);
        double direction = Math.signum(routeAlongX ? target[0] - from[0] : target[2] - from[2]);
        double remaining = direction * ((routeAlongX ? target[0] - pos.x : target[2] - pos.z));
        double crossError = routeAlongX ? target[2] - pos.z : target[0] - pos.x;

        // Crossing the target plane counts as arrival, even if one tick overshoots it.
        boolean nearTargetPlane = remaining <= ARRIVAL_XZ && remaining >= -MAX_GATE_OVERSHOOT;
        if (nearTargetPlane && Math.abs(crossError) <= ROUTE_CORRIDOR) {
            releaseKeys();
            dwellTicks = 0;
            dwellDurationTicks = randomStopTicks();
            state = State.DWELLING;
            return;
        }

        stalledMoveTicks = remaining < previousRemaining - 0.005 ? 0 : stalledMoveTicks + 1;
        previousRemaining = remaining;
        if (Math.abs(crossError) > ROUTE_CORRIDOR || remaining < -ARRIVAL_XZ
                || stalledMoveTicks >= STALLED_MOVE_TICKS) recoveringWaypoint = true;
        if (recoveringWaypoint && Math.abs(crossError) < RECOVERY_EXIT_CORRIDOR
                && remaining > ARRIVAL_XZ && stalledMoveTicks == 0) recoveringWaypoint = false;

        if (recoveringWaypoint) {
            steerToward(target, pos, routeAlongX);
        } else {
            KeyMapping current = moveKeyFor(from, target);
            if (current == null) releaseKeys();
            else pressOnly(current);
        }
    }

    private void tickDwelling() {
        releaseKeys();
        dwellTicks++;
        if (dwellTicks >= dwellDurationTicks) {
            releaseKeys();
            targetIndex++;
            dwellTicks = 0;
            if (targetIndex >= waypoints.size()) {
                beginPreWarp();
            } else {
                beginMovingToTarget();
            }
        }
    }

    private void beginPreWarp() {
        releaseAll();
        preWarpTicks = 0;

        if (pestCleaner.isToggled() && rewarpOnly.isToggled() && checkAliveThreshold()) {
            state = State.IDLE;
            pauseForPestClean();
            return;
        }

        state = State.PRE_WARP;
    }

    private void checkPestTrigger() {
        if (rewarpOnly.isToggled()) return;
        if (mc.getConnection() == null) return;
        for (var entry : mc.getConnection().getOnlinePlayers()) {
            if (entry.getTabListDisplayName() == null) continue;
            String line = entry.getTabListDisplayName().getString().trim();
            if (!line.toLowerCase().contains("alive:")) continue;
            String after = line.substring(line.toLowerCase().indexOf("alive:") + 6).trim();
            String num = after.split("[^0-9]")[0];
            try {
                int alive = Integer.parseInt(num);
                if (alive >= (int) triggerAmount.getInput()) pauseForPestClean();
            } catch (NumberFormatException ignored) {
            }
            break;
        }
    }

    private boolean checkAliveThreshold() {
        if (mc.getConnection() == null) return false;
        for (var entry : mc.getConnection().getOnlinePlayers()) {
            if (entry.getTabListDisplayName() == null) continue;
            String line = entry.getTabListDisplayName().getString().trim();
            if (!line.toLowerCase().contains("alive:")) continue;
            String after = line.substring(line.toLowerCase().indexOf("alive:") + 6).trim();
            String num = after.split("[^0-9]")[0];
            try {
                return Integer.parseInt(num) >= (int) triggerAmount.getInput();
            } catch (NumberFormatException ignored) {
            }
            break;
        }
        return false;
    }

    public void pauseForPestClean() {
        if (pestPaused) return;
        pestPaused = true;
        pestResumePt = targetIndex;
        pestCleanCompletedLap = rewarpOnly.isToggled()
                && pestResumePt >= waypoints.size();
        releaseAll();

        boolean useRewarp = rewarpOnly.isToggled();

        Vec3 resumeVec = null;
        if (!useRewarp && pestResumePt < waypoints.size()) {
            double[] wp = waypoints.get(pestResumePt);
            resumeVec = new Vec3(wp[0], wp[1], wp[2]);
        }

        PestCleaner pc = ModuleManager.pestCleaner;
        if (pc != null) pc.requestPestClean(resumeVec, useRewarp);
    }

    public void resumeFromPestClean() {
        if (!pestPaused) return;
        pestPaused = false;

        if (rewarpOnly.isToggled()) {
            if (pestCleanCompletedLap) {
                lapCount++;
            }
            targetIndex = 1;
        } else {
            targetIndex = Math.max(1, Math.min(pestResumePt, waypoints.size() - 1));
        }
        pestCleanCompletedLap = false;

        hoeSlot = findHoeSlot();
        if (hoeSlot != -1) mc.player.getInventory().setSelectedSlot(hoeSlot);

        pendingResume = true;
        pendingResumeTick = 0;
    }

    private void tickPreWarp() {
        preWarpTicks++;
        if (preWarpTicks >= PRE_WARP_TICKS) {
            preWarpPos = mc.player.position();
            warpCooldown = WARP_COOLDOWN_TICKS;
            warpTimeout = 0;
            mc.player.connection.sendCommand("warp garden");
            state = State.WARPING;
        }
    }

    private void tickWarping() {
        releaseKeys();
        warpCooldown = Math.max(0, warpCooldown - 1);
        warpTimeout++;

        if (warpTimeout >= WARP_TIMEOUT_TICKS) {
            Utils.addModuleMessage(this.getName(), "§eWarp timed out, resuming...");
            resumeAfterWarp();
            return;
        }

        if (warpCooldown > 0) return;

        if (preWarpPos != null) {
            Vec3 now = mc.player.position();
            double dist = Math.sqrt(
                    Math.pow(now.x - preWarpPos.x, 2) +
                            Math.pow(now.z - preWarpPos.z, 2));
            if (dist >= WARP_DETECT_DIST) {
                resumeAfterWarp();
            }
        }
    }

    private void resumeAfterWarp() {
        lapCount++;
        targetIndex = 1;
        dwellTicks = 0;
        preWarpTicks = 0;
        warpCooldown = 0;
        warpTimeout = 0;
        preWarpPos = null;
        activeKey = null;
        beginTurning();
    }

    private void snapYaw(float targetYaw, float targetPitch) {
        if (mc.player == null) return;
        float cy = mc.player.getYRot();
        mc.player.setYRot(cy + Mth.wrapDegrees(targetYaw - cy) * 0.35f);
        float cp = mc.player.getXRot();
        mc.player.setXRot(cp + (targetPitch - cp) * 0.35f);
    }

    private void lockYaw() {
        if (mc.player == null) return;
        float targetYaw = faceYaw();
        float targetPitch = (float) pitch.getInput();
        float cy = mc.player.getYRot();
        float cp = mc.player.getXRot();
        mc.player.setYRot(cy + Mth.wrapDegrees(targetYaw - cy) * 0.15f);
        mc.player.setXRot(cp + (targetPitch - cp) * 0.15f);
    }

    private float faceYaw() {
        int faceIdx = (int) face.getValue();
        if (faceIdx == 4) {
            float customYaw = (float) yaw.getInput();
            return Mth.wrapDegrees(customYaw + (rotateOnFinish.isToggled() && (lapCount % 2 == 1) ? 180f : 0f));
        }
        if (rotateOnFinish.isToggled() && (lapCount % 2 == 1)) {
            faceIdx = oppositeCardinalIdx(faceIdx);
        }
        return switch (faceIdx) {
            case 0 -> 180f;
            case 1 -> 0f;
            case 2 -> -90f;
            case 3 -> 90f;
            default -> 0f;
        };
    }

    private int oppositeCardinalIdx(int idx) {
        return switch (idx) {
            case 0 -> 1;
            case 1 -> 0;
            case 2 -> 3;
            case 3 -> 2;
            default -> idx;
        };
    }

    private KeyMapping moveKeyFor(double[] from, double[] to) {
        double dx = to[0] - from[0];
        double dz = to[2] - from[2];

        WorldDir needed;
        if (Math.abs(dx) >= Math.abs(dz)) {
            if (Math.abs(dx) < 0.1) return null;
            needed = dx > 0 ? WorldDir.EAST : WorldDir.WEST;
        } else {
            if (Math.abs(dz) < 0.1) return null;
            needed = dz > 0 ? WorldDir.SOUTH : WorldDir.NORTH;
        }

        WorldDir forward = yawToWorldDir(faceYaw());

        if (needed == forward) return mc.options.keyUp;
        if (needed == forward.opposite()) return mc.options.keyDown;
        if (needed == forward.clockwise()) return mc.options.keyRight;
        if (needed == forward.counterClockwise()) return mc.options.keyLeft;

        return null;
    }

    private void steerToward(double[] target, Vec3 pos, boolean routeAlongX) {
        double dx = Mth.clamp(target[0] - pos.x, -2.0, 2.0);
        double dz = Mth.clamp(target[2] - pos.z, -2.0, 2.0);
        if (routeAlongX) dz *= 2.0;
        else dx *= 2.0;
        double yawRadians = Math.toRadians(mc.player.getYRot());
        double forwardX = -Math.sin(yawRadians);
        double forwardZ = Math.cos(yawRadians);
        double rightX = -Math.cos(yawRadians);
        double rightZ = -Math.sin(yawRadians);
        int bestForward = 0;
        int bestStrafe = 0;
        double bestScore = -Double.MAX_VALUE;

        // Recovery uses the actual yaw, so diagonal Custom angles can still reach the recorded point.
        for (int forward = -1; forward <= 1; forward++) {
            for (int strafe = -1; strafe <= 1; strafe++) {
                if (forward == 0 && strafe == 0) continue;
                double vx = forward * forwardX + strafe * rightX;
                double vz = forward * forwardZ + strafe * rightZ;
                double score = (vx * dx + vz * dz) / Math.hypot(vx, vz);
                if (score > bestScore) {
                    bestScore = score;
                    bestForward = forward;
                    bestStrafe = strafe;
                }
            }
        }

        setKeyPressed(mc.options.keyUp, bestForward > 0);
        setKeyPressed(mc.options.keyDown, bestForward < 0);
        setKeyPressed(mc.options.keyLeft, bestStrafe < 0);
        setKeyPressed(mc.options.keyRight, bestStrafe > 0);
    }

    private void pressOnly(KeyMapping key) {
        pressMovementKeys(key);
    }

    private void pressMovementKeys(KeyMapping key) {
        int held = (int) hold.getValue();
        // An opposite hold would cancel the waypoint key and stall the macro.
        setKeyPressed(mc.options.keyUp, key == mc.options.keyUp || (held == 1 && key != mc.options.keyDown));
        setKeyPressed(mc.options.keyDown, key == mc.options.keyDown || (held == 2 && key != mc.options.keyUp));
        setKeyPressed(mc.options.keyLeft, key == mc.options.keyLeft);
        setKeyPressed(mc.options.keyRight, key == mc.options.keyRight);
    }

    private int randomStopTicks() {
        int minMs = (int) Math.round(Math.min(stopTime.getInputMin(), stopTime.getInputMax()));
        int maxMs = (int) Math.round(Math.max(stopTime.getInputMin(), stopTime.getInputMax()));
        int minTicks = msToTicks(minMs);
        int maxTicks = msToTicks(maxMs);
        return ThreadLocalRandom.current().nextInt(minTicks, maxTicks + 1);
    }

    private void releaseKeys() {
        setKeyPressed(mc.options.keyUp, false);
        setKeyPressed(mc.options.keyDown, false);
        setKeyPressed(mc.options.keyLeft, false);
        setKeyPressed(mc.options.keyRight, false);
    }

    private void releaseAll() {
        releaseKeys();
        setKeyPressed(mc.options.keyAttack, false);
        mc.options.keyShift.setDown(false);
    }

    private void setKeyPressed(KeyMapping key, boolean pressed) {
        key.setDown(pressed);
    }

    private void prepareMouseForMacroStart() {
        mc.mouseHandler.releaseMouse();
        GLFW.glfwSetInputMode(mc.getWindow().handle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        if (mc.screen == null) {
            awaitingStart = false;
            running = true;
            beginTurning();
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (waypoints.isEmpty()) return;

        for (int i = 0; i < waypoints.size(); i++) {
            double[] wp = waypoints.get(i);
            BlockPos bp = new BlockPos(
                    (int) Math.floor(wp[0]),
                    (int) Math.floor(wp[1]),
                    (int) Math.floor(wp[2]));

            Color color;
            if (!running) {
                color = Color.CYAN;
            } else if (i < targetIndex) {
                color = Color.GREEN;
            } else if (i == targetIndex) {
                color = Color.PINK;
            } else {
                color = Color.RED;
            }

            RenderUtils.drawBox(event.getMatrix(), bp, color, 2f, false);

            String label = "#" + (i + 1);
            if (running && i == targetIndex) {
                label = "➔ #" + (i + 1);
            }

            double textX = wp[0];
            double textY = wp[1] + 3;
            double textZ = wp[2];

            RenderUtils.drawText3D(event.getMatrix(), label, textX, textY, textZ, color, false);
        }
    }

    private int findHoeSlot() {
        Inventory inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i).getItem() instanceof HoeItem) return i;
        }
        return -1;
    }

    private int msToTicks(int ms) {
        return Math.max(1, ms / 50);
    }

    private enum State {
        IDLE, TURNING, MOVING, DWELLING, PRE_WARP, WARPING
    }

    private enum WorldDir {
        NORTH, SOUTH, EAST, WEST;

        WorldDir opposite() {
            return switch (this) {
                case NORTH -> SOUTH;
                case SOUTH -> NORTH;
                case EAST -> WEST;
                case WEST -> EAST;
            };
        }

        WorldDir clockwise() {
            return switch (this) {
                case NORTH -> EAST;
                case EAST -> SOUTH;
                case SOUTH -> WEST;
                case WEST -> NORTH;
            };
        }

        WorldDir counterClockwise() {
            return clockwise().opposite();
        }
    }
}
