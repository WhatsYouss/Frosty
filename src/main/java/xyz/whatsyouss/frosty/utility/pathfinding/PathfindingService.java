package xyz.whatsyouss.frosty.utility.pathfinding;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class PathfindingService {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "frosty-pathfinder");
        t.setDaemon(true);
        return t;
    });
    private static PathfindingService instance;
    private final NavMeshGenerator generator = new NavMeshGenerator();
    private final NavMeshPathfinder pathfinder = new NavMeshPathfinder();
    private final FlyPathfinder flyPathfinder = new FlyPathfinder();

    private final List<BlacklistEntry> blacklist = new CopyOnWriteArrayList<>();

    private volatile NavMeshPath lastPath = null;
    private volatile NavMesh lastMesh = null;
    private volatile List<BlockPos> lastBlockPath = null;
    private volatile long tick = 0;

    public static PathfindingService getInstance() {
        if (instance == null) instance = new PathfindingService();
        return instance;
    }

    public void tick() {
        tick++;
        blacklist.removeIf(e -> e.isExpired(tick));
    }

    public CompletableFuture<NavMeshPath> findNavMeshPath(Vec3d start, Vec3d goal, ClientWorld world, int maxRange) {
        return CompletableFuture.supplyAsync(() -> findNavMeshPathSync(start, goal, world, maxRange), EXECUTOR);
    }

    public NavMeshPath findNavMeshPathSync(Vec3d start, Vec3d goal, ClientWorld world, int maxRange) {
        Vec3d sg = findGroundVec(start, world), gg = findGroundVec(goal, world);
        if (sg == null || gg == null) return NavMeshPath.empty();

        NavMesh mesh = generator.generate(world, sg, gg, maxRange);
        lastMesh = mesh;
        if (mesh.getPolyCount() == 0) return NavMeshPath.empty();

        applyBlacklistPenalties(mesh);
        NavMeshPath result = pathfinder.findPath(mesh, sg, gg, tick, world);
        lastPath = result;
        return result;
    }

    public CompletableFuture<NavMeshPath> findFlyPath(Vec3d start, Vec3d goal, ClientWorld world, int maxRange) {
        return CompletableFuture.supplyAsync(() -> {
            NavMeshPath r = flyPathfinder.findPath(start, goal, world, maxRange);
            lastPath = r;
            return r;
        }, EXECUTOR);
    }

    public CompletableFuture<HybridResult> findHybridPath(Vec3d start, Vec3d goal, ClientWorld world, int maxRange) {
        return CompletableFuture.supplyAsync(() -> findHybridSync(start, goal, world, maxRange), EXECUTOR);
    }

    public HybridResult findHybridSync(Vec3d start, Vec3d goal, ClientWorld world, int maxRange) {
        NavMeshPath ground = findNavMeshPathSync(start, goal, world, maxRange);
        if (ground.isFound() && !ground.getWaypoints().isEmpty()) return new HybridResult(ground, false);

        NavMeshPath fly = flyPathfinder.findPath(start, goal, world, maxRange);
        lastPath = fly;
        if (fly.isFound() && !fly.getWaypoints().isEmpty()) return new HybridResult(fly, true);

        return new HybridResult(NavMeshPath.empty(), false);
    }

    public CompletableFuture<List<BlockPos>> findPathAsync(BlockPos start, BlockPos goal, ClientWorld world, int maxRange) {
        Vec3d sv = new Vec3d(start.getX() + 0.5, start.getY(), start.getZ() + 0.5);
        Vec3d gv = new Vec3d(goal.getX() + 0.5, goal.getY(), goal.getZ() + 0.5);
        return CompletableFuture.supplyAsync(() -> {
            NavMeshPath p = findNavMeshPathSync(sv, gv, world, maxRange);
            List<BlockPos> bp = new ArrayList<>();
            if (p.isFound()) for (Vec3d wp : p.getWaypoints()) bp.add(BlockPos.ofFloored(wp));
            lastBlockPath = bp.isEmpty() ? Collections.emptyList() : bp;
            return lastBlockPath;
        }, EXECUTOR);
    }

    public void blacklistArea(Vec3d center, double radius, long durationTicks) {
        blacklist.add(new BlacklistEntry(center, radius, tick + durationTicks));
    }

    public void clearBlacklist() {
        blacklist.clear();
    }

    public int getBlacklistCount() {
        return blacklist.size();
    }

    private void applyBlacklistPenalties(NavMesh mesh) {
        if (blacklist.isEmpty()) return;
        for (NavPoly poly : mesh.getAllPolygons()) {
            for (BlacklistEntry e : blacklist) {
                if (e.isExpired(tick)) continue;
                if (poly.overlapsCircle(e.center.x, e.center.z, e.radius))
                    poly.setTraversalCost(poly.getTraversalCost() * 10.0);
            }
        }
    }

    private Vec3d findGroundVec(Vec3d pos, ClientWorld world) {
        BlockPos bp = BlockPos.ofFloored(pos);
        for (int dist = 0; dist <= 10; dist++) {
            for (int sign = 0; sign <= 1; sign++) {
                int dy = sign == 0 ? dist : -dist;
                if (dy < 0 && dist == 0) continue;
                BlockPos check = bp.add(0, dy, 0);

                double sy = NavMeshGenerator.getSurfaceY(check, world);
                if (!Double.isNaN(sy)) return new Vec3d(pos.x, sy, pos.z);

                if (world.isChunkLoaded(check.getX() >> 4, check.getZ() >> 4)) {
                    var state = world.getBlockState(check);
                    var shape = state.getCollisionShape(world, check);
                    if (!shape.isEmpty() && !NavMeshGenerator.isNonSolidObstacle(state) && !NavMeshGenerator.isHazardous(state)) {
                        double top = check.getY() + shape.getMax(net.minecraft.util.math.Direction.Axis.Y);
                        BlockPos fp = BlockPos.ofFloored(pos.x, top, pos.z);
                        double cs = NavMeshGenerator.getSurfaceY(fp, world);
                        if (!Double.isNaN(cs) && Math.abs(cs - top) < 0.1) return new Vec3d(pos.x, top, pos.z);
                    }
                }
            }
        }
        return null;
    }

    public BlockPos findGround(BlockPos pos, ClientWorld world) {
        for (int d = 0; d <= 10; d++) {
            if (NavMeshGenerator.isWalkable(pos.add(0, d, 0), world)) return pos.add(0, d, 0);
            if (d > 0 && NavMeshGenerator.isWalkable(pos.add(0, -d, 0), world)) return pos.add(0, -d, 0);
        }
        return null;
    }

    public NavMeshPath getLastPath() {
        return lastPath;
    }

    public NavMesh getLastMesh() {
        return lastMesh;
    }

    public List<BlockPos> getLastBlockPath() {
        return lastBlockPath;
    }

    public long getCurrentTick() {
        return tick;
    }

    public static class HybridResult {
        private final NavMeshPath path;
        private final boolean usedFly;

        public HybridResult(NavMeshPath p, boolean fly) {
            path = p;
            usedFly = fly;
        }

        public NavMeshPath getPath() {
            return path;
        }

        public boolean isUsedFly() {
            return usedFly;
        }

        public boolean isFound() {
            return path.isFound();
        }
    }

    private static class BlacklistEntry {
        final Vec3d center;
        final double radius;
        final long expiry;

        BlacklistEntry(Vec3d c, double r, long e) {
            center = c;
            radius = r;
            expiry = e;
        }

        boolean isExpired(long t) {
            return t >= expiry;
        }
    }
}