package xyz.whatsyouss.frosty.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.whatsyouss.frosty.modules.impl.render.PlayerESP;
import xyz.whatsyouss.frosty.utility.Rotations;

import static xyz.whatsyouss.frosty.Frosty.mc;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin extends LivingEntityRenderer<Avatar, AvatarRenderState, PlayerModel> {

    public AvatarRendererMixin(EntityRendererProvider.Context ctx, PlayerModel model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("RETURN"))
    private void extractRenderState$rotations(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
        if (Rotations.rotating && entity == mc.player) {
            state.yRot = 0;
            state.bodyRot = Rotations.serverYaw;
            state.xRot = Rotations.serverPitch;
        }
    }
}