package com.getirkit.irkit.net;

import com.google.gson.annotations.SerializedName;

/**
 * API error
 */
public class IRAPIError {
    @SerializedName("message")
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
