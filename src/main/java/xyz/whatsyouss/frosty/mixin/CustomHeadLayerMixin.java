package xyz.whatsyouss.frosty.mixin;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.whatsyouss.frosty.interfaces.IEntityRenderState;
import xyz.whatsyouss.frosty.modules.ModuleManager;

import static xyz.whatsyouss.frosty.Frosty.mc;

@Mixin(CustomHeadLayer.class)
public abstract class CustomHeadLayerMixin<S extends LivingEntityRenderState, M extends EntityModel<S> & HeadedModel> {
    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V", at = @At("HEAD"), cancellable = true)
    private void onRender(PoseStack matrixStack, SubmitNodeCollector orderedRenderCommandQueue, int i, S livingEntityRenderState, float f, float g, CallbackInfo ci) {
        Entity entity = ((IEntityRenderState) livingEntityRenderState).frosty$getEntity();
        if (livingEntityRenderState instanceof AvatarRenderState && ModuleManager.armorHider.isEnabled() && ModuleManager.armorHider.head.isToggled()) {
            if (ModuleManager.armorHider.selfOnly.isToggled() && entity != mc.player) {
                return;
            }
            ci.cancel();
        }
    }
}

