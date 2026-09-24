package xyz.whatsyouss.frosty.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.whatsyouss.frosty.modules.ModuleManager;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private <S extends BlockEntityRenderState> void frosty$hide(S state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        if (ModuleManager.xray != null && ModuleManager.xray.shouldHideBlockEntity(state)) ci.cancel();
    }
}
