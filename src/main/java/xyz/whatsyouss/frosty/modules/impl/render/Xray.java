package xyz.whatsyouss.frosty.modules.impl.render;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.phys.AABB;
import xyz.whatsyouss.frosty.events.impl.ChunkOcclusionEvent;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.RenderUtils;
import xyz.whatsyouss.frosty.utility.Utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class Xray extends Module {
    private static final long SCAN_INTERVAL_MS = 1_500L;
    private static final int SCAN_BLOCK_BUDGET = 65_536;
    public final SliderSetting distance, opacity;
    public final ButtonSetting ore, glass, esp;
    private long lastScan, rebuildAt;
    private int lastRange = -1;
    private String observedRenderSignature = "", appliedRenderSignature = "", lastTargetSignature = "";
    private ClientLevel scannedLevel;
    private List<RenderUtils.ColoredBox> oreTargets = List.of(), glassTargets = List.of();
    private ScanTask scanTask;
    private boolean lastEspEnabled;

    public Xray() {
        super("Xray", "矿物透视", category.Render);

        this.registerSetting(distance = new SliderSetting("Distance", " blocks", 64, 16, 256, 4, "范围"));
        this.registerSetting(opacity = new SliderSetting("Opacity", "%", 10, 0, 99, 1, "透明度"));
        this.registerSetting(ore = new ButtonSetting("Ore", "矿石", true));
        this.registerSetting(glass = new ButtonSetting("Glass", "玻璃", true));
        this.registerSetting(esp = new ButtonSetting("ESP", "方块透视", true));
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
        return shouldTransparentBlock(state) ? ModuleManager.xray.opacityAlpha() : -1;
    }

    public static boolean isOpacityMode() {
        return isActive() && ModuleManager.xray.opacityAlpha() > 0 && ModuleManager.xray.opacityAlpha() < 255;
    }

    public static int opacityColorMask() {
        return ModuleManager.xray.opacityAlpha() << 24 | 0xFFFFFF;
    }

    public static float opacityFloat() {
        return ModuleManager.xray.opacityAlpha() / 255f;
    }

    @Override
    public void onEnable() {
        lastScan = rebuildAt = 0;
        lastRange = -1;
        observedRenderSignature = appliedRenderSignature = settingSignature();
        lastTargetSignature = targetSignature();
        scannedLevel = null;
        scanTask = null;
        lastEspEnabled = esp.isToggled();
        reload();
    }

    @Override
    public void onDisable() {
        oreTargets = glassTargets = List.of();
        scanTask = null;
        scannedLevel = null;
        reload();
    }

    @Override
    public void onUpdate() {
        if (esp.isToggled() != lastEspEnabled) {
            lastEspEnabled = esp.isToggled();
            oreTargets = glassTargets = List.of();
            scanTask = null;
            lastScan = 0;
            lastRange = -1;
        }
        String signature = settingSignature(), targets = targetSignature();
        long now = System.currentTimeMillis();
        if (!targets.equals(lastTargetSignature)) {
            lastTargetSignature = targets;
            oreTargets = glassTargets = List.of();
            scanTask = null;
            lastScan = 0;
        }
        if (!signature.equals(observedRenderSignature)) {
            observedRenderSignature = signature;
            rebuildAt = signature.equals(appliedRenderSignature) ? 0 : now + 200;
        }
        if (rebuildAt != 0 && now >= rebuildAt) {
            appliedRenderSignature = observedRenderSignature;
            rebuildAt = 0;
            reload();
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (!esp.isToggled() || !Utils.nullCheck()) return;
        if (scannedLevel != mc.level) {
            scannedLevel = mc.level;
            oreTargets = glassTargets = List.of();
            scanTask = null;
            lastScan = 0;
            lastRange = -1;
        }
        int range = (int) Math.round(distance.getInput());
        long now = System.currentTimeMillis();
        if (scanTask != null && scanTask.range != range) scanTask = null;
        if (scanTask == null && (now - lastScan >= SCAN_INTERVAL_MS || range != lastRange))
            scanTask = new ScanTask(mc.level, mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ(), range);
        if (scanTask != null && scanTask.step(SCAN_BLOCK_BUDGET)) {
            oreTargets = scanTask.ores();
            glassTargets = scanTask.glass();
            lastScan = now;
            lastRange = range;
            scanTask = null;
        }
        if (ore.isToggled()) RenderUtils.drawBoxes(event.getMatrix(), oreTargets, 0, 1, 3, false, true, false);
        if (glass.isToggled()) RenderUtils.drawBoxes(event.getMatrix(), glassTargets, .35f, 0, 1, true, false, false);
    }

    public Boolean shouldDrawSide(BlockState state, BlockPos pos) {
        if (!isEnabled()) return null;
        boolean visible = isVisible(state.getBlock());
        return !visible && isOpacityMode() ? null : visible;
    }

    public boolean shouldHideBlockEntity(BlockEntityRenderState state) {
        return isEnabled() && mc.level != null && !isVisible(mc.level.getBlockState(state.blockPos).getBlock());
    }

    public boolean isVisible(Block block) {
        return ore.isToggled() && oreFor(block) != null || glass.isToggled() && glassFor(block) != null;
    }

    @EventHandler
    public void onChunkOcclusion(ChunkOcclusionEvent event) {
        if (isEnabled()) event.cancel();
    }

    private void reload() {
        if (mc.level != null) mc.levelRenderer.allChanged();
    }

    private int opacityAlpha() {
        return Math.clamp((int) Math.round(opacity.getInput() * 2.55), 0, 255);
    }

    private String settingSignature() {
        return opacityAlpha() + ":" + targetSignature();
    }

    private String targetSignature() {
        return ore.isToggled() + ":" + glass.isToggled();
    }

    private Ore oreFor(Block b) {
        if (b == Blocks.COAL_ORE || b == Blocks.DEEPSLATE_COAL_ORE) return Ore.COAL;
        if (b == Blocks.REDSTONE_ORE || b == Blocks.DEEPSLATE_REDSTONE_ORE) return Ore.REDSTONE;
        if (b == Blocks.IRON_ORE || b == Blocks.DEEPSLATE_IRON_ORE) return Ore.IRON;
        if (b == Blocks.GOLD_ORE || b == Blocks.DEEPSLATE_GOLD_ORE || b == Blocks.NETHER_GOLD_ORE) return Ore.GOLD;
        if (b == Blocks.DIAMOND_ORE || b == Blocks.DEEPSLATE_DIAMOND_ORE) return Ore.DIAMOND;
        if (b == Blocks.EMERALD_ORE || b == Blocks.DEEPSLATE_EMERALD_ORE) return Ore.EMERALD;
        if (b == Blocks.LAPIS_ORE || b == Blocks.DEEPSLATE_LAPIS_ORE) return Ore.LAPIS;
        if (b == Blocks.COPPER_ORE || b == Blocks.DEEPSLATE_COPPER_ORE) return Ore.COPPER;
        if (b == Blocks.ANCIENT_DEBRIS) return Ore.DEBRIS;
        return b == Blocks.NETHER_QUARTZ_ORE ? Ore.QUARTZ : null;
    }

    private Color glassFor(Block b) {
        return switch (BuiltInRegistries.BLOCK.getKey(b).getPath().replace("_stained_glass_pane", "").replace("_stained_glass", "")) {
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

    private enum Ore {
        COAL(0x3B3B3B), REDSTONE(0xFF3131), IRON(0xD8B77E), GOLD(0xFFE45D), DIAMOND(0x42E8FF), EMERALD(0x37F06D), LAPIS(0x466CFF), COPPER(0xF09254), DEBRIS(0x8E5A46), QUARTZ(0xECE6D4);
        final Color color;

        Ore(int color) {
            this.color = new Color(color);
        }
    }

    private final class ScanTask {
        final int x, y, z, range, rangeSq, minZ, maxZ, minY, maxY;
        final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        final ArrayList<RenderUtils.ColoredBox> ores = new ArrayList<>(), glasses = new ArrayList<>();
        int nextX, nextZ, nextY, columnX, columnZ, columnMaxY;
        boolean columnReady;

        ScanTask(ClientLevel level, int x, int y, int z, int range) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.range = range;
            rangeSq = range * range;
            nextX = x - range;
            minZ = z - range;
            maxZ = z + range;
            nextZ = minZ;
            minY = level.getMinY();
            maxY = level.getMaxY() - 1;
        }

        boolean step(int budget) {
            for (int checked = 0; checked < budget; checked++) {
                if (!columnReady && !column()) return true;
                cursor.set(columnX, nextY, columnZ);
                Block b = mc.level.getBlockState(cursor).getBlock();
                AABB box = new AABB(columnX, nextY, columnZ, columnX + 1d, nextY + 1d, columnZ + 1d);
                Ore ore = oreFor(b);
                if (ore != null) ores.add(new RenderUtils.ColoredBox(box, ore.color));
                Color glass = glassFor(b);
                if (glass != null) glasses.add(new RenderUtils.ColoredBox(box, glass));
                if (++nextY > columnMaxY) columnReady = false;
            }
            return false;
        }

        boolean column() {
            for (int maxX = x + range; nextX <= maxX; nextX++, nextZ = minZ)
                for (; nextZ <= maxZ; nextZ++) {
                    int cz = nextZ++, dx = nextX - x, dz = cz - z, hs = dx * dx + dz * dz;
                    if (hs > rangeSq || !mc.level.hasChunk(nextX >> 4, cz >> 4)) continue;
                    int vertical = (int) Math.sqrt(rangeSq - hs);
                    nextY = Math.max(minY, y - vertical);
                    columnMaxY = Math.min(maxY, y + vertical);
                    if (nextY <= columnMaxY) {
                        columnX = nextX;
                        columnZ = cz;
                        columnReady = true;
                        return true;
                    }
                }
            return false;
        }

        List<RenderUtils.ColoredBox> ores() {
            return List.copyOf(ores);
        }

        List<RenderUtils.ColoredBox> glass() {
            return List.copyOf(glasses);
        }
    }
}
