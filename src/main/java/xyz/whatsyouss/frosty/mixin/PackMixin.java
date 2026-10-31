package xyz.whatsyouss.frosty.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Pack.class)
public class PackMixin {
    @Shadow @Final private PackLocationInfo location;

    @ModifyReturnValue(method = "getDefaultPosition", at = @At("RETURN"))
    private Pack.Position adjustDefaultPosition(Pack.Position original) {
        if (original == Pack.Position.TOP && location.id().startsWith("server/")) {
            return Pack.Position.BOTTOM;
        }
        return original;
    }
}