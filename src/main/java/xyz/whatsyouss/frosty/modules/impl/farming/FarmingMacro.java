package xyz.whatsyouss.frosty.modules.impl.farming;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.HoeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
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

public class FarmingMacro extends Module {

    private static final double ARRIVAL_XZ = 0.45;
    private static final float YAW_SNAP_DEG = 2.0f;
    private static final int TURN_MAX_TICKS = 40;
    private static final int PRE_WARP_TICKS = 20;
    private static final int WARP_COOLDOWN_TICKS = 20;
    private static final int WARP_TIMEOUT_TICKS = 200;
    private static final double WARP_DETECT_DIST = 8.0;
    private static final int PENDING_RESUME_TIMEOUT = 200;
    private final List<double[]> waypoints = new ArrayList<>();
    private SelectSetting face;
    private String[] FACES = new String[]{"North", "South", "East", "West"};
    private SliderSetting pitch, stopTime, triggerAmount;
    private ButtonSetting rotateOnFinish, pestCleaner, rewarpOnly;
    private State state = State.IDLE;
    private int targetIndex = 1;
    private int lapCount = 0;
    private int dwellTicks = 0;
    private int turnTicks = 0;
    private int preWarpTicks = 0;
    private int warpCooldown = 0;
    private int warpTimeout = 0;

    private boolean awaitingGrab = false;
    private boolean pestPaused = false;
    private int pestResumePt = 1;

    private KeyMapping activeKey = null;
    private int hoeSlot = -1;
    public boolean running = false;

    private Vec3 preWarpPos = null;

    private boolean pendingResume = false;
    private int pendingResumeTick = 0;
    public FarmingMacro() {
        super("FarmingMacro", category.Farming);

        this.registerSetting(face = new SelectSetting("Face", 0, FACES));
        this.registerSetting(pitch = new SliderSetting("Pitch", 0, -10, 10, 1));
        this.registerSetting(stopTime = new SliderSetting("Stop time", 500, 100, 6000, 50));
        this.registerSetting(rotateOnFinish = new ButtonSetting("Rotate on finish", false));
        this.registerSetting(pestCleaner = new ButtonSetting("Pest cleaner", true));
        this.registerSetting(triggerAmount = new SliderSetting("Trigger amount", 4, 1, 8, 1));
        this.registerSetting(rewarpOnly = new ButtonSetting("Rewarp only", true));
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
        state = State.IDLE;
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

        lapCount = 0;
        targetIndex = Math.min(startIndex + 1, waypoints.size() - 1);
        dwellTicks = 0;
        turnTicks = 0;
        preWarpTicks = 0;
        warpCooldown = 0;
        warpTimeout = 0;
        preWarpPos = null;
        activeKey = null;
        awaitingGrab = true;

        releaseAll();

        if (pestCleaner.isToggled() && rewarpOnly.isToggled() && checkAliveThreshold()) {
            pauseForPestClean();
            return;
        }
    }

    public void stopMacro() {
        running = false;
        state = State.IDLE;
        activeKey = null;
        dwellTicks = 0;
        preWarpTicks = 0;
        warpCooldown = 0;
        preWarpPos = null;
        releaseAll();
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck()) return;

        if (awaitingGrab) {
            if (mc.mouseHandler.isMouseGrabbed()) {
                awaitingGrab = false;
                running = true;
                beginTurning();
            }
            return;
        }
        if (!running) {
            return;
        }
        if (!mc.mouseHandler.isMouseGrabbed()) {
            if (mc.gui.screen() == null) {
                mc.mouseHandler.grabMouse();
                setKeyPressed(mc.options.keyAttack, true);
            } else {
                setKeyPressed(mc.options.keyAttack, false);
            }
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

        if (!mc.options.keyAttack.isDown()) {
            setKeyPressed(mc.options.keyAttack, true);
        }

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

        boolean arrivedX = Math.abs(pos.x - target[0]) < ARRIVAL_XZ;
        boolean arrivedZ = Math.abs(pos.z - target[2]) < ARRIVAL_XZ;

        if (arrivedX && arrivedZ) {
            releaseKeys();
            dwellTicks = 0;
            state = State.DWELLING;
            return;
        }

        double[] from = waypoints.get(targetIndex - 1);
        KeyMapping current = moveKeyFor(from, target);
        if (current == null) {
            releaseKeys();
        } else {
            pressOnly(current);
        }
    }

    private void tickDwelling() {
        if (targetIndex - 1 >= 0 && targetIndex < waypoints.size()) {
            double[] from = waypoints.get(targetIndex - 1);
            double[] to = waypoints.get(targetIndex);
            KeyMapping cur = moveKeyFor(from, to);
            if (cur != null) setKeyPressed(cur, true);
        } else if (activeKey != null) {
            setKeyPressed(activeKey, true);
        }

        dwellTicks++;
        if (dwellTicks >= msToTicks((int) stopTime.getInput())) {
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
            targetIndex = 1;
        } else {
            targetIndex = Math.max(1, Math.min(pestResumePt, waypoints.size() - 1));
        }

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

    private void pressOnly(KeyMapping key) {
        setKeyPressed(mc.options.keyUp, key == mc.options.keyUp);
        setKeyPressed(mc.options.keyDown, key == mc.options.keyDown);
        setKeyPressed(mc.options.keyLeft, key == mc.options.keyLeft);
        setKeyPressed(mc.options.keyRight, key == mc.options.keyRight);
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