package xyz.whatsyouss.frosty.utility;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;

public final class DirectionalInput {
    private final boolean forwards;
    private final boolean backwards;
    private final boolean left;
    private final boolean right;

    public static final DirectionalInput NONE = new DirectionalInput(false, false, false, false);
    public static final DirectionalInput FORWARDS = new DirectionalInput(true, false, false, false);
    public static final DirectionalInput BACKWARDS = new DirectionalInput(false, true, false, false);
    public static final DirectionalInput LEFT = new DirectionalInput(false, false, true, false);
    public static final DirectionalInput RIGHT = new DirectionalInput(false, false, false, true);
    public static final DirectionalInput FORWARDS_LEFT = new DirectionalInput(true, false, true, false);
    public static final DirectionalInput FORWARDS_RIGHT = new DirectionalInput(true, false, false, true);
    public static final DirectionalInput BACKWARDS_LEFT = new DirectionalInput(false, true, true, false);
    public static final DirectionalInput BACKWARDS_RIGHT = new DirectionalInput(false, true, false, true);

    public DirectionalInput(boolean forwards, boolean backwards, boolean left, boolean right) {
        this.forwards = forwards;
        this.backwards = backwards;
        this.left = left;
        this.right = right;
    }

    public DirectionalInput(Input input) {
        this(
                input.getMovementInput().y > 0.0,  // forwards (W)
                input.getMovementInput().y < 0.0,  // backwards (S)
                input.getMovementInput().x < 0.0,  // left (A)
                input.getMovementInput().x > 0.0    // right (D)
        );
    }

    public DirectionalInput(PlayerInput input) {
        this(input.forward(), input.backward(), input.left(), input.right());
    }

    public DirectionalInput(float movementForward, float movementSideways) {
        this(movementForward > 0.0,
                movementForward < 0.0,
                movementSideways > 0.0,
                movementSideways < 0.0);
    }

    public DirectionalInput invert() {
        return new DirectionalInput(backwards, forwards, right, left);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DirectionalInput)) return false;
        DirectionalInput that = (DirectionalInput) other;
        return forwards == that.forwards &&
                backwards == that.backwards &&
                left == that.left &&
                right == that.right;
    }

    @Override
    public int hashCode() {
        int result = Boolean.hashCode(forwards);
        result = 30 * result + Boolean.hashCode(backwards);
        result = 30 * result + Boolean.hashCode(left);
        result = 30 * result + Boolean.hashCode(right);
        return result;
    }

    public boolean getForwards() {
        return forwards;
    }

    public boolean getBackwards() {
        return backwards;
    }

    public boolean getLeft() {
        return left;
    }

    public boolean getRight() {
        return right;
    }

    public boolean isMoving() {
        return (forwards && !backwards) || (backwards && !forwards) ||
                (left && !right) || (right && !left);
    }

    public boolean isForwards() {
        return forwards;
    }

    public boolean isBackwards() {
        return backwards;
    }

    public boolean isLeft() {
        return left;
    }

    public boolean isRight() {
        return right;
    }
}