package xyz.whatsyouss.frosty.modules.impl.mining;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frosty.utility.commission.pathfinder.Path;
import xyz.whatsyouss.frosty.interfaces.IKeyMapping;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

final class CommissionRouteNavigator {
    private static final String GRAPH_RESOURCE = "/assets/frosty/commission/Commission Macro.json";
    private final Minecraft mc = Minecraft.getInstance();
    private final CommissionVeinPathfinder pathfinder = new CommissionVeinPathfinder();
    private final Map<RoutePoint, Set<RoutePoint>> graph = new HashMap<>();
    private List<RoutePoint> route = Collections.emptyList();
    private int routeIndex;
    private final CommissionPathExecutor pathExecutor = new CommissionPathExecutor();
    private CompletableFuture<Path> pendingPath;
    private Status status = Status.IDLE;

    CommissionRouteNavigator() {
        loadGraph();
    }

    void start(BlockPos playerBlock, Vec3 destination) {
        stop();
        List<RoutePoint> planned = findPathWithNearestReachableStart(playerBlock, destination);
        if (planned.isEmpty()) {
            status = Status.FAILED;
            return;
        }
        route = planned;
        routeIndex = 0;
        status = Status.RUNNING;
        planNextSegment(playerBlock);
    }

    boolean tick() {
        if (status != Status.RUNNING || mc.player == null) return false;
        if (pendingPath != null) {
            if (!pendingPath.isDone()) return false;
            try {
                pathExecutor.start(pendingPath.join());
            } catch (RuntimeException ignored) {
                pathExecutor.start(null);
            }
            pendingPath = null;
            return false;
        }

        if (routeIndex >= route.size()) {
            succeed();
            return true;
        }

        if (!pathExecutor.isRunning()) {
            if (pathExecutor.failed()) {
                fail();
                return false;
            }
            routeIndex++;
            if (routeIndex < route.size()) planNextSegment(blockStandingOn());
            return false;
        }
        pathExecutor.tick();
        return false;
    }

    void stop() {
        releaseMovement();
        route = Collections.emptyList();
        routeIndex = 0;
        pathExecutor.stop();
        if (pendingPath != null) pendingPath.cancel(true);
        pendingPath = null;
        status = Status.IDLE;
    }

    boolean isRunning() { return status == Status.RUNNING; }
    boolean failed() { return status == Status.FAILED; }
    boolean succeeded() { return status == Status.SUCCEEDED; }
    BlockPos getTarget() {
        BlockPos pathTarget = pathExecutor.getTarget();
        if (pathTarget != null) return pathTarget;
        return routeIndex < route.size() ? route.get(routeIndex).asBlockPos() : null;
    }

    private void planNextSegment(BlockPos start) {
        if (routeIndex >= route.size()) return;
        pendingPath = pathfinder.find(start, route.get(routeIndex).asBlockPos());
    }

    private void loadGraph() {
        try (InputStream stream = CommissionRouteNavigator.class.getResourceAsStream(GRAPH_RESOURCE)) {
            if (stream == null) throw new IllegalStateException("Missing commission graph resource");
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, RoutePoint> nodes = new HashMap<>();
            JsonArray nodeArray = root.getAsJsonArray("nodes");
            for (JsonElement element : nodeArray) {
                JsonObject node = element.getAsJsonObject();
                RoutePoint point = new RoutePoint(node.get("x").getAsInt(), node.get("y").getAsInt(),
                        node.get("z").getAsInt(), node.get("transport").getAsString());
                nodes.put(node.get("id").getAsString(), point);
                graph.put(point, new LinkedHashSet<>());
            }
            for (JsonElement element : root.getAsJsonArray("edges")) {
                JsonObject edge = element.getAsJsonObject();
                RoutePoint from = nodes.get(edge.get("from").getAsString());
                RoutePoint to = nodes.get(edge.get("to").getAsString());
                if (from == null || to == null) throw new IllegalStateException("Commission graph contains a dangling edge");
                graph.get(from).add(to);
            }
        } catch (Exception exception) {
            graph.clear();
            throw new IllegalStateException("Unable to load Commission Macro graph", exception);
        }
    }

    private List<RoutePoint> findPathWithNearestReachableStart(BlockPos startPos, Vec3 endCandidate) {
        RoutePoint end = resolveWaypoint(endCandidate);
        if (end == null) return Collections.emptyList();

        RoutePoint start = findNearestReachableStart(startPos, end);
        if (start == null) return Collections.emptyList();

        return trimFirstWaypointIfSafe(findPath(start, end), startPos);
    }

    private RoutePoint resolveWaypoint(Vec3 candidate) {
        RoutePoint fallbackWalk = null;
        RoutePoint fallbackAny = null;
        for (RoutePoint point : graph.keySet()) {
            if (point.x != Mth.floor(candidate.x) || point.y != Mth.floor(candidate.y) || point.z != Mth.floor(candidate.z)) continue;
            if ("WALK".equals(point.transport)) fallbackWalk = point;
            if (fallbackAny == null) fallbackAny = point;
        }
        return fallbackWalk != null ? fallbackWalk : fallbackAny;
    }

    private RoutePoint findNearestReachableStart(BlockPos from, RoutePoint end) {
        Set<RoutePoint> reachable = collectNodesThatCanReachEnd(end);
        return reachable.stream().min(Comparator.comparingDouble(point -> point.distanceToSqr(from))).orElse(null);
    }

    private Set<RoutePoint> collectNodesThatCanReachEnd(RoutePoint end) {
        Map<RoutePoint, Set<RoutePoint>> reverse = new HashMap<>();
        for (RoutePoint point : graph.keySet()) reverse.put(point, new LinkedHashSet<>());
        for (Map.Entry<RoutePoint, Set<RoutePoint>> entry : graph.entrySet()) {
            for (RoutePoint neighbor : entry.getValue()) {
                reverse.computeIfAbsent(neighbor, ignored -> new LinkedHashSet<>()).add(entry.getKey());
            }
        }

        Set<RoutePoint> visited = new HashSet<>();
        Deque<RoutePoint> queue = new ArrayDeque<>();
        queue.add(end);
        while (!queue.isEmpty()) {
            RoutePoint current = queue.poll();
            if (!visited.add(current)) continue;
            for (RoutePoint parent : reverse.getOrDefault(current, Collections.emptySet())) {
                if (!visited.contains(parent)) queue.add(parent);
            }
        }
        return visited;
    }

    private List<RoutePoint> findPath(RoutePoint start, RoutePoint end) {
        Deque<RoutePoint> queue = new ArrayDeque<>();
        Set<RoutePoint> visited = new HashSet<>();
        Map<RoutePoint, RoutePoint> parent = new HashMap<>();
        queue.add(start);
        visited.add(start);
        parent.put(start, null);

        while (!queue.isEmpty()) {
            RoutePoint current = queue.poll();
            if (current.equals(end)) {
                List<RoutePoint> path = new ArrayList<>();
                for (RoutePoint at = end; at != null; at = parent.get(at)) path.addFirst(at);
                return path;
            }
            for (RoutePoint neighbor : graph.getOrDefault(current, Collections.emptySet())) {
                if (visited.add(neighbor)) {
                    parent.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }
        return Collections.emptyList();
    }

    private List<RoutePoint> trimFirstWaypointIfSafe(List<RoutePoint> planned, BlockPos playerBlock) {
        if (planned.size() < 2 || playerBlock == null) return planned;
        if (playerBlock.distSqr(planned.getFirst().asBlockPos()) >= planned.getFirst().distanceToSqr(planned.get(1).asBlockPos())) {
            return planned;
        }
        List<RoutePoint> trimmed = new ArrayList<>(planned);
        trimmed.removeFirst();
        return trimmed;
    }

    private BlockPos blockStandingOn() {
        return BlockPos.containing(mc.player.getX(), Math.ceil(mc.player.getY() - 0.25) - 1, mc.player.getZ());
    }

    private void succeed() {
        releaseMovement();
        status = Status.SUCCEEDED;
    }

    private void fail() {
        releaseMovement();
        status = Status.FAILED;
    }

    private void releaseMovement() {
        if (mc.options == null) return;
        IKeyMapping.get(mc.options.keyUp).setDown(false);
        IKeyMapping.get(mc.options.keyDown).setDown(false);
        IKeyMapping.get(mc.options.keyLeft).setDown(false);
        IKeyMapping.get(mc.options.keyRight).setDown(false);
        IKeyMapping.get(mc.options.keyJump).setDown(false);
        IKeyMapping.get(mc.options.keySprint).setDown(false);
    }

    private enum Status { IDLE, RUNNING, SUCCEEDED, FAILED }

    private record RoutePoint(int x, int y, int z, String transport) {
        BlockPos asBlockPos() { return new BlockPos(x, y, z); }
        double distanceToSqr(BlockPos pos) { return asBlockPos().distSqr(pos); }
    }
}

