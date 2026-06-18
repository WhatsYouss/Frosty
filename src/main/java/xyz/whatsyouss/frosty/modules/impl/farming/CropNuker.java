package xyz.whatsyouss.frosty.modules.impl.farming;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.BlockUtils;
import xyz.whatsyouss.frosty.utility.RenderUtils;
import xyz.whatsyouss.frosty.utility.Utils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CropNuker extends Module {

    private SliderSetting bps;
    private ButtonSetting ignoreBaby;

    private final List<Block> CROPS = List.of(Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.SUGAR_CANE, Blocks.MELON, Blocks.PUMPKIN, Blocks.NETHER_WART, Blocks.COCOA);
    private final int radius = 5;
    private BlockPos currentTarget = null;
    private int instantTickCounter = 0;

    public CropNuker() {
        super("CropNuker", category.Farming);

        this.registerSetting(bps = new SliderSetting("BPS", 20 ,3, 20, 1));
        this.registerSetting(ignoreBaby = new ButtonSetting("Ignore baby", true));
    }

    @Override
    public void onUpdate() {
        if (!Utils.nullCheck()) {
            return;
        }

        if (currentTarget != null && mc.level.getBlockState(currentTarget).isAir()) {
            currentTarget = null;
        }

        handleInstantMode();
    }

    private List<BlockPos> findTargetBlocks() {
        List<BlockPos> targets = new ArrayList<>();
        if (!Utils.nullCheck()) {
            return targets;
        }
        BlockPos playerPos = mc.player.blockPosition();
        for (int x = playerPos.getX() - radius; x <= playerPos.getX() + radius; x++) {
            for (int y = playerPos.getY() - radius; y <= playerPos.getY() + radius; y++) {
                for (int z = playerPos.getZ() - radius; z <= playerPos.getZ() + radius; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = mc.level.getBlockState(pos);

                    if (state.isAir()) continue;
                    if (ignoreBaby.isToggled() && state.getBlock() instanceof CropBlock crop && !crop.isMaxAge(state)) continue;

                    if (CROPS.contains(state.getBlock())) {
                        targets.add(pos);
                    }
                }
            }
        }
        return targets;
    }

    private void handleInstantMode() {
        instantTickCounter++;

        int currentRequiredTicks = Utils.getAPSToTicks(bps, 20.0);

        if (currentRequiredTicks == -1) return;

        if (instantTickCounter >= currentRequiredTicks) {
            instantTickCounter = 0;

            List<BlockPos> validBlocks = findTargetBlocks();
            if (validBlocks.isEmpty()) {
                currentTarget = null;
                return;
            }

            BlockPos target = getClosestBlock(validBlocks);
            if (target != null && isInRange(target)) {
                interactBlock(target);
            }
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (currentTarget != null) {
            RenderUtils.drawBox(event.getMatrix(), currentTarget, Color.CYAN, 2f, false);
        }
    }

    private BlockPos getClosestBlock(List<BlockPos> blocks) {
        BlockPos closest = null;
        double minDistance = Double.MAX_VALUE;
        Vec3 playerPos = mc.player.position();

        for (BlockPos pos : blocks) {
            double distance = playerPos.distanceTo(Vec3.atCenterOf(pos));
            if (distance < minDistance) {
                minDistance = distance;
                closest = pos;
            }
        }
        return closest;
    }

    private boolean isInRange(BlockPos pos) {
        return mc.player.position().distanceTo(Vec3.atCenterOf(pos)) <= 4.5;
    }

    private void interactBlock(BlockPos pos) {
        if (!Utils.nullCheck() || mc.level.getBlockState(pos).isAir()) {
            return;
        }

        mc.gameMode.startDestroyBlock(pos, BlockUtils.getDirection(pos));
        mc.player.swing(InteractionHand.MAIN_HAND);

        currentTarget = pos;
    }
}
