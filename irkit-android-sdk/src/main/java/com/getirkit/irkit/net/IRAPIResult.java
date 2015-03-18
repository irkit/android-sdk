package com.getirkit.irkit.net;

/**
 * General callback
 */
public interface IRAPIResult {
    public void onSuccess();
    public void onError(IRAPIError error);
    public void onTimeout();
}
