package xyz.whatsyouss.frosty.modules.impl.farming;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.BlockUtils;
import xyz.whatsyouss.frosty.utility.MathUtils;
import xyz.whatsyouss.frosty.utility.RenderUtils;
import xyz.whatsyouss.frosty.utility.Utils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GardenCleaner extends Module {

    private SelectSetting mode;
    private SliderSetting bps;

    private String[] modes = new String[]{"Normal", "Instant"};

    private final int radius = 5;
    private final List<Item> PICKAXES = List.of(Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.IRON_PICKAXE, Items.GOLDEN_PICKAXE, Items.DIAMOND_PICKAXE);
    private final List<Block> WOOD_LOGS = List.of(Blocks.OAK_LOG, Blocks.BIRCH_LOG, Blocks.OAK_WOOD, Blocks.BIRCH_WOOD);
    private final List<Block> DIGGABLES = List.of(Blocks.GRASS_BLOCK, Blocks.DIRT);
    private final List<Block> STONE_TARGETS = List.of(Blocks.STONE);
    private final List<Block> OTHER_TARGETS = List.of(Blocks.OAK_LEAVES, Blocks.BIRCH_LEAVES, Blocks.SHORT_GRASS, Blocks.TALL_GRASS, Blocks.DANDELION, Blocks.POPPY, Blocks.AZURE_BLUET);
    private BlockPos currentTarget = null;
    private int instantTickCounter = 0;
    private int requiredTicks = 0;

    public GardenCleaner() {
        super("GardenCleaner", category.Farming);

        this.registerSetting(mode = new SelectSetting("Mode", 1, modes));
        this.registerSetting(bps = new SliderSetting("BPS", 10 ,1, 15, 1));
    }

    @Override
    public void guiUpdate() {
        this.bps.setVisibilityCondition(() -> mode.getValue() == 1);
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

        Item handItem = mc.player.getMainHandItem().getItem();

        BlockPos playerPos = mc.player.blockPosition();
        for (int x = playerPos.getX() - radius; x <= playerPos.getX() + radius; x++) {
            for (int y = playerPos.getY() - radius; y <= playerPos.getY() + radius; y++) {
                for (int z = playerPos.getZ() - radius; z <= playerPos.getZ() + radius; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = mc.level.getBlockState(pos);
                    Block block = state.getBlock();

                    if (state.isAir()) continue;

                    if (handItem instanceof AxeItem) {
                        if (WOOD_LOGS.contains(block)) targets.add(pos);
                    }
                    else if (handItem instanceof ShovelItem) {
                        if (DIGGABLES.contains(block) && pos.getY() > 70) {
                            targets.add(pos);
                        }
                    }
                    else if (PICKAXES.contains(handItem)) {
                        if (STONE_TARGETS.contains(block)) {
                            targets.add(pos);
                        }
                    }
                    else if (handItem instanceof HoeItem) {
                        if (OTHER_TARGETS.contains(block)) targets.add(pos);
                    }
                }
            }
        }
        return targets;
    }

    private void handleInstantMode() {
        instantTickCounter++;

        int currentRequiredTicks = mode.getValue() == 1 ? Utils.getAPSToTicks(bps, 20.0) : (int) MathUtils.randomizeDouble(6.0D, 7.0D);

        if (currentRequiredTicks == -1) return;

        if (instantTickCounter >= currentRequiredTicks) {
            instantTickCounter = 0;

            if (!Utils.stripColor(Utils.getScoreboardSidebarLines().toString().toLowerCase()).contains("cleanup")) {
                currentTarget = null;
                return;
            }

            List<BlockPos> validBlocks = findTargetBlocks();
            if (validBlocks.isEmpty()) {
                currentTarget = null;
                return;
            }

            BlockPos target = getClosestBlock(validBlocks);
            if (target != null && isInRange(target)) {
                if (mode.getValue() == 1) {
                    interactBlock(target);
                } else {
                    interactBlockPacket(target);
                }
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

    private void interactBlockPacket(BlockPos pos) {
        if (!Utils.nullCheck() || mc.level.getBlockState(pos).isAir()) {
            return;
        }

        if (currentTarget != pos) {
            mc.player.connection.send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, pos, BlockUtils.getDirection(pos)));
        }

        mc.player.connection.send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, BlockUtils.getDirection(pos)));
        mc.player.swing(InteractionHand.MAIN_HAND);
        mc.player.connection.send(new ServerboundPlayerActionPacket(
            ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, BlockUtils.getDirection(pos)));

        currentTarget = pos;
    }
}