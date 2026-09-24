package xyz.whatsyouss.frosty.mixin;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import xyz.whatsyouss.frosty.modules.impl.render.Xray;

@Mixin(SectionCompiler.class)
public abstract class SectionCompilerMixin {
    @ModifyVariable(method = "getOrBeginLayer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private ChunkSectionLayer frosty$translucent(ChunkSectionLayer layer) {
        return Xray.isOpacityMode() ? ChunkSectionLayer.TRANSLUCENT : layer;
    }
}
