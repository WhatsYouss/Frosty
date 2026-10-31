package xyz.whatsyouss.frosty.mixin;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.modules.impl.render.NickHider;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {

    @Inject(method = "extractNameTags(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;FDD)V", at = @At("TAIL"))
    private void onExtractNameTags(T entity, S state, float partialTicks, double nameTagDistance, double belowNameDistance, CallbackInfo ci) {
        if (state == null || state.nameTag == null) return;

        if (entity instanceof Player && ModuleManager.nametags.isEnabled()) {
            state.isDiscrete = false;
        }

        if (entity instanceof Player && ModuleManager.nickHider.isEnabled() && !ModuleManager.nickHider.name.getValue().isEmpty()) {

            state.nameTag = NickHider.processText((MutableComponent) state.nameTag);
        }
    }

    @Inject(method = "shouldShowName", at = @At("HEAD"), cancellable = true)
    private void onShouldShowName(T entity, double distance, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof Player && ModuleManager.nametags.isEnabled()) {
            cir.setReturnValue(true);
        } else if (entity instanceof Player) {
            cir.setReturnValue(false);
        }
    }
}
