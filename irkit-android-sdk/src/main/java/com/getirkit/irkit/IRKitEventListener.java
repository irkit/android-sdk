package com.getirkit.irkit;

/**
 * IRKit発見イベントを受け取るリスナインタフェースです。
 * Listener class to be notified IRKit discovery events.
 */
public interface IRKitEventListener {
    /**
     * <p class="ja">
     * IRKit.sharedInstance().peripheralsに保存されていないIRKitを
     * ローカルネットワーク上に発見した場合に呼ばれます。
     * </p>
     *
     * <p class="en">
     * Called when SDK found an IRKit which is not in
     * IRKit.sharedInstance().peripheral on local network.
     * </p>
     *
     * @param peripheral 新しく発見したIRKitデバイス。 Newly found IRKit device.
     */
    void onNewIRKitFound(IRPeripheral peripheral);

    /**
     * <p class="ja">
     * IRKit.sharedInstance().peripheralsに保存されているIRKitを
     * ローカルネットワーク上に発見した場合に呼ばれます。
     * </p>
     *
     * <p class="en">
     * Called when SDK found an IRKit which is in
     * IRKit.sharedInstance().peripheral on local network.
     * </p>
     *
     * @param peripheral 発見したIRKitデバイス。 Found IRKit device.
     */
    void onExistingIRKitFound(IRPeripheral peripheral);
}
