package xyz.whatsyouss.frosty.utility.pathfinding;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class FlyPathfinder {

    private static final int MAX_ITERATIONS = 200_000;
    private static final double DIAGONAL_COST = 1.414;

    private static final int[][] NEIGHBORS_26 = {
            {1,0,0},{-1,0,0},{0,0,1},{0,0,-1},
            {0,1,0},{0,-1,0},
            {1,0,1},{1,0,-1},{-1,0,1},{-1,0,-1},
            {1,1,0},{1,-1,0},{-1,1,0},{-1,-1,0},
            {0,1,1},{0,1,-1},{0,-1,1},{0,-1,-1},
            {1,1,1},{1,1,-1},{1,-1,1},{1,-1,-1},
            {-1,1,1},{-1,1,-1},{-1,-1,1},{-1,-1,-1}
    };

    public NavMeshPath findPath(Vec3d start, Vec3d goal, ClientWorld world, int maxRange) {
        BlockPos sb = BlockPos.ofFloored(start), gb = BlockPos.ofFloored(goal);
        if (!isPassable3D(sb, world) || !isPassable3D(gb, world)) return NavMeshPath.empty();

        PriorityQueue<FlyNode> open = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fCost));
        Map<Long, FlyNode> all = new HashMap<>();
        Set<Long> closed = new HashSet<>();

        long startKey = key(sb), goalKey = key(gb);
        FlyNode startNode = new FlyNode(sb, null, 0, heuristic(sb, gb));
        open.add(startNode);
        all.put(startKey, startNode);

        int iter = 0;
        while (!open.isEmpty() && iter++ < MAX_ITERATIONS) {
            FlyNode cur = open.poll();
            long curKey = key(cur.pos);
            if (closed.contains(curKey)) continue;
            closed.add(curKey);

            if (curKey == goalKey) {
                List<Vec3d> raw = reconstruct(cur);
                List<Vec3d> smoothed = smooth(raw, world);
                return new NavMeshPath(smoothed, Collections.emptyList(), true);
            }

            if (cur.pos.getManhattanDistance(sb) > maxRange) continue;

            for (int[] dir : NEIGHBORS_26) {
                BlockPos nb = cur.pos.add(dir[0], dir[1], dir[2]);
                long nk = key(nb);
                if (closed.contains(nk) || !isPassable3D(nb, world)) continue;

                int axes = (dir[0] != 0 ? 1 : 0) + (dir[1] != 0 ? 1 : 0) + (dir[2] != 0 ? 1 : 0);
                double cost = axes == 1 ? 1.0 : axes == 2 ? DIAGONAL_COST : 1.732;
                double tentG = cur.gCost + cost;

                FlyNode nn = all.get(nk);
                if (nn == null) {
                    nn = new FlyNode(nb, cur, tentG, tentG + heuristic(nb, gb));
                    all.put(nk, nn); open.add(nn);
                } else if (tentG < nn.gCost) {
                    nn.parent = cur; nn.gCost = tentG;
                    nn.fCost = tentG + heuristic(nb, gb);
                    open.add(nn);
                }
            }
        }
        return NavMeshPath.empty();
    }

    private boolean isPassable3D(BlockPos pos, ClientWorld world) {
        return NavMeshGenerator.isPassable(pos, world) && NavMeshGenerator.isPassable(pos.up(), world);
    }

    private double heuristic(BlockPos a, BlockPos b) {
        int dx = Math.abs(a.getX() - b.getX()), dy = Math.abs(a.getY() - b.getY()), dz = Math.abs(a.getZ() - b.getZ());
        int d1 = Math.max(dx, Math.max(dy, dz)), d3 = Math.min(dx, Math.min(dy, dz)), d2 = dx + dy + dz - d1 - d3;
        return d3 * 0.318 + (d2 - d3) * 0.414 + d1;
    }

    private List<Vec3d> reconstruct(FlyNode goal) {
        List<Vec3d> path = new ArrayList<>();
        for (FlyNode cur = goal; cur != null; cur = cur.parent)
            path.add(new Vec3d(cur.pos.getX() + 0.5, cur.pos.getY(), cur.pos.getZ() + 0.5));
        Collections.reverse(path);
        return path;
    }

    private List<Vec3d> smooth(List<Vec3d> raw, ClientWorld world) {
        if (raw.size() <= 2) return raw;
        List<Vec3d> out = new ArrayList<>();
        out.add(raw.get(0));
        int cur = 0;
        while (cur < raw.size() - 1) {
            int far = cur + 1;
            for (int a = raw.size() - 1; a > cur + 1; a--) {
                if (los3D(raw.get(cur), raw.get(a), world)) { far = a; break; }
            }
            out.add(raw.get(far));
            cur = far;
        }
        return out;
    }

    private boolean los3D(Vec3d from, Vec3d to, ClientWorld world) {
        double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
        double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (dist < 0.5) return true;
        int steps = (int) Math.ceil(dist / 0.4);
        for (int i = 1; i < steps; i++) {
            double t = (double) i / steps;
            BlockPos fp = BlockPos.ofFloored(from.x + dx*t, from.y + dy*t, from.z + dz*t);
            if (!NavMeshGenerator.isPassable(fp, world) || !NavMeshGenerator.isPassable(fp.up(), world)) return false;
        }
        return true;
    }

    private static long key(BlockPos p) {
        return ((long) p.getX() & 0x3FFFFFFL) << 38 | ((long) p.getY() & 0xFFFL) << 26 | ((long) p.getZ() & 0x3FFFFFFL);
    }

    private static class FlyNode {
        final BlockPos pos;
        FlyNode parent;
        double gCost, fCost;
        FlyNode(BlockPos pos, FlyNode parent, double g, double f) { this.pos = pos; this.parent = parent; this.gCost = g; this.fCost = f; }
    }
}