package xyz.whatsyouss.frosty.mixin;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.whatsyouss.frosty.modules.impl.render.Xray;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateXrayMixin {
    @Inject(method = "isSolidRender", at = @At("HEAD"), cancellable = true)
    private void frosty$xraySolidRender(CallbackInfoReturnable<Boolean> cir) {
        if (Xray.isActive() && !Xray.shouldShowBlock((BlockState) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canOcclude", at = @At("HEAD"), cancellable = true)
    private void frosty$xrayCanOcclude(CallbackInfoReturnable<Boolean> cir) {
        if (Xray.isActive() && !Xray.shouldShowBlock((BlockState) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
