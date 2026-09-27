package xyz.whatsyouss.frosty.utility.commission.pathfinder;

import net.minecraft.core.BlockPos;
import java.util.Objects;

public final class PathNode {
    public final int x, y, z; public final Goal goal;
    public double costSoFar = 1e6, costToEnd, totalCost = 1; public int heapPosition = -1; public PathNode parentNode;
    public PathNode(int x, int y, int z, Goal goal) { this.x=x; this.y=y; this.z=z; this.goal=goal; costToEnd=goal.heuristic(x,y,z); }
    public BlockPos getBlock() { return new BlockPos(x,y,z); }
    public static long longHash(int x, int y, int z) { long hash=3241L; hash=3457689L*hash+x; hash=8734625L*hash+y; return 2873465L*hash+z; }
    @Override public boolean equals(Object object) { return object instanceof PathNode node && x == node.x && y == node.y && z == node.z; }
    @Override public int hashCode() { return Objects.hash(x,y,z); }
}

