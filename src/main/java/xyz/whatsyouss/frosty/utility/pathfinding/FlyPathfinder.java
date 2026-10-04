package xyz.whatsyouss.frosty.utility.pathfinding;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class FlyPathfinder {

    private static final int MAX_ITERATIONS = 300_000;
    private static final int ENDPOINT_SEARCH_RADIUS = 3;
    private static final double SQRT_2 = Math.sqrt(2.0);
    private static final double SQRT_3 = Math.sqrt(3.0);
    private static final double SAMPLE_STEP = 0.25;

    private static final double PLAYER_HALF_WIDTH = 0.34;
    private static final double PLAYER_HEIGHT = 1.8;
    private static final double COLLISION_EPSILON = 1.0E-4;

    private static final int[][] FLIGHT_NEIGHBORS = {
            {1,0,0},{-1,0,0},{0,0,1},{0,0,-1},
            {0,1,0},{0,-1,0},
            {1,0,1},{1,0,-1},{-1,0,1},{-1,0,-1}
    };

    public NavMeshPath findPath(Vec3 start, Vec3 goal, ClientLevel world, int maxRange) {
        return findPathInternal(start, goal, world, maxRange, -1.0);
    }

    public NavMeshPath findPathToRange(Vec3 start, Vec3 goal, ClientLevel world,
                                       int maxRange, double goalTolerance) {
        return findPathInternal(start, goal, world, maxRange, Math.max(0.0, goalTolerance));
    }

    public boolean hasClearPath(Vec3 from, Vec3 to, ClientLevel world) {
        return isSegmentClear(from, to, world);
    }

    public boolean isPositionClear(Vec3 feetPosition, ClientLevel world) {
        AABB box = playerBox(feetPosition);
        if (!areChunksLoaded(box, world) || !world.noBlockCollision(null, box)) return false;

        int minX = Mth.floor(box.minX);
        int minY = Mth.floor(box.minY);
        int minZ = Mth.floor(box.minZ);
        int maxX = Mth.floor(box.maxX - 1.0E-7);
        int maxY = Mth.floor(box.maxY - 1.0E-7);
        int maxZ = Mth.floor(box.maxZ - 1.0E-7);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockState state = world.getBlockState(new BlockPos(x, y, z));
                    if (NavMeshGenerator.isHazardous(state)
                            || NavMeshGenerator.isNonSolidObstacle(state)) return false;
                }
            }
        }
        return true;
    }

    private NavMeshPath findPathInternal(Vec3 start, Vec3 goal, ClientLevel world,
                                         int maxRange, double goalTolerance) {
        if (world == null || maxRange <= 0) return NavMeshPath.empty();

        Map<Long, Boolean> passability = new HashMap<>();
        BlockPos startBlock = findStartNode(start, world, passability);
        if (startBlock == null) return NavMeshPath.empty();

        Set<Long> goalKeys;
        BlockPos heuristicGoal = BlockPos.containing(goal);
        if (goalTolerance >= 0.0) {
            goalKeys = collectGoalNodes(goal, goalTolerance, world, passability);
            if (goalKeys.isEmpty()) return NavMeshPath.empty();
        } else {
            BlockPos resolvedGoal = findNearestPassableNode(heuristicGoal, goal, world, passability,
                    ENDPOINT_SEARCH_RADIUS, false);
            if (resolvedGoal == null) return NavMeshPath.empty();
            heuristicGoal = resolvedGoal;
            goalKeys = Set.of(key(resolvedGoal));
        }

        PriorityQueue<FlyNode> open = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fCost));
        Map<Long, FlyNode> all = new HashMap<>();
        Set<Long> closed = new HashSet<>();

        long startKey = key(startBlock);
        FlyNode startNode = new FlyNode(startBlock, null, 0,
                heuristic(startBlock, heuristicGoal, goalTolerance));
        open.add(startNode);
        all.put(startKey, startNode);

        int iter = 0;
        while (!open.isEmpty() && iter++ < MAX_ITERATIONS) {
            FlyNode cur = open.poll();
            long curKey = key(cur.pos);
            if (!closed.add(curKey)) continue;

            if (goalKeys.contains(curKey)) {
                List<Vec3> raw = reconstruct(cur);
                if (raw.isEmpty() || start.distanceToSqr(raw.getFirst()) > 0.01) raw.addFirst(start);
                List<Vec3> smoothed = smooth(raw, world);
                return new NavMeshPath(smoothed, Collections.emptyList(), true);
            }

            if (cur.pos.distManhattan(startBlock) > maxRange) continue;

            for (int[] dir : FLIGHT_NEIGHBORS) {
                BlockPos next = cur.pos.offset(dir[0], dir[1], dir[2]);
                long nextKey = key(next);
                if (closed.contains(nextKey)
                        || !isNodePassable(next, world, passability)
                        || !isNodeTransitionClear(cur.pos, next, world)) continue;

                int axes = (dir[0] != 0 ? 1 : 0)
                        + (dir[1] != 0 ? 1 : 0)
                        + (dir[2] != 0 ? 1 : 0);
                double moveCost = axes == 1 ? 1.0 : axes == 2 ? SQRT_2 : SQRT_3;
                double tentativeG = cur.gCost + moveCost;

                FlyNode node = all.get(nextKey);
                if (node == null) {
                    node = new FlyNode(next, cur, tentativeG,
                            tentativeG + heuristic(next, heuristicGoal, goalTolerance));
                    all.put(nextKey, node);
                    open.add(node);
                } else if (tentativeG < node.gCost) {
                    node.parent = cur;
                    node.gCost = tentativeG;
                    node.fCost = tentativeG + heuristic(next, heuristicGoal, goalTolerance);
                    open.add(node);
                }
            }
        }
        return NavMeshPath.empty();
    }

    private BlockPos findStartNode(Vec3 start, ClientLevel world, Map<Long, Boolean> passability) {
        BlockPos origin = BlockPos.containing(start);
        return findNearestPassableNode(origin, start, world, passability,
                ENDPOINT_SEARCH_RADIUS, true);
    }

    private BlockPos findNearestPassableNode(BlockPos origin, Vec3 exactPosition, ClientLevel world,
                                             Map<Long, Boolean> passability, int radius,
                                             boolean requireConnectionToExactPosition) {
        List<BlockPos> candidates = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    candidates.add(origin.offset(dx, dy, dz));
                }
            }
        }
        candidates.sort(Comparator.comparingDouble(p -> nodePosition(p).distanceToSqr(exactPosition)));

        for (BlockPos candidate : candidates) {
            if (!isNodePassable(candidate, world, passability)) continue;
            if (!requireConnectionToExactPosition
                    || isSegmentClear(exactPosition, nodePosition(candidate), world)) return candidate;
        }
        return null;
    }

    private Set<Long> collectGoalNodes(Vec3 goal, double tolerance, ClientLevel world,
                                       Map<Long, Boolean> passability) {
        int radius = Math.max(1, (int) Math.ceil(tolerance + 1.0));
        BlockPos center = BlockPos.containing(goal);
        Set<Long> result = new HashSet<>();
        double toleranceSq = tolerance * tolerance;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos candidate = center.offset(dx, dy, dz);
                    if (nodePosition(candidate).distanceToSqr(goal) <= toleranceSq
                            && isNodePassable(candidate, world, passability)) {
                        result.add(key(candidate));
                    }
                }
            }
        }
        return result;
    }

    private boolean isNodePassable(BlockPos pos, ClientLevel world, Map<Long, Boolean> passability) {
        return passability.computeIfAbsent(key(pos), ignored -> isPositionClear(nodePosition(pos), world));
    }

    private boolean isNodeTransitionClear(BlockPos from, BlockPos to, ClientLevel world) {
        return isSegmentClear(nodePosition(from), nodePosition(to), world);
    }

    private boolean isSegmentClear(Vec3 from, Vec3 to, ClientLevel world) {
        double distance = from.distanceTo(to);
        if (distance < 1.0E-6) return isPositionClear(to, world);

        int steps = Math.max(1, (int) Math.ceil(distance / SAMPLE_STEP));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            if (!isPositionClear(from.lerp(to, t), world)) return false;
        }
        return true;
    }

    private boolean areChunksLoaded(AABB box, ClientLevel world) {
        int minChunkX = Mth.floor(box.minX) >> 4;
        int maxChunkX = Mth.floor(box.maxX - 1.0E-7) >> 4;
        int minChunkZ = Mth.floor(box.minZ) >> 4;
        int maxChunkZ = Mth.floor(box.maxZ - 1.0E-7) >> 4;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (!world.hasChunk(cx, cz)) return false;
            }
        }
        return true;
    }

    private AABB playerBox(Vec3 feetPosition) {
        return new AABB(
                feetPosition.x - PLAYER_HALF_WIDTH,
                feetPosition.y + COLLISION_EPSILON,
                feetPosition.z - PLAYER_HALF_WIDTH,
                feetPosition.x + PLAYER_HALF_WIDTH,
                feetPosition.y + PLAYER_HEIGHT - COLLISION_EPSILON,
                feetPosition.z + PLAYER_HALF_WIDTH
        );
    }

    private double heuristic(BlockPos a, BlockPos b, double goalTolerance) {
        int dx = Math.abs(a.getX() - b.getX());
        int dy = Math.abs(a.getY() - b.getY());
        int dz = Math.abs(a.getZ() - b.getZ());
        int largest = Math.max(dx, Math.max(dy, dz));
        int smallest = Math.min(dx, Math.min(dy, dz));
        int middle = dx + dy + dz - largest - smallest;
        double gridDistance = largest + (SQRT_2 - 1.0) * middle + (SQRT_3 - SQRT_2) * smallest;
        return goalTolerance >= 0.0 ? Math.max(0.0, gridDistance - goalTolerance) : gridDistance;
    }

    private List<Vec3> reconstruct(FlyNode goal) {
        LinkedList<Vec3> path = new LinkedList<>();
        for (FlyNode cur = goal; cur != null; cur = cur.parent) path.addFirst(nodePosition(cur.pos));
        return path;
    }

    private List<Vec3> smooth(List<Vec3> raw, ClientLevel world) {
        if (raw.size() <= 2) return raw;
        List<Vec3> result = new ArrayList<>();
        result.add(raw.getFirst());
        int anchor = 0;
        while (anchor < raw.size() - 1) {
            int lastValid = anchor + 1;
            if (!isSegmentClear(raw.get(anchor), raw.get(lastValid), world)) return raw;
            for (int candidate = anchor + 2; candidate < raw.size(); candidate++) {
                if (!canMergeFlightSegment(raw.get(anchor), raw.get(candidate), world)) break;
                lastValid = candidate;
            }
            result.add(raw.get(lastValid));
            anchor = lastValid;
        }
        return result;
    }

    private boolean canMergeFlightSegment(Vec3 from, Vec3 to, ClientLevel world) {
        boolean level = Mth.floor(from.y) == Mth.floor(to.y);
        boolean column = Mth.floor(from.x) == Mth.floor(to.x)
                && Mth.floor(from.z) == Mth.floor(to.z);
        if (level || column) return isSegmentClear(from, to, world);
        if (Math.abs(Mth.floor(to.y) - Mth.floor(from.y)) > 3) return false;
        Vec3 corner = new Vec3(from.x, to.y, from.z);
        return isSegmentClear(from, to, world)
                && isSegmentClear(from, corner, world)
                && isSegmentClear(corner, to, world);
    }

    private static Vec3 nodePosition(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
    }

    private static long key(BlockPos pos) {
        return pos.asLong();
    }

    private static class FlyNode {
        final BlockPos pos;
        FlyNode parent;
        double gCost;
        double fCost;

        FlyNode(BlockPos pos, FlyNode parent, double gCost, double fCost) {
            this.pos = pos;
            this.parent = parent;
            this.gCost = gCost;
            this.fCost = fCost;
        }
    }
}
