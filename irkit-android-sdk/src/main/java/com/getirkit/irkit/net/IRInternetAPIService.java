package com.getirkit.irkit.net;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

import retrofit.Callback;
import retrofit.http.FieldMap;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.QueryMap;

/**
 * Internet HTTP API
 */
public interface IRInternetAPIService {
    public static class PostDevicesResponse {
        public String devicekey;
        public String deviceid;
    }

    public static class PostDoorResponse {
        /**
         * Bonjour を使うことで同じWiFiアクセスポイントに接続したクライアントから #{hostname}.local として接続するために使います。
         */
        public String hostname;
    }

    public static class GetRecommendediOSAppsResponse {
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

    public static class AndroidApp {
        public String title_ja;
        public String title_en;
        public String image_url;
        public String detail_ja;
        public String detail_en;
        public String package_name;
    }

    public static class GetRecommendedGooglePlayAppsResponse {
        public AndroidApp[] apps;
    }

    public static class GetRecommendedAmazonAppsResponse {
        public AndroidApp[] apps;
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
     * 赤外線信号を deviceid で指定するIRKitデバイスから送信します。
     *
     * @param params
     * @param callback
     */
    @FormUrlEncoded
    @POST("/1/messages")
    void postMessages(@FieldMap Map<String, String> params, Callback<PostMessagesResponse> callback);

    /**
     * clientkey を作成します。
     *
     * @param params
     * @param callback
     */
    @FormUrlEncoded
    @POST("/1/clients")
    void postClients(@FieldMap Map<String, String> params, Callback<GetClientsResponse> callback);

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
     * devicekey, deviceid を作成します。
     *
     * この後にDevice APIのpostWifiを呼ぶ。
     *
     * @param params
     * @param callback
     */
    @FormUrlEncoded
    @POST("/1/devices")
    void postDevices(@FieldMap Map<String, String> params, Callback<PostDevicesResponse> callback);

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
     * おすすめGoogle Playアプリの一覧を取得する。非公開API。
     *
     * @param callback
     */
    @GET("/1/apps/recommended/android")
    void getRecommendedGooglePlayApps(Callback<GetRecommendedGooglePlayAppsResponse> callback);

    /**
     * おすすめAmazon Appstoreアプリの一覧を取得する。非公開API。
     *
     * @param callback
     */
    @GET("/1/apps/recommended/amazon")
    void getRecommendedAmazonApps(Callback<GetRecommendedAmazonAppsResponse> callback);

    /**
     * おすすめiOSアプリの一覧を取得する。非公開API。
     *
     * @param callback
     */
    @GET("/1/apps/recommended")
    void getRecommendediOSApps(Callback<GetRecommendediOSAppsResponse> callback);

    /**
     * Response of getClients
     */
    class GetClientsResponse {
        @SerializedName("clientkey")
        public String clientkey;
    }

    /**
     * Response of getMessages
     */
    class GetMessagesResponse {
        public IRDeviceAPIService.GetMessagesResponse message;
        public String hostname;
        public String deviceid;
    }

    /**
     * Response of postKeys
     */
    class PostKeysResponse {
        public String deviceid;
        public String clientkey;
    }

    /**
     * Response of postMessages
     */
    class PostMessagesResponse {
    }
}
