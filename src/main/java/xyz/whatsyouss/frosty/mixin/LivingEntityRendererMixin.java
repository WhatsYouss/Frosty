package xyz.whatsyouss.frosty.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.whatsyouss.frosty.interfaces.IEntityRenderState;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.modules.impl.other.AntiBot;
import xyz.whatsyouss.frosty.utility.RenderLayers;
import static xyz.whatsyouss.frosty.Frosty.mc;
import static xyz.whatsyouss.frosty.modules.impl.render.PlayerESP.chams;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {
    @Shadow
    public abstract Identifier getTextureLocation(S renderState);

    @ModifyExpressionValue(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getTeam()Lnet/minecraft/world/scores/PlayerTeam;"))
    private PlayerTeam hasLabelClientPlayerEntityGetScoreboardTeamProxy(PlayerTeam team) {
        return (mc.player == null) ? null : team;
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
    private void render$Head(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo ci) {
        Entity entity = ((IEntityRenderState) state).frosty$getEntity();
        if (!(entity instanceof LivingEntity livingEntity)) return;
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("TAIL"))
    private void render$Tail(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo ci) {
        Entity entity = ((IEntityRenderState) state).frosty$getEntity();
        if (!(entity instanceof LivingEntity livingEntity)) return;
    }

    @ModifyReturnValue(method = "getRenderType", at = @At("RETURN"))
    private RenderType frosty$chamsRenderType(RenderType original, S state, boolean visible, boolean translucent, boolean glowing) {
        Entity entity = ((IEntityRenderState) state).frosty$getEntity();
        if (frosty$shouldRenderPlayerChams(entity)) {
            return RenderLayers.entityTranslucentNoDepth(getTextureLocation(state));
        }
        return original;
    }

    @Unique
    private boolean frosty$shouldRenderPlayerChams(Entity entity) {
        return entity instanceof Player && entity != mc.player && ModuleManager.playerESP.isEnabled() && chams.isToggled() && (!ModuleManager.antiBot.isEnabled() || !AntiBot.isBot((Player) entity));
    }
}
