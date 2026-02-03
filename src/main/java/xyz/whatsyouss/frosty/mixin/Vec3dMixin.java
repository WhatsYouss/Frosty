package xyz.whatsyouss.frosty.mixin;

import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import xyz.whatsyouss.frosty.interfaces.IVec3d;

@Mixin(Vec3d.class)
public abstract class Vec3dMixin implements IVec3d {
    @Shadow @Final @Mutable public double x;
    @Shadow @Final @Mutable public double y;
    @Shadow @Final @Mutable public double z;

    @Override
    public void frosty$set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void frosty$setXZ(double x, double z) {
        this.x = x;
        this.z = z;
    }

    @Override
    public void frosty$setY(double y) {
        this.y = y;
    }
}