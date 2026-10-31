package xyz.whatsyouss.frosty.mixin;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import xyz.whatsyouss.frosty.interfaces.IInput;

@Mixin(ClientInput.class)
public abstract class ClientInputMixin implements IInput {

    @Shadow
    public Input keyPresses;

    @Shadow
    public abstract Vec2 getMoveVector();

    @Unique
    protected Input initial = Input.EMPTY;

    @Unique
    protected Input untransformed = Input.EMPTY;

    @Override
    public Input frosty$getInitial() {
        return initial;
    }

    @Override
    public Input frosty$getUntransformed() {
        return untransformed;
    }

}