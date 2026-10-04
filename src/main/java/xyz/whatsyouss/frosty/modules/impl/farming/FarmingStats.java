package xyz.whatsyouss.frosty.modules.impl.farming;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class FarmingStats {
    private static final long BPS_WINDOW_MS = 30_000L;
    private static final long CLICK_EXPIRY_MS = 1_000L;
    private static final long MAX_CULTIVATING_DELTA = 50_000L;
    private static final Pattern FORMATTING_CODES = Pattern.compile("§[0-9a-fk-or]");
    private static final Set<String> BASE_CROPS = Set.of(
            "Wheat", "Potato", "Carrot", "Melon Slice", "Pumpkin", "Sugar Cane",
            "Cactus", "Nether Wart", "Cocoa Beans", "Red Mushroom", "Brown Mushroom",
            "Sunflower", "Moonflower", "Wild Rose", "Seeds");
    private static final Map<String, Double> NPC_PRICES = Map.ofEntries(
            Map.entry("Wheat", 6.0), Map.entry("Enchanted Wheat", 960.0), Map.entry("Enchanted Hay Bale", 153600.0),
            Map.entry("Seeds", 3.0), Map.entry("Enchanted Seeds", 480.0), Map.entry("Box of Seeds", 76800.0),
            Map.entry("Potato", 3.0), Map.entry("Enchanted Potato", 480.0), Map.entry("Enchanted Baked Potato", 76800.0),
            Map.entry("Carrot", 3.0), Map.entry("Enchanted Carrot", 480.0), Map.entry("Enchanted Golden Carrot", 76800.0),
            Map.entry("Melon Slice", 2.0), Map.entry("Melon Block", 18.0), Map.entry("Enchanted Melon Slice", 320.0),
            Map.entry("Enchanted Melon", 51200.0),
            Map.entry("Pumpkin", 10.0), Map.entry("Enchanted Pumpkin", 1600.0), Map.entry("Polished Pumpkin", 256000.0),
            Map.entry("Sugar Cane", 4.0), Map.entry("Enchanted Sugar", 640.0), Map.entry("Enchanted Sugar Cane", 102400.0),
            Map.entry("Cactus", 4.0), Map.entry("Enchanted Cactus Green", 640.0), Map.entry("Enchanted Cactus", 102400.0),
            Map.entry("Red Mushroom", 10.0), Map.entry("Brown Mushroom", 10.0),
            Map.entry("Enchanted Red Mushroom", 1600.0), Map.entry("Enchanted Brown Mushroom", 1600.0),
            Map.entry("Enchanted Red Mushroom Block", 256000.0), Map.entry("Enchanted Brown Mushroom Block", 256000.0),
            Map.entry("Cocoa Beans", 3.0), Map.entry("Enchanted Cocoa Beans", 480.0), Map.entry("Enchanted Cookie", 76800.0),
            Map.entry("Nether Wart", 4.0), Map.entry("Enchanted Nether Wart", 640.0), Map.entry("Mutant Nether Wart", 102400.0),
            Map.entry("Sunflower", 4.0), Map.entry("Enchanted Sunflower", 640.0), Map.entry("Compacted Sunflower", 102400.0),
            Map.entry("Moonflower", 4.0), Map.entry("Enchanted Moonflower", 640.0), Map.entry("Compacted Moonflower", 102400.0),
            Map.entry("Wild Rose", 4.0), Map.entry("Enchanted Wild Rose", 640.0), Map.entry("Compacted Wild Rose", 102400.0),
            Map.entry("Cropie", 25000.0), Map.entry("Squash", 75000.0), Map.entry("Fermento", 250000.0));
    private static final Set<String> BONUS_DROPS = Set.of("Cropie", "Squash", "Fermento");

    private static final Deque<Long> breakTimes = new ArrayDeque<>();
    private static final Map<BlockPos, Long> recentClicks = new HashMap<>();
    private static final Map<String, Long> previousInventory = new LinkedHashMap<>();
    private static final Map<String, Long> farmedItems = new LinkedHashMap<>();
    private static long farmingClockMs;
    private static long sessionStartMs;
    private static long sessionEndMs;
    private static long lastCultivating = -1L;
    private static String currentCrop = "Wheat";
    private static boolean inventoryBaselined;

    private FarmingStats() {
    }

    public static void reset() {
        breakTimes.clear();
        recentClicks.clear();
        previousInventory.clear();
        farmedItems.clear();
        farmingClockMs = 0L;
        sessionStartMs = System.currentTimeMillis();
        sessionEndMs = 0L;
        lastCultivating = -1L;
        currentCrop = "Wheat";
        inventoryBaselined = false;
    }

    public static void freezeSession() {
        if (sessionStartMs > 0L && sessionEndMs == 0L) sessionEndMs = System.currentTimeMillis();
    }

    public static void onBreakClick(Minecraft client, BlockPos pos) {
        if (!isFarming() || client.level == null || client.level.getBlockState(pos).isAir()) return;
        recentClicks.put(pos.immutable(), System.currentTimeMillis());
    }

    public static void onImmediateBreak(BlockPos pos) {
        recentClicks.remove(pos);
        onConfirmedBreak();
    }

    public static void discardBreakClick(BlockPos pos) {
        recentClicks.remove(pos);
    }

    public static void onBlockChanged(BlockPos pos, BlockState newState) {
        if (newState.isAir() && recentClicks.remove(pos) != null) onConfirmedBreak();
    }

    private static void onConfirmedBreak() {
        if (!isFarming()) return;
        breakTimes.addLast(farmingClockMs);
        trimBreaks();
    }

    public static void tick(Minecraft client, boolean farming) {
        if (client.player == null || client.level == null) return;
        if (farming) farmingClockMs += 50L;
        trimBreaks();
        long now = System.currentTimeMillis();
        recentClicks.entrySet().removeIf(entry -> {
            if (now - entry.getValue() > CLICK_EXPIRY_MS) return true;
            if (client.level.getBlockState(entry.getKey()).isAir()) {
                onConfirmedBreak();
                return true;
            }
            return false;
        });
        trackProfit(client);
    }

    private static void trimBreaks() {
        while (!breakTimes.isEmpty() && farmingClockMs - breakTimes.peekFirst() > BPS_WINDOW_MS) {
            breakTimes.removeFirst();
        }
    }

    private static boolean isFarming() {
        return xyz.whatsyouss.frosty.modules.ModuleManager.farmingMacro != null
                && xyz.whatsyouss.frosty.modules.ModuleManager.farmingMacro.isFarmingState();
    }

    public static float getBps() {
        trimBreaks();
        float seconds = Math.max(0.1f, Math.min(30.0f, farmingClockMs / 1000.0f));
        return breakTimes.size() / seconds;
    }

    public static long getProfitPerHour() {
        if (sessionStartMs <= 0L) return 0L;
        long elapsedMs = Math.max(1L, (sessionEndMs == 0L ? System.currentTimeMillis() : sessionEndMs) - sessionStartMs);
        double total = 0.0;
        for (Map.Entry<String, Long> entry : farmedItems.entrySet()) {
            total += NPC_PRICES.getOrDefault(entry.getKey(), 0.0) * entry.getValue();
        }
        return (long) (total / (elapsedMs / 3_600_000.0));
    }

    private static void trackProfit(Minecraft client) {
        Map<String, Long> currentCounts = new LinkedHashMap<>();
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = client.player.getInventory().getItem(slot);
            if (stack.isEmpty()) continue;
            String name = FORMATTING_CODES.matcher(stack.getHoverName().getString()).replaceAll("").trim();
            if (BASE_CROPS.contains(name) || BONUS_DROPS.contains(name)) {
                currentCounts.merge(name, (long) stack.getCount(), Long::sum);
            }
        }

        String detectedCrop = null;
        long maxIncrease = 0L;
        if (inventoryBaselined) {
            for (Map.Entry<String, Long> entry : currentCounts.entrySet()) {
                long increase = entry.getValue() - previousInventory.getOrDefault(entry.getKey(), 0L);
                if (BASE_CROPS.contains(entry.getKey()) && increase > maxIncrease) {
                    maxIncrease = increase;
                    detectedCrop = entry.getKey();
                }
                if (BONUS_DROPS.contains(entry.getKey()) && increase > 0L) {
                    record(entry.getKey(), increase);
                }
            }
        }
        if (detectedCrop != null) currentCrop = detectedCrop;

        ItemStack held = client.player.getMainHandItem();
        CustomData custom = held.isEmpty() ? null : held.get(DataComponents.CUSTOM_DATA);
        long cultivating = -1L;
        if (custom != null) {
            var tag = custom.copyTag();
            if (tag.contains("farmed_cultivating")) {
                cultivating = tag.getLong("farmed_cultivating").orElse(-1L);
            }
        }
        if (cultivating >= 0L) {
            long delta = cultivating - lastCultivating;
            if (lastCultivating >= 0L && delta > 0L && delta <= MAX_CULTIVATING_DELTA) {
                if (currentCrop.equals("Wheat") || currentCrop.equals("Seeds")) {
                    long wheat = Math.round(delta / 2.5);
                    record("Wheat", wheat);
                    record("Seeds", delta - wheat);
                } else {
                    record(currentCrop, delta);
                }
            }
        } else if (inventoryBaselined) {
            // Tools without Cultivating still have measurable inventory gains.
            for (String crop : BASE_CROPS) {
                long before = previousInventory.getOrDefault(crop, 0L);
                long increase = currentCounts.getOrDefault(crop, 0L) - before;
                if (increase > 0L) record(crop, increase);
            }
        }
        lastCultivating = cultivating;
        previousInventory.clear();
        previousInventory.putAll(currentCounts);
        inventoryBaselined = true;
    }

    private static void record(String item, long count) {
        if (count > 0L) farmedItems.merge(item, count, Long::sum);
    }
}
