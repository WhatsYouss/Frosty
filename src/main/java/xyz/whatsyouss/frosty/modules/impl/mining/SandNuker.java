package xyz.whatsyouss.frosty.modules.impl.mining;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.modules.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.BlockUtils;
import xyz.whatsyouss.frosty.utility.RenderUtils;
import xyz.whatsyouss.frosty.utility.Utils;
import xyz.whatsyouss.frosty.utility.MathUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SandNuker extends Module {

    private SelectSetting mode;
    private SliderSetting bps;

    private String[] modes = new String[]{"Normal", "Instant"};

    private final int radius = 5;
    private final List<Block> targetBlocks = List.of(Blocks.SAND, Blocks.RED_SAND);
    private int tickCounter = 0;
    private final int breakDelay = 2;
    private BlockPos currentTarget = null;
    private boolean sent;
    private int instantTickCounter = 0;
    private int requiredTicks = 0;

    public SandNuker() {
        super("SandNuker", category.Mining);

        this.registerSetting(mode = new SelectSetting("Mode", 1, modes));
        this.registerSetting(bps = new SliderSetting("BPS", 20 ,10, 20, 1));
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

        if (mode.getValue() == 0) { // Normal
            handleNormalMode();
        } else { // Instant
            handleInstantMode();
        }
    }

    private void handleNormalMode() {
        tickCounter++;
        if (tickCounter < breakDelay) return;
        tickCounter = 0;

        if (currentTarget != null) {
            BlockState state = mc.world.getBlockState(currentTarget);
            boolean isStillTarget = targetBlocks.contains(state.getBlock()) &&
                    isInRange(currentTarget) &&
                    !state.isAir();

            if (!isStillTarget) {
                currentTarget = null;
                sent = false;
            }
        }

        if (currentTarget == null) {
            List<BlockPos> sandBlocks = findSandBlocks();
            if (!sandBlocks.isEmpty()) {
                currentTarget = getClosestBlock(sandBlocks);
                sent = false;
            }
        }

        if (currentTarget != null) {
            breakBlock(currentTarget);
        }
    }

    private void handleInstantMode() {
        instantTickCounter++;

        if (requiredTicks == 0) {
            requiredTicks = Utils.getAPSToTicks(bps, 20.0);
            if (requiredTicks == -1) return;
        }

        if (instantTickCounter >= requiredTicks) {
            instantTickCounter = 0;

            List<BlockPos> sandBlocks = findSandBlocks();
            if (sandBlocks.isEmpty()) {
                currentTarget = null;
                return;
            }

            BlockPos target = getClosestBlock(sandBlocks);
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

    private List<BlockPos> findSandBlocks() {
        List<BlockPos> sandBlocks = new ArrayList<>();
        if (!Utils.nullCheck()) return sandBlocks;

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
                        sandBlocks.add(pos);
                    }
                }
            }
        }
        return sandBlocks;
    }

    private boolean isInRange(BlockPos pos) {
        return mc.player.getEntityPos().distanceTo(Vec3d.ofCenter(pos)) <= 4.5;
    }

    private void breakBlock(BlockPos pos) {
        if (!Utils.nullCheck() || mc.world.getBlockState(pos).isAir()) {
            currentTarget = null;
            return;
        }

        if (!sent) {
            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
                    pos, BlockUtils.getDirection(pos)));
            sent = true;
        }
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void interactBlock(BlockPos pos) {
        if (!Utils.nullCheck() || mc.world.getBlockState(pos).isAir()) {
            return;
        }

        mc.interactionManager.attackBlock(pos, BlockUtils.getDirection(pos));
        mc.player.swingHand(Hand.MAIN_HAND);

        currentTarget = pos;
    }
}