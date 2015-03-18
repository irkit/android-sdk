package com.getirkit.irkit;

/**
 * Listener for use with IRKit class
 */
public interface IRKitEventListener {
    public void onNewIRKitFound(IRPeripheral peripheral);
    public void onExistingIRKitFound(IRPeripheral peripheral);
}
