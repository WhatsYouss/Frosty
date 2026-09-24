package xyz.whatsyouss.frosty.mixin.sodium;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import xyz.whatsyouss.frosty.modules.impl.render.Xray;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer", remap = false)
public abstract class BlockRendererMixin extends AbstractBlockRenderContextMixin {
    @ModifyExpressionValue(method = "processQuad", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/model/MutableQuadViewImpl;getRenderType()Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;"), require = 0)
    private ChunkSectionLayer frosty$useTranslucentLayer(ChunkSectionLayer original) {
        return Xray.transparentAlphaFor(state) >= 0 && Xray.isOpacityMode() ? ChunkSectionLayer.TRANSLUCENT : original;
    }

    @ModifyExpressionValue(method = "bufferQuad(Lnet/caffeinemc/mods/sodium/client/render/model/MutableQuadViewImpl;[FLnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/Material;)V", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/model/MutableQuadViewImpl;baseColor(I)I"), require = 0)
    private int frosty$applyXrayOpacity(int color) {
        int alpha = Xray.transparentAlphaFor(state);
        return alpha >= 0 && alpha < 255 ? alpha << 24 | color & 0x00FFFFFF : color;
    }
}
