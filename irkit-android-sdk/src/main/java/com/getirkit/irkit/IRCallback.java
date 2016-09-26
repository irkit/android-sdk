package com.getirkit.irkit;

/**
 * 汎用的なコールバックインタフェースです。いずれかのメソッドが呼ばれると他のメソッドは呼ばれません。
 * An interface for general callbacks. Only one of the methods will be called.
 *
 * @since 1.3.1
 */
public interface IRCallback {
    /**
     * 正常終了した際に呼ばれます。
     * Called when processing has done without errors.
     *
     * @since 1.3.1
     */
    void onSuccess();

    /**
     * エラーが発生したため正常終了しなかった際に呼ばれます。
     * Called when an error occurs and processing cannot be done.
     *
     * @param ex 発生したエラー。 The error occurred during processing.
     * @since 1.3.1
     */
    void onError(Exception ex);

    /*
     * 一定時間経過しても処理が終了せずタイムアウトした際に呼ばれます。
     * Called when processing has not finished within a certain amount of time.
     *
     * @since 1.3.1
     */
    void onTimeout();
}
