package xyz.whatsyouss.frosty.modules.impl.mining;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public enum DwarvenCommission {
    MITHRIL_MINER("Mithril Miner", Kind.MINING, false,
            point(-41, 138, -13), point(-58, 146, -18), point(93, 144, 51), point(28, 130, 26),
            point(115, 153, 83), point(141, 152, 27), point(-111, 166, -74), point(-145, 206, -30),
            point(53, 197, -24)),
    TITANIUM_MINER("Titanium Miner", Kind.MINING, true,
            point(-41, 138, -13), point(-58, 146, -18), point(93, 144, 51), point(28, 130, 26),
            point(115, 153, 83), point(141, 152, 27), point(-111, 166, -74), point(-145, 206, -30),
            point(53, 197, -24)),
    UPPER_MITHRIL("Upper Mines Mithril", Kind.MINING, false, point(-111, 166, -74), point(-145, 206, -30)),
    UPPER_TITANIUM("Upper Mines Titanium", Kind.MINING, true, point(-111, 166, -74), point(-145, 206, -30)),
    ROYAL_MITHRIL("Royal Mines Mithril", Kind.MINING, false, point(115, 153, 83), point(141, 152, 27)),
    ROYAL_TITANIUM("Royal Mines Titanium", Kind.MINING, true, point(115, 153, 83), point(141, 152, 27)),
    LAVA_MITHRIL("Lava Springs Mithril", Kind.MINING, false, point(53, 197, -24)),
    LAVA_TITANIUM("Lava Springs Titanium", Kind.MINING, true, point(53, 197, -24)),
    CLIFFSIDE_MITHRIL("Cliffside Veins Mithril", Kind.MINING, false, point(93, 144, 51), point(28, 130, 26)),
    CLIFFSIDE_TITANIUM("Cliffside Veins Titanium", Kind.MINING, true, point(93, 144, 51), point(28, 130, 26)),
    RAMPARTS_MITHRIL("Rampart's Quarry Mithril", Kind.MINING, false, point(-41, 138, -13), point(-58, 146, -18)),
    RAMPARTS_TITANIUM("Rampart's Quarry Titanium", Kind.MINING, true, point(-41, 138, -13), point(-58, 146, -18)),
    GOBLIN_SLAYER("Goblin Slayer", Kind.SLAYER, false, point(-56, 134, 153)),
    GLACITE_WALKER_SLAYER("Glacite Walker Slayer", Kind.SLAYER, false, point(5, 127, 143)),
    MINES_SLAYER("Mines Slayer", Kind.SLAYER, false, point(5, 127, 143)),
    CLAIM("Claim Commission", Kind.CLAIM, false,
            point(44, 134, 21), point(58, 197, -11), point(171, 149, 33), point(-75, 152, -11), point(-132, 173, -53));

    public enum Kind { MINING, SLAYER, CLAIM }

    private static final Map<String, DwarvenCommission> BY_NAME = new HashMap<>();
    private static final Set<DwarvenCommission> GENERIC_MINERS = EnumSet.of(MITHRIL_MINER, TITANIUM_MINER);

    static {
        for (DwarvenCommission commission : values()) {
            BY_NAME.put(commission.name, commission);
        }
    }

    private final String name;
    private final Kind kind;
    private final boolean titanium;
    private final List<Vec3> waypoints;

    DwarvenCommission(String name, Kind kind, boolean titanium, Vec3... waypoints) {
        this.name = name;
        this.kind = kind;
        this.titanium = titanium;
        this.waypoints = List.of(waypoints);
    }

    public String getName() { return name; }
    public Kind getKind() { return kind; }
    public boolean isTitanium() { return titanium; }
    public boolean isGenericMiner() { return GENERIC_MINERS.contains(this); }

    public static DwarvenCommission fromName(String name) {
        return BY_NAME.get(name);
    }

    public Vec3 getBestWaypoint() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return waypoints.getFirst();

        return waypoints.stream().min(Comparator
                .comparingInt(this::nearbyPlayerCount)
                .thenComparingDouble(point -> point.distanceToSqr(mc.player.position())))
                .orElse(waypoints.getFirst());
    }

    public Vec3 getClosestWaypointTo(Vec3 position) {
        return waypoints.stream()
                .min(Comparator.comparingDouble(point -> point.distanceToSqr(position)))
                .orElse(waypoints.getFirst());
    }

    public String[] getMobNames() {
        return switch (this) {
            case GOBLIN_SLAYER -> new String[]{"Goblin", "Knifethrower", "Fireslinger"};
            case GLACITE_WALKER_SLAYER -> new String[]{"Glacite Walker"};
            case MINES_SLAYER -> new String[]{"Glacite Walker"};
            default -> new String[0];
        };
    }

    public static DwarvenCommission selectBest(Map<DwarvenCommission, Double> commissions) {
        return commissions.entrySet().stream()
                .min(Comparator.comparingDouble(entry -> selectionScore(entry.getKey(), entry.getValue())))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private static double selectionScore(DwarvenCommission commission, double progress) {
        double base = switch (commission) {
            case TITANIUM_MINER, UPPER_TITANIUM, ROYAL_TITANIUM, LAVA_TITANIUM,
                    CLIFFSIDE_TITANIUM, RAMPARTS_TITANIUM -> 5;
            case MITHRIL_MINER -> 15;
            case UPPER_MITHRIL, ROYAL_MITHRIL, LAVA_MITHRIL, CLIFFSIDE_MITHRIL,
                    RAMPARTS_MITHRIL -> 10;
            case GLACITE_WALKER_SLAYER, MINES_SLAYER -> 20;
            case GOBLIN_SLAYER -> 30;
            default -> 100;
        };
        return base + ((1.0 - Math.clamp(progress, 0.0, 1.0)) * 6.0);
    }

    private int nearbyPlayerCount(Vec3 point) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return 0;
        return (int) mc.level.players().stream()
                .filter(player -> player != mc.player && player.isAlive())
                .filter(player -> player.position().distanceToSqr(point) <= 81.0)
                .count();
    }

    public boolean usesMiningToolForSlayer() {
        return this == GLACITE_WALKER_SLAYER || this == MINES_SLAYER;
    }

    private static Vec3 point(double x, double y, double z) {
        return new Vec3(x, y, z);
    }
}

