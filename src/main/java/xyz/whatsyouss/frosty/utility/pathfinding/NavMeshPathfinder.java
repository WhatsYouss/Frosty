package xyz.whatsyouss.frosty.utility.pathfinding;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;


public class NavMeshPathfinder {

    private static final double LOS_MAX_DIST = 50.0;
    private static final int LOS_MAX_AHEAD = 40;

    private static volatile boolean preferFlatGround = true;
    private static volatile boolean enablePathCoarsening = true;

    public static void setPreferFlatGround(boolean v) {
        preferFlatGround = v;
    }

    public static void setEnablePathCoarsening(boolean v) {
        enablePathCoarsening = v;
    }

    private static double triXZ(Vec3d a, Vec3d b, Vec3d c) {
        return (b.x - a.x) * (c.z - a.z) - (c.x - a.x) * (b.z - a.z);
    }

    private static double crossXZ(Vec3d dir, Vec3d edge) {
        return dir.x * edge.z - dir.z * edge.x;
    }

    private static boolean vecEq(Vec3d a, Vec3d b) {
        return Math.abs(a.x - b.x) < 0.001 && Math.abs(a.z - b.z) < 0.001;
    }

    public NavMeshPath findPath(NavMesh mesh, Vec3d start, Vec3d goal, long tick, ClientWorld world) {
        NavPoly sp = mesh.getPolyAt(start);
        if (sp == null) sp = mesh.getNearestPoly(start, 8.0);
        NavPoly gp = mesh.getPolyAt(goal);
        if (gp == null) gp = mesh.getNearestPoly(goal, 8.0);
        if (sp == null || gp == null) return NavMeshPath.empty();

        if (sp.equals(gp)) {
            return new NavMeshPath(Arrays.asList(start, goal), Collections.singletonList(sp), true);
        }

        CorridorResult corridor = findCorridor(mesh, sp, gp, tick);
        if (corridor == null) return NavMeshPath.empty();

        List<Vec3d> waypoints = buildSafePath(start, goal, corridor.portals, corridor.polys, world);
        return new NavMeshPath(waypoints, corridor.polys, true);
    }

    private CorridorResult findCorridor(NavMesh mesh, NavPoly sp, NavPoly gp, long tick) {
        PriorityQueue<PolyNode> open = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fCost));
        Map<Integer, PolyNode> all = new HashMap<>();
        Set<Integer> closed = new HashSet<>();

        Vec3d gc = gp.getCenter();
        PolyNode start = new PolyNode(sp, null, null, 0, sp.getCenter().distanceTo(gc));
        open.add(start);
        all.put(sp.getId(), start);

        int maxIter = mesh.getPolyCount() * 4, iter = 0;

        while (!open.isEmpty()) {
            PolyNode cur = open.poll();
            if (closed.contains(cur.poly.getId())) continue;
            if (++iter > maxIter) break;
            closed.add(cur.poly.getId());

            if (cur.poly.equals(gp)) return reconstructCorridor(cur);

            for (NavEdge edge : cur.poly.getEdges()) {
                NavPoly nb = edge.getOther(cur.poly);
                if (closed.contains(nb.getId())) continue;

                double edgeCost = cur.poly.getCenter().distanceTo(nb.getCenter());
                double cl = nb.getClearance();
                double cMult = 1.0 + 1.0 / (1.0 + cl);
                double cFixed = 1.5 / (1.0 + cl);
                double tG = cur.gCost + edgeCost * nb.getTraversalCost() * cMult + cFixed;

                if (preferFlatGround) {
                    double fy = nb.getY() % 1.0;
                    if (fy < 0) fy += 1.0;
                    if (fy > 0.001 && fy < 0.999) tG *= 1.3;
                }

                if (edge.isYTransition()) {
                    double diff = Math.abs(nb.getY() - cur.poly.getY());
                    if (nb.getY() < cur.poly.getY()) tG += 2.0 * diff;
                    else if (diff <= 1.0) tG += 20.0;
                    else tG += 25.0 * diff;
                }

                PolyNode nn = all.get(nb.getId());
                if (nn == null) {
                    nn = new PolyNode(nb, cur, edge, tG, tG + nb.getCenter().distanceTo(gc));
                    all.put(nb.getId(), nn);
                    open.add(nn);
                } else if (tG < nn.gCost) {
                    nn.parent = cur;
                    nn.entryEdge = edge;
                    nn.gCost = tG;
                    nn.fCost = tG + nb.getCenter().distanceTo(gc);
                    open.add(nn);
                }
            }
        }
        return null;
    }

    private CorridorResult reconstructCorridor(PolyNode goalNode) {
        List<PolyNode> nodes = new ArrayList<>();
        for (PolyNode n = goalNode; n != null; n = n.parent) nodes.add(n);
        Collections.reverse(nodes);

        List<NavPoly> polys = new ArrayList<>();
        List<Portal> portals = new ArrayList<>();
        for (PolyNode n : nodes) polys.add(n.poly);

        for (int i = 0; i < nodes.size() - 1; i++) {
            NavEdge edge = nodes.get(i + 1).entryEdge;
            if (edge == null) continue;
            Vec3d mvDir = nodes.get(i + 1).poly.getCenter().subtract(nodes.get(i).poly.getCenter());
            Vec3d el = edge.getLeftVertex(), er = edge.getRightVertex();
            double cross = crossXZ(mvDir, er.subtract(el));
            portals.add(cross < 0 ? new Portal(er, el, edge) : new Portal(el, er, edge));
        }
        return new CorridorResult(polys, portals);
    }

    private List<Vec3d> funnelSmooth(Vec3d start, Vec3d goal, List<Portal> portals, List<NavPoly> corridor) {
        List<Vec3d> path = new ArrayList<>();
        path.add(start);
        if (portals.isEmpty()) {
            path.add(goal);
            return path;
        }

        List<Portal> shrunk = new ArrayList<>();
        for (Portal p : portals) {
            Vec3d mid = p.left.add(p.right).multiply(0.5);
            double w = p.left.distanceTo(p.right);
            double shrink = Math.min(0.8, w * 0.3);
            double half = Math.max(0.15, w * 0.5 - shrink);
            Vec3d dir = w > 0.01 ? p.right.subtract(p.left).multiply(1.0 / w) : Vec3d.ZERO;
            shrunk.add(new Portal(mid.subtract(dir.multiply(half)), mid.add(dir.multiply(half)), p.edge));
        }

        Vec3d apex = start, pLeft = start, pRight = start;
        int apexIdx = 0, leftIdx = 0, rightIdx = 0;

        for (int i = 0; i < shrunk.size(); i++) {
            Vec3d nL = shrunk.get(i).left, nR = shrunk.get(i).right;

            if (triXZ(apex, pRight, nR) <= 0) {
                if (vecEq(apex, pRight) || triXZ(apex, pLeft, nR) > 0) {
                    pRight = nR;
                    rightIdx = i;
                } else {
                    path.add(withY(pLeft, corridor, leftIdx));
                    apex = pLeft;
                    apexIdx = leftIdx;
                    pRight = apex;
                    rightIdx = apexIdx;
                    i = apexIdx;
                    pLeft = apex;
                    leftIdx = apexIdx;
                    continue;
                }
            }
            if (triXZ(apex, pLeft, nL) >= 0) {
                if (vecEq(apex, pLeft) || triXZ(apex, pRight, nL) < 0) {
                    pLeft = nL;
                    leftIdx = i;
                } else {
                    path.add(withY(pRight, corridor, rightIdx));
                    apex = pRight;
                    apexIdx = rightIdx;
                    pLeft = apex;
                    leftIdx = apexIdx;
                    i = apexIdx;
                    pRight = apex;
                    rightIdx = apexIdx;
                    continue;
                }
            }
        }
        path.add(goal);

        List<Vec3d> clean = new ArrayList<>();
        for (Vec3d wp : path) if (clean.isEmpty() || !vecEq(clean.get(clean.size() - 1), wp)) clean.add(wp);
        return nudgeCorners(clean);
    }

    private Vec3d withY(Vec3d p, List<NavPoly> corridor, int pIdx) {
        return pIdx + 1 < corridor.size() ? new Vec3d(p.x, corridor.get(pIdx + 1).getY(), p.z) : p;
    }

    private List<Vec3d> buildSafePath(Vec3d start, Vec3d goal, List<Portal> portals, List<NavPoly> corridor, ClientWorld world) {
        List<Vec3d> base = funnelSmooth(start, goal, portals, corridor);

        if (base.size() <= 2) {
            if (base.size() == 2 && !hasLOS(base.get(0), base.get(1), world))
                return fallbackPath(start, goal, portals, corridor);
            return nudgeCorners(base);
        }

        boolean valid = true;
        for (int i = 0; i < base.size() - 1; i++)
            if (!hasLOS(base.get(i), base.get(i + 1), world)) {
                valid = false;
                break;
            }
        if (!valid) base = fallbackPath(start, goal, portals, corridor);

        for (int pass = 0; pass < 3 && base.size() > 2; pass++) {
            List<Vec3d> opt = new ArrayList<>();
            opt.add(base.get(0));
            int cur = 0;
            while (cur < base.size() - 1) {
                int far = cur + 1;
                int maxA = Math.min(base.size() - 1, cur + LOS_MAX_AHEAD);
                for (int a = maxA; a > cur + 1; a--) {
                    if (base.get(cur).distanceTo(base.get(a)) <= LOS_MAX_DIST && hasLOS(base.get(cur), base.get(a), world)) {
                        far = a;
                        break;
                    }
                }
                opt.add(base.get(far));
                cur = far;
            }
            if (opt.size() >= base.size()) break;
            base = opt;
        }

        if (enablePathCoarsening) base = coarsen(base, 0.3, 0.15);
        return nudgeCorners(base);
    }

    private List<Vec3d> coarsen(List<Vec3d> path, double xzTol, double yTol) {
        if (path.size() <= 3) return path;
        List<Vec3d> res = new ArrayList<>();
        res.add(path.get(0));
        int anchor = 0;
        for (int probe = 2; probe < path.size(); probe++) {
            Vec3d a = path.get(anchor), b = path.get(probe);
            boolean ok = true;
            for (int mid = anchor + 1; mid < probe && ok; mid++) {
                Vec3d p = path.get(mid);
                double t = (double) (mid - anchor) / (probe - anchor);
                if (Math.abs(p.y - (a.y + (b.y - a.y) * t)) > yTol) {
                    ok = false;
                    break;
                }
                double abDx = b.x - a.x, abDz = b.z - a.z, abLen = Math.sqrt(abDx * abDx + abDz * abDz);
                if (abLen < 0.01) {
                    ok = false;
                    break;
                }
                double perp = Math.abs((p.x - a.x) * (-abDz / abLen) + (p.z - a.z) * (abDx / abLen));
                if (perp > xzTol) ok = false;
            }
            if (!ok) {
                res.add(path.get(probe - 1));
                anchor = probe - 1;
            }
        }
        res.add(path.get(path.size() - 1));
        return res;
    }

    private List<Vec3d> fallbackPath(Vec3d start, Vec3d goal, List<Portal> portals, List<NavPoly> corridor) {
        List<Vec3d> path = new ArrayList<>();
        path.add(start);
        for (int i = 0; i < portals.size(); i++) {
            Vec3d mid = portals.get(i).left.add(portals.get(i).right).multiply(0.5);
            if (i + 1 < corridor.size()) mid = new Vec3d(mid.x, corridor.get(i + 1).getY(), mid.z);
            path.add(mid);
        }
        path.add(goal);
        List<Vec3d> dedup = new ArrayList<>();
        for (Vec3d wp : path) if (dedup.isEmpty() || !vecEq(dedup.get(dedup.size() - 1), wp)) dedup.add(wp);
        return dedup;
    }

    private boolean hasLOS(Vec3d from, Vec3d to, ClientWorld world) {
        double dx = to.x - from.x, dz = to.z - from.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 0.5) return true;

        double hw = 0.3, px = -dz / dist, pz = dx / dist;
        int steps = (int) Math.ceil(dist / 0.3);
        double sx = dx / steps, sy = (to.y - from.y) / steps, sz = dz / steps;
        double[] offsets = {0.0, -hw, hw};

        for (int i = 1; i < steps; i++) {
            double cx = from.x + sx * i, cy = from.y + sy * i, cz = from.z + sz * i;

            double actualY = cy;
            boolean ground = false;
            for (int yo = 2; yo >= -2 && !ground; yo--) {
                BlockPos gp = BlockPos.ofFloored(cx, cy - 0.05 + yo, cz);
                if (!world.isChunkLoaded(gp.getX() >> 4, gp.getZ() >> 4)) return false;
                var gs = world.getBlockState(gp);
                var gsh = gs.getCollisionShape(world, gp);
                if (!gsh.isEmpty()) {
                    double gt = gp.getY() + gsh.getMax(net.minecraft.util.math.Direction.Axis.Y);
                    if (gt >= cy - 2.0 && gt <= cy + 1.5) {
                        ground = true;
                        actualY = gt;
                    }
                }
            }
            if (!ground) return false;

            for (double off : offsets) {
                double x = cx + px * off, z = cz + pz * off;
                int minBY = (int) Math.floor(actualY), maxBY = (int) Math.floor(actualY + 1.8);
                for (int by = minBY; by <= maxBY; by++) {
                    BlockPos cp = BlockPos.ofFloored(x, by, z);
                    if (!world.isChunkLoaded(cp.getX() >> 4, cp.getZ() >> 4)) return false;
                    var cs = world.getBlockState(cp);
                    var csh = cs.getCollisionShape(world, cp);
                    if (!csh.isEmpty()) {
                        double bot = by + csh.getMin(net.minecraft.util.math.Direction.Axis.Y);
                        double top = by + csh.getMax(net.minecraft.util.math.Direction.Axis.Y);
                        if (top > actualY + 0.01 && bot < actualY + 1.8) return false;
                    }
                    if (NavMeshGenerator.isNonSolidObstacle(cs) || NavMeshGenerator.isHazardous(cs)) return false;
                }
            }
        }
        return true;
    }

    private List<Vec3d> nudgeCorners(List<Vec3d> path) {
        if (path.size() <= 2) return path;
        List<Vec3d> res = new ArrayList<>();
        res.add(path.get(0));
        final double M = 0.35, P = 0.4;
        for (int i = 1; i < path.size() - 1; i++) {
            Vec3d wp = path.get(i);
            double fx = wp.x - Math.floor(wp.x), fz = wp.z - Math.floor(wp.z);
            double nx = wp.x, nz = wp.z;
            if (fx < M) nx = Math.floor(wp.x) + P;
            else if (fx > 1 - M) nx = Math.floor(wp.x) + 1 - P;
            if (fz < M) nz = Math.floor(wp.z) + P;
            else if (fz > 1 - M) nz = Math.floor(wp.z) + 1 - P;
            res.add(new Vec3d(nx, wp.y, nz));
        }
        res.add(path.get(path.size() - 1));
        return res;
    }

    private static class PolyNode {
        final NavPoly poly;
        PolyNode parent;
        NavEdge entryEdge;
        double gCost, fCost;

        PolyNode(NavPoly p, PolyNode par, NavEdge e, double g, double f) {
            poly = p;
            parent = par;
            entryEdge = e;
            gCost = g;
            fCost = f;
        }
    }

    static class Portal {
        final Vec3d left, right;
        final NavEdge edge;

        Portal(Vec3d l, Vec3d r, NavEdge e) {
            left = l;
            right = r;
            edge = e;
        }
    }

    static class CorridorResult {
        final List<NavPoly> polys;
        final List<Portal> portals;

        CorridorResult(List<NavPoly> p, List<Portal> pt) {
            polys = p;
            portals = pt;
        }
    }
}