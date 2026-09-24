package xyz.whatsyouss.frosty.mixin.sodium;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext", remap = false)
public abstract class AbstractBlockRenderContextMixin {
    @Shadow
    protected BlockState state;
    @Shadow
    protected BlockPos pos;

    @Inject(method = "isFaceCulled(Lnet/minecraft/core/Direction;)Z", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void frosty$face(Direction face, CallbackInfoReturnable<Boolean> cir) {
        var xray = xyz.whatsyouss.frosty.modules.ModuleManager.xray;
        Boolean draw = xray.shouldDrawSide(state, pos);
        if (draw != null) cir.setReturnValue(!draw);
    }
}
