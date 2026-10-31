package xyz.whatsyouss.frosty.mixin.indigo;

import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.whatsyouss.frosty.modules.impl.render.Xray;

@Pseudo
@Mixin(targets = "net.fabricmc.fabric.impl.client.indigo.renderer.render.AltModelBlockRendererImpl", remap = false)
public abstract class AltModelBlockRendererImplMixin {
    @Shadow private BlockPos pos;
    @Shadow private BlockState blockState;

    @Inject(method = "shouldCullFace", at = @At("HEAD"), require = 0, cancellable = true)
    private void frosty$chooseXrayFace(Direction face, CallbackInfoReturnable<Boolean> cir) {
        var xray = xyz.whatsyouss.frosty.modules.ModuleManager.xray;
        Boolean shouldDrawSide = xray.shouldDrawSide(blockState, pos);
        if (shouldDrawSide != null) cir.setReturnValue(!shouldDrawSide);
    }

    @Inject(method = "transform", at = @At("RETURN"), require = 0)
    private void frosty$applyXrayOpacity(MutableQuadView quad, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        var xray = xyz.whatsyouss.frosty.modules.ModuleManager.xray;
        if (!xray.isOpacityMode() || xray.isVisible(blockState.getBlock())) return;
        int alpha = Xray.opacityColorMask() >>> 24;

        quad.chunkLayer(ChunkSectionLayer.TRANSLUCENT);
        for (int index = 0; index < 4; index++) {
            quad.color(index, alpha << 24 | quad.color(index) & 0x00FFFFFF);
        }
    }
}
