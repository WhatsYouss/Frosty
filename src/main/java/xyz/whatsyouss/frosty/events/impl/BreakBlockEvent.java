package xyz.whatsyouss.frosty.events.impl;

import net.minecraft.core.BlockPos;
import xyz.whatsyouss.frosty.events.Cancellable;

public class BreakBlockEvent extends Cancellable {
    private static final BreakBlockEvent INSTANCE = new BreakBlockEvent();

    public BlockPos blockPos;

    public static BreakBlockEvent get(BlockPos blockPos) {
        INSTANCE.setCancelled(false);
        INSTANCE.blockPos = blockPos;
        return INSTANCE;
    }
}