package xyz.whatsyouss.frosty.mixin;

import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin
{
    @Shadow @Final private LevelRenderState levelRenderState;

    @Inject(
            method = "render(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;ZLnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;Lorg/joml/Vector4f;ZZ)V",
            at = @At("RETURN"))
    private void onRender(GraphicsResourceAllocator allocator,
                          boolean renderBlockOutline, CameraRenderState cameraState,
                          GpuBufferSlice gpuBufferSlice, Vector4f vector4f,
                          boolean shouldRenderSky, boolean renderClouds, CallbackInfo ci)
    {
        PoseStack matrixStack = new PoseStack();
        matrixStack.mulPose(cameraState.viewRotationMatrix);
        Render3DEvent event = new Render3DEvent(
                matrixStack,
                levelRenderState.worldPartialTicks,
                cameraState.pos.x(),
                cameraState.pos.y(),
                cameraState.pos.z()
        );
        Frosty.EVENT_BUS.post(event);
    }
}
