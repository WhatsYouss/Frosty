package xyz.whatsyouss.frosty.modules.impl.render;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.phys.AABB;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.events.impl.ChunkOcclusionEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.BufferSource;
import xyz.whatsyouss.frosty.utility.RenderUtils;
import xyz.whatsyouss.frosty.utility.Utils;

import java.awt.Color;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class Xray extends Module {
    private static final long SCAN_INTERVAL_MS = 1_500L;
    private static final long REBUILD_DEBOUNCE_MS = 200L;
    private static final int SCAN_BLOCK_BUDGET = 65_536;

    public SliderSetting distance;
    public SliderSetting opacity;
    public ButtonSetting ore;
    public ButtonSetting glass;
    public ButtonSetting esp;

    private long lastScan;
    private long rebuildAt;
    private int lastRange = -1;
    private String observedRenderSignature = "";
    private String appliedRenderSignature = "";
    private String lastTargetSignature = "";
    private WeakReference<ClientLevel> scannedLevel = new WeakReference<>(null);
    private List<RenderUtils.ColoredBox> oreTargets = List.of();
    private List<RenderUtils.ColoredBox> glassTargets = List.of();
    private ScanTask scanTask;
    private boolean lastEspEnabled;
    private BufferSource boxBuffer;

    public Xray() {
        super("Xray", "矿物透视", category.Render);

        this.registerSetting(distance = new SliderSetting("Distance", " blocks", 64, 16, 256, 4, "范围"));
        this.registerSetting(opacity = new SliderSetting("Opacity", "%", 10, 0, 99, 1, "透明度"));
        this.registerSetting(ore = new ButtonSetting("Ore", "矿石", true));
        this.registerSetting(glass = new ButtonSetting("Glass", "玻璃", true));
        this.registerSetting(esp = new ButtonSetting("ESP", "方块透视", true));
    }

    @Override
    public void onEnable() {
        lastScan = 0L;
        rebuildAt = 0L;
        lastRange = -1;
        observedRenderSignature = settingSignature();
        appliedRenderSignature = observedRenderSignature;
        lastTargetSignature = targetSettingSignature();
        scannedLevel.clear();
        scanTask = null;
        lastEspEnabled = esp.isToggled();
        reloadWorldRenderer();
    }

    @Override
    public void onDisable() {
        oreTargets = List.of();
        glassTargets = List.of();
        scannedLevel.clear();
        scanTask = null;
        closeBoxBuffer();
        reloadWorldRenderer();
    }

    @Override
    public void onUpdate() {
        if (esp.isToggled() != lastEspEnabled) {
            lastEspEnabled = esp.isToggled();
            oreTargets = List.of();
            glassTargets = List.of();
            scanTask = null;
            lastScan = 0L;
            lastRange = -1;
            if (!lastEspEnabled) closeBoxBuffer();
        }

        String signature = settingSignature();
        String targetSignature = targetSettingSignature();
        long now = System.currentTimeMillis();
        if (!targetSignature.equals(lastTargetSignature)) {
            lastTargetSignature = targetSignature;
            oreTargets = List.of();
            glassTargets = List.of();
            scanTask = null;
            lastScan = 0L;
        }
        if (!signature.equals(observedRenderSignature)) {
            observedRenderSignature = signature;
            rebuildAt = signature.equals(appliedRenderSignature) ? 0L : now + REBUILD_DEBOUNCE_MS;
        }
        if (rebuildAt != 0L && now >= rebuildAt) {
            appliedRenderSignature = observedRenderSignature;
            rebuildAt = 0L;
            reloadWorldRenderer();
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (!esp.isToggled() || !Utils.nullCheck()) return;

        if (scannedLevel.get() != mc.level) {
            scannedLevel = new WeakReference<>(mc.level);
            oreTargets = List.of();
            glassTargets = List.of();
            scanTask = null;
            lastScan = 0L;
            lastRange = -1;
        }

        int range = (int) Math.round(distance.getInput());
        long now = System.currentTimeMillis();
        if (scanTask != null && scanTask.range != range) scanTask = null;
        if (scanTask == null && (now - lastScan >= SCAN_INTERVAL_MS || range != lastRange)) {
            scanTask = new ScanTask(mc.level, mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ(), range);
        }
        if (scanTask != null && scanTask.step(mc.level, SCAN_BLOCK_BUDGET)) {
            oreTargets = scanTask.oreResults();
            glassTargets = scanTask.glassResults();
            lastScan = now;
            lastRange = scanTask.range;
            scanTask = null;
        }

        if (boxBuffer == null) boxBuffer = BufferSource.reusable();
        if (ore.isToggled()) {
            RenderUtils.drawBoxes(boxBuffer, event.getMatrix(), oreTargets, 0.0f,
                    1.0f, 3.0f, false, true, false);
        }
        if (glass.isToggled()) {
            RenderUtils.drawBoxes(boxBuffer, event.getMatrix(), glassTargets, 0.35f,
                    0.0f, 1.0f, true, false, false);
        }
    }

    public static boolean isActive() {
        return ModuleManager.xray != null && ModuleManager.xray.isEnabled();
    }

    public static boolean shouldShowBlock(BlockState state) {
        return state != null && isActive() && ModuleManager.xray.isVisible(state.getBlock());
    }

    public static boolean shouldTransparentBlock(BlockState state) {
        return state != null && isActive() && !shouldShowBlock(state);
    }

    public static int transparentAlphaFor(BlockState state) {
        if (!shouldTransparentBlock(state)) return -1;
        Xray xray = ModuleManager.xray;
        return xray.opacityAlpha();
    }

    public static boolean isOpacityMode() {
        return isActive() && ModuleManager.xray.opacityAlpha() > 0 && ModuleManager.xray.opacityAlpha() < 255;
    }

    public static int opacityColorMask() {
        return ModuleManager.xray.opacityAlpha() << 24 | 0x00FFFFFF;
    }

    public static float opacityFloat() {
        return ModuleManager.xray.opacityAlpha() / 255.0f;
    }

    public Boolean shouldDrawSide(BlockState state, BlockPos pos) {
        if (!isEnabled()) return null;

        boolean visible = isVisible(state.getBlock());
        if (!visible && isOpacityMode()) return null;
        return visible;
    }

    public boolean shouldHideBlockEntity(BlockEntityRenderState state) {
        return isEnabled() && mc.level != null && !isVisible(mc.level.getBlockState(state.blockPos).getBlock());
    }

    public boolean isVisible(Block block) {
        return ore.isToggled() && oreFor(block) != null || glass.isToggled() && glassColorFor(block) != null;
    }

    @EventHandler
    public void onChunkOcclusion(ChunkOcclusionEvent event) {
        if (isEnabled()) event.cancel();
    }

    private Ore oreFor(Block block) {
        if (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE) return Ore.COAL;
        if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) return Ore.REDSTONE;
        if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) return Ore.IRON;
        if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE || block == Blocks.NETHER_GOLD_ORE) return Ore.GOLD;
        if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) return Ore.DIAMOND;
        if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) return Ore.EMERALD;
        if (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE) return Ore.LAPIS;
        if (block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE) return Ore.COPPER;
        if (block == Blocks.ANCIENT_DEBRIS) return Ore.DEBRIS;
        if (block == Blocks.NETHER_QUARTZ_ORE) return Ore.QUARTZ;
        return null;
    }

    private Color glassColorFor(Block block) {
        String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
        return switch (path.replace("_stained_glass_pane", "").replace("_stained_glass", "")) {
            case "white" -> new Color(0xF9FFFE);
            case "orange" -> new Color(0xF9801D);
            case "magenta" -> new Color(0xC74EBD);
            case "light_blue" -> new Color(0x3AB3DA);
            case "yellow" -> new Color(0xFED83D);
            case "lime" -> new Color(0x80C71F);
            case "pink" -> new Color(0xF38BAA);
            case "gray" -> new Color(0x474F52);
            case "light_gray" -> new Color(0x9D9D97);
            case "cyan" -> new Color(0x169C9C);
            case "purple" -> new Color(0x8932B8);
            case "blue" -> new Color(0x3C44AA);
            case "brown" -> new Color(0x835432);
            case "green" -> new Color(0x5E7C16);
            case "red" -> new Color(0xB02E26);
            case "black" -> new Color(0x1D1D21);
            default -> null;
        };
    }

    private int opacityAlpha() {
        return Math.clamp((int) Math.round(opacity.getInput() * 2.55), 0, 255);
    }
    private String settingSignature() { return opacityAlpha() + ":" + targetSettingSignature(); }
    private String targetSettingSignature() { return ore.isToggled() + ":" + glass.isToggled(); }

    private void reloadWorldRenderer() {
        if (mc.level != null) mc.levelExtractor.allChanged();
    }

    private void closeBoxBuffer() {
        if (boxBuffer != null) {
            boxBuffer.close();
            boxBuffer = null;
        }
    }

    private final class ScanTask {
        private final int centerX, centerY, centerZ, range, rangeSq, minZ, maxZ, worldMinY, worldMaxY;
        private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        private final ArrayList<RenderUtils.ColoredBox> foundOres = new ArrayList<>();
        private final ArrayList<RenderUtils.ColoredBox> foundGlass = new ArrayList<>();
        private int nextX, nextZ, columnX, columnZ, nextY, columnMaxY;
        private boolean columnReady;

        private ScanTask(ClientLevel level, int centerX, int centerY, int centerZ, int range) {
            this.centerX = centerX; this.centerY = centerY; this.centerZ = centerZ; this.range = range; this.rangeSq = range * range;
            nextX = centerX - range; minZ = centerZ - range; maxZ = centerZ + range; nextZ = minZ;
            worldMinY = level.getMinY(); worldMaxY = level.getMaxY() - 1;
        }

        private boolean step(ClientLevel level, int budget) {
            int checked = 0;
            while (checked < budget) {
                if (!columnReady && !prepareNextColumn(level)) return true;
                cursor.set(columnX, nextY, columnZ);
                Block block = level.getBlockState(cursor).getBlock();
                AABB box = new AABB(columnX, nextY, columnZ, columnX + 1.0, nextY + 1.0, columnZ + 1.0);
                Ore oreType = oreFor(block);
                if (oreType != null) foundOres.add(new RenderUtils.ColoredBox(box, oreType.color));
                Color glassColor = glassColorFor(block);
                if (glassColor != null) foundGlass.add(new RenderUtils.ColoredBox(box, glassColor));
                checked++;
                if (++nextY > columnMaxY) columnReady = false;
            }
            return false;
        }

        private boolean prepareNextColumn(ClientLevel level) {
            int maxX = centerX + range;
            while (nextX <= maxX) {
                while (nextZ <= maxZ) {
                    int z = nextZ++; int dx = nextX - centerX; int dz = z - centerZ;
                    int horizontalSq = dx * dx + dz * dz;
                    if (horizontalSq > rangeSq || !level.hasChunk(nextX >> 4, z >> 4)) continue;
                    int verticalRange = (int) Math.sqrt(rangeSq - horizontalSq);
                    int minY = Math.max(worldMinY, centerY - verticalRange);
                    int maxY = Math.min(worldMaxY, centerY + verticalRange);
                    if (minY > maxY) continue;
                    columnX = nextX; columnZ = z; nextY = minY; columnMaxY = maxY; columnReady = true;
                    return true;
                }
                nextX++; nextZ = minZ;
            }
            return false;
        }

        private List<RenderUtils.ColoredBox> oreResults() { return List.copyOf(foundOres); }
        private List<RenderUtils.ColoredBox> glassResults() { return List.copyOf(foundGlass); }
    }

    private enum Ore {
        COAL(new Color(0x3B3B3B)), REDSTONE(new Color(0xFF3131)), IRON(new Color(0xD8B77E)),
        GOLD(new Color(0xFFE45D)), DIAMOND(new Color(0x42E8FF)), EMERALD(new Color(0x37F06D)),
        LAPIS(new Color(0x466CFF)), COPPER(new Color(0xF09254)), DEBRIS(new Color(0x8E5A46)), QUARTZ(new Color(0xECE6D4));
        private final Color color;
        Ore(Color color) { this.color = color; }
    }
}
