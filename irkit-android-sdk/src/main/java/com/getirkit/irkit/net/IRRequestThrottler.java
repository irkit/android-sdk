package com.getirkit.irkit.net;

import android.os.Handler;
import android.util.Log;

import com.getirkit.irkit.IRPeripheral;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.FieldMap;
import retrofit.http.QueryMap;
import retrofit.mime.TypedInput;

/**
 * Device HTTP APIとInternet HTTP APIにスロットル制御付きでアクセスするためのクラスです。
 * A class which does request throttling for Device HTTP API and Internet HTTP API.
 */
public class IRRequestThrottler {
    public static final String TAG = IRRequestThrottler.class.getSimpleName();

    public static final int DELAY_BETWEEN_REQUESTS_MS = 1000;
    private final ArrayDeque<APICall> pendingCalls = new ArrayDeque<>();

    private IRDeviceAPIService deviceAPIService;
    private IRInternetAPIService internetAPIService;
    private IRDeviceAPIRequester deviceAPIRequester;
    private IRInternetAPIRequester internetAPIRequester;

    private static final HashMap<String, IRRequestThrottler> throttlers = new HashMap<>();

    /**
     * 単一のAPI呼び出し。
     */
    private static class APICall {
        private enum Method {
            DEVICE_POST_KEYS,
            DEVICE_GET_MESSAGES,
            DEVICE_POST_MESSAGES,
            DEVICE_POST_WIFI,
            DEVICE_GET_HOME,
            INTERNET_GET_MESSAGES,
            INTERNET_POST_MESSAGES,
        }

        private Object requestParameters;
        private Callback callback;
        private Method method;

        public APICall(Method method, Object requestParameters, Callback callback) {
            this.method = method;
            this.requestParameters = requestParameters;
            this.callback = callback;
        }

        @Override
        public String toString() {
            return "DeviceAPICall[method=" + method + "]";
        }
    }

    /**
     * peripheralを対象とするIRRequestThrottlerを返します。
     * Returns an IRRequestThrottler for sending requests to the peripheral.
     *
     * @param peripheral 対象とするIRKitデバイス。 Target IRKit device.
     * @param deviceAPIService IRDeviceAPIService instance.
     * @param internetAPIService IRInternetAPIService instance.
     * @return
     */
    public static IRRequestThrottler getThrottler(IRPeripheral peripheral, IRDeviceAPIService deviceAPIService, IRInternetAPIService internetAPIService) {
        return getThrottler(peripheral.getDeviceId(), deviceAPIService, internetAPIService);
    }

    /**
     * deviceIdを対象とするIRRequestThrottlerを返します。
     * Returns an IRRequestThrottler for sending requests to the deviceId.
     *
     * @param deviceId 対象とするIRKitデバイスのdeviceid。 Deviceid of target IRKit device.
     * @param deviceAPIService IRDeviceAPIService instance.
     * @param internetAPIService IRInternetAPIService instance.
     * @return
     */
    public static IRRequestThrottler getThrottler(String deviceId, IRDeviceAPIService deviceAPIService, IRInternetAPIService internetAPIService) {
        IRRequestThrottler throttler;
        synchronized (throttlers) {
            throttler = throttlers.get(deviceId);
            if (throttler == null) {
                throttler = new IRRequestThrottler(deviceAPIService, internetAPIService);
                throttlers.put(deviceId, throttler);
            }
        }
        return throttler;
    }

    public IRRequestThrottler(IRDeviceAPIService deviceAPIService, IRInternetAPIService internetAPIService) {
        this.deviceAPIService = deviceAPIService;
        this.internetAPIService = internetAPIService;
        this.deviceAPIRequester = new IRDeviceAPIRequester();
        this.internetAPIRequester = new IRInternetAPIRequester();
    }

    /**
     * Device HTTP APIにアクセスするためのクラスを返します。そのメソッドを呼ぶ際、適宜
     * スロットル制御が有効になります。
     *
     * Returns a class which sends requests to Device HTTP API. When the methods of
     * the returned object are called, request throttling will be automatically enabled.
     *
     * @return IRDeviceAPIServiceインタフェースを実装したクラス。
     */
    public IRDeviceAPIRequester getDeviceAPIRequester() {
        return deviceAPIRequester;
    }

    /**
     * Internet HTTP APIにアクセスするためのクラスを返します。そのメソッドを呼ぶ際、適宜
     * スロットル制御が有効になります。
     *
     * Returns a class which sends requests to Internet HTTP API. When the methods of
     * the returned object are called, request throttling will be automatically enabled.
     *
     * @return IRInternetAPIServiceインタフェースを実装したクラス。
     */
    public IRInternetAPIRequester getInternetAPIRequester() {
        return internetAPIRequester;
    }

    private <T> Callback<T> createCallback(final Callback<T> callback) {
        return new Callback<T>() {
            @Override
            public void success(T t, Response response) {
                onRequestDone();
                callback.success(t, response);
            }

            @Override
            public void failure(RetrofitError error) {
                onRequestDone();
                callback.failure(error);
            }
        };
    }

    /**
     * API呼び出しが1つ終わるたびに呼び出されるメソッドです。
     * Called when an API call has been done.
     */
    private void onRequestDone() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                synchronized (pendingCalls) {
                    pendingCalls.poll();
                    Log.d(TAG, "remaining pending calls: " + pendingCalls.size());
                }
                consumeNextRequest();
            }
        }, DELAY_BETWEEN_REQUESTS_MS);
    }

    /**
     * リクエストをキューに追加します。
     * Add a request to the queue.
     *
     * @param call キューに追加するAPI呼び出し。 API call which will be added to the queue.
     */
    private void request(APICall call) {
        Log.d(TAG, "request: " + call);
        boolean consumeRequest = false;
        synchronized (pendingCalls) {
            Log.d(TAG, "pendingCalls.add");
            pendingCalls.add(call);
            if (pendingCalls.size() == 1) {
                consumeRequest = true;
            }
        }
        if (consumeRequest) {
            consumeNextRequest();
        }
    }

    /**
     * 待機中の次のAPICallを処理します。
     */
    private void consumeNextRequest() {
        APICall call = null;
        synchronized (pendingCalls) {
            if (!pendingCalls.isEmpty()) {
                call = pendingCalls.peek();
            }
        }
        if (call != null) {
            switch (call.method) {
                case DEVICE_POST_KEYS:
                    deviceAPIService.postKeys(call.callback);
                    break;
                case DEVICE_GET_MESSAGES:
                    deviceAPIService.getMessages(call.callback);
                    break;
                case DEVICE_POST_MESSAGES:
                    deviceAPIService.postMessages((IRDeviceAPIService.PostMessagesRequest) call.requestParameters,
                            call.callback);
                    break;
                case DEVICE_POST_WIFI:
                    deviceAPIService.postWifi((TypedInput) call.requestParameters, call.callback);
                    break;
                case DEVICE_GET_HOME:
                    deviceAPIService.getHome(call.callback);
                    break;
                case INTERNET_GET_MESSAGES:
                    internetAPIService.getMessages((Map<String, String>) call.requestParameters, call.callback);
                    break;
                case INTERNET_POST_MESSAGES:
                    internetAPIService.postMessages((Map<String, String>) call.requestParameters, call.callback);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown method: " + call.method);
            }
        }
    }

    /**
     * IRDeviceAPIServiceにスロットル制御をかけるためのクラスです。
     */
    private class IRDeviceAPIRequester implements IRDeviceAPIService {
        @Override
        public void postKeys(Callback<PostKeysResponse> callback) {
            request(new APICall(APICall.Method.DEVICE_POST_KEYS, null, createCallback(callback)));
        }

        @Override
        public void getMessages(Callback<GetMessagesResponse> callback) {
            request(new APICall(APICall.Method.DEVICE_GET_MESSAGES, null, createCallback(callback)));
        }

        @Override
        public void postMessages(@Body PostMessagesRequest request, Callback<PostMessagesResponse> callback) {
            request(new APICall(APICall.Method.DEVICE_POST_MESSAGES, request, createCallback(callback)));
        }

        @Override
        public void postWifi(@Body TypedInput body, Callback<PostWifiResponse> callback) {
            request(new APICall(APICall.Method.DEVICE_POST_WIFI, body, createCallback(callback)));
        }

        @Override
        public void getHome(Callback<GetHomeResponse> callback) {
            request(new APICall(APICall.Method.DEVICE_GET_HOME, null, createCallback(callback)));
        }
    }

    /**
     * IRInternetAPIServiceの一部にスロットル制御をかけるためのクラスです。
     */
    private class IRInternetAPIRequester implements IRInternetAPIService {
        @Override
        public void getMessages(@QueryMap Map<String, String> params, Callback<GetMessagesResponse> callback) {
            // This API may not need throttling
            internetAPIService.getMessages(params, callback);
        }

        @Override
        public void postMessages(@FieldMap Map<String, String> params, Callback<PostMessagesResponse> callback) {
            request(new APICall(APICall.Method.INTERNET_POST_MESSAGES, params, createCallback(callback)));
        }

        @Override
        public void postClients(@FieldMap Map<String, String> params, Callback<PostClientsResponse> callback) {
            // This API does not need throttling
            internetAPIService.postClients(params, callback);
        }

        @Override
        public void postKeys(@FieldMap Map<String, String> params, Callback<PostKeysResponse> callback) {
            // This API does not need throttling
            internetAPIService.postKeys(params, callback);
        }

        @Override
        public void postDevices(@FieldMap Map<String, String> params, Callback<PostDevicesResponse> callback) {
            // This API does not need throttling
            internetAPIService.postDevices(params, callback);
        }

        @Override
        public void postDoor(@FieldMap Map<String, String> params, Callback<PostDoorResponse> callback) {
            // This API does not need throttling
            internetAPIService.postDoor(params, callback);
        }

        @Override
        public void getRecommendedGooglePlayApps(Callback<GetRecommendedGooglePlayAppsResponse> callback) {
            // This API does not need throttling
            internetAPIService.getRecommendedGooglePlayApps(callback);
        }

        @Override
        public void getRecommendedAmazonApps(Callback<GetRecommendedAmazonAppsResponse> callback) {
            // This API does not need throttling
            internetAPIService.getRecommendedAmazonApps(callback);
        }

        @Override
        public void getRecommendediOSApps(Callback<GetRecommendediOSAppsResponse> callback) {
            // This API does not need throttling
            internetAPIService.getRecommendediOSApps(callback);
        }

        @Override
        public void postApps(@FieldMap Map<String, String> params, Callback<PostAppsResponse> callback) {
            // This API does not need throttling
            internetAPIService.postApps(params, callback);
        }
    }
}
