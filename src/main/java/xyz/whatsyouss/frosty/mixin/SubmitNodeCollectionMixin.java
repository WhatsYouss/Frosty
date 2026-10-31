package xyz.whatsyouss.frosty.mixin;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.feature.submit.TranslucentSubmit;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.modules.impl.render.Nametags;

@Mixin(SubmitNodeCollection.class)
public class SubmitNodeCollectionMixin {

    @WrapOperation(
            method = "submitNameTag(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZILnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"))
    private void wrapLabelScale(PoseStack matrices, float x, float y, float z,
                                Operation<Void> original, PoseStack matrices2,
                                @Nullable Vec3 nameTagAttachment, int offset, Component name,
                                boolean seeThrough, int lightCoords, CameraRenderState camera)


    {
        Nametags nametags = ModuleManager.nametags;
        if(!nametags.isEnabled())
        {
            original.call(matrices, x, y, z);
            return;
        }

        float scale = (float) (0.025F * nametags.scale.getInput());
        Matrix4f pose = new Matrix4f(matrices.last().pose());
        double distance =
                Math.sqrt(TranslucentSubmit.computeDistanceToCameraSq(pose));
        if (distance > 10)
            scale *= distance / 10;

        original.call(matrices, scale, -scale, scale);
    }

    @ModifyVariable(
            method = "submitNameTag(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZILnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD"),
            argsOnly = true)
    private boolean forceNotSneaking(boolean notSneaking) {
        return notSneaking || ModuleManager.nametags.isEnabled();
    }
}