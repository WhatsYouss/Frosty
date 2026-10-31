package xyz.whatsyouss.frosty.events.impl;

import xyz.whatsyouss.frosty.utility.DirectionalInput;

public class SprintEvent {
    private final DirectionalInput directionalInput;
    private boolean sprint;
    private final Source source;

    public SprintEvent(DirectionalInput directionalInput, boolean sprint, Source source) {
        this.directionalInput = directionalInput;
        this.sprint = sprint;
        this.source = source;
    }

    public DirectionalInput getDirectionalInput() {
        return directionalInput;
    }

    public boolean isSprint() {
        return sprint;
    }

    public boolean getSprint() {
        return sprint;
    }

    public void setSprint(boolean sprint) {
        this.sprint = sprint;
    }

    public Source getSource() {
        return source;
    }

    public enum Source {
        INPUT,
        MOVEMENT_TICK,
        NETWORK
    }
}