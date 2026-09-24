package xyz.whatsyouss.frosty.mixin.sodium;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.whatsyouss.frosty.modules.impl.render.Xray;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer", remap = false)
public abstract class DefaultFluidRendererMixin {
    @Inject(method = "isFullBlockFluidSideVisible(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/material/FluidState;)Z", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void frosty$chooseFullFluidSide(BlockGetter level, BlockPos pos, Direction direction,
                                             FluidState fluid, CallbackInfoReturnable<Boolean> cir) {
        var xray = xyz.whatsyouss.frosty.modules.ModuleManager.xray;
        BlockState state = level.getBlockState(pos);
        Boolean shouldDrawSide = xray.shouldDrawSide(state, null);
        if (shouldDrawSide == null) return;

        BlockState neighbor = level.getBlockState(pos.offset(direction.getUnitVec3i()));
        cir.setReturnValue(!neighbor.getFluidState().getType().isSame(fluid.getType()) && shouldDrawSide);
    }

    @Inject(method = "isFluidSideExposed(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;F)Z", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void frosty$chooseFluidSide(BlockAndTintGetter level, BlockState state, BlockPos neighborPos,
                                         Direction direction, float height, CallbackInfoReturnable<Boolean> cir) {
        var xray = xyz.whatsyouss.frosty.modules.ModuleManager.xray;
        Boolean shouldDrawSide = xray.shouldDrawSide(state, null);
        if (shouldDrawSide == null) return;

        BlockState neighbor = level.getBlockState(neighborPos);
        cir.setReturnValue(!neighbor.getFluidState().getType().isSame(state.getFluidState().getType()) && shouldDrawSide);
    }

    @Inject(method = "getUpFaceExposureByNeighbors(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/material/FluidState;)I", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void frosty$exposeXrayFluidTop(BlockAndTintGetter level, BlockPos pos, FluidState fluid,
                                            CallbackInfoReturnable<Integer> cir) {
        var xray = xyz.whatsyouss.frosty.modules.ModuleManager.xray;
        Boolean shouldDrawSide = xray.shouldDrawSide(fluid.createLegacyBlock(), null);
        if (shouldDrawSide != null) cir.setReturnValue(shouldDrawSide ? 3 : 0);
    }

    @ModifyExpressionValue(method = "updateQuad", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/api/util/ColorARGB;toABGR(I)I"), require = 0)
    private int frosty$applyXrayOpacity(int color, @Local(argsOnly = true) BlockPos pos,
                                         @Local(argsOnly = true) FluidState state) {
        int alpha = Xray.transparentAlphaFor(state.createLegacyBlock());
        return alpha >= 0 && alpha < 255 ? alpha << 24 | color & 0x00FFFFFF : color;
    }
}
