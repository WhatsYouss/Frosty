package xyz.whatsyouss.frosty.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.core.ClientAsset;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.modules.ModuleManager;

@Mixin(PlayerInfo.class)
public class PlayerInfoMixin {
    @Shadow
    private GameProfile profile;

    @Inject(method = "getSkin", at = @At("TAIL"), cancellable = true)
    private void getSkinTextures(CallbackInfoReturnable<PlayerSkin> cir) {
        PlayerSkin original = cir.getReturnValue();
        if (Frosty.mc.player != null && profile.id().equals(Frosty.mc.player.getUUID())) {
            if (ModuleManager.cape.isEnabled()) {
                Identifier capeTexture = ModuleManager.cape.getCurrentCape();

                if (capeTexture != null) {
                    cir.setReturnValue(new PlayerSkin(
                            original.body(),
                            new ClientAsset.ResourceTexture(capeTexture, capeTexture), new ClientAsset.ResourceTexture(capeTexture, capeTexture),
                            original.model(), original.secure()));
                }
            } else {
                cir.setReturnValue(original);
            }
        }
    }
}