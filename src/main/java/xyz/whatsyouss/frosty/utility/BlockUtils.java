package xyz.whatsyouss.frosty.utility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import static xyz.whatsyouss.frosty.Frosty.mc;

public class BlockUtils {

    public static Direction getDirection(BlockPos pos) {
        Vec3 eyesPos = new Vec3(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());
        if ((double) pos.getY() > eyesPos.y) {
            if (mc.level.getBlockState(pos.offset(0, -1, 0)).canBeReplaced()) return Direction.DOWN;
            else return mc.player.getDirection().getOpposite();
        }
        if (!mc.level.getBlockState(pos.offset(0, 1, 0)).canBeReplaced()) return mc.player.getDirection().getOpposite();
        return Direction.UP;
    }

    public static boolean isEdgeOfBlock(final double posX, final double posY, final double posZ) {
        int blockY = Mth.floor(posY);
        if (posY % 1.0 == 0.0) {
            blockY -= 1;
        }

        BlockPos pos = new BlockPos(Mth.floor(posX), blockY, Mth.floor(posZ));

        return mc.level.getBlockState(pos).isAir();
    }

    public static boolean isEdgeOfBlock() {
        return isEdgeOfBlock(mc.player.getX(), mc.player.getY(), mc.player.getZ());
    }
}
