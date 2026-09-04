package xyz.whatsyouss.frosty.mixin;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.whatsyouss.frosty.modules.impl.render.Xray;

@Mixin(Block.class)
public abstract class BlockXrayMixin {
    @Inject(method = "shouldRenderFace", at = @At("HEAD"), cancellable = true)
    private static void frosty$xrayShouldRenderFace(BlockState state, BlockState adjacentState,
                                                     Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (Xray.shouldShowBlock(state)) {
            cir.setReturnValue(true);
        }
    }
}
