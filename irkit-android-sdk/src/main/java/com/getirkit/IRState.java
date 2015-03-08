package com.getirkit;

/**
 * Represents one of "not finished" or "finished" state.
 */
public class IRState {
    public static final int STATE_NOT_FINISHED = 1;
    public static final int STATE_FINISHED = 2;

    private int state;

    public IRState() {
        this.state = STATE_NOT_FINISHED;
    }

    public boolean isFinished() {
        return this.state == STATE_FINISHED;
    }

    public void finish() {
        this.state = STATE_FINISHED;
    }
}
