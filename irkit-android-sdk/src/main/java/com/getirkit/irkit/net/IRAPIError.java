package com.getirkit.irkit.net;

/**
 * API error
 */
public class IRAPIError {
    public String message;

    /**
     * Error code
     */
    public int code;

    public IRAPIError() {
    }

    public IRAPIError(String message) {
        this.message = message;
    }

    public IRAPIError(String message, int code) {
        this.message = message;
        this.code = code;
    }

    @Override
    public String toString() {
        return message;
    }
}
