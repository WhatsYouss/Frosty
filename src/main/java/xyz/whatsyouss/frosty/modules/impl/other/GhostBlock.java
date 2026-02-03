package xyz.whatsyouss.frosty.modules.impl.other;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
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
        super("GhostBlock", category.Other);

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
        if (!holdingPickaxe() || mc.currentScreen != null) {
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

        Vec3d cameraPos = mc.player.getEyePos();
        Vec3d viewVector = mc.player.getRotationVec(1.0F);
        Vec3d direction = new Vec3d(
                viewVector.x * rangeValue,
                viewVector.y * rangeValue,
                viewVector.z * rangeValue
        );
        Vec3d rayEnd = cameraPos.add(direction);

        RaycastContext context = new RaycastContext(
                cameraPos,
                rayEnd,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                mc.player
        );

        BlockHitResult hitResult = mc.world.raycast(context);

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos targetPos = hitResult.getBlockPos();

            BlockState state = mc.world.getBlockState(targetPos);
            if (!state.isAir() && state.getHardness(mc.world, targetPos) >= 0) {
                mc.player.swingHand(Hand.MAIN_HAND);
                mc.world.setBlockState(targetPos, net.minecraft.block.Blocks.AIR.getDefaultState(), 3);
            }
        }
    }

    private boolean holdingPickaxe() {
        return mc.player.getMainHandStack().getItem() == Items.WOODEN_PICKAXE ||
                mc.player.getMainHandStack().getItem() == Items.STONE_PICKAXE ||
                mc.player.getMainHandStack().getItem() == Items.IRON_PICKAXE ||
                mc.player.getMainHandStack().getItem() == Items.GOLDEN_PICKAXE ||
                mc.player.getMainHandStack().getItem() == Items.DIAMOND_PICKAXE ||
                mc.player.getMainHandStack().getItem() == Items.NETHERITE_PICKAXE;
    }
}
