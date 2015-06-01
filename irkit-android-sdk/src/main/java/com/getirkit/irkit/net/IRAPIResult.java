package com.getirkit.irkit.net;

/**
 * General callback
 */
public interface IRAPIResult {
    void onSuccess();
    void onError(IRAPIError error);
    void onTimeout();
}
