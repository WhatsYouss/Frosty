package xyz.whatsyouss.frosty.utility;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import xyz.whatsyouss.frosty.utility.pathfinding.NavMeshGenerator;
import xyz.whatsyouss.frosty.utility.pathfinding.NavMeshPath;
import xyz.whatsyouss.frosty.utility.pathfinding.PathfindingService;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PathfinderUtils {

    private static PathfinderUtils instance;
    public static PathfinderUtils getInstance() {
        if (instance == null) instance = new PathfinderUtils();
        return instance;
    }

    // ── 状态 ──────────────────────────────────────────────────────

    private NavMeshPath  currentPath       = null;
    private List<Vec3d>  waypoints         = null;
    private int          pathIndex         = 0;
    private boolean      pathfinding       = false;
    private boolean      navigating        = false;
    private Vec3d        dest              = null;
    private Runnable     onArrival         = null;
    private int          maxRange          = 256;

    // ── 路点高度过滤 ──────────────────────────────────────────────
    // 设置后，低于此 Y 的路点将被过滤掉；默认 Double.NEGATIVE_INFINITY（不过滤）
    private double minWaypointY = Double.NEGATIVE_INFINITY;

    // ── 卡住检测 ──────────────────────────────────────────────────

    private int   lastProgressIndex  = -1;
    private int   noProgressTicks    = 0;
    private Vec3d lastStuckCheckPos  = null;
    private int   positionStuckTicks = 0;

    private boolean strafingToRecover  = false;
    private boolean strafeRight        = false;
    private int     strafeTicks        = 0;
    private boolean repathingFromStuck = false;

    private static final int STRAFE_THRESHOLD = 40;
    private static final int STUCK_THRESHOLD  = 80;

    // ── 前瞻 ──────────────────────────────────────────────────────

    private int lookAheadCooldown = 0;
    private static final int LOOK_AHEAD_INTERVAL = 10;

    // ── 到达判定 ──────────────────────────────────────────────────

    private static final double WAYPOINT_REACH = 1.2;
    private static final double ARRIVAL_REACH  = 0.5;

    // ── 跳跃节流 ──────────────────────────────────────────────────

    private int jumpCooldown = 0;

    // ── 渲染颜色 ──────────────────────────────────────────────────

    private static final Color COLOR_PASSED         = new Color(120, 120, 120, 60);
    private static final Color COLOR_AHEAD          = new Color(0, 200, 255, 130);
    private static final Color COLOR_TARGET         = new Color(255, 220, 0, 200);
    private static final Color COLOR_OUTLINE_AHEAD  = new Color(0, 200, 255, 255);
    private static final Color COLOR_OUTLINE_TARGET = new Color(255, 220, 0, 255);
    private static final double WP_HALF = 0.12;
    private static final double WP_H    = 0.08;

    // ── 公共 API ──────────────────────────────────────────────────

    /** 最简重载，不限制路点高度 */
    public void startPathTo(Vec3d destination, ClientWorld world, Runnable callback) {
        startPathTo(destination, world, callback, 512, Double.NEGATIVE_INFINITY);
    }

    /** 指定 range 重载 */
    public void startPathTo(Vec3d destination, ClientWorld world, Runnable callback, int range) {
        startPathTo(destination, world, callback, range, Double.NEGATIVE_INFINITY);
    }

    /**
     * 完整重载：同时指定 range 和路点最低 Y。
     * CommissionMacro 传入 minWaypointY=120.0 以避免寻路进入矿洞深处。
     */
    public void startPathTo(Vec3d destination, ClientWorld world, Runnable callback,
                            int range, double minWaypointY) {
        if (pathfinding) pathfinding = false;

        Vec3d requestedDest = destination;

        resetState();
        this.dest           = destination;
        this.onArrival      = callback;
        this.maxRange       = range;
        this.minWaypointY   = minWaypointY;
        this.pathfinding    = true;

        PathfindingService.getInstance()
                .findHybridPath(mc().player.getEntityPos(), destination, world, range)
                .thenAccept(result -> mc().execute(() -> {
                    if (dest == null || !dest.equals(requestedDest)) return;

                    pathfinding = false;
                    if (result.isFound() && !result.getPath().getWaypoints().isEmpty()) {
                        currentPath = result.getPath();
                        waypoints   = applyWaypointFilter(currentPath.getWaypoints());
                        if (waypoints.isEmpty()) {
                            Utils.addChatMessage("[PF] 路径过滤后为空（minY="
                                    + (int) minWaypointY + "），寻路失败");
                            return;
                        }
                        pathIndex  = 0;
                        navigating = true;
                        Utils.addChatMessage("[PF] 路径找到，" + waypoints.size() + " 个路点");
                    } else {
                        Utils.addChatMessage("[PF] 寻路失败（range=" + range + "，dist="
                                + String.format("%.0f", mc().player != null
                                ? mc().player.getEntityPos().distanceTo(requestedDest) : -1)
                                + "）");
                    }
                }));
    }

    /**
     * 注入外部预先计算好的路点列表，立即开始导航（用于战斗预取路径）。
     * 路点会经过同样的 minWaypointY 过滤。
     */
    public void injectPath(List<Vec3d> precomputedWaypoints, Vec3d destination,
                           Runnable onArrivalCallback, double minWaypointY) {
        if (precomputedWaypoints == null || precomputedWaypoints.isEmpty()) return;
        resetState();
        this.minWaypointY = minWaypointY;
        this.dest         = destination;
        this.onArrival    = onArrivalCallback;
        waypoints         = applyWaypointFilter(precomputedWaypoints);
        if (waypoints.isEmpty()) {
            Utils.addChatMessage("[PF] 注入路径过滤后为空，跳过");
            return;
        }
        pathIndex  = 0;
        navigating = true;
    }

    public void repath(ClientWorld world) {
        if (dest == null) return;
        Vec3d currentDest       = dest;
        double savedMinWpY      = this.minWaypointY;
        pathfinding             = true;

        PathfindingService.getInstance()
                .findHybridPath(mc().player.getEntityPos(), currentDest, world, maxRange)
                .thenAccept(result -> mc().execute(() -> {
                    if (dest == null || !dest.equals(currentDest)) return;
                    pathfinding        = false;
                    repathingFromStuck = false;
                    if (result.isFound() && !result.getPath().getWaypoints().isEmpty()) {
                        currentPath        = result.getPath();
                        this.minWaypointY  = savedMinWpY;
                        waypoints          = applyWaypointFilter(currentPath.getWaypoints());
                        pathIndex          = 0;
                        lastProgressIndex  = -1;
                        positionStuckTicks = 0;
                        lastStuckCheckPos  = null;
                    }
                }));
    }

    /** 路点高度过滤：移除 Y < minWaypointY 的路点 */
    private List<Vec3d> applyWaypointFilter(List<Vec3d> raw) {
        if (minWaypointY == Double.NEGATIVE_INFINITY) return raw;
        List<Vec3d> filtered = raw.stream()
                .filter(wp -> wp.y >= minWaypointY)
                .collect(Collectors.toList());
        if (filtered.size() < raw.size()) {
            Utils.addChatMessage("[PF] 过滤掉 " + (raw.size() - filtered.size())
                    + " 个低于 Y=" + (int) minWaypointY + " 的路点");
        }
        return filtered;
    }

    /**
     * 每 tick 调用，返回 true 表示已到达。
     */
    public boolean tick() {
        ClientPlayerEntity player = mc().player;
        ClientWorld world = mc().world;
        if (player == null || world == null) return false;
        if (!navigating || waypoints == null) return false;

        PathfindingService.getInstance().tick();

        Vec3d playerPos = player.getEntityPos();

        // ── dest 直接距离检测 ─────────────────────────────────────
        if (dest != null) {
            double dx = dest.x - playerPos.x, dz = dest.z - playerPos.z;
            double hDist = Math.sqrt(dx * dx + dz * dz);
            if (hDist < 4.0 && Math.abs(dest.y - playerPos.y) < 4.0) {
                arrive();
                return true;
            }
        }

        if (pathIndex >= waypoints.size()) { arrive(); return true; }

        advanceWaypoints(playerPos);

        if (pathIndex >= waypoints.size()) { arrive(); return true; }

        doLookAhead(playerPos, world);

        handleStuck(player, world);
        if (repathingFromStuck) return false;

        moveToward(player, waypoints.get(pathIndex));
        return false;
    }

    public void stop() {
        releaseKeys();
        resetState();
    }

    public boolean isNavigating()  { return navigating; }
    public boolean isPathfinding() { return pathfinding; }
    public boolean isBusy()        { return navigating || pathfinding; }
    public Vec3d   getDest()       { return dest; }

    // ── Render3DEvent ─────────────────────────────────────────────
    // 关键修复：必须有 @EventHandler 注解，Orbit 才会注册并调用此方法

    @EventHandler
    public void onRender3D(xyz.whatsyouss.frosty.events.impl.Render3DEvent event) {
        if (!Utils.nullCheck()) return;
        if (waypoints == null || waypoints.isEmpty()) return;
        if (!navigating && !pathfinding) return;

        MatrixStack stack = event.getMatrix();

        for (int i = 0; i < waypoints.size(); i++) {
            Vec3d wp = waypoints.get(i);
            Box box = waypointBox(wp);

            if (i < pathIndex) {
                RenderUtils.drawBoxFilled(stack, box, COLOR_PASSED);
            } else if (i == pathIndex) {
                RenderUtils.drawBoxFilled(stack, box, COLOR_TARGET);
                RenderUtils.drawBox(stack, box, COLOR_OUTLINE_TARGET, 1.5);
            } else {
                RenderUtils.drawBoxFilled(stack, box, COLOR_AHEAD);
                RenderUtils.drawBox(stack, box, COLOR_OUTLINE_AHEAD, 1.0);
            }

            if (i + 1 < waypoints.size() && i >= pathIndex - 1) {
                drawSegment(stack, wp, waypoints.get(i + 1),
                        i < pathIndex ? COLOR_PASSED : COLOR_AHEAD);
            }
        }

        if (dest != null) {
            Box destBox = new Box(dest.x-.25, dest.y, dest.z-.25, dest.x+.25, dest.y+.15, dest.z+.25);
            RenderUtils.drawBox(stack, destBox, new Color(255, 80, 80, 200), 2.0);
        }
    }

    private void drawSegment(MatrixStack stack, Vec3d from, Vec3d to, Color color) {
        double dx = to.x-from.x, dy = to.y-from.y, dz = to.z-from.z;
        double len = Math.sqrt(dx*dx + dz*dz);
        if (len < 0.5) return;
        int dots = Math.max(1, (int)(len / 0.5));
        double r = 0.04;
        Color dotColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 80);
        for (int d = 1; d < dots; d++) {
            double t = (double)d/dots;
            double cx = from.x+dx*t, cy = from.y+dy*t, cz = from.z+dz*t;
            RenderUtils.drawBoxFilled(stack, new Box(cx-r,cy,cz-r,cx+r,cy+r*2,cz+r), dotColor);
        }
    }

    private static Box waypointBox(Vec3d wp) {
        return new Box(wp.x-WP_HALF, wp.y, wp.z-WP_HALF, wp.x+WP_HALF, wp.y+WP_H, wp.z+WP_HALF);
    }

    // ── 路点推进（Crossing-plane） ────────────────────────────────

    private void advanceWaypoints(Vec3d playerPos) {
        while (pathIndex < waypoints.size()) {
            Vec3d wp    = waypoints.get(pathIndex);
            boolean isLast = pathIndex == waypoints.size() - 1;
            double reach   = isLast ? ARRIVAL_REACH : WAYPOINT_REACH;

            double dx = wp.x - playerPos.x, dz = wp.z - playerPos.z;
            double hDist = Math.sqrt(dx*dx + dz*dz);

            if (hDist < reach && Math.abs(wp.y - playerPos.y) < 2.5) {
                pathIndex++; continue;
            }

            if (pathIndex + 1 < waypoints.size()) {
                Vec3d next = waypoints.get(pathIndex + 1);
                double dirX = next.x-wp.x, dirZ = next.z-wp.z;
                double len  = Math.sqrt(dirX*dirX + dirZ*dirZ);
                if (len > 0.01) {
                    dirX /= len; dirZ /= len;
                    double dot     = (playerPos.x-wp.x)*dirX + (playerPos.z-wp.z)*dirZ;
                    double lateral = Math.abs((playerPos.x-wp.x)*(-dirZ) + (playerPos.z-wp.z)*dirX);
                    double maxLat  = MathHelper.clamp((float)(len*0.4), 1.5f, 4.0f);
                    if (dot > -0.1 && lateral < maxLat) { pathIndex++; continue; }
                }
            }
            break;
        }
    }

    // ── 前瞻 ──────────────────────────────────────────────────────

    private void doLookAhead(Vec3d playerPos, ClientWorld world) {
        if (--lookAheadCooldown > 0 || waypoints == null) return;
        lookAheadCooldown = LOOK_AHEAD_INTERVAL;
        int limit = Math.min(pathIndex + 20, waypoints.size() - 1);
        for (int i = limit; i > pathIndex; i--) {
            Vec3d wp = waypoints.get(i);
            if (playerPos.squaredDistanceTo(wp) > 1024) continue;
            if (wp.y - playerPos.y > 1.0) continue;
            if (hasLOS(playerPos, wp, world)) { pathIndex = i; break; }
        }
    }

    private boolean hasLOS(Vec3d from, Vec3d to, ClientWorld world) {
        double dx = to.x-from.x, dy = to.y-from.y, dz = to.z-from.z;
        double dist = Math.sqrt(dx*dx + dz*dz);
        if (dist < 0.5) return true;

        int steps = (int) Math.ceil(dist / 0.4);
        for (int i = 1; i < steps; i++) {
            double t  = (double)i/steps;
            double cx = from.x+dx*t, cy = from.y+dy*t, cz = from.z+dz*t;

            boolean ground = false; double actualY = cy;
            for (int yo = 1; yo >= -1 && !ground; yo--) {
                BlockPos gp = BlockPos.ofFloored(cx, cy+yo, cz);
                if (!world.isChunkLoaded(gp.getX()>>4, gp.getZ()>>4)) return false;
                var gs = world.getBlockState(gp).getCollisionShape(world, gp);
                if (!gs.isEmpty()) {
                    double top = gp.getY() + gs.getMax(net.minecraft.util.math.Direction.Axis.Y);
                    if (top >= cy-1.5 && top <= cy+1.0) { ground = true; actualY = top; }
                }
            }
            if (!ground) return false;

            for (int by = (int)Math.floor(actualY); by <= (int)Math.floor(actualY+1.8); by++) {
                BlockPos cp = BlockPos.ofFloored(cx, by, cz);
                if (!world.isChunkLoaded(cp.getX()>>4, cp.getZ()>>4)) return false;
                var cs = world.getBlockState(cp);
                var sh = cs.getCollisionShape(world, cp);
                if (!sh.isEmpty()) {
                    double bot = by+sh.getMin(net.minecraft.util.math.Direction.Axis.Y);
                    double top = by+sh.getMax(net.minecraft.util.math.Direction.Axis.Y);
                    if (top > actualY+0.01 && bot < actualY+1.8) return false;
                }
                if (NavMeshGenerator.isNonSolidObstacle(cs) || NavMeshGenerator.isHazardous(cs)) return false;
            }
        }
        return true;
    }

    // ── 移动控制 ──────────────────────────────────────────────────

    private void moveToward(ClientPlayerEntity player, Vec3d target) {
        Vec3d pos = player.getEntityPos();
        double dx = target.x-pos.x, dz = target.z-pos.z;
        double hDist = Math.sqrt(dx*dx + dz*dz);

        if (hDist < 0.3) { setKey(mc().options.forwardKey, true); return; }

        float targetYaw = yaw(dx, dz);
        float curYaw    = player.getYaw();
        float diff      = MathHelper.wrapDegrees(targetYaw - curYaw);
        player.setYaw(curYaw + MathHelper.clamp(diff, -15f, 15f));

        float absDiff = Math.abs(diff);

        if (!strafingToRecover) {
            if (absDiff > 145f) {
                setKey(mc().options.forwardKey, false);
                setKey(mc().options.backKey,    true);
                setKey(mc().options.leftKey,    diff < 0);
                setKey(mc().options.rightKey,   diff > 0);
                return;
            }
            setKey(mc().options.leftKey,  absDiff > 20f && diff < 0);
            setKey(mc().options.rightKey, absDiff > 20f && diff > 0);
        }

        setKey(mc().options.backKey,    false);
        setKey(mc().options.forwardKey, true);
        setKey(mc().options.sprintKey,  absDiff < 40f);

        // 跳跃
        if (jumpCooldown > 0) { jumpCooldown--; setKey(mc().options.jumpKey, false); return; }

        boolean needJump = false;
        if (target.y - pos.y > 0.5 && hDist < 2.0 && player.isOnGround()) needJump = true;

        if (!needJump && player.isOnGround() && hDist > 0.3 && mc().world != null) {
            double nx = dx/hDist, nz = dz/hDist;
            outer:
            for (double probe : new double[]{0.5, 0.9, 1.3}) {
                double px = pos.x+nx*probe, pz = pos.z+nz*probe;
                for (int yo = 0; yo <= 1; yo++) {
                    BlockPos bp = BlockPos.ofFloored(px, pos.y+yo, pz);
                    if (!mc().world.isChunkLoaded(bp.getX()>>4, bp.getZ()>>4)) break;
                    var sh = mc().world.getBlockState(bp).getCollisionShape(mc().world, bp);
                    if (!sh.isEmpty()) {
                        double top = bp.getY() + sh.getMax(net.minecraft.util.math.Direction.Axis.Y);
                        double stepH = top - pos.y;
                        if (stepH > 0.6 && stepH <= NavMeshGenerator.getMaxJumpHeight()) { needJump = true; break outer; }
                    }
                }
            }
        }

        if (needJump && player.isOnGround()) { setKey(mc().options.jumpKey, true); jumpCooldown = 5; }
        else setKey(mc().options.jumpKey, false);
    }

    // ── 卡住检测 & 恢复 ───────────────────────────────────────────

    private void handleStuck(ClientPlayerEntity player, ClientWorld world) {
        Vec3d pos = player.getEntityPos();

        if (pathIndex != lastProgressIndex) {
            lastProgressIndex  = pathIndex;
            noProgressTicks    = 0;
            positionStuckTicks = 0;
            lastStuckCheckPos  = null;
            if (strafingToRecover) {
                strafingToRecover = false;
                setKey(mc().options.leftKey,  false);
                setKey(mc().options.rightKey, false);
                setKey(mc().options.backKey,  false);
            }
            return;
        }

        noProgressTicks++;
        if (lastStuckCheckPos != null) {
            double moved = pos.squaredDistanceTo(lastStuckCheckPos);
            if (moved < 0.04) positionStuckTicks++;
            else positionStuckTicks = Math.max(0, positionStuckTicks - 2);
        }
        lastStuckCheckPos = pos;

        boolean isStuck     = noProgressTicks >= STRAFE_THRESHOLD && positionStuckTicks >= STRAFE_THRESHOLD / 2;
        boolean isVeryStuck = noProgressTicks >= STUCK_THRESHOLD  && positionStuckTicks >= STUCK_THRESHOLD  / 2;

        if (isStuck && !isVeryStuck && !strafingToRecover && !repathingFromStuck) {
            strafingToRecover = true;
            strafeRight       = Math.random() > 0.5;
            strafeTicks       = 0;
        }

        if (strafingToRecover && !repathingFromStuck) {
            strafeTicks++;
            if (strafeTicks <= 15) {
                setKey(mc().options.forwardKey, false);
                setKey(mc().options.backKey,    true);
                setKey(mc().options.leftKey,    false);
                setKey(mc().options.rightKey,   false);
                if (player.isOnGround()) setKey(mc().options.jumpKey, true);
            } else {
                setKey(mc().options.backKey, false);
                if (strafeTicks % 10 == 0) strafeRight = !strafeRight;
                setKey(mc().options.leftKey,    !strafeRight);
                setKey(mc().options.rightKey,   strafeRight);
                setKey(mc().options.forwardKey, true);
                if (player.isOnGround()) setKey(mc().options.jumpKey, true);
            }
        }

        if (isVeryStuck && !repathingFromStuck) {
            strafingToRecover = false;
            setKey(mc().options.leftKey,  false);
            setKey(mc().options.rightKey, false);
            setKey(mc().options.backKey,  false);

            repathingFromStuck = true;
            noProgressTicks    = 0;

            PathfindingService.getInstance().blacklistArea(player.getEntityPos(), 2.0, 3000);
            if (pathIndex < waypoints.size())
                PathfindingService.getInstance().blacklistArea(waypoints.get(pathIndex), 1.5, 3000);

            repath(world);
        }
    }

    // ── 到达 ──────────────────────────────────────────────────────

    private void arrive() {
        releaseKeys();
        navigating = false;
        Runnable cb = onArrival;
        onArrival = null;
        if (cb != null) cb.run();
    }

    // ── 工具 ──────────────────────────────────────────────────────

    private void setKey(net.minecraft.client.option.KeyBinding key, boolean pressed) { key.setPressed(pressed); }

    private void releaseKeys() {
        if (mc().options == null) return;
        setKey(mc().options.forwardKey, false);
        setKey(mc().options.backKey,    false);
        setKey(mc().options.leftKey,    false);
        setKey(mc().options.rightKey,   false);
        setKey(mc().options.sprintKey,  false);
        setKey(mc().options.jumpKey,    false);
    }

    private void resetState() {
        currentPath        = null;
        waypoints          = null;
        pathIndex          = 0;
        pathfinding        = false;
        navigating         = false;
        dest               = null;
        onArrival          = null;
        lastProgressIndex  = -1;
        noProgressTicks    = 0;
        positionStuckTicks = 0;
        lastStuckCheckPos  = null;
        strafingToRecover  = false;
        repathingFromStuck = false;
        strafeTicks        = 0;
        lookAheadCooldown  = 0;
        jumpCooldown       = 0;
        minWaypointY       = Double.NEGATIVE_INFINITY;
    }

    private float yaw(double dx, double dz) { return (float) Math.toDegrees(Math.atan2(-dx, dz)); }
    private static MinecraftClient mc() { return MinecraftClient.getInstance(); }
}