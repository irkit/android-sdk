package com.getirkit.irkit.net;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

import retrofit.Callback;
import retrofit.http.FieldMap;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.QueryMap;
import retrofit.mime.TypedInput;

/**
 * IRKit Internet HTTP APIのインタフェースです。
 * Interface for IRKit Internet HTTP API.
 *
 * <p>Example:
 * <pre><code>
 * // Get Internet HTTP API service
 * IRInternetAPIService internetAPI = IRKit.sharedInstance().getHTTPClient().getInternetAPIService();
 *
 * // Request parameters
 * HashMap<String, String> params = new HashMap<>();
 * params.put("clientkey", "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
 * params.put("deviceid", "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
 * params.put("message", "{\"format\":\"raw\",\"freq\":38,\"data\":[18031,8755,1190,1190,1190, ]}");
 *
 * // Send POST /1/messages
 * internetAPI.postMessages(params, new Callback<IRInternetAPIService.PostMessagesResponse>() {
 *     {@literal @}Override
 *     public void success(IRInternetAPIService.PostMessagesResponse postMessagesResponse, Response response) {
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
 * @see <a href="http://getirkit.com/#IRKit-Internet-API">IRKit Internet HTTP API</a>
 */
public interface IRInternetAPIService {
    /**
     * getMessages()のレスポンスです。
     * Response of getMessages().
     *
     * @see #getMessages(Map, Callback)
     */
    class GetMessagesResponse {
        public IRDeviceAPIService.GetMessagesResponse message;
        public String hostname;
        public String deviceid;
    }

    /**
     * 最も新しい受信した赤外線信号を返します。 このリクエストは、ロングポーリングなリクエストです。 clear を指定すると、過去にIRKitデバイスがサーバに送信しサーバで保存している赤外線信号を消去し、 新しい赤外線信号がIRKitデバイスから届いたらただちにレスポンスを返します。 規定値でタイムアウトすると空のレスポンスを返します。
     *
     * 赤外線信号を学習するシーンでは、最初に clear=1 をつけてリクエストをした後、リクエストがタイムアウトしたら clear パラメータを付与せずに再度リクエストするとよいでしょう。
     * リクエストパラメータの clientkey と関連するIRKitデバイスが複数ある場合には、 レスポンスに含まれる deviceid と hostname を使ってどのIRKitが赤外線信号を受信したかを識別します。
     *
     * @param params
     * @param callback
     */
    @GET("/1/messages")
    void getMessages(@QueryMap Map<String, String> params, Callback<GetMessagesResponse> callback);

    /**
     * postMessages()のレスポンスです。
     * Response of postMessages().
     *
     * @see #postMessages(Map, Callback)
     */
    class PostMessagesResponse {
    }

    /**
     * 赤外線信号を deviceid で指定するIRKitデバイスから送信します。
     *
     * @param params
     * @param callback
     */
    @FormUrlEncoded
    @POST("/1/messages")
    void postMessages(@FieldMap Map<String, String> params, Callback<PostMessagesResponse> callback);

    /**
     * postClients()のレスポンスです。
     * Response of postClients().
     *
     * @see #postClients(Map, Callback)
     * @since 1.1.2
     */
    class PostClientsResponse {
        @SerializedName("clientkey")
        public String clientkey;
    }

    /**
     * clientkey を作成します。
     *
     * @param params
     * @param callback
     */
    @FormUrlEncoded
    @POST("/1/clients")
    void postClients(@FieldMap Map<String, String> params, Callback<PostClientsResponse> callback);

    /**
     * postKeys()のレスポンスです。
     * Response of postKeys().
     *
     * @see #postKeys(Map, Callback)
     */
    class PostKeysResponse {
        public String deviceid;
        public String clientkey;
    }

    /**
     * deviceid を取得するために使います。 clientkey リクエストパラメータを付加すると、この clientkey と deviceid を関連づけてサーバ側に保存します。
     *
     * @param params
     * @param callback
     */
    @FormUrlEncoded
    @POST("/1/keys")
    void postKeys(@FieldMap Map<String, String> params, Callback<PostKeysResponse> callback);

    /**
     * postDevices()のレスポンスです。
     * Response of postDevices().
     *
     * @see #postDevices(Map, Callback)
     */
    class PostDevicesResponse {
        public String devicekey;
        public String deviceid;
    }

    /**
     * devicekey, deviceid を作成します。
     *
     * この後にDevice APIのPOST /wifiを呼びます。
     *
     * @see IRDeviceAPIService#postWifi(TypedInput, Callback)
     *
     * @param params
     * @param callback
     */
    @FormUrlEncoded
    @POST("/1/devices")
    void postDevices(@FieldMap Map<String, String> params, Callback<PostDevicesResponse> callback);

    /**
     * postDoor()のレスポンスです。
     * Response of postDoor().
     *
     * @see #postDoor(Map, Callback)
     */
    class PostDoorResponse {
        /**
         * Bonjour を使うことで同じWiFiアクセスポイントに接続したクライアントから #{hostname}.local として接続するために使います。
         */
        public String hostname;
    }

    /**
     * IRKitのアクセスポイントを使用して、家のWiFiアクセスポイントの認証情報とともにdevicekeyをIRKitデバイスに送った後(POST /wifi)、POST /1/doorを使用して、IRKitデバイスが正常に家のWiFiアクセスポイントを通してインターネットに接続できたことを確認します。
     *
     * このリクエストは、ロングポーリングなリクエストです。 IRKitデバイスが正常にインターネット上のAPIサーバに接続できるとただちにレスポンスを返します。 規定値でタイムアウトすると空のレスポンスを返します。
     *
     * @param params
     * @param callback
     */
    @FormUrlEncoded
    @POST("/1/door")
    void postDoor(@FieldMap Map<String, String> params, Callback<PostDoorResponse> callback);

    /**
     * GetRecommendedGooglePlayAppsResponseとGetRecommendedAmazonAppsResponseに含まれるappsの1エントリです。
     * An entry in apps of GetRecommendedGooglePlayAppsResponse and GetRecommendedAmazonAppsResponse.
     *
     * @see GetRecommendedAmazonAppsResponse
     * @see GetRecommendedGooglePlayAppsResponse
     */
    class AndroidApp {
        public String title_ja;
        public String title_en;
        public String image_url;
        public String detail_ja;
        public String detail_en;
        public String package_name;
    }

    /**
     * getRecommendedGooglePlayApps()のレスポンスです。
     * Response of getRecommendedGooglePlayApps().
     *
     * @see #getRecommendedGooglePlayApps(Callback)
     */
    class GetRecommendedGooglePlayAppsResponse {
        public AndroidApp[] apps;
    }

    /**
     * getRecommendedAmazonApps()のレスポンスです。
     * Response of getRecommendedAmazonApps().
     *
     * @see #getRecommendedAmazonApps(Callback)
     */
    class GetRecommendedAmazonAppsResponse {
        public AndroidApp[] apps;
    }

    /**
     * おすすめGoogle Playアプリの一覧を取得します（非公開API）。
     *
     * @param callback
     */
    @GET("/1/apps/recommended/android")
    void getRecommendedGooglePlayApps(Callback<GetRecommendedGooglePlayAppsResponse> callback);

    /**
     * おすすめAmazon Appstoreアプリの一覧を取得します（非公開API）。
     *
     * @param callback
     */
    @GET("/1/apps/recommended/amazon")
    void getRecommendedAmazonApps(Callback<GetRecommendedAmazonAppsResponse> callback);

    /**
     * getRecommendediOSApps()のレスポンスです。
     * Response of getRecommendediOSApps().
     *
     * @see #getRecommendediOSApps(Callback)
     */
    class GetRecommendediOSAppsResponse {
        public App[] apps;

        public static class App {
            public String title_ja;
            public String title_en;
            public String image_url;
            public String detail_ja;
            public String detail_en;
            public String appstore_url;
        }
    }

    /**
     * おすすめiOSアプリの一覧を取得します（非公開API）。
     *
     * @param callback
     */
    @GET("/1/apps/recommended")
    void getRecommendediOSApps(Callback<GetRecommendediOSAppsResponse> callback);

    /**
     * postApps()のレスポンスです。
     * Response of postApps().
     *
     * @see #postApps(Map, Callback)
     * @since 1.1.2
     */
    class PostAppsResponse {
        public String message;
    }

    /**
     * apikeyを作成します。apikeyは通常、開発時に一度だけ取得してアプリに埋め込んでおきます。
     * 公開用アプリ内でこのメソッドを使う場合は、その必要性があるかよく確認してください。
     *
     * @param callback レスポンスを受け取るコールバック。 Callback for receiving a response.
     * @since 1.1.2
     */
    @FormUrlEncoded
    @POST("/1/apps")
    void postApps(@FieldMap Map<String, String> params, Callback<PostAppsResponse> callback);
}
