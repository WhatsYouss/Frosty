package xyz.whatsyouss.frosty.utility.pathfinding;

import net.minecraft.util.math.Vec3d;

import java.util.Collections;
import java.util.List;

public class NavMeshPath {
    private final List<Vec3d> waypoints;
    private final List<NavPoly> corridor;
    private final boolean found;

    public NavMeshPath(List<Vec3d> waypoints, List<NavPoly> corridor, boolean found) {
        this.waypoints = waypoints != null ? waypoints : Collections.emptyList();
        this.corridor = corridor != null ? corridor : Collections.emptyList();
        this.found = found;
    }

    public static NavMeshPath empty() {
        return new NavMeshPath(Collections.emptyList(), Collections.emptyList(), false);
    }

    public List<Vec3d> getWaypoints() {
        return waypoints;
    }

    public List<NavPoly> getCorridor() {
        return corridor;
    }

    public boolean isFound() {
        return found;
    }

    public double getLength() {
        double total = 0;
        for (int i = 0; i < waypoints.size() - 1; i++)
            total += waypoints.get(i).distanceTo(waypoints.get(i + 1));
        return total;
    }

    public int getWaypointCount() {
        return waypoints.size();
    }

    @Override
    public String toString() {
        return "NavMeshPath{found=" + found + ", waypoints=" + waypoints.size() + "}";
    }
}