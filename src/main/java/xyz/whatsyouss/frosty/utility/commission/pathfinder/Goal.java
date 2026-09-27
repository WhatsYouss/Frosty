package xyz.whatsyouss.frosty.utility.commission.pathfinder;

public final class Goal {
    public final int goalX, goalY, goalZ;
    private final CalculationContext ctx;
    public Goal(int x, int y, int z, CalculationContext ctx) { goalX = x; goalY = y; goalZ = z; this.ctx = ctx; }
    public boolean isAtGoal(int x, int y, int z) { return goalX == x && goalY == y && goalZ == z; }
    public double heuristic(int x, int y, int z) {
        int dx = Math.abs(goalX - x), dz = Math.abs(goalZ - z); double vertical = Math.abs(goalY - y), diagonal = Math.min(dx, dz);
        vertical *= goalY > y ? 6.234399666206506 : ctx.cost.N_BLOCK_FALL_COST[2] / 2;
        return (Math.abs(dx - dz) + diagonal * Math.sqrt(2)) * ctx.cost.ONE_BLOCK_SPRINT_COST + vertical;
    }
}
