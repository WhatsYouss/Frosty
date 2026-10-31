package xyz.whatsyouss.frosty.mixin;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.whatsyouss.frosty.interfaces.IEntityRenderState;
import xyz.whatsyouss.frosty.modules.ModuleManager;

import static xyz.whatsyouss.frosty.Frosty.mc;

@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin<S extends HumanoidRenderState, M extends HumanoidModel<S>, A extends HumanoidModel<S>> {
    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V", at = @At("HEAD"), cancellable = true)
    private void onRender(PoseStack matrixStack, SubmitNodeCollector orderedRenderCommandQueue, int i, S bipedEntityRenderState, float f, float g, CallbackInfo ci) {
        Entity entity = ((IEntityRenderState) bipedEntityRenderState).frosty$getEntity();
        if (bipedEntityRenderState instanceof AvatarRenderState && ModuleManager.armorHider.isEnabled()) {
            if (ModuleManager.armorHider.selfOnly.isToggled() && entity != mc.player) {
                return;
            }
            ci.cancel();
        }
    }
}
