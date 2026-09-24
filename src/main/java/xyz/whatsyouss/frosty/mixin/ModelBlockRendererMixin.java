package xyz.whatsyouss.frosty.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.modules.impl.render.Xray;

import java.util.List;

@Mixin(ModelBlockRenderer.class)
public abstract class ModelBlockRendererMixin {
    private static final ThreadLocal<Float> FROSTY_XRAY_OPACITY = ThreadLocal.withInitial(() -> 1.0f);

    @WrapOperation(method = "shouldRenderFace", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;shouldRenderFace(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z"))
    private boolean frosty$chooseXrayFace(BlockState state, BlockState neighborState, Direction side,
                                           Operation<Boolean> original, BlockAndTintGetter level,
                                           BlockState ignoredState, Direction ignoredSide, BlockPos neighborPos) {
        Xray xray = ModuleManager.xray;
        BlockPos pos = neighborPos.relative(side.getOpposite());
        Boolean shouldDrawSide = xray.shouldDrawSide(state, pos);
        FROSTY_XRAY_OPACITY.set(xray.isOpacityMode() && !xray.isVisible(state.getBlock()) ? Xray.opacityFloat() : 1.0f);
        return shouldDrawSide != null ? shouldDrawSide : original.call(state, neighborState, side);
    }

    @WrapOperation(method = "putQuadWithTint", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/BlockQuadOutput;put(FFFLnet/minecraft/client/resources/model/geometry/BakedQuad;Lcom/mojang/blaze3d/vertex/QuadInstance;)V"))
    private void frosty$applyXrayOpacity(BlockQuadOutput output, float x, float y, float z,
                                          BakedQuad quad, QuadInstance instance, Operation<Void> original) {
        float opacity = FROSTY_XRAY_OPACITY.get();
        if (opacity < 1.0f) {
            for (int index = 0; index < 4; index++) {
                int color = instance.getColor(index);
                instance.setColor(index, ARGB.color(Math.round(ARGB.alpha(color) * opacity),
                        ARGB.red(color), ARGB.green(color), ARGB.blue(color)));
            }
        }
        original.call(output, x, y, z, quad, instance);
    }

    @WrapOperation(method = {"tesselateFlat", "tesselateAmbientOcclusion"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/dispatch/BlockStateModelPart;getQuads(Lnet/minecraft/core/Direction;)Ljava/util/List;", ordinal = 1))
    private List<BakedQuad> frosty$hideUnculledXrayQuads(BlockStateModelPart part, Direction direction,
                                                           Operation<List<BakedQuad>> original,
                                                           BlockQuadOutput output, float x, float y, float z,
                                                           List<BlockStateModelPart> parts, BlockAndTintGetter level,
                                                           BlockState state, BlockPos pos) {
        Xray xray = ModuleManager.xray;
        return Boolean.FALSE.equals(xray.shouldDrawSide(state, pos)) ? List.of() : original.call(part, direction);
    }
}
