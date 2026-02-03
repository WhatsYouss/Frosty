package xyz.whatsyouss.frosty.mixin;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.modules.impl.render.NickHider;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {

    @Inject(
            method = "getAndUpdateRenderState",
            at = @At("RETURN")
    )
    private void modifyRenderState(Entity entity, float tickDelta, CallbackInfoReturnable<S> cir) {
        S state = cir.getReturnValue();
        if (state == null) return;

        if (entity instanceof PlayerEntity player &&
                ModuleManager.nickHider.isEnabled() &&
                !ModuleManager.nickHider.name.getValue().isEmpty()) {

            state.displayName = NickHider.processText(state.displayName);
        }
    }
}