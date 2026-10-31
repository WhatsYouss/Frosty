package xyz.whatsyouss.frosty.mixin;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.modules.impl.render.FreeLook;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @ModifyArgs(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V"))
    private void onAlignSetRotationArgs(Args args, float partialTicks) {
        FreeLook freeLook = ModuleManager.freeLook;
        if (FreeLook.freelooking) {
            args.set(0, freeLook.cameraYaw);
            args.set(1, freeLook.cameraPitch);
        }
    }
}
