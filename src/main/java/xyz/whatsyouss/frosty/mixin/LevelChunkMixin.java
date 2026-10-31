package xyz.whatsyouss.frosty.mixin;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.events.impl.BlockUpdateEvent;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {
    @Shadow
    @Final
    Level level;

    @Inject(method = "setBlockState", at = @At("TAIL"))
    private void onSetBlockState(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> info) {
        if (level.isClientSide()) Frosty.EVENT_BUS.post(BlockUpdateEvent.get(pos, info.getReturnValue(), state));
    }
}
