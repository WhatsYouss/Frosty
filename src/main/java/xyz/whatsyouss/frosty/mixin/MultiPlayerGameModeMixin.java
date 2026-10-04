package xyz.whatsyouss.frosty.mixin;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.events.impl.BreakBlockEvent;
import xyz.whatsyouss.frosty.events.impl.StartBreakingBlockEvent;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.modules.impl.farming.FarmingStats;
import xyz.whatsyouss.frosty.utility.Utils;

import static xyz.whatsyouss.frosty.Frosty.mc;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void onInteractBlock(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (!Utils.nullCheck() || !ModuleManager.stopPlacement.isEnabled()) {
            return;
        }

        ItemStack stack = player.getItemInHand(hand);

        if (ModuleManager.stopPlacement.isPlaceable(stack.getItem())) {
            mc.getConnection().send(new ServerboundUseItemOnPacket(hand, hitResult, 0));
            if (ModuleManager.stopPlacement.swing.isToggled()) {
                mc.player.swing(hand);
            }
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (Frosty.EVENT_BUS.post(StartBreakingBlockEvent.get(pos, direction)).isCancelled()) {
            cir.cancel();
        } else {
            FarmingStats.onBreakClick(mc, pos);
        }
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void onBreakBlock(BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
        if (Frosty.EVENT_BUS.post(BreakBlockEvent.get(blockPos)).isCancelled()) {
            FarmingStats.discardBreakClick(blockPos);
            cir.setReturnValue(false);
        } else {
            FarmingStats.onImmediateBreak(blockPos);
        }
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"))
    private void frosty$trackBreakContinue(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        FarmingStats.onBreakClick(mc, pos);
    }

}
