package xyz.whatsyouss.frosty.modules.impl.mining;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.BlockUtils;
import xyz.whatsyouss.frosty.utility.RotationUtils;
import xyz.whatsyouss.frosty.utility.Utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MithrilMacro extends Module {

    private final String[] mithrilTypes = new String[]{"Gray", "Prismarine", "Blue"};

    public SelectSetting prioritize;
    public SliderSetting rotateSmoothing, maxBreaktime;
    public ButtonSetting titanium, smartRotation;

    private int toolSlot = -1;

    private long targetStartTime;
    private BlockPos targetPos;
    private Block lastTarget;
    private Vec3d targetHitVec;

    private boolean startPacketSent = false;

    public MithrilMacro() {
        super("MithrilMacro", category.Mining);
        this.registerSetting(prioritize      = new SelectSetting("Prioritize", 0, mithrilTypes));
        this.registerSetting(rotateSmoothing = new SliderSetting("Rotate smoothing", 0, 0, 5, 1));
        this.registerSetting(smartRotation   = new ButtonSetting("Smart Rotation", true));
        this.registerSetting(maxBreaktime    = new SliderSetting("Max Breaktime", "s", 1, 0.5, 7, 0.1));
        this.registerSetting(titanium        = new ButtonSetting("Titanium", true));
    }

    @Override
    public void onEnable() {
//        if (ModuleManager.commissionMacro.isEnabled() && CommissionMacro.state != MINING) {
//            ModuleManager.commissionMacro.disable();
//        }
        if (!Utils.nullCheck()) return;
        toolSlot = -1;
        for (int i = 0; i < 9; i++) {
            var stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            String name = Utils.getLiteral(stack.getCustomName() != null
                    ? stack.getCustomName().toString() : "").toLowerCase();
            if (name.contains("drill") || name.contains("pickaxe") || name.contains("2000")) {
                toolSlot = i;
                break;
            }
        }
        if (toolSlot == -1) {
            Utils.addChatMessage("Mining Tool not found!");
            this.disable();
        }
    }

    @Override
    public void onDisable() { resetTarget(); }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }

        if (mc.player.getInventory().getSelectedSlot() != toolSlot && toolSlot != -1)
            mc.player.getInventory().setSelectedSlot(toolSlot);

        if (targetPos != null && mc.world.getBlockState(targetPos).isOf(Blocks.BEDROCK))
            resetTarget();

        if (targetPos != null
                && System.currentTimeMillis() - targetStartTime > maxBreaktime.getInput() * 1000)
            resetTarget();

        if (targetPos == null) findBestTarget();

        if (targetPos != null && targetHitVec != null) {
            RotationUtils.aimByPos(targetHitVec, (float) rotateSmoothing.getInput() + 2);
            if (isLookingAt(targetPos)) breakBlock(targetPos);
        }
    }

    private void findBestTarget() {
        List<MithrilNode> nodes = new ArrayList<>();
        BlockPos playerPos = mc.player.getBlockPos();

        for (int x = -3; x <= 3; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -3; z <= 3; z++) {
                    BlockPos pos = playerPos.add(x, y, z);

                    if (mc.world.getBlockState(pos).getOutlineShape(mc.world, pos).isEmpty())
                        continue;

                    int weight = getWeight(pos);
                    if (weight <= 0) continue;

                    Vec3d hitVec = getVisibleHitVec(pos);
                    if (hitVec != null) nodes.add(new MithrilNode(pos, hitVec, weight));
                }
            }
        }

        MithrilNode best = selectBest(nodes);
        if (best != null) {
            targetPos       = best.pos;
            targetHitVec    = best.hitVec;
            targetStartTime = System.currentTimeMillis();
            startPacketSent = false;
        }
    }

    private MithrilNode selectBest(List<MithrilNode> nodes) {
        if (nodes.isEmpty()) return null;

        if (!smartRotation.isToggled()) {
            return nodes.stream()
                    .sorted(Comparator.comparingInt(MithrilNode::getWeight).reversed()
                            .thenComparingDouble(n -> mc.player.getEyePos().squaredDistanceTo(n.hitVec)))
                    .findFirst().orElse(null);
        }

        Vec3d eyes = mc.player.getEyePos();
        float curYaw = mc.player.getYaw();
        for (MithrilNode n : nodes) {
            double dx = n.hitVec.x - eyes.x, dz = n.hitVec.z - eyes.z;
            float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            n.angle = Math.abs(MathHelper.wrapDegrees(targetYaw - curYaw));
        }

        List<MithrilNode> hi = nodes.stream().filter(n -> n.weight >= 100).collect(Collectors.toList());
        List<MithrilNode> lo = nodes.stream().filter(n -> n.weight < 100).collect(Collectors.toList());

        Optional<MithrilNode> hiSmall = hi.stream()
                .filter(n -> n.angle < 90f)
                .min(Comparator.comparingDouble(n -> n.angle));
        if (hiSmall.isPresent()) return hiSmall.get();

        Optional<MithrilNode> hiMin = hi.stream().min(Comparator.comparingDouble(n -> n.angle));
        Optional<MithrilNode> loMin = lo.stream().min(Comparator.comparingDouble(n -> n.angle));

        if (loMin.isPresent() && hiMin.isPresent()
                && loMin.get().angle < hiMin.get().angle)
            return loMin.get();

        return hiMin.orElse(loMin.orElse(null));
    }

    private Vec3d getVisibleHitVec(BlockPos pos) {
        Vec3d eyes   = mc.player.getEyePos();
        Vec3d center = Vec3d.ofCenter(pos);
        BlockHitResult result = mc.world.raycast(new RaycastContext(
                eyes, center,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                mc.player));
        if (result != null && result.getType() == HitResult.Type.BLOCK
                && result.getBlockPos().equals(pos))
            return result.getPos();
        return null;
    }

    private int getWeight(BlockPos pos) {
        String name = mc.world.getBlockState(pos).getBlock().getTranslationKey();
        int mode = (int) prioritize.getValue();
//        int mode = ModuleManager.commissionMacro.isEnabled() ? 0 : (int) prioritize.getValue();

        if (name.contains("gray_wool") || name.contains("cyan_terracotta"))
            return (mode == 0) ? 100 : 10;
        if (name.contains("prismarine") || name.contains("dark_prismarine"))
            return (mode == 1) ? 100 : 10;
        if (name.contains("light_blue_wool"))
            return (mode == 2) ? 100 : 10;
        if (name.contains("polished_diorite"))
//            return (ModuleManager.commissionMacro.isEnabled() || titanium.isToggled()) ? 100 : 0;
            return (titanium.isToggled()) ? 100 : 0;
        return 0;
    }

    private boolean isLookingAt(BlockPos pos) {
        return mc.crosshairTarget instanceof BlockHitResult bhr && bhr.getBlockPos().equals(pos);
    }

    private void breakBlock(BlockPos pos) {
        Block currentBlock = mc.world.getBlockState(pos).getBlock();

        if (lastTarget != null && lastTarget != currentBlock) {
            sendAction(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, pos);
            lastTarget      = currentBlock;
            startPacketSent = false;
            return;
        }

        if (!startPacketSent) {
            sendAction(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos);
            startPacketSent = true;
        }

        lastTarget = currentBlock;
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void sendAction(PlayerActionC2SPacket.Action action, BlockPos pos) {
        mc.player.networkHandler.sendPacket(
                new PlayerActionC2SPacket(action, pos, BlockUtils.getDirection(pos)));
    }

    private void resetTarget() {
        if (targetPos != null && startPacketSent) {
            if (mc.crosshairTarget instanceof BlockHitResult bhr) {
                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, targetPos, bhr.getSide()));
            } else {
                sendAction(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, targetPos);
            }
        }
        targetPos       = null;
        targetHitVec    = null;
        targetStartTime = 0;
        lastTarget      = null;
        startPacketSent = false;
    }

    private static class MithrilNode {
        final BlockPos pos;
        final Vec3d    hitVec;
        final int      weight;
        float angle = 0f;

        MithrilNode(BlockPos pos, Vec3d hitVec, int weight) {
            this.pos    = pos;
            this.hitVec = hitVec;
            this.weight = weight;
        }
        int getWeight() { return weight; }
    }
}