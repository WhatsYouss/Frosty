package xyz.whatsyouss.frosty.utility.commission.pathfinder;

public final class ActionCosts {
    public final double INF_COST = 1e6;
    public final double[] N_BLOCK_FALL_COST = generateNBlocksFallCost();
    public final double ONE_UP_LADDER_COST = 1 / (0.12 * 9.8);
    public final double ONE_DOWN_LADDER_COST = 1 / 0.15;
    public final double JUMP_ONE_BLOCK_COST;
    public final double ONE_BLOCK_WALK_COST;
    public final double ONE_BLOCK_SPRINT_COST;
    public final double ONE_BLOCK_SNEAK_COST;
    public final double ONE_BLOCK_WALK_IN_WATER_COST;
    public final double ONE_BLOCK_WALK_OVER_SOUL_SAND_COST;
    public final double WALK_OFF_ONE_BLOCK_COST;
    public final double CENTER_AFTER_FALL_COST;
    public final double SPRINT_MULTIPLIER;

    public ActionCosts(double sprintFactor, double walkingFactor, double sneakingFactor, int jumpBoostLevel) {
        ONE_BLOCK_WALK_COST = 1 / actionTime(walkingFriction(walkingFactor));
        ONE_BLOCK_SPRINT_COST = 1 / actionTime(walkingFriction(sprintFactor));
        ONE_BLOCK_SNEAK_COST = 1 / actionTime(walkingFriction(sneakingFactor));
        ONE_BLOCK_WALK_IN_WATER_COST = 20 * actionTime(waterFriction(walkingFactor));
        ONE_BLOCK_WALK_OVER_SOUL_SAND_COST = ONE_BLOCK_WALK_COST * 2;
        WALK_OFF_ONE_BLOCK_COST = ONE_BLOCK_WALK_COST * .8;
        CENTER_AFTER_FALL_COST = ONE_BLOCK_WALK_COST * .2;
        SPRINT_MULTIPLIER = walkingFactor / sprintFactor;
        double velocity = .42 + (jumpBoostLevel + 1) * .1, height = 0, time = 1;
        for (int i = 1; i <= 20; i++) { height += velocity; velocity = (velocity - .08) * .98; if (velocity < 0) break; time++; }
        JUMP_ONE_BLOCK_COST = time + fallDistanceToTicks(height - 1);
    }
    private static double walkingFriction(double factor) { return factor * (.16277136 / (.91 * .91 * .91)); }
    private static double waterFriction(double factor) { return .02 + (factor - .02) / 3; }
    private static double actionTime(double friction) { return friction * 10; }
    public double motionYAtTick(int tick) { double velocity = -.0784000015258789; for (int i = 1; i <= tick; i++) velocity = (velocity - .08) * .9800000190734863; return velocity; }
    public double fallDistanceToTicks(double distance) { if (distance == 0) return 0; int ticks = 0; while (true) { double fall = downwardMotionAtTick(ticks); if (distance <= fall) return ticks + distance / fall; distance -= fall; ticks++; } }
    private static double downwardMotionAtTick(int tick) { return (Math.pow(.98, tick) - 1) * -3.92; }
    private static double[] generateNBlocksFallCost() { double[] costs = new double[257]; double current = 0; int target = 1, ticks = 0; while (true) { double velocity = downwardMotionAtTick(ticks); if (current + velocity >= target) { costs[target] = ticks + (target - current) / velocity; if (++target > 256) return costs; } else { current += velocity; ticks++; } } }
}
