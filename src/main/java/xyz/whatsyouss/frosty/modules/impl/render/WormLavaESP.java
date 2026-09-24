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
import xyz.whatsyouss.frosty.utility.BufferSource;
import xyz.whatsyouss.frosty.utility.RenderUtils;
import xyz.whatsyouss.frosty.utility.Utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class WormLavaESP extends Module {

    private SliderSetting scanRange;
    private static final int MIN_COORDINATE = 500;
    private static final int MAX_COORDINATE = 1000;
    private static final int MIN_Y = 65;
    private static final long SCAN_INTERVAL_MS = 3_000L;
    private static final Color LAVA_COLOR = new Color(255, 96, 0);

    private List<RenderUtils.ColoredBox> targets = List.of();
    private BufferSource boxBuffer;

    private long lastScanTime = 0L;

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

        int playerX = mc.player.getBlockX();
        int playerY = mc.player.getBlockY();
        int playerZ = mc.player.getBlockZ();
        long now = System.currentTimeMillis();

        if (now - lastScanTime >= SCAN_INTERVAL_MS) {
            lastScanTime = now;

            rescanLavaFast(playerX, playerY, playerZ);
        }

        if (!targets.isEmpty()) {
            if (boxBuffer == null) boxBuffer = BufferSource.reusable();
            RenderUtils.drawBoxes(boxBuffer, event.getMatrix(), targets, 0.25f, 1.0f, 2.0f, true, false, false);
        }
    }

    private void rescanLavaFast(int playerX, int playerY, int playerZ) {
        int SCAN_RANGE = (int) scanRange.getInput();
        int minX = Math.max(MIN_COORDINATE, playerX - SCAN_RANGE);
        int maxX = Math.min(MAX_COORDINATE, playerX + SCAN_RANGE);
        int minZ = Math.max(MIN_COORDINATE, playerZ - SCAN_RANGE);
        int maxZ = Math.min(MAX_COORDINATE, playerZ + SCAN_RANGE);
        int minY = Math.max(MIN_Y, playerY - SCAN_RANGE);
        int maxY = Math.min(mc.level.getMaxY() - 1, playerY + SCAN_RANGE);

        List<RenderUtils.ColoredBox> list = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                LevelChunk chunk = mc.level.getChunkSource().getChunk(cx, cz, false);
                if (chunk == null) continue;

                LevelChunkSection[] sections = chunk.getSections();

                int startX = Math.max(minX, cx << 4);
                int endX = Math.min(maxX, (cx << 4) + 15);
                int startZ = Math.max(minZ, cz << 4);
                int endZ = Math.min(maxZ, (cz << 4) + 15);

                for (int y = minY; y <= maxY; y++) {
                    int sectionIndex = chunk.getSectionIndex(y);
                    if (sectionIndex < 0 || sectionIndex >= sections.length) continue;

                    LevelChunkSection section = sections[sectionIndex];
                    if (section == null || section.hasOnlyAir()) continue;

                    for (int x = startX; x <= endX; x++) {
                        for (int z = startZ; z <= endZ; z++) {
                            if (section.getBlockState(x & 15, y & 15, z & 15).is(Blocks.LAVA)) {
                                cursor.set(x, y, z);
                                list.add(new RenderUtils.ColoredBox(new AABB(cursor), LAVA_COLOR));
                            }
                        }
                    }
                }
            }
        }
        this.targets = list;
    }

    private boolean isCrystalHollows() {
        Map<String, String> location = Utils.getCurrentLocation();
        return Objects.equals(location.get("Area"), "Crystal Hollows");
    }

    private void clearTargets() {
        targets = List.of();
        lastScanTime = 0L;
        if (boxBuffer != null) {
            boxBuffer.close();
            boxBuffer = null;
        }
    }
}