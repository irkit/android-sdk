package com.getirkit.irkit;

public interface IRCallback {
    void onSuccess();
    void onError(Exception ex);
    void onTimeout();
}
