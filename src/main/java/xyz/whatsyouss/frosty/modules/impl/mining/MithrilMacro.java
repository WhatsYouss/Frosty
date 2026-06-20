package xyz.whatsyouss.frosty.modules.impl.mining;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.interfaces.IKeyMapping;
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
    public ButtonSetting titanium, smartRotation, aimCheck;

    private int toolSlot = -1;

    private long targetStartTime;
    private BlockPos targetPos;
    private Block lastTarget;
    private Vec3 targetHitVec;

    private boolean startPacketSent = false;

    public MithrilMacro() {
        super("MithrilMacro", category.Mining);

        this.registerSetting(prioritize = new SelectSetting("Prioritize", 0, mithrilTypes));
        this.registerSetting(rotateSmoothing = new SliderSetting("Rotate smoothing", 0, 0, 5, 1));
        this.registerSetting(smartRotation = new ButtonSetting("Smart Rotation", true));
        this.registerSetting(maxBreaktime = new SliderSetting("Max Breaktime", "s", 1, 0.5, 7, 0.1));
        this.registerSetting(titanium = new ButtonSetting("Titanium", true));
        this.registerSetting(aimCheck = new ButtonSetting("Aim check", true));
    }

    @Override
    public void onEnable() {
//        if (ModuleManager.commissionMacro.isEnabled() && CommissionMacro.state != MINING) {
//            ModuleManager.commissionMacro.disable();
//        }
        if (!Utils.nullCheck()) return;
        toolSlot = -1;
        for (int i = 0; i < 9; i++) {
            var stack = mc.player.getInventory().getItem(i);
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
    public void onDisable() {
        resetTarget();
        if (mc.options.keyAttack.isDown()) {
            IKeyMapping.get(mc.options.keyAttack).setDown(false);
        }
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }

        if (mc.player.getInventory().getSelectedSlot() != toolSlot && toolSlot != -1)
            mc.player.getInventory().setSelectedSlot(toolSlot);

        if (!mc.mouseHandler.isMouseGrabbed() && !aimCheck.isEnabled) {
            if (mc.gui.screen() == null) {
                mc.mouseHandler.grabMouse();
                IKeyMapping.get(mc.options.keyAttack).setDown(true);
            }
        }
        if (!mc.options.keyAttack.isDown() && !aimCheck.isEnabled) {
            IKeyMapping.get(mc.options.keyAttack).setDown(true);
        }

        if (targetPos != null && mc.level.getBlockState(targetPos).is(Blocks.BEDROCK))
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
        BlockPos playerPos = mc.player.blockPosition();

        for (int x = -3; x <= 3; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -3; z <= 3; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);

                    if (mc.level.getBlockState(pos).getShape(mc.level, pos).isEmpty())
                        continue;

                    int weight = getWeight(pos);
                    if (weight <= 0) continue;

                    Vec3 hitVec = getVisibleHitVec(pos);
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
                            .thenComparingDouble(n -> mc.player.getEyePosition().distanceToSqr(n.hitVec)))
                    .findFirst().orElse(null);
        }

        Vec3 eyes = mc.player.getEyePosition();
        float curYaw = mc.player.getYRot();
        for (MithrilNode n : nodes) {
            double dx = n.hitVec.x - eyes.x, dz = n.hitVec.z - eyes.z;
            float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            n.angle = Math.abs(Mth.wrapDegrees(targetYaw - curYaw));
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

    private Vec3 getVisibleHitVec(BlockPos pos) {
        if (mc.player == null || mc.level == null) return null;

        Vec3 eyes   = mc.player.getEyePosition();
        Vec3 center = Vec3.atCenterOf(pos);

        BlockHitResult result = mc.level.clip(new ClipContext(
                eyes, center,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                mc.player));

        if (result != null && result.getType() == HitResult.Type.BLOCK
                && result.getBlockPos().equals(pos)) {
            return result.getLocation();
        }
        return null;
    }

    private int getWeight(BlockPos pos) {
        String name = mc.level.getBlockState(pos).getBlock().getDescriptionId();
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
        return mc.hitResult instanceof BlockHitResult bhr && bhr.getBlockPos().equals(pos);
    }

    private void breakBlock(BlockPos pos) {
        Block currentBlock = mc.level.getBlockState(pos).getBlock();

        if (lastTarget != null && lastTarget != currentBlock) {
            sendAction(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, pos);
            lastTarget      = currentBlock;
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
        if (targetPos != null && startPacketSent) {
            if (mc.hitResult instanceof BlockHitResult bhr) {
                mc.player.connection.send(new ServerboundPlayerActionPacket(
                        ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, targetPos, bhr.getDirection()));
            } else {
                sendAction(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, targetPos);
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
        final Vec3 hitVec;
        final int      weight;
        float angle = 0f;

        MithrilNode(BlockPos pos, Vec3 hitVec, int weight) {
            this.pos    = pos;
            this.hitVec = hitVec;
            this.weight = weight;
        }
        int getWeight() { return weight; }
    }
}