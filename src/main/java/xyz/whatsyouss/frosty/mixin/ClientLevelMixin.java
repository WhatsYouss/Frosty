package xyz.whatsyouss.frosty.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.events.impl.EntityJoinEvent;
import xyz.whatsyouss.frosty.modules.impl.farming.FarmingStats;

import static xyz.whatsyouss.frosty.Frosty.mc;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {
    @Inject(method = "addEntity", at = @At("HEAD"))
    private void onEntityJoin(Entity entity, CallbackInfo ci) {
        Frosty.EVENT_BUS.post(new EntityJoinEvent(entity));
    }

    @Inject(method = "sendBlockUpdated", at = @At("HEAD"))
    private void frosty$onBlockUpdated(BlockPos pos, BlockState oldState, BlockState newState,
                                      int flags, CallbackInfo ci) {
        FarmingStats.onBlockChanged(pos, newState);
    }
}
