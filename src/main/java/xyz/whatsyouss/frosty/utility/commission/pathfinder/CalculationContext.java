package xyz.whatsyouss.frosty.utility.commission.pathfinder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.state.BlockState;

public final class CalculationContext {
    public final ClientLevel world;
    public final LocalPlayer player;
    public final BlockStateAccessor bsa;
    public final ActionCosts cost;
    public final int maxFallHeight = 17;
    public CalculationContext(double sprintFactor, double walkFactor, double sneakFactor) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) throw new IllegalStateException("World or player not available");
        world = mc.level; player = mc.player; bsa = new BlockStateAccessor(world);
        var effect = player.getEffect(MobEffects.JUMP_BOOST);
        cost = new ActionCosts(sprintFactor, walkFactor, sneakFactor, effect == null ? -1 : effect.getAmplifier());
    }
    public BlockState get(int x, int y, int z) { return bsa.get(x, y, z); }
}
