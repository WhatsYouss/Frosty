package xyz.whatsyouss.frosty.utility.pathfinding;

import net.minecraft.world.phys.Vec3;

public class NavEdge {
    private final NavPoly polyA;
    private final NavPoly polyB;
    private final Vec3 leftVertex;
    private final Vec3 rightVertex;
    private final boolean yTransition;

    public NavEdge(NavPoly polyA, NavPoly polyB, Vec3 leftVertex, Vec3 rightVertex, boolean yTransition) {
        this.polyA = polyA;
        this.polyB = polyB;
        this.leftVertex = leftVertex;
        this.rightVertex = rightVertex;
        this.yTransition = yTransition;
    }

    public NavPoly getOther(NavPoly from) {
        return from.equals(polyA) ? polyB : polyA;
    }

    public Vec3 getMidpoint() {
        return new Vec3(
                (leftVertex.x + rightVertex.x) / 2.0,
                (leftVertex.y + rightVertex.y) / 2.0,
                (leftVertex.z + rightVertex.z) / 2.0
        );
    }

    public double getWidth() { return leftVertex.distanceTo(rightVertex); }

    public NavPoly getPolyA() { return polyA; }
    public NavPoly getPolyB() { return polyB; }
    public Vec3 getLeftVertex() { return leftVertex; }
    public Vec3 getRightVertex() { return rightVertex; }
    public boolean isYTransition() { return yTransition; }

    @Override
    public String toString() {
        return "NavEdge{" + polyA.getId() + " <-> " + polyB.getId() +
                ", L=" + leftVertex + ", R=" + rightVertex +
                (yTransition ? " [Y]" : "") + "}";
    }
}