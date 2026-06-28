package xyz.whatsyouss.frosty.modules.impl.foraging;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
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

    private Block lastTarget;
    private boolean startPacketSent = false;

    public WoodNuker() {
        super("WoodNuker", "木头光环",category.Foraging);

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
        resetTarget();
        Rotations.cancelRotate(this);
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

        if (currentTarget != null) {
            BlockState state = mc.level.getBlockState(currentTarget);
            boolean isStillTarget = (targetBlocks.contains(state.getBlock())) &&
                    isInRange(currentTarget) &&
                    !state.isAir();

            if (!isStillTarget) {
                resetTarget();
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

    private List<BlockPos> findWoodBlocks() {
        List<BlockPos> woodBlocks = new ArrayList<>();
        if (!Utils.nullCheck()) {
            return woodBlocks;
        }

        BlockPos playerPos = mc.player.blockPosition();

        Vec3 minPos = new Vec3(playerPos.offset(-radius, -radius, -radius));
        Vec3 maxPos = new Vec3(playerPos.offset(radius, radius, radius));

        AABB area = new AABB(minPos, maxPos);

        for (int x = (int) Math.floor(area.minX); x <= Math.ceil(area.maxX); x++) {
            for (int y = (int) Math.floor(area.minY); y <= Math.ceil(area.maxY); y++) {
                for (int z = (int) Math.floor(area.minZ); z <= Math.ceil(area.maxZ); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    Block block = mc.level.getBlockState(pos).getBlock();
                    if (targetBlocks.contains(block)) {
                        woodBlocks.add(pos);
                    }
                }
            }
        }
        return woodBlocks;
    }

    private boolean isInRange(BlockPos pos) {
        return mc.player.position().distanceTo(Vec3.atCenterOf(pos)) <= 4.5;
    }

    private void breakBlock(BlockPos pos) {
        if (!Utils.nullCheck()) return;

        Block currentBlock = mc.level.getBlockState(pos).getBlock();

        if (aim.isToggled()) {
            float[] a = RotationUtils.getYawPitchTo(mc.player.getEyePosition(), new Vec3(pos));
            if (silent.isToggled()) {
                Rotations.setRotate(this, a[0], a[1], 4, 2);
            } else {
                RotationUtils.aimByPos(new Vec3(pos), 2);
            }
        }

        if (lastTarget != null && lastTarget != currentBlock) {
            sendAction(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, pos);
            lastTarget = currentBlock;
            startPacketSent = false;
            return;
        }

        if (!startPacketSent) {
            sendAction(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos);
            startPacketSent = true;
        }

        lastTarget = currentBlock;
        mc.player.swing(InteractionHand.MAIN_HAND);
    }

    private void sendAction(ServerboundPlayerActionPacket.Action action, BlockPos pos) {
        mc.player.connection.send(
                new ServerboundPlayerActionPacket(action, pos, BlockUtils.getDirection(pos)));
    }

    private void resetTarget() {
        if (currentTarget != null && startPacketSent) {
            sendAction(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, currentTarget);
        }
        currentTarget   = null;
        lastTarget      = null;
        startPacketSent = false;
    }

    @EventHandler
    public void onSettingUpdate(SettingUpdateEvent event) {
        if (!targetBlocks.isEmpty()) {
            targetBlocks.clear();
        }
        getTargetBlocks();
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