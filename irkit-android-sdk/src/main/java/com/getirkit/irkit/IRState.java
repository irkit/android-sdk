package com.getirkit.irkit;

/**
 * 未完了または完了のいずれかの状態を表します。メソッドはいずれもスレッドセーフでは
 * ないため、複数のスレッドから参照する場合は適切に排他制御を行ってください。
 *
 * Represents either "not finished" or "finished" state. Methods of this
 * class are not thread-safe.
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
