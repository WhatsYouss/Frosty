package xyz.whatsyouss.frosty.mixin.accessor;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;


@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Accessor("missTime")
    int frosty$getMissTime();

    @Accessor("missTime")
    void frosty$setMissTime(int attackCooldown);

    @Invoker("startAttack")
    boolean frosty$leftClick();

    @Accessor("rightClickDelay")
    int frosty$getRightClickDelay();

    @Accessor("rightClickDelay")
    void frosty$setRightClickDelay(int itemUseCooldown);
}
