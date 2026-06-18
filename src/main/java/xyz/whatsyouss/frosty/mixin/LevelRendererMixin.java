package xyz.whatsyouss.frosty.mixin;

import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin
{
    @Inject(
            method = "render(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/renderer/state/level/CameraRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V",
            at = @At("RETURN"))
    private void onRender(GraphicsResourceAllocator allocator,
                          DeltaTracker tickCounter, boolean renderBlockOutline,
                          CameraRenderState cameraState, Matrix4fc positionMatrix,
                          GpuBufferSlice gpuBufferSlice, Vector4f vector4f,
                          boolean shouldRenderSky, CallbackInfo ci)
    {
        PoseStack matrixStack = new PoseStack();
        matrixStack.mulPose(positionMatrix);
        Render3DEvent event = new Render3DEvent(
                matrixStack,
                tickCounter.getGameTimeDeltaPartialTick(false),
                cameraState.pos.x(),
                cameraState.pos.y(),
                cameraState.pos.z()
        );
        Frosty.EVENT_BUS.post(event);
    }
}