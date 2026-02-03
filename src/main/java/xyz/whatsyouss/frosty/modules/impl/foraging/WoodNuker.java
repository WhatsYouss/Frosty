package xyz.whatsyouss.frosty.modules.impl.foraging;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.events.impl.SettingUpdateEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.utility.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class WoodNuker extends Module {

    private SelectSetting type;
    private ButtonSetting aim, silent;

    private String[] types = new String[]{"Oak", "Spruce", "Birch", "Dark Oak", "Acacia", "Jungle", "Fig", "Mangrove"};

    private final int radius = 5;
    private List<Block> targetBlocks = new ArrayList<>();
    private int tickCounter = 0;
    private final int breakDelay = 2;
    private BlockPos currentTarget = null;
    private long lastBreakTime;
    private BlockPos lastTargetPos;
    private boolean packetSent;

    public WoodNuker() {
        super("WoodNuker", category.Foraging);

        this.registerSetting(type = new SelectSetting("Type", 0, types));
        this.registerSetting(aim = new ButtonSetting("Aim", false));
        this.registerSetting(silent = new ButtonSetting("Silent", false));
    }

    @Override
    public void guiUpdate() {
        this.silent.setVisibilityCondition(() -> aim.isToggled());
    }

    @Override
    public void onDisable() {
        if (lastTargetPos != null) {
            mc.player.networkHandler.sendPacket(
                    new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK,
                            lastTargetPos, BlockUtils.getDirection(lastTargetPos)));
        }
        Rotations.cancelRotate(this);
        packetSent = false;
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }

        if (targetBlocks.isEmpty()) {
            getTargetBlocks();
        }

        if (targetBlocks.size() >= 3) {
            targetBlocks.clear();
        }

        tickCounter++;
        if (tickCounter < breakDelay) return;
        tickCounter = 0;

        // Check if current target is still valid
        if (currentTarget != null) {
            BlockState state = mc.world.getBlockState(currentTarget);
            boolean isStillTarget = (targetBlocks.contains(state.getBlock())) &&
                    isInRange(currentTarget) &&
                    !state.isAir();

            if (!isStillTarget) {
                if (packetSent) {
                    Direction dir = BlockUtils.getDirection(currentTarget);
                    mc.player.networkHandler.sendPacket(
                            new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
                                    currentTarget, dir));
                    packetSent = false;
                }
                currentTarget = null;
            }
        }

        if (currentTarget == null) {
            List<BlockPos> woodBlocks = findWoodBlocks();
            if (!woodBlocks.isEmpty()) {
                currentTarget = getClosestBlock(woodBlocks);
            }
        }

        if (currentTarget != null) {
            breakBlock(currentTarget);
        } else {
            Rotations.cancelRotate(this);
        }
    }
    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (currentTarget != null) {
            RenderUtils.drawBox(event.getMatrix(), currentTarget, Color.CYAN, 2f);
        }
    }

    private BlockPos getClosestBlock(List<BlockPos> blocks) {
        BlockPos closest = null;
        double minDistance = Double.MAX_VALUE;
        Vec3d playerPos = mc.player.getEntityPos();

        for (BlockPos pos : blocks) {
            double distance = playerPos.distanceTo(Vec3d.ofCenter(pos));
            if (distance < minDistance) {
                minDistance = distance;
                closest = pos;
            }
        }
        return closest;
    }

    private List<BlockPos> findWoodBlocks() {
        List<BlockPos> woodBlocks = new ArrayList<>();
        if (!Utils.nullCheck()) {
            return woodBlocks;
        }

        BlockPos playerPos = mc.player.getBlockPos();

        Vec3d minPos = Vec3d.of(playerPos.add(-radius, -radius, -radius));
        Vec3d maxPos = Vec3d.of(playerPos.add(radius, radius, radius));

        Box area = new Box(minPos, maxPos);

        for (int x = (int) Math.floor(area.minX); x <= Math.ceil(area.maxX); x++) {
            for (int y = (int) Math.floor(area.minY); y <= Math.ceil(area.maxY); y++) {
                for (int z = (int) Math.floor(area.minZ); z <= Math.ceil(area.maxZ); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    Block block = mc.world.getBlockState(pos).getBlock();
                    if (targetBlocks.contains(block)) {
                        woodBlocks.add(pos);
                    }
                }
            }
        }
        return woodBlocks;
    }

    private boolean isInRange(BlockPos pos) {
        return mc.player.getEntityPos().distanceTo(Vec3d.ofCenter(pos)) <= 4.5;
    }

    private void breakBlock(BlockPos pos) {
        if (mc.interactionManager == null || mc.player == null) return;

        if (System.currentTimeMillis() - lastBreakTime < 50) {
            return;
        }

        if (lastTargetPos != null && !lastTargetPos.equals(pos)) {
            packetSent = false;
            mc.player.networkHandler.sendPacket(
                    new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK,
                            lastTargetPos, BlockUtils.getDirection(lastTargetPos)));
        }
        lastTargetPos = pos;

        float[] a = RotationUtils.getYawPitchTo(mc.player.getEyePos(), Vec3d.of(pos));

        if (aim.isToggled()) {
            if (silent.isToggled()) {
                Rotations.setRotate(this, a[0], a[1], 4);
            } else {
                RotationUtils.aimByPos(Vec3d.of(pos));
            }
        }

        if (isGalateaWood(mc.world.getBlockState(pos))) {
            if (!packetSent) {
                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
                        pos, BlockUtils.getDirection(pos)));
                packetSent = true;
            }
            mc.player.swingHand(Hand.MAIN_HAND);
        } else {
            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
                    pos, BlockUtils.getDirection(pos)));
            mc.player.swingHand(Hand.MAIN_HAND);
            mc.player.networkHandler.sendPacket(
                    new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, BlockUtils.getDirection(pos)));
        }

        lastBreakTime = System.currentTimeMillis();
    }

    @EventHandler
    public void onSettingUpdate(SettingUpdateEvent event) {
        if (!targetBlocks.isEmpty()) {
            targetBlocks.clear();
        }
        getTargetBlocks();
    }

    private boolean isGalateaWood(BlockState state) {
        return state.getBlock() == Blocks.STRIPPED_SPRUCE_LOG ||
                state.getBlock() == Blocks.STRIPPED_SPRUCE_WOOD ||
                state.getBlock() == Blocks.MANGROVE_LOG ||
                state.getBlock() == Blocks.MANGROVE_WOOD;
    }

    private void getTargetBlocks() {
        switch ((int) type.getValue()) {
            case 0:
                targetBlocks.add(Blocks.OAK_LOG);
                targetBlocks.add(Blocks.OAK_WOOD);
                break;
            case 1:
                targetBlocks.add(Blocks.SPRUCE_LOG);
                targetBlocks.add(Blocks.SPRUCE_WOOD);
                break;
            case 2:
                targetBlocks.add(Blocks.BIRCH_LOG);
                targetBlocks.add(Blocks.BIRCH_WOOD);
                break;
            case 3:
                targetBlocks.add(Blocks.DARK_OAK_LOG);
                targetBlocks.add(Blocks.DARK_OAK_WOOD);
                break;
            case 4:
                targetBlocks.add(Blocks.ACACIA_LOG);
                targetBlocks.add(Blocks.ACACIA_WOOD);
                break;
            case 5:
                targetBlocks.add(Blocks.JUNGLE_LOG);
                targetBlocks.add(Blocks.JUNGLE_WOOD);
                break;
            case 6:
                targetBlocks.add(Blocks.STRIPPED_SPRUCE_LOG);
                targetBlocks.add(Blocks.STRIPPED_SPRUCE_WOOD);
                break;
            case 7:
                targetBlocks.add(Blocks.MANGROVE_LOG);
                targetBlocks.add(Blocks.MANGROVE_WOOD);
                break;
        }
    }
}