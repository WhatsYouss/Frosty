package xyz.whatsyouss.frosty.mixin;

import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import xyz.whatsyouss.frosty.modules.impl.render.Xray;

@Mixin(SectionCompiler.class)
public abstract class SectionCompilerXrayMixin {
    @ModifyArg(
            method = {"lambda$compile$0", "lambda$compile$1"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/chunk/SectionCompiler;getOrBeginLayer(Ljava/util/Map;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;)Lcom/mojang/blaze3d/vertex/BufferBuilder;"
            ),
            index = 2
    )
    private ChunkSectionLayer frosty$xrayLayer(ChunkSectionLayer original) {
        return Xray.useTranslucentLayer() ? ChunkSectionLayer.TRANSLUCENT : original;
    }
}
