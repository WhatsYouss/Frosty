package xyz.whatsyouss.frosty.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.input.Input;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import xyz.whatsyouss.frosty.interfaces.IInput;

@Mixin(Input.class)
public abstract class InputMixin implements IInput {

    @Shadow
    public PlayerInput playerInput;

    @Shadow
    public abstract Vec2f getMovementInput();

    @Unique
    protected PlayerInput initial = PlayerInput.DEFAULT;

    @Unique
    protected PlayerInput untransformed = PlayerInput.DEFAULT;

    @Override
    public PlayerInput frosty$getInitial() {
        return initial;
    }

    @Override
    public PlayerInput frosty$getUntransformed() {
        return untransformed;
    }

}