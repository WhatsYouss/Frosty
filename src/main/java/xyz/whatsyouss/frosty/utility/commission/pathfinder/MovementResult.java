package xyz.whatsyouss.frosty.utility.commission.pathfinder;

import net.minecraft.core.BlockPos;
public final class MovementResult {
    public int x, y, z; public double cost = 1e6;
    public void set(int x, int y, int z) { this.x=x; this.y=y; this.z=z; }
    public void reset() { x=y=z=0; cost=1e6; }
    public BlockPos getDest() { return new BlockPos(x,y,z); }
}
