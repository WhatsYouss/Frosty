package xyz.whatsyouss.frosty.mixin.sodium;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.whatsyouss.frosty.modules.impl.render.Xray;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer", remap = false)
public abstract class DefaultFluidRendererMixin {
    @Inject(method = "isFullBlockFluidSideVisible(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/material/FluidState;)Z", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void frosty$full(BlockGetter level, BlockPos pos, Direction direction, FluidState fluid, CallbackInfoReturnable<Boolean> cir) {
        var xray = xyz.whatsyouss.frosty.modules.ModuleManager.xray;
        Boolean draw = xray.shouldDrawSide(level.getBlockState(pos), null);
        if (draw != null)
            cir.setReturnValue(!level.getBlockState(pos.offset(direction.getUnitVec3i())).getFluidState().getType().isSame(fluid.getType()) && draw);
    }

    @Inject(method = "isFluidSideExposed(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;F)Z", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void frosty$side(BlockAndTintGetter level, BlockState state, BlockPos pos, Direction direction, float height, CallbackInfoReturnable<Boolean> cir) {
        var xray = xyz.whatsyouss.frosty.modules.ModuleManager.xray;
        Boolean draw = xray.shouldDrawSide(state, null);
        if (draw != null)
            cir.setReturnValue(!level.getBlockState(pos).getFluidState().getType().isSame(state.getFluidState().getType()) && draw);
    }

    @ModifyExpressionValue(method = "updateQuad", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/api/util/ColorARGB;toABGR(I)I"), require = 0)
    private int frosty$opacity(int color, @Local(argsOnly = true) FluidState state) {
        int a = Xray.transparentAlphaFor(state.createLegacyBlock());
        return a >= 0 && a < 255 ? a << 24 | color & 0xFFFFFF : color;
    }
}
