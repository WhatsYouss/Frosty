package xyz.whatsyouss.frosty.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.GpuTexture;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.util.ARGB;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.whatsyouss.frosty.modules.ModuleManager;

@Mixin(Lightmap.class)
public abstract class LightmapMixin {
    @Shadow
    @Final
    private GpuTexture texture;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void render$fullbright(LightmapRenderState renderState, CallbackInfo ci) {
        if (ModuleManager.fullbright.isEnabled() && ModuleManager.fullbright.selectedMode == 0) {
            var profile = Profiler.get();
            profile.push("lightmap");

            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(texture, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
            profile.pop();
            ci.cancel();
        }
    }
}