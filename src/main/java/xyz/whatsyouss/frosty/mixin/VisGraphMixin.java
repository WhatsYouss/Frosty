package xyz.whatsyouss.frosty.mixin;

import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.events.impl.ChunkOcclusionEvent;

@Mixin(VisGraph.class)
public abstract class VisGraphMixin {
    @Inject(method = "setOpaque", at = @At("HEAD"), cancellable = true)
    private void onMarkClosed(BlockPos pos, CallbackInfo ci) {
        ChunkOcclusionEvent event = Frosty.EVENT_BUS.post(ChunkOcclusionEvent.get());

        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}