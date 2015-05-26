package com.getirkit.irkit.net;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.mime.TypedInput;

/**
 * IRKit Device HTTP APIのインタフェースです。
 * Interface for IRKit Device HTTP API.
 *
 * <p>Example:
 * <pre><code>
 * IRHTTPClient httpClient = IRKit.sharedInstance().getHTTPClient();
 *
 * // Set base URL
 * httpClient.setDeviceAPIEndpoint("http://192.168.1.1"); // IRKit IP address
 * // Or from IRPeripheral
 * httpClient.setDeviceAPIEndpoint(peripheral.getDeviceAPIEndpoint());
 *
 * // Get Device HTTP API service
 * IRDeviceAPIService deviceAPI = httpClient.getDeviceAPIService();
 *
 * // Request parameters
 * IRDeviceAPIService.PostMessagesRequest req = new IRDeviceAPIService.PostMessagesRequest();
 * req.format = "raw";
 * req.freq = 38.0f;
 * req.data = new int[] {
 *     18031, 8755, 1190, 1190, 1190, // ...
 * };
 *
 * // Send POST /messages
 * deviceAPI.postMessages(req, new Callback<IRDeviceAPIService.PostMessagesResponse>() {
 *     {@literal @}Override
 *     public void success(IRDeviceAPIService.PostMessagesResponse postMessagesResponse, Response response) {
 *         // Success
 *     }
 *
 *     {@literal @}Override
 *     public void failure(RetrofitError error) {
 *         // Error
 *     }
 * });
 * </code></pre></p>
 *
 * @see <a href="http://getirkit.com/#IRKit-Device-API">IRKit Device HTTP API</a>
 */
public interface IRDeviceAPIService {
    /**
     * postKeys()のレスポンスです。
     * Response of postKeys().
     *
     * @see #postKeys(Callback)
     */
    class PostKeysResponse {
        public String clienttoken;
    }

    /**
     * clienttoken を取得します。
     *
     * @param callback
     */
    @POST("/keys")
    void postKeys(Callback<PostKeysResponse> callback);

    /**
     * getMessages()のレスポンスです。
     * Response of getMessages().
     *
     * @see #getMessages(Callback)
     */
    class GetMessagesResponse {
        public String format;
        public double freq;
        public int[] data;
    }

    /**
     * 最も新しい受信した赤外線信号を返します。Long pollingには対応していません。
     *
     * @param callback
     */
    @GET("/messages")
    void getMessages(Callback<GetMessagesResponse> callback);

    /**
     * postMessages()のリクエストパラメータです。
     * Request parameters for postMessages().
     *
     * @see #postMessages(PostMessagesRequest, Callback)
     */
    public static class PostMessagesRequest {
        public String format;
        public float freq;
        public int[] data;
    }

    /**
     * postMessages()のレスポンスです。
     * Response of postMessages().
     *
     * @see #postMessages(PostMessagesRequest, Callback)
     */
    public static class PostMessagesResponse {
    }

    /**
     * 赤外線信号を送信します。
     *
     * @param request
     * @param callback
     */
    @POST("/messages")
    void postMessages(@Body PostMessagesRequest request, Callback<PostMessagesResponse> callback);

    /**
     * postWifi()のレスポンスです。
     * Response of postWifi().
     *
     * @see #postWifi(TypedInput, Callback)
     */
    public static class PostWifiResponse {
    }

    /**
     * IRKitをWi-Fiに接続させます。
     *
     * @param callback
     */
    @POST("/wifi")
    void postWifi(@Body TypedInput body, Callback<PostWifiResponse> callback);

    /**
     * getHome()のレスポンスです。
     *
     * @see #getHome(Callback)
     */
    public static class GetHomeResponse {
    }

    /**
     * "/"（トップページ）にアクセスします。実質的な影響のないリクエストです。接続確認やヘッダ取得用に使います。
     * Fetches "/" (root). This request has no effect. Used for checking connection or retrieving headers.
     *
     * @param callback レスポンスを受け取るコールバック。 Callback for receiving the response.
     */
    @GET("/")
    void getHome(Callback<GetHomeResponse> callback);
}
