package xyz.whatsyouss.frosty.modules.impl.mining;

import meteordevelopment.orbit.EventHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
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
        super("SandNuker", "沙子光环", category.Mining);

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

        if (mode.getValue() == 0) {
            handleNormalMode();
        } else {
            handleInstantMode();
        }
    }

    private void handleNormalMode() {
        tickCounter++;
        if (tickCounter < breakDelay) return;
        tickCounter = 0;

        if (currentTarget != null) {
            BlockState state = mc.level.getBlockState(currentTarget);
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

    private List<BlockPos> findSandBlocks() {
        List<BlockPos> sandBlocks = new ArrayList<>();
        if (!Utils.nullCheck()) return sandBlocks;

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
                        sandBlocks.add(pos);
                    }
                }
            }
        }
        return sandBlocks;
    }

    private boolean isInRange(BlockPos pos) {
        return mc.player.position().distanceTo(Vec3.atCenterOf(pos)) <= 4.5;
    }

    private void breakBlock(BlockPos pos) {
        if (!Utils.nullCheck() || mc.level.getBlockState(pos).isAir()) {
            currentTarget = null;
            return;
        }

        if (!sent) {
            mc.player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                    pos, BlockUtils.getDirection(pos)));
            sent = true;
        }
        mc.player.swing(InteractionHand.MAIN_HAND);
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