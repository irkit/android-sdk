package com.getirkit.irkit.net;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.mime.TypedInput;

/**
 * Device HTTP API
 */
public interface IRDeviceAPIService {
    public static class PostWifiResponse {
    }

    public static class PostMessagesRequest {
        public String format;
        public float freq;
        public int[] data;
    }

    public static class PostMessagesResponse {
    }

    /**
     * clienttoken を取得します。
     *
     * @param callback
     */
    @POST("/keys")
    void postKeys(Callback<PostKeysResponse> callback);

    /**
     * 最も新しい受信した赤外線信号を返します。
     *
     * Long pollingは不可。
     *
     * @param callback
     */
    @GET("/messages")
    void getMessages(Callback<GetMessagesResponse> callback);

    /**
     * 赤外線信号を送ります。
     *
     * @param request
     * @param callback
     */
    @POST("/messages")
    void postMessages(@Body PostMessagesRequest request, Callback<PostMessagesResponse> callback);

    /**
     * IRKitをWi-Fiに接続させる。
     *
     * @param callback
     */
    @POST("/wifi")
    void postWifi(@Body TypedInput body, Callback<PostWifiResponse> callback);

    /**
     * Response of getMessages
     */
    class GetMessagesResponse {
        public String format;
        public double freq;
        public int[] data;
    }

    /**
     * Response of postKeys
     */
    class PostKeysResponse {
        public String clienttoken;
    }
}
