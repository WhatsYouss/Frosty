package xyz.whatsyouss.frosty.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.whatsyouss.frosty.modules.ModuleManager;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(method = "handleParticleEvent", at = @At("HEAD"), require = 0)
    private void frosty$onPestTrackerParticle(ClientboundLevelParticlesPacket packet, CallbackInfo ci) {
        if (ModuleManager.pestCleaner != null && ModuleManager.pestCleaner.isEnabled()) {
            ModuleManager.pestCleaner.onTrackerParticle(packet);
        }
    }
}

