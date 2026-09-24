package xyz.whatsyouss.frosty.modules.impl.render;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.RenderUtils;
import xyz.whatsyouss.frosty.utility.Utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class WormLavaESP extends Module {
    private static final int MIN_COORDINATE = 500;
    private static final int MAX_COORDINATE = 1000;
    private static final int MIN_Y = 65;
    private static final long SCAN_INTERVAL_MS = 3_000L;
    private static final Color LAVA_COLOR = new Color(255, 96, 0);

    private final SliderSetting scanRange;
    private List<RenderUtils.ColoredBox> targets = List.of();
    private long lastScanTime;

    public WormLavaESP() {
        super("WormLavaESP", "蠕虫岩浆透视", category.Render);

        this.registerSetting(scanRange = new SliderSetting("Scan Range", 64, 32, 512, 4, "扫描范围"));
    }

    @Override
    public void onDisable() {
        clearTargets();
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (!Utils.nullCheck() || !isCrystalHollows()) {
            clearTargets();
            return;
        }
        if (System.currentTimeMillis() - lastScanTime >= SCAN_INTERVAL_MS) {
            lastScanTime = System.currentTimeMillis();
            rescanLavaFast(mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ());
        }
        RenderUtils.drawBoxes(event.getMatrix(), targets, 0.25f, 1.0f, 2.0f, true, false, false);
    }

    private void rescanLavaFast(int playerX, int playerY, int playerZ) {
        int range = (int) scanRange.getInput();
        int minX = Math.max(MIN_COORDINATE, playerX - range), maxX = Math.min(MAX_COORDINATE, playerX + range);
        int minZ = Math.max(MIN_COORDINATE, playerZ - range), maxZ = Math.min(MAX_COORDINATE, playerZ + range);
        int minY = Math.max(MIN_Y, playerY - range), maxY = Math.min(mc.level.getMaxY() - 1, playerY + range);
        List<RenderUtils.ColoredBox> result = new ArrayList<>();
        for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++)
            for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                LevelChunk chunk = mc.level.getChunkSource().getChunk(chunkX, chunkZ, false);
                if (chunk == null) continue;
                LevelChunkSection[] sections = chunk.getSections();
                int startX = Math.max(minX, chunkX << 4), endX = Math.min(maxX, (chunkX << 4) + 15);
                int startZ = Math.max(minZ, chunkZ << 4), endZ = Math.min(maxZ, (chunkZ << 4) + 15);
                for (int y = minY; y <= maxY; y++) {
                    int sectionIndex = chunk.getSectionIndex(y);
                    if (sectionIndex < 0 || sectionIndex >= sections.length || sections[sectionIndex] == null || sections[sectionIndex].hasOnlyAir())
                        continue;
                    for (int x = startX; x <= endX; x++)
                        for (int z = startZ; z <= endZ; z++)
                            if (sections[sectionIndex].getBlockState(x & 15, y & 15, z & 15).is(Blocks.LAVA))
                                result.add(new RenderUtils.ColoredBox(new AABB(x, y, z, x + 1, y + 1, z + 1), LAVA_COLOR));
                }
            }
        targets = result;
    }

    private boolean isCrystalHollows() {
        Map<String, String> location = Utils.getCurrentLocation();
        return Objects.equals(location.get("Area"), "Crystal Hollows");
    }

    private void clearTargets() {
        targets = List.of();
        lastScanTime = 0L;
    }
}
