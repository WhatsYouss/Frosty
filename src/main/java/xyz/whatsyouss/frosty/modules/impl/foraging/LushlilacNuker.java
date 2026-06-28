package xyz.whatsyouss.frosty.modules.impl.foraging;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.BlockUtils;
import xyz.whatsyouss.frosty.utility.Utils;

import java.util.Map;
import java.util.Objects;

public class LushlilacNuker extends Module {

    private SliderSetting range;

    private long lastBreakTime;

    public LushlilacNuker() {
        super("LushlilacNuker", "丁香光环", category.Foraging);
        this.registerSetting(range = new SliderSetting("Range", 4.2, 3, 6, 0.1));
    }

    @Override
    public void onUpdate() {
        if (!Utils.nullCheck()) {
            return;
        }
        Map<String, String> location = Utils.getCurrentLocation();
        if (!Objects.equals(location.get("Area"), "Galatea")) {
            return;
        }

        double rangeValue = range.getInput();
        BlockPos playerPos = mc.player.blockPosition();

        for (int x = (int) -rangeValue; x <= rangeValue; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = (int) -rangeValue; z <= rangeValue; z++) {
                    BlockPos targetPos = playerPos.offset(x, y, z);
                    if (mc.level.getBlockState(targetPos).getBlock() == Blocks.FLOWERING_AZALEA) {
                        breakBlock(targetPos);
                    }
                }
            }
        }
    }

    private void breakBlock(BlockPos pos) {
        if (System.currentTimeMillis() - lastBreakTime < 50) {
            return;
        }

        mc.gameMode.startDestroyBlock(pos, BlockUtils.getDirection(pos));
        mc.player.swing(InteractionHand.MAIN_HAND);

        lastBreakTime = System.currentTimeMillis();
    }
}