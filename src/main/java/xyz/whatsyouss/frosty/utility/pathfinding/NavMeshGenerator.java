package xyz.whatsyouss.frosty.utility.pathfinding;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

import java.util.*;

public class NavMeshGenerator {

    
    
    private static final int PADDING = 48;
    private static final int MAX_POLY_SIZE = 6;

    private static volatile int maxJumpHeight = 1;
    private static volatile int maxFallHeight = 3;

    public static void setMovementParams(int jump, int fall) { maxJumpHeight = jump; maxFallHeight = fall; }
    public static void resetMovementParams() { maxJumpHeight = 1; maxFallHeight = 3; }
    public static int getMaxJumpHeight() { return maxJumpHeight; }
    public static int getMaxFallHeight() { return maxFallHeight; }

    public NavMesh generate(ClientWorld world, Vec3d start, Vec3d goal, int maxRange) {
        NavPoly.resetIdCounter();

        int yDiff = (int) Math.abs(start.y - goal.y);
        int scanPad = Math.min(Math.max(PADDING, PADDING + yDiff / 3), 64);
        scanPad = Math.min(scanPad, maxRange);

        int scanMinX = (int) Math.floor(Math.min(start.x, goal.x)) - scanPad;
        int scanMaxX = (int) Math.ceil(Math.max(start.x, goal.x)) + scanPad;
        int scanMinZ = (int) Math.floor(Math.min(start.z, goal.z)) - scanPad;
        int scanMaxZ = (int) Math.ceil(Math.max(start.z, goal.z)) + scanPad;
        
        int scanMinY = (int) Math.floor(Math.min(start.y, goal.y)) - 40;
        int scanMaxY = (int) Math.ceil(Math.max(start.y, goal.y)) + 40;

        int cx = (scanMinX + scanMaxX) / 2, cz = (scanMinZ + scanMaxZ) / 2;
        scanMinX = Math.max(scanMinX, cx - maxRange); scanMaxX = Math.min(scanMaxX, cx + maxRange);
        scanMinZ = Math.max(scanMinZ, cz - maxRange); scanMaxZ = Math.min(scanMaxZ, cz + maxRange);

        int sizeX = scanMaxX - scanMinX + 1, sizeZ = scanMaxZ - scanMinZ + 1;

        
        Map<Long, List<WalkableSurface>> surfacesByY = new HashMap<>();
        for (int lx = 0; lx < sizeX; lx++) {
            for (int lz = 0; lz < sizeZ; lz++) {
                int wx = scanMinX + lx, wz = scanMinZ + lz;
                for (int by = scanMinY - 1; by <= scanMaxY; by++) {
                    double sy = getWalkableSurfaceY(new BlockPos(wx, by, wz), world);
                    if (Double.isNaN(sy) || sy < scanMinY || sy > scanMaxY + 1) continue;
                    long qy = Math.round(sy * 16.0);
                    surfacesByY.computeIfAbsent(qy, k -> new ArrayList<>()).add(new WalkableSurface(lx, lz, sy));
                }
            }
        }

        
        List<NavPoly> allPolys = new ArrayList<>();
        for (Map.Entry<Long, List<WalkableSurface>> e : surfacesByY.entrySet()) {
            List<WalkableSurface> surfaces = e.getValue();
            double layerY = surfaces.get(0).y;
            boolean[][] walkable = new boolean[sizeX][sizeZ];
            for (WalkableSurface s : surfaces) walkable[s.lx][s.lz] = true;
            allPolys.addAll(greedyMerge(walkable, scanMinX, scanMinZ, sizeX, sizeZ, layerY));
        }

        if (allPolys.isEmpty()) return new NavMesh(Collections.emptyList());

        computeWallClearance(allPolys, surfacesByY, sizeX, sizeZ, scanMinX, scanMinZ);
        findSameLayerAdjacencies(allPolys);
        findYTransitionAdjacencies(allPolys, world);

        return new NavMesh(allPolys);
    }

    private double getWalkableSurfaceY(BlockPos groundPos, ClientWorld world) {
        if (!world.isChunkLoaded(groundPos.getX() >> 4, groundPos.getZ() >> 4)) return Double.NaN;
        BlockState gs = world.getBlockState(groundPos);
        VoxelShape gShape = gs.getCollisionShape(world, groundPos);
        if (gShape.isEmpty() || isNonSolidObstacle(gs) || isHazardous(gs)) return Double.NaN;

        double surfaceY = groundPos.getY() + gShape.getMax(Direction.Axis.Y);
        int feetBlockY = (int) Math.floor(surfaceY);
        int headBlockY = (int) Math.floor(surfaceY + 1.8);

        for (int cy = feetBlockY; cy <= headBlockY; cy++) {
            if (cy == groundPos.getY()) continue;
            BlockPos cp = new BlockPos(groundPos.getX(), cy, groundPos.getZ());
            BlockState cs = world.getBlockState(cp);
            VoxelShape cShape = cs.getCollisionShape(world, cp);
            if (!cShape.isEmpty()) {
                double bot = cp.getY() + cShape.getMin(Direction.Axis.Y);
                double top = cp.getY() + cShape.getMax(Direction.Axis.Y);
                if (top > surfaceY + 0.1 && bot < surfaceY + 1.8) return Double.NaN;
            }
            if (isNonSolidObstacle(cs) || isHazardous(cs)) return Double.NaN;
        }
        return surfaceY;
    }

    private List<NavPoly> greedyMerge(boolean[][] walkable, int originX, int originZ, int sizeX, int sizeZ, double y) {
        List<NavPoly> polys = new ArrayList<>();
        boolean[][] visited = new boolean[sizeX][sizeZ];

        for (int lx = 0; lx < sizeX; lx++) {
            for (int lz = 0; lz < sizeZ; lz++) {
                if (!walkable[lx][lz] || visited[lx][lz]) continue;
                int endX = lx;
                while (endX + 1 < sizeX && (endX - lx + 1) < MAX_POLY_SIZE && walkable[endX + 1][lz] && !visited[endX + 1][lz]) endX++;
                int endZ = lz;
                boolean canExpand = true;
                while (canExpand && endZ + 1 < sizeZ && (endZ - lz + 1) < MAX_POLY_SIZE) {
                    for (int ex = lx; ex <= endX; ex++) {
                        if (!walkable[ex][endZ + 1] || visited[ex][endZ + 1]) { canExpand = false; break; }
                    }
                    if (canExpand) endZ++;
                }
                for (int ex = lx; ex <= endX; ex++) for (int ez = lz; ez <= endZ; ez++) visited[ex][ez] = true;
                polys.add(new NavPoly(originX + lx, originZ + lz, originX + endX, originZ + endZ, y));
            }
        }
        return polys;
    }

    private void findSameLayerAdjacencies(List<NavPoly> polys) {
        Map<Long, List<NavPoly>> byY = new HashMap<>();
        for (NavPoly p : polys) byY.computeIfAbsent(Math.round(p.getY() * 16.0), k -> new ArrayList<>()).add(p);

        for (List<NavPoly> layer : byY.values()) {
            Map<Long, List<NavPoly>> hash = new HashMap<>();
            for (NavPoly p : layer) {
                for (int x = p.getMinX() - 1; x <= p.getMaxX() + 1; x++) {
                    insertHash(hash, x, p.getMinZ() - 1, p);
                    insertHash(hash, x, p.getMaxZ() + 1, p);
                }
                for (int z = p.getMinZ(); z <= p.getMaxZ(); z++) {
                    insertHash(hash, p.getMinX() - 1, z, p);
                    insertHash(hash, p.getMaxX() + 1, z, p);
                }
            }
            Set<Long> checked = new HashSet<>();
            for (List<NavPoly> bucket : hash.values()) {
                for (int i = 0; i < bucket.size(); i++) for (int j = i + 1; j < bucket.size(); j++) {
                    NavPoly a = bucket.get(i), b = bucket.get(j);
                    long key = a.getId() < b.getId() ? ((long) a.getId() << 32) | b.getId() : ((long) b.getId() << 32) | a.getId();
                    if (checked.add(key)) tryCreateEdge(a, b, false);
                }
            }
        }
    }

    private static void insertHash(Map<Long, List<NavPoly>> hash, int x, int z, NavPoly p) {
        hash.computeIfAbsent(((long) x << 32) | (z & 0xFFFFFFFFL), k -> new ArrayList<>(2)).add(p);
    }

    private void tryCreateEdge(NavPoly a, NavPoly b, boolean yT) {
        if (a.getMaxX() + 1 == b.getMinX()) { createEdgeX(a, b, b.getMinX(), yT); return; }
        if (b.getMaxX() + 1 == a.getMinX()) { createEdgeX(a, b, a.getMinX(), yT); return; }
        if (a.getMaxZ() + 1 == b.getMinZ()) { createEdgeZ(a, b, b.getMinZ(), yT); return; }
        if (b.getMaxZ() + 1 == a.getMinZ()) { createEdgeZ(a, b, a.getMinZ(), yT); }
    }

    private void createEdgeX(NavPoly a, NavPoly b, int edgeX, boolean yT) {
        int minZ = Math.max(a.getMinZ(), b.getMinZ()), maxZ = Math.min(a.getMaxZ(), b.getMaxZ());
        if (minZ > maxZ) return;
        double ey = yT ? b.getY() : a.getY();
        NavEdge edge = new NavEdge(a, b, new Vec3d(edgeX, ey, minZ), new Vec3d(edgeX, ey, maxZ + 1), yT);
        a.addEdge(edge); b.addEdge(edge);
    }

    private void createEdgeZ(NavPoly a, NavPoly b, int edgeZ, boolean yT) {
        int minX = Math.max(a.getMinX(), b.getMinX()), maxX = Math.min(a.getMaxX(), b.getMaxX());
        if (minX > maxX) return;
        double ey = yT ? b.getY() : a.getY();
        NavEdge edge = new NavEdge(a, b, new Vec3d(minX, ey, edgeZ), new Vec3d(maxX + 1, ey, edgeZ), yT);
        a.addEdge(edge); b.addEdge(edge);
    }

    private void findYTransitionAdjacencies(List<NavPoly> polys, ClientWorld world) {
        Map<Long, List<NavPoly>> byQY = new HashMap<>();
        for (NavPoly p : polys) byQY.computeIfAbsent(Math.round(p.getY() * 16.0), k -> new ArrayList<>()).add(p);
        List<Long> levels = new ArrayList<>(byQY.keySet());
        Collections.sort(levels);

        for (int i = 0; i < levels.size(); i++) {
            double yLow = levels.get(i) / 16.0;
            for (int j = i + 1; j < levels.size(); j++) {
                double yHigh = levels.get(j) / 16.0;
                double diff = yHigh - yLow;
                if (diff > maxFallHeight) break;
                if (diff <= maxJumpHeight)
                    for (NavPoly lo : byQY.get(levels.get(i))) for (NavPoly hi : byQY.get(levels.get(j)))
                        tryYTransition(lo, hi, diff, world);
                if (diff > 0)
                    for (NavPoly hi : byQY.get(levels.get(j))) for (NavPoly lo : byQY.get(levels.get(i)))
                        tryFallEdge(hi, lo, diff, world);
            }
        }
    }

    private void tryYTransition(NavPoly lo, NavPoly hi, double diff, ClientWorld world) {
        boolean isYT = diff > 0.5625;
        if (lo.getMaxX() + 1 == hi.getMinX() || hi.getMaxX() + 1 == lo.getMinX()) {
            int minZ = Math.max(lo.getMinZ(), hi.getMinZ()), maxZ = Math.min(lo.getMaxZ(), hi.getMaxZ());
            if (minZ <= maxZ && hasJumpClearance(lo, hi, world)) {
                int ex = lo.getMaxX() + 1 == hi.getMinX() ? hi.getMinX() : lo.getMinX();
                NavEdge e = new NavEdge(lo, hi, new Vec3d(ex, hi.getY(), minZ), new Vec3d(ex, hi.getY(), maxZ + 1), isYT);
                lo.addEdge(e); hi.addEdge(e);
            }
        }
        if (lo.getMaxZ() + 1 == hi.getMinZ() || hi.getMaxZ() + 1 == lo.getMinZ()) {
            int minX = Math.max(lo.getMinX(), hi.getMinX()), maxX = Math.min(lo.getMaxX(), hi.getMaxX());
            if (minX <= maxX && hasJumpClearance(lo, hi, world)) {
                int ez = lo.getMaxZ() + 1 == hi.getMinZ() ? hi.getMinZ() : lo.getMinZ();
                NavEdge e = new NavEdge(lo, hi, new Vec3d(minX, hi.getY(), ez), new Vec3d(maxX + 1, hi.getY(), ez), isYT);
                lo.addEdge(e); hi.addEdge(e);
            }
        }
    }

    private void tryFallEdge(NavPoly hi, NavPoly lo, double drop, ClientWorld world) {
        boolean isYT = drop > 0.5625;
        if (hi.getMaxX() + 1 == lo.getMinX() || lo.getMaxX() + 1 == hi.getMinX()) {
            int minZ = Math.max(hi.getMinZ(), lo.getMinZ()), maxZ = Math.min(hi.getMaxZ(), lo.getMaxZ());
            if (minZ <= maxZ && hasClearFall(hi, lo, world)) {
                int ex = hi.getMaxX() + 1 == lo.getMinX() ? lo.getMinX() : hi.getMinX();
                NavEdge e = new NavEdge(hi, lo, new Vec3d(ex, lo.getY(), minZ), new Vec3d(ex, lo.getY(), maxZ + 1), isYT);
                hi.addEdge(e); lo.addEdge(e);
            }
        }
        if (hi.getMaxZ() + 1 == lo.getMinZ() || lo.getMaxZ() + 1 == hi.getMinZ()) {
            int minX = Math.max(hi.getMinX(), lo.getMinX()), maxX = Math.min(hi.getMaxX(), lo.getMaxX());
            if (minX <= maxX && hasClearFall(hi, lo, world)) {
                int ez = hi.getMaxZ() + 1 == lo.getMinZ() ? lo.getMinZ() : hi.getMinZ();
                NavEdge e = new NavEdge(hi, lo, new Vec3d(minX, lo.getY(), ez), new Vec3d(maxX + 1, lo.getY(), ez), isYT);
                hi.addEdge(e); lo.addEdge(e);
            }
        }
    }

    private boolean hasJumpClearance(NavPoly lo, NavPoly hi, ClientWorld world) {
        int cx = (Math.max(lo.getMinX(), hi.getMinX()) + Math.min(lo.getMaxX(), hi.getMaxX())) / 2;
        int cz = (Math.max(lo.getMinZ(), hi.getMinZ()) + Math.min(lo.getMaxZ(), hi.getMaxZ())) / 2;
        for (int y = (int) Math.floor(lo.getY()); y <= (int) Math.floor(hi.getY()) + 2; y++) {
            if (y == (int) Math.floor(lo.getY()) - 1 || y == (int) Math.floor(hi.getY()) - 1) continue;
            BlockPos p = new BlockPos(cx, y, cz);
            BlockState s = world.getBlockState(p);
            VoxelShape sh = s.getCollisionShape(world, p);
            if (!sh.isEmpty()) {
                double bot = y + sh.getMin(Direction.Axis.Y), top = y + sh.getMax(Direction.Axis.Y);
                if (top > lo.getY() + 0.01 && bot < hi.getY() + 1.8) {
                    if (Math.abs(top - lo.getY()) >= 0.01 && Math.abs(top - hi.getY()) >= 0.01) return false;
                }
            }
        }
        return true;
    }

    private boolean hasClearFall(NavPoly hi, NavPoly lo, ClientWorld world) {
        int mx = (hi.getMinX() + hi.getMaxX()) / 2, mz = (hi.getMinZ() + hi.getMaxZ()) / 2;
        for (int y = (int) Math.floor(lo.getY()); y <= (int) Math.floor(hi.getY()) + 1; y++) {
            BlockPos p = new BlockPos(mx, y, mz);
            BlockState s = world.getBlockState(p);
            VoxelShape sh = s.getCollisionShape(world, p);
            if (!sh.isEmpty()) {
                double bot = y + sh.getMin(Direction.Axis.Y), top = y + sh.getMax(Direction.Axis.Y);
                if (Math.abs(top - hi.getY()) >= 0.01 && Math.abs(top - lo.getY()) >= 0.01
                        && top > lo.getY() + 0.01 && bot < hi.getY() + 1.8) return false;
            }
        }
        return true;
    }

    private void computeWallClearance(List<NavPoly> polys, Map<Long, List<WalkableSurface>> surfacesByY,
                                      int sizeX, int sizeZ, int ox, int oz) {
        final int CAP = 4;
        Map<Long, int[][]> distGrids = new HashMap<>();

        for (Map.Entry<Long, List<WalkableSurface>> e : surfacesByY.entrySet()) {
            boolean[][] grid = new boolean[sizeX][sizeZ];
            for (WalkableSurface s : e.getValue()) grid[s.lx][s.lz] = true;

            int[][] dist = new int[sizeX][sizeZ];
            Queue<int[]> q = new ArrayDeque<>();

            for (int x = 0; x < sizeX; x++) for (int z = 0; z < sizeZ; z++) {
                if (!grid[x][z] || x == 0 || x == sizeX - 1 || z == 0 || z == sizeZ - 1) {
                    dist[x][z] = 0; q.add(new int[]{x, z});
                } else { dist[x][z] = CAP; }
            }

            int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
            while (!q.isEmpty()) {
                int[] c = q.poll();
                if (dist[c[0]][c[1]] >= CAP) continue;
                for (int[] d : dirs) {
                    int nx = c[0] + d[0], nz = c[1] + d[1];
                    if (nx < 0 || nx >= sizeX || nz < 0 || nz >= sizeZ) continue;
                    if (dist[nx][nz] > dist[c[0]][c[1]] + 1) { dist[nx][nz] = dist[c[0]][c[1]] + 1; q.add(new int[]{nx, nz}); }
                }
            }
            distGrids.put(e.getKey(), dist);
        }

        for (NavPoly p : polys) {
            int[][] dist = distGrids.get(Math.round(p.getY() * 16.0));
            if (dist == null) continue;
            int minC = CAP;
            for (int x = p.getMinX(); x <= p.getMaxX(); x++) for (int z = p.getMinZ(); z <= p.getMaxZ(); z++) {
                int lx = x - ox, lz = z - oz;
                if (lx >= 0 && lx < sizeX && lz >= 0 && lz < sizeZ) minC = Math.min(minC, dist[lx][lz]);
            }
            p.setClearance(minC);
        }
    }

    

    public static double getSurfaceY(BlockPos feetPos, ClientWorld world) {
        BlockPos gp = feetPos.down();
        if (!world.isChunkLoaded(gp.getX() >> 4, gp.getZ() >> 4)) return Double.NaN;
        BlockState gs = world.getBlockState(gp);
        VoxelShape gSh = gs.getCollisionShape(world, gp);
        if (gSh.isEmpty() || isNonSolidObstacle(gs) || isHazardous(gs)) return Double.NaN;
        double sy = gp.getY() + gSh.getMax(Direction.Axis.Y);
        for (int cy = (int) Math.floor(sy); cy <= (int) Math.floor(sy + 1.8); cy++) {
            if (cy == gp.getY()) continue;
            BlockPos cp = new BlockPos(feetPos.getX(), cy, feetPos.getZ());
            BlockState cs = world.getBlockState(cp);
            VoxelShape cSh = cs.getCollisionShape(world, cp);
            if (!cSh.isEmpty()) {
                double bot = cp.getY() + cSh.getMin(Direction.Axis.Y), top = cp.getY() + cSh.getMax(Direction.Axis.Y);
                if (top > sy + 0.1 && bot < sy + 1.8) return Double.NaN;
            }
            if (isNonSolidObstacle(cs) || isHazardous(cs)) return Double.NaN;
        }
        return sy;
    }

    public static boolean isWalkable(BlockPos feet, ClientWorld world) {
        BlockPos below = feet.down(), head = feet.up();
        if (!world.isChunkLoaded(feet.getX() >> 4, feet.getZ() >> 4)) return false;
        BlockState bs = world.getBlockState(below), fs = world.getBlockState(feet), hs = world.getBlockState(head);
        if (bs.getCollisionShape(world, below).isEmpty() || isNonSolidObstacle(bs)) return false;
        if (!fs.getCollisionShape(world, feet).isEmpty() || !hs.getCollisionShape(world, head).isEmpty()) return false;
        if (isNonSolidObstacle(fs) || isNonSolidObstacle(hs)) return false;
        if (isHazardous(fs) || isHazardous(hs) || isHazardous(bs)) return false;
        return true;
    }

    public static boolean isPassable(BlockPos pos, ClientWorld world) {
        if (!world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) return false;
        BlockState s = world.getBlockState(pos);
        if (!s.getCollisionShape(world, pos).isEmpty()) return false;
        if (isNonSolidObstacle(s) || isHazardous(s)) return false;
        return true;
    }

    public static boolean isHazardous(BlockState s) {
        return s.isOf(Blocks.LAVA) || s.isOf(Blocks.FIRE) || s.isOf(Blocks.SOUL_FIRE)
                || s.isOf(Blocks.CACTUS) || s.isOf(Blocks.SWEET_BERRY_BUSH)
                || s.isOf(Blocks.MAGMA_BLOCK) || s.isOf(Blocks.COBWEB)
                || s.isOf(Blocks.POWDER_SNOW);
    }

    public static boolean isNonSolidObstacle(BlockState s) {
        return s.isIn(BlockTags.LEAVES) || s.isOf(Blocks.SCAFFOLDING);
    }

    private static class WalkableSurface {
        final int lx, lz; final double y;
        WalkableSurface(int lx, int lz, double y) { this.lx = lx; this.lz = lz; this.y = y; }
    }
}