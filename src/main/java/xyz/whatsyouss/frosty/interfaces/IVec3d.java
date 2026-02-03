package xyz.whatsyouss.frosty.interfaces;

import net.minecraft.util.math.Vec3i;
import org.joml.Vector3d;

public interface IVec3d {
    void frosty$set(double x, double y, double z);

    default void frosty$set(Vec3i vec) {
        frosty$set(vec.getX(), vec.getY(), vec.getZ());
    }

    default void frosty$set(Vector3d vec) {
        frosty$set(vec.x, vec.y, vec.z);
    }

    void frosty$setXZ(double x, double z);

    void frosty$setY(double y);
}