package xyz.whatsyouss.frosty.utility.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NavMesh {
    private final List<NavPoly> polygons;
    private final Map<Integer, NavPoly> polyById;

    public NavMesh(List<NavPoly> polygons) {
        this.polygons = new ArrayList<>(polygons);
        this.polyById = new HashMap<>();
        for (NavPoly p : polygons) polyById.put(p.getId(), p);
    }

    public NavPoly getPolyAt(Vec3 pos) {
        NavPoly best = null;
        double bestYDist = Double.MAX_VALUE;
        for (NavPoly p : polygons) {
            if (p.containsXZ(pos.x, pos.z)) {
                double d = Math.abs(pos.y - p.getY());
                if (d < 3.0 && d < bestYDist) { best = p; bestYDist = d; }
            }
        }
        return best;
    }

    public NavPoly getPolyAt(BlockPos pos) {
        return getPolyAt(new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
    }

    public NavPoly getNearestPoly(Vec3 pos, double maxDist) {
        NavPoly best = null;
        double bestDist = Double.MAX_VALUE;
        for (NavPoly p : polygons) {
            if (Math.abs(pos.y - p.getY()) > 5.0) continue;
            Vec3 c = p.getCenter();
            double xzDist = Math.sqrt((pos.x - c.x) * (pos.x - c.x) + (pos.z - c.z) * (pos.z - c.z));
            double total = xzDist + Math.abs(pos.y - p.getY()) * 2.0;
            if (total < bestDist && total < maxDist) { best = p; bestDist = total; }
        }
        return best;
    }

    public List<NavPoly> getAllPolygons() { return polygons; }
    public NavPoly getPolyById(int id) { return polyById.get(id); }
    public int getPolyCount() { return polygons.size(); }

    public void clearBlacklists() { polygons.forEach(NavPoly::resetBlacklist); }

    public int getBlacklistedCount(long tick) {
        int n = 0;
        for (NavPoly p : polygons) if (p.isBlacklisted(tick)) n++;
        return n;
    }

    @Override public String toString() { return "NavMesh{polygons=" + polygons.size() + "}"; }
}