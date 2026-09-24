package xyz.whatsyouss.frosty.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.whatsyouss.frosty.modules.impl.render.Xray;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {
    @Inject(method = "getShadeBrightness", at = @At("RETURN"), cancellable = true, order = 980)
    private void frosty$brightenXrayBlocks(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (Xray.isActive()) cir.setReturnValue(1.0f);
    }
}
