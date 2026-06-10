package xyz.whatsyouss.frosty.utility.pathfinding;

import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class NavPoly {
    private static volatile int nextId = 0;

    private final int id;
    private final int minX, minZ, maxX, maxZ;
    private final double y;
    private final List<NavEdge> edges = new ArrayList<>();

    private double traversalCost = 1.0;
    private double clearance = 4.0;
    private boolean blacklisted = false;
    private long blacklistExpiry = 0;

    public NavPoly(int minX, int minZ, int maxX, int maxZ, double y) {
        this.id = nextId++;
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
        this.y = y;
    }

    public static void resetIdCounter() { nextId = 0; }

    public boolean containsXZ(double x, double z) {
        return x >= minX && x <= maxX + 1 && z >= minZ && z <= maxZ + 1;
    }

    public boolean containsBlock(int bx, int bz) {
        return bx >= minX && bx <= maxX && bz >= minZ && bz <= maxZ;
    }

    public Vec3d getCenter() {
        return new Vec3d((minX + maxX + 1) / 2.0, y, (minZ + maxZ + 1) / 2.0);
    }

    public int getArea() {
        return (maxX - minX + 1) * (maxZ - minZ + 1);
    }

    public void addEdge(NavEdge edge) { edges.add(edge); }

    public boolean isBlacklisted(long tick) {
        if (!blacklisted) return false;
        if (tick >= blacklistExpiry) { blacklisted = false; return false; }
        return true;
    }

    public void blacklist(long duration, long tick) {
        this.blacklisted = true;
        this.blacklistExpiry = tick + duration;
    }

    public void resetBlacklist() { blacklisted = false; blacklistExpiry = 0; }

    public boolean overlapsCircle(double cx, double cz, double radius) {
        double closestX = Math.max(minX, Math.min(cx, maxX + 1));
        double closestZ = Math.max(minZ, Math.min(cz, maxZ + 1));
        double dx = cx - closestX, dz = cz - closestZ;
        return dx * dx + dz * dz <= radius * radius;
    }

    public int getId() { return id; }
    public int getMinX() { return minX; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxZ() { return maxZ; }
    public double getY() { return y; }
    public int getBlockY() { return (int) Math.floor(y); }
    public List<NavEdge> getEdges() { return edges; }
    public double getTraversalCost() { return traversalCost; }
    public void setTraversalCost(double cost) { this.traversalCost = cost; }
    public double getClearance() { return clearance; }
    public void setClearance(double c) { this.clearance = c; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NavPoly)) return false;
        return id == ((NavPoly) o).id;
    }
    @Override public int hashCode() { return id; }
    @Override public String toString() {
        return "NavPoly{id=" + id + ", [" + minX + "," + minZ + "]->[" + maxX + "," + maxZ + "], y=" + String.format("%.2f", y) + "}";
    }
}