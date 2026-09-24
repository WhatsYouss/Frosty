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
    @Unique
    private static final ThreadLocal<Float> OPACITY = ThreadLocal.withInitial(() -> 1f);

    @WrapOperation(method = "tesselate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/FluidRenderer;isFaceOccludedByNeighbor(Lnet/minecraft/core/Direction;FLnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean frosty$face(Direction side, float height, BlockState neighbor, Operation<Boolean> original, BlockAndTintGetter level, BlockPos pos, FluidRenderer.Output output, BlockState state, FluidState fluid) {
        Xray xray = ModuleManager.xray;
        Boolean draw = xray.shouldDrawSide(state, null);
        OPACITY.set(xray.isOpacityMode() && !xray.isVisible(state.getBlock()) ? Xray.opacityFloat() : 1f);
        return draw != null ? !draw : original.call(side, height, neighbor);
    }

    @ModifyArg(method = "vertex", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(FFFIFFIIFFF)V"), index = 3)
    private int frosty$opacity(int color) {
        float opacity = OPACITY.get();
        return opacity >= 1 ? color : ARGB.color(Math.round(ARGB.alpha(color) * opacity), ARGB.red(color), ARGB.green(color), ARGB.blue(color));
    }
}
