package xyz.whatsyouss.frosty.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.modules.impl.render.Xray;

@Mixin(FluidRenderer.class)
public abstract class FluidRendererMixin {
    @Unique private static final ThreadLocal<Float> FROSTY_XRAY_OPACITY = ThreadLocal.withInitial(() -> 1.0f);

    @WrapOperation(method = "tesselate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/FluidRenderer;isFaceOccludedByNeighbor(Lnet/minecraft/core/Direction;FLnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean frosty$chooseXrayFluidFace(Direction side, float height, BlockState neighborState,
                                                Operation<Boolean> original, BlockAndTintGetter level,
                                                BlockPos pos, FluidRenderer.Output output, BlockState blockState,
                                                FluidState fluidState) {
        Xray xray = ModuleManager.xray;
        Boolean shouldDrawSide = xray.shouldDrawSide(blockState, null);
        FROSTY_XRAY_OPACITY.set(xray.isOpacityMode() && !xray.isVisible(blockState.getBlock()) ? Xray.opacityFloat() : 1.0f);
        return shouldDrawSide != null ? !shouldDrawSide : original.call(side, height, neighborState);
    }

    @ModifyArg(method = "vertex", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(FFFIFFIIFFF)V"), index = 3)
    private int frosty$applyXrayFluidOpacity(int color) {
        float opacity = FROSTY_XRAY_OPACITY.get();
        return opacity >= 1.0f ? color : ARGB.color(Math.round(ARGB.alpha(color) * opacity),
                ARGB.red(color), ARGB.green(color), ARGB.blue(color));
    }
}
