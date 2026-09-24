package xyz.whatsyouss.frosty.mixin.sodium;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import xyz.whatsyouss.frosty.modules.impl.render.Xray;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer", remap = false)
public abstract class BlockRendererMixin extends AbstractBlockRenderContextMixin {
    @ModifyExpressionValue(method = "processQuad", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/model/MutableQuadViewImpl;getRenderType()Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;"), require = 0)
    private ChunkSectionLayer frosty$layer(ChunkSectionLayer original) {
        return Xray.transparentAlphaFor(state) >= 0 && Xray.isOpacityMode() ? ChunkSectionLayer.TRANSLUCENT : original;
    }

    @ModifyExpressionValue(method = "bufferQuad(Lnet/caffeinemc/mods/sodium/client/render/model/MutableQuadViewImpl;[FLnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/Material;)V", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/model/MutableQuadViewImpl;baseColor(I)I"), require = 0)
    private int frosty$opacity(int color) {
        int a = Xray.transparentAlphaFor(state);
        return a >= 0 && a < 255 ? a << 24 | color & 0xFFFFFF : color;
    }
}
