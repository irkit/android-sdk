package com.getirkit.irkit.net;

import android.os.Handler;
import android.util.Log;

import com.getirkit.irkit.IRKit;
import com.getirkit.irkit.IRPeripheral;
import com.getirkit.irkit.IRPeripherals;
import com.getirkit.irkit.IRSignal;
import com.getirkit.irkit.IRState;
import com.getirkit.irkit.IRWifiInfo;
import com.squareup.okhttp.OkHttpClient;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Header;
import retrofit.client.OkClient;
import retrofit.client.Response;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedString;

/**
 * Device HTTP APIとInternet HTTP APIを利用するためのクラスです。
 * Client for HTTP API and Internet HTTP API.
 */
public class IRHTTPClient {
    public static final String TAG = IRHTTPClient.class.getSimpleName();

    /**
     * Internet HTTP APIのエンドポイントです。
     * Endpoint for Internet HTTP API.
     */
    public static final String APIENDPOINT_BASE = "https://api.getirkit.com";

    /**
     * IRKit Wi-Fiに接続しているときのDevice HTTP APIのエンドポイントです。
     * Endpoint for Device HTTP API when IRKit Wi-Fi is active.
     */
    public static final String DEVICE_API_ENDPOINT_IRKITWIFI = "http://192.168.1.1";

    /**
     * Device HTTP APIのリクエストに付けるX-Requested-Withヘッダの値。
     * Value of X-Requested-With header which is added to every request to Device HTTP API.
     */
    public static final String DEVICE_API_X_REQUESTED_WITH = "IRKit Android SDK";

    // Retrofit
    private RestAdapter internetRestAdapter;
    private RestAdapter deviceRestAdapter;
    private IRInternetAPIService internetAPIService;
    private IRDeviceAPIService deviceAPIService;

    private String clientkey;
    private OkHttpClient internetHttpClient;
    private OkHttpClient localHttpClient;
    private Date lastRequestDate;
    private IRInternetAPIService.PostDevicesResponse holdingPostDevicesResponse;
    private Date lastPostDoorRequestDate;
    private IRDeviceEndpoint deviceEndpoint;

    // singleton
    private static IRHTTPClient ourInstance = new IRHTTPClient();
    public static IRHTTPClient sharedInstance() {
        return ourInstance;
    }

    /**
     * コンストラクタです。インスタンスを取得するときはsharedInstance()メソッドを使ってください。
     * Constructor. To get an instance, use sharedInstance().
     *
     * @see IRHTTPClient#sharedInstance()
     */
    public IRHTTPClient() {
        internetHttpClient = new OkHttpClient();

        // TODO: choose timeout values wisely
        internetHttpClient.setConnectTimeout(10, TimeUnit.SECONDS);
        internetHttpClient.setReadTimeout(0, TimeUnit.SECONDS);

        localHttpClient = new OkHttpClient();
        localHttpClient.setConnectTimeout(5, TimeUnit.SECONDS);
        // A request may take 2-24 seconds
        localHttpClient.setReadTimeout(30, TimeUnit.SECONDS);
        localHttpClient.getDispatcher().setMaxRequestsPerHost(1);

        internetRestAdapter = new RestAdapter.Builder()
                .setClient(new OkClient(internetHttpClient))
                .setEndpoint(APIENDPOINT_BASE)
//                .setLogLevel(RestAdapter.LogLevel.FULL)
                .build();
        internetAPIService = internetRestAdapter.create(IRInternetAPIService.class);

        deviceEndpoint = new IRDeviceEndpoint();
        deviceEndpoint.setUrl(DEVICE_API_ENDPOINT_IRKITWIFI);

        // Add X-Requested-With header to every request to Device HTTP API
        RequestInterceptor requestInterceptor = new RequestInterceptor() {
            @Override
            public void intercept(RequestFacade request) {
                request.addHeader("X-Requested-With", DEVICE_API_X_REQUESTED_WITH);
            }
        };

        deviceRestAdapter = new RestAdapter.Builder()
                .setClient(new OkClient(localHttpClient))
                .setEndpoint(deviceEndpoint)
                .setRequestInterceptor(requestInterceptor)
//                .setLogLevel(RestAdapter.LogLevel.FULL)
                .build();
        deviceAPIService = deviceRestAdapter.create(IRDeviceAPIService.class);
    }

    /**
     * clientkeyをセットします。
     * Set a clientkey.
     *
     * @param key clientkey
     */
    public void setClientKey(String key) {
        clientkey = key;
    }

    /**
     * Device HTTP APIのエンドポイントをセットします。
     * Set an endpoint for Device HTTP API.
     *
     * @param endpoint A string like "http://127.0.0.1"
     */
    public void setDeviceAPIEndpoint(String endpoint) {
        deviceEndpoint.setUrl(endpoint);
    }

    /**
     * apikeyを元にclientkeyを取得します。
     * Fetch clientkey using apikey.
     *
     * @param apiKey apikey
     * @param callback 結果を受け取るコールバック。 Callback to be notified a result.
     */
    public void registerClient(String apiKey, final IRAPICallback<IRInternetAPIService.PostClientsResponse> callback) {
        HashMap<String, String> params = new HashMap<>();
        params.put("apikey", apiKey);
        internetAPIService.postClients(params, new Callback<IRInternetAPIService.PostClientsResponse>() {
            @Override
            public void success(IRInternetAPIService.PostClientsResponse postClientsResponse, Response response) {
                clientkey = postClientsResponse.clientkey;
                IRKit.sharedInstance().savePreference("clientkey", clientkey);
                callback.success(postClientsResponse, response);
            }

            @Override
            public void failure(RetrofitError error) {
                IRAPIError apiError = (IRAPIError) error.getBodyAs(IRAPIError.class);
                Log.e(TAG, "registerClient failed: " + error.getMessage());
                if (apiError != null) {
                    Log.e(TAG, "API error message: " + apiError.message);
                }
                callback.failure(error);
            }
        });
    }

    /**
     * clientkeyを取得していない場合は取得します。
     * Fetch clientkey if it is not fetched yet.
     *
     * @param apiKey apikey
     * @param callback 結果を受け取るコールバック。 Callback to be notified a result.
     */
    public void ensureRegisteredAndCall(String apiKey, IRAPICallback<IRInternetAPIService.PostClientsResponse> callback) {
        if (clientkey == null) {
            IRHTTPClient.sharedInstance().registerClient(apiKey, callback);
        } else {
            callback.success(null, null);
        }
    }

    /**
     * Internet HTTP APIで赤外線信号を送信します。
     * Send signal over Internet HTTP API.
     *
     * @param signal IRSignal
     * @param callback 結果を受け取るコールバック。 Callback to be notified a result.
     */
    public void sendSignalOverInternet(IRSignal signal, final IRAPICallback<IRInternetAPIService.PostMessagesResponse> callback) {
        HashMap<String, String> params = new HashMap<>();
        params.put("deviceid", signal.getDeviceId());
        params.put("message", signal.toJson());
        this.addClientKey(params);
        getThrottledInternetAPIService(signal.getDeviceId()).postMessages(params, new IRAPICallback<IRInternetAPIService.PostMessagesResponse>() {
            @Override
            public void success(IRInternetAPIService.PostMessagesResponse postMessagesResponse, Response response) {
                if (callback != null) {
                    callback.success(postMessagesResponse, response);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                if (callback != null) {
                    callback.failure(error);
                }
            }
        });
    }

    /**
     * Device HTTP APIで赤外線信号を送信します。
     * Send IRSignal over Device HTTP API.
     *
     * @param signal IRSignal
     * @param result 結果を受け取るコールバック。 Callback to be notified a result.
     */
    public void sendSignalOverLocalNetwork(final IRSignal signal, final IRAPIResult result) {
        IRDeviceAPIService.PostMessagesRequest request = new IRDeviceAPIService.PostMessagesRequest();
        request.format = signal.getFormat();
        request.freq = signal.getFrequency();
        request.data = signal.getData();

        getThrottledDeviceAPIService(signal.getDeviceId()).postMessages(request, new Callback<IRDeviceAPIService.PostMessagesResponse>() {
            @Override
            public void success(IRDeviceAPIService.PostMessagesResponse postMessagesResponse, Response response) {
                IRPeripherals peripherals = IRKit.sharedInstance().peripherals;
                IRPeripheral peripheral = peripherals.getPeripheralByDeviceId(signal.getDeviceId());
                if (peripheral != null) {
                    if (peripheral.storeResponseHeaders(response)) {
                        peripherals.save();
                    }
                }

                if (result != null) {
                    result.onSuccess();
                }
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "device postMessages failure: message=" + error.getMessage() + " kind=" + error.getKind());
                if (error.getKind() == RetrofitError.Kind.NETWORK) { // network error
                    if (result != null) {
                        result.onTimeout();
                    }
                } else {
                    if (result != null) {
                        result.onError(new IRAPIError(error.getMessage()));
                    }
                }
            }
        });
    }

    /**
     * IRKitサーバに保存されている最新の赤外線信号を削除して、新しく赤外線信号を待機します。
     * Clear the IR signal saved in IRKit server, then wait for a new IR signal.
     *
     * @param callback 結果を受け取るコールバック。 Callback to be notified a result.
     */
    public void waitForSignal(IRAPICallback<IRInternetAPIService.GetMessagesResponse> callback) {
        waitForSignal(callback, true);
    }

    /**
     * 赤外線信号を受信します。
     * Receive an IR signal.
     *
     * @param callback 結果を受け取るコールバック。 Callback to be notified a result.
     * @param clear trueの場合、IRKitサーバに保存されている信号を削除して、新しい信号を待機します。
     *              If true, delete the IR signal saved in IRKit server, then wait for a new IR signal.
     */
    public void waitForSignal(final IRAPICallback<IRInternetAPIService.GetMessagesResponse> callback, boolean clear) {
        HashMap<String, String> params = new HashMap<>(2);
        if (clear) {
            params.put("clear", "1");
        }
        this.addClientKey(params);

        final Date requestDate = lastRequestDate = new Date();
        internetAPIService.getMessages(params, new Callback<IRInternetAPIService.GetMessagesResponse>() {
            @Override
            public void success(IRInternetAPIService.GetMessagesResponse getMessagesResponse, Response response) {
                if (lastRequestDate == null) {
                    // The request has been cancelled. Discard this response.
                    return;
                }
                if (lastRequestDate.after(requestDate)) {
                    // The request is obsolete. Discard this response.
                    return;
                }
                if (getMessagesResponse == null) {
                    // Server returned null response. Try again.
                    IRHTTPClient.sharedInstance().waitForSignal(callback, false);
                } else {
                    callback.success(getMessagesResponse, response);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "internet getMessages failure: " + error.getMessage());
                callback.failure(error);
            }
        });
    }

    /**
     * <p class="ja">
     * waitForSignal()をキャンセルします。現在進行中のwaitForSignal()については
     * コールバックは呼ばれません。
     * </p>
     *
     * <p class="en">
     * Cancel waitForSignal(). The callback passed to ongoing waitForSignal()
     * will never be called.
     * </p>
     */
    public void cancelRequests() {
        lastRequestDate = null;
    }

    /**
     * 引数のparamsにclientkeyを追加します。
     * Add clientkey to the given params.
     *
     * @param params Map object
     */
    public void addClientKey(Map<String, String> params) {
        if (clientkey != null) {
            params.put("clientkey", clientkey);
        }
    }

    /**
     * <p class="ja">
     * Internet HTTP APIのPOST /1/devicesのレスポンスはキャッシュされますが、
     * そのキャッシュを削除します。IRHTTPClientインスタンスを作成するたびに
     * キャッシュは空になります。
     * </p>
     *
     * <p class="en">
     * Delete the cached response of POST /1/devices. A newly-created
     * IRHTTPClient instance does not have a cached response. When it
     * receives the response of POST /1/devices of Internet HTTP API,
     * it will be cached.
     * </p>
     */
    public void clearDeviceKeyCache() {
        holdingPostDevicesResponse = null;
    }

    /**
     * devicekeyを取得します。
     * Fetch a devicekey.
     *
     * @param callback devicekey取得結果を受け取るコールバック。
     *                 Callback to be notified a result which contains devicekey.
     */
    public void obtainDeviceKey(final IRAPICallback<IRInternetAPIService.PostDevicesResponse> callback) {
        if (clientkey == null) {
            throw new IllegalStateException("clientkey is not set");
        }
        if (holdingPostDevicesResponse != null) {
            // We already have devicekey/id
            callback.success(holdingPostDevicesResponse, null);
            return;
        }
        HashMap<String, String> params = new HashMap<>(1);
        addClientKey(params);
        internetAPIService.postDevices(params, new Callback<IRInternetAPIService.PostDevicesResponse>() {
            @Override
            public void success(IRInternetAPIService.PostDevicesResponse postDevicesResponse, Response response) {
                holdingPostDevicesResponse = postDevicesResponse;
                callback.success(postDevicesResponse, response);
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "failed to get devicekey: " + error.getMessage());
                callback.failure(error);
            }
        });
    }

    /**
     * IRKitをWi-Fiに接続させます。Device HTTP APIが使える場合のみ動作します。
     * Connect an IRKit to Wi-Fi. This method works only if Device HTTP API is available.
     *
     * @param irWifiInfo 接続先のWi-Fi情報。 Target Wi-Fi.
     * @param callback 結果を受け取るコールバック。 Callback to be notified a result.
     */
    public void connectDeviceToWifi(IRWifiInfo irWifiInfo, final IRAPICallback<IRDeviceAPIService.PostWifiResponse> callback) {
        if (holdingPostDevicesResponse == null) {
            throw new IllegalStateException("holdingPostDevicesResponse is null");
        }
        String morseString = irWifiInfo.createMorseString(holdingPostDevicesResponse.devicekey);
        TypedInput in = new TypedString(morseString);
        deviceAPIService.postWifi(in, new Callback<IRDeviceAPIService.PostWifiResponse>() {
            @Override
            public void success(IRDeviceAPIService.PostWifiResponse postWifiResponse, Response response) {
                clearDeviceKeyCache();
                callback.success(postWifiResponse, response);
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "postWifi failure: " + error.getMessage());
                callback.failure(error);
            }
        });
    }

    /**
     * waitForDoor()をキャンセルします。
     * Cancel previously called waitForDoor().
     */
    public void cancelPostDoor() {
        lastPostDoorRequestDate = null;
    }

    /**
     * <p class="ja">
     * POST /1/doorを呼んで結果を待機します。サーバ側でタイムアウトして408が返ってきた場合は再度リクエストして待機します。
     * </p>
     *
     * <p class="en">
     * Call POST/1/door and wait for a result. When server returns 408 (server-side timeout), another
     * request will be sent and this method will keep waiting.
     * </p>
     *
     * @param deviceId deviceid
     * @param callback 結果を受け取るコールバック。 Callback to be notified a result.
     */
    public void waitForDoor(final String deviceId, final IRAPICallback<IRInternetAPIService.PostDoorResponse> callback) {
        HashMap<String, String> params = new HashMap<>(2);
        addClientKey(params);
        params.put("deviceid", deviceId);
        final Date requestDate = lastPostDoorRequestDate = new Date();
        internetAPIService.postDoor(params, new Callback<IRInternetAPIService.PostDoorResponse>() {
            @Override
            public void success(IRInternetAPIService.PostDoorResponse postDoorResponse, Response response) {
                if (lastPostDoorRequestDate == null) {
                    // This request has been canceled. Discard this response.
                    return;
                }
                if (lastPostDoorRequestDate.after(requestDate)) {
                    // This request is obsolete. Discard this response.
                    return;
                }
                if (postDoorResponse.hostname == null) {
                    // Empty response. Retry.
                    waitForDoor(deviceId, callback);
                } else {
                    // Success
                    callback.success(postDoorResponse, response);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                if (lastPostDoorRequestDate == null) {
                    // This request has been canceled. Discard this response.
                    return;
                }
                if (lastPostDoorRequestDate.after(requestDate)) {
                    // This request is obsolete. Discard this response.
                    return;
                }
                if (error != null && error.getResponse() != null) {
                    int statusCode = error.getResponse().getStatus();
                    // IRKit server returns 408 when /door didn't success in a certain amount of time
                    if (statusCode >= 400 && statusCode < 500) {
                        // Retry postDoor
                        waitForDoor(deviceId, callback);
                    } else {
                        Log.e(TAG, "postDoor error: statusCode=" + statusCode);
                        callback.failure(error);
                    }
                } else {
                    Log.e(TAG, "postDoor failure: " + error);
                    callback.failure(error);
                }
            }
        });
    }

    /**
     * Checks if the response header denotes IRKit.
     *
     * @param response Response object
     * @param result If the response header denotes IRKit, onSuccess() will be called.
     *               Otherwise onError() will be called with a null argument.
     */
    private void checkIfResponseDenotesIRKit(Response response, IRAPIResult result) {
        for (Header header : response.getHeaders()) {
            String name = header.getName();
            if (name != null && name.toLowerCase().equals("server")) {
                String value = header.getValue();
                if (value != null) {
                    Map<String, String> map = IRPeripheral.parseServerHeaderValue(value);
                    String modelName = map.get("modelName");
                    if (modelName != null && modelName.equals(IRPeripheral.IRKIT_MODEL_NAME)) {
                        result.onSuccess();
                        return;
                    }
                }
            }
        }
        result.onError(null);
    }

    /**
     * 192.168.1.1にリクエストを発行してレスポンスヘッダに"IRKit"が含まれていることを確認します。
     * Sends a request to 192.168.1.1 and checks if the response header contains "IRKit".
     *
     * @param result レスポンスヘッダにIRKitが含まれていればonSuccess()、含まれていなければonError()が呼ばれる。
     *               If the response header contains "IRKit", onSuccess() will be called.
     *               Otherwise onError() will be called.
     * @since 1.2.0
     */
    public void testIfIRKitWifiConnected(final IRAPIResult result) {
        setDeviceAPIEndpoint(DEVICE_API_ENDPOINT_IRKITWIFI);

        final IRState state = new IRState();
        final Handler handler = new Handler();
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                boolean isTimedOut = false;
                synchronized (state) {
                    if ( !state.isFinished() ) {
                        state.finish();
                        isTimedOut = true;
                    }
                }
                if (isTimedOut) {
                    Log.e(TAG, "testIfIRKitWifiConnected: timeout");
                    result.onTimeout();
                }
            }
        };
        handler.postDelayed(r, 3000);

        // TODO: throttle?
        deviceAPIService.getHome(new Callback<IRDeviceAPIService.GetHomeResponse>() {
            @Override
            public void success(IRDeviceAPIService.GetHomeResponse getRootResponse, Response response) {
                boolean isTimedOut = false;
                synchronized (state) {
                    if (state.isFinished()) {
                        isTimedOut = true;
                    } else {
                        state.finish();
                    }
                }
                if (!isTimedOut) {
                    handler.removeCallbacks(r);
                    checkIfResponseDenotesIRKit(response, result);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                // Actually this is success (IRKit returns 404 when a clients requests /)

                boolean isTimedOut = false;
                synchronized (state) {
                    if (state.isFinished()) {
                        isTimedOut = true;
                    } else {
                        state.finish();
                    }
                }
                if (!isTimedOut) {
                    handler.removeCallbacks(r);
                    checkIfResponseDenotesIRKit(error.getResponse(), result);
                }
            }
        });
    }

    /**
     * Internet HTTP APIを直接利用するためのインスタンスを返します。
     * Return the instance which provides direct access to Internet HTTP API.
     *
     * @return Internet HTTP APIを提供するインスタンス。 Instance which provides Internet HTTP API.
     */
    public IRInternetAPIService getInternetAPIService() {
        return internetAPIService;
    }

    /**
     * Device HTTP APIを直接利用するためのインスタンスを返します。
     * Return the instance which provides direct access to Device HTTP API.
     *
     * @return Device HTTP APIを提供するインスタンス。 Instance which provides Device HTTP API.
     */
    public IRDeviceAPIService getDeviceAPIService() {
        return deviceAPIService;
    }

    /**
     * ランダムな文字列を返します。
     * Returns a random string.
     *
     * @return ランダムな文字列。 A random string.
     */
    private String getRandomString() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 特定のIRKitに対してスロットル制御を行いながらDevice HTTP APIにアクセスするためのオブジェクトを返します。
     * Returns an object which accesses Device HTTP API with request throttling.
     *
     * @param peripheral 対象となるIRKitデバイス。 Target IRKit device.
     * @return IRDeviceAPIService instance.
     * @since 1.2.1
     */
    public IRDeviceAPIService getThrottledDeviceAPIService(IRPeripheral peripheral) {
        return IRRequestThrottler.getThrottler(peripheral, deviceAPIService, internetAPIService).getDeviceAPIRequester();
    }

    /**
     * 特定のIRKitに対してスロットル制御を行いながらDevice HTTP APIにアクセスするためのオブジェクトを返します。
     * Returns an object which accesses Device HTTP API with request throttling.
     *
     * @param deviceId 対象となるIRKitデバイスのdeviceid。 Deviceid of the target IRKit device.
     * @return IRDeviceAPIService instance.
     * @since 1.2.1
     */
    public IRDeviceAPIService getThrottledDeviceAPIService(String deviceId) {
        return IRRequestThrottler.getThrottler(deviceId, deviceAPIService, internetAPIService).getDeviceAPIRequester();
    }

    /**
     * スロットル制御を行いながらDevice HTTP APIにアクセスするためのオブジェクトを返します。
     * Returns an object which accesses Device HTTP API with request throttling.
     *
     * @return IRDeviceAPIService instance.
     * @since 1.2.2
     */
    public IRDeviceAPIService getThrottledDeviceAPIService() {
        return IRRequestThrottler.getThrottler(getRandomString(), deviceAPIService, internetAPIService).getDeviceAPIRequester();
    }

    /**
     * 特定のIRKitに対してスロットル制御を行いながらInternet HTTP APIにアクセスするためのオブジェクトを返します。
     * Returns an object which accesses Internet HTTP API with request throttling.
     *
     * @param peripheral 対象となるIRKitデバイス。 Target IRKit device.
     * @return IRInternetAPIService instance.
     * @since 1.2.1
     */
    public IRInternetAPIService getThrottledInternetAPIService(IRPeripheral peripheral) {
        return IRRequestThrottler.getThrottler(peripheral, deviceAPIService, internetAPIService).getInternetAPIRequester();
    }

    /**
     * 特定のIRKitに対してスロットル制御を行いながらInternet HTTP APIにアクセスするためのオブジェクトを返します。
     * Returns an object which accesses Internet HTTP API with request throttling.
     *
     * @param deviceId 対象となるIRKitデバイスのdeviceid。 Deviceid of the target IRKit device.
     * @return IRInternetAPIService instance.
     * @since 1.2.1
     */
    public IRInternetAPIService getThrottledInternetAPIService(String deviceId) {
        return IRRequestThrottler.getThrottler(deviceId, deviceAPIService, internetAPIService).getInternetAPIRequester();
    }

    /**
     * スロットル制御を行いながらInternet HTTP APIにアクセスするためのオブジェクトを返します。
     * Returns an object which accesses Internet HTTP API with request throttling.
     *
     * @return IRInternetAPIService instance.
     * @since 1.2.2
     */
    public IRInternetAPIService getThrottledInternetAPIService() {
        return IRRequestThrottler.getThrottler(getRandomString(), deviceAPIService, internetAPIService).getInternetAPIRequester();
    }

    /**
     * clientkeyを返します。
     * Return the clientkey.
     *
     * @return clientkey
     */
    public String getClientKey() {
        return clientkey;
    }

    /**
     * clientkeyがセット済みかどうかを返します。
     * Return whether clientkey has been set.
     *
     * @return clientkeyがセット済みならtrue。 True if clientkey has been set.
     */
    public boolean hasClientKey() {
        return clientkey != null;
    }
}
