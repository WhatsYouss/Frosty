package xyz.whatsyouss.frosty.modules.impl.other;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import xyz.whatsyouss.frosty.events.impl.MouseButtonEvent;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.KeyAction;
import xyz.whatsyouss.frosty.utility.Utils;

public class GhostBlock extends Module {

    private SliderSetting range;

    private int tick;
    private boolean rcing;

    public GhostBlock() {
        super("GhostBlock", "幽灵方块", category.Other);

        this.registerSetting(range = new SliderSetting("Range", 8, 5, 15, 1));
    }

    @Override
    public void onDisable() {
        tick = 0;
        rcing = false;
    }

    @EventHandler
    public void onMouseButton(MouseButtonEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (!holdingPickaxe() || mc.gui.screen() != null) {
            return;
        }
        if (event.button == 1 && event.action == KeyAction.Press) {
            rcing = true;
        }
        if (event.action == KeyAction.Release){
            rcing = false;
        }
        if (rcing) {
            event.cancel();
        }
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (rcing) {
            if (tick < 3) {
                tick ++;
            } else {
                breakTargetedBlock();
                tick = 0;
            }
        }
    }

    private void breakTargetedBlock() {
        float rangeValue = (float) range.getInput();

        Vec3 cameraPos = mc.player.getEyePosition();
        Vec3 viewVector = mc.player.getViewVector(1.0F);
        Vec3 direction = new Vec3(
                viewVector.x * rangeValue,
                viewVector.y * rangeValue,
                viewVector.z * rangeValue
        );
        Vec3 rayEnd = cameraPos.add(direction);

        ClipContext context = new ClipContext(cameraPos, rayEnd,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player);

        BlockHitResult hitResult = mc.level.clip(context);

        if (hitResult.getType() == BlockHitResult.Type.BLOCK) {
            BlockPos targetPos = hitResult.getBlockPos();
            BlockState state = mc.level.getBlockState(targetPos);

            if (!state.isAir() && state.getBlock() != Blocks.CHEST && state.getBlock() != Blocks.LEVER
                    && state.getDestroySpeed(mc.level, targetPos) >= 0) {

                mc.player.swing(InteractionHand.MAIN_HAND);

                mc.level.syncBlockState(targetPos, Blocks.AIR.defaultBlockState(), null);
            }
        }
    }

    private boolean holdingPickaxe() {
        return mc.player.getMainHandItem().getItem() == Items.WOODEN_PICKAXE ||
                mc.player.getMainHandItem().getItem() == Items.STONE_PICKAXE ||
                mc.player.getMainHandItem().getItem() == Items.IRON_PICKAXE ||
                mc.player.getMainHandItem().getItem() == Items.GOLDEN_PICKAXE ||
                mc.player.getMainHandItem().getItem() == Items.DIAMOND_PICKAXE ||
                mc.player.getMainHandItem().getItem() == Items.NETHERITE_PICKAXE;
    }
}
