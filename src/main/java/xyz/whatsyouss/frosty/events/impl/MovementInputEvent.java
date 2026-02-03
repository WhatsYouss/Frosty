package xyz.whatsyouss.frosty.events.impl;

import xyz.whatsyouss.frosty.utility.DirectionalInput;

public class MovementInputEvent {
    private DirectionalInput directionalInput;
    private boolean jump;
    private boolean sneak;

    public MovementInputEvent(DirectionalInput directionalInput, boolean jump, boolean sneak) {
        this.directionalInput = directionalInput;
        this.jump = jump;
        this.sneak = sneak;
    }

    public DirectionalInput getDirectionalInput() {
        return directionalInput;
    }

    public void setDirectionalInput(DirectionalInput directionalInput) {
        this.directionalInput = directionalInput;
    }

    public boolean getJump() {
        return jump;
    }

    public boolean getSneak() {
        return sneak;
    }

    public boolean isJump() {
        return jump;
    }

    public void setJump(boolean jump) {
        this.jump = jump;
    }

    public boolean isSneak() {
        return sneak;
    }

    public void setSneak(boolean sneak) {
        this.sneak = sneak;
    }
}
