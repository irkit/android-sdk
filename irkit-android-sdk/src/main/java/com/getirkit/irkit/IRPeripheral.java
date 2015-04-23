package com.getirkit.irkit;

import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.getirkit.irkit.net.IRDeviceAPIService;
import com.getirkit.irkit.net.IRHTTPClient;
import com.getirkit.irkit.net.IRInternetAPIService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.Date;
import java.util.HashMap;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Header;
import retrofit.client.Response;

/**
 * An IRKit device.
 * IRKitデバイスを表します。
 */
public class IRPeripheral implements Serializable, Parcelable {
    // Never change this or you'll get InvalidClassException!
    private static final long serialVersionUID = 1L;

    public transient static final String TAG = "IRPeripheral";

    /**
     * Hostname which uniquely identifies an IRKit device. Hostname will remain unchanged over time.
     * IRKitデバイスに固有のホスト名。ホスト名はIRKitをリセットしても変わりません。
     */
    private String hostname;

    /**
     * User-provided nickname.
     * ユーザが設定したニックネーム。
     */
    private String customizedName;

    /**
     * First found date on a local network.
     * このIRKitを初めてローカルネットワーク内に発見した日時。
     */
    private Date foundDate;

    /**
     * A deviceid which is assigned by IRKit Server.
     * IRKitサーバから割り当てられたdeviceid。
     */
    private String deviceId;

    /**
     * IRKit model name provided by Server header (e.g. "IRKit").
     * IRKitのモデル名。Device HTTP APIのServerヘッダから取得されます。
     */
    private String modelName;

    /**
     * IRKit firmware version provided by Server header (e.g. "2.0.2.0.g838e0ea").
     * ファームウェアバージョン。Device HTTP APIのServerヘッダから取得されます。
     */
    private String firmwareVersion;

    // transient == prevent the field from serializing
    private transient InetAddress host;
    private transient int port;
    private transient boolean isFetchingDeviceId = false;

    @Override
    public String toString() {
        return "IRPeripheral[hostname=" + hostname + ";deviceId=" + deviceId + ";customizedName=" + customizedName + ";modelName=" + modelName + ";firmwareVersion=" + firmwareVersion + ";host=" + host + ";port=" + port + "]";
    }

    public interface IRPeripheralListener {
        public void onErrorFetchingDeviceId(String message);
        public void onDeviceIdStatusChange();
        public void onFetchDeviceIdSuccess();
        public void onFetchModelInfoSuccess();
        public void onErrorFetchingModelInfo(String message);
    }

    // listener won't be packed in a Parcelable since it's transient
    private transient IRPeripheralListener listener;

    public IRPeripheralListener getListener() {
        return listener;
    }

    public void setListener(IRPeripheralListener listener) {
        this.listener = listener;
    }

    public IRPeripheral() {
        this.foundDate = new Date();
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getCustomizedName() {
        return customizedName;
    }

    public void setCustomizedName(String customizedName) {
        this.customizedName = customizedName;
    }

    public Date getFoundDate() {
        return foundDate;
    }

    public void setFoundDate(Date foundDate) {
        this.foundDate = foundDate;
    }

    public boolean hasDeviceId() {
        return deviceId != null;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public boolean hasModelInfo() {
        return modelName != null;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public boolean isFetchingDeviceId() {
        return isFetchingDeviceId;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public InetAddress getHost() {
        return host;
    }

    public void setHost(InetAddress host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Parse the value of Server header in Device HTTP API response.
     * modelName and firmwareVersion may be updated.
     * Device HTTP APIのレスポンスに含まれるServerヘッダの値を解釈します。
     * modelNameとfirmwareVersionの値が変化している場合はフィールドに保存します。
     *
     * @param server Server header value
     * @return True if modelName or firmwareVersion has modified.
     *         modelNameまたはfirmwareVersionの値が更新された場合はtrue。
     */
    public boolean parseServerValue(String server) {
        String[] params = server.split("/", 2);
        boolean isModified = false;
        if (params.length >= 2) {
            if (modelName == null || !modelName.equals(params[0])) {
                modelName = params[0];
                isModified = true;
            }
            if (firmwareVersion == null || !firmwareVersion.equals(params[1])) {
                firmwareVersion = params[1];
                isModified = true;
            }
        }
        return isModified;
    }

    /**
     * Parse headers in Device HTTP API response and store in fields if updated.
     * Device HTTP APIのレスポンスヘッダを解釈してフィールドを必要に応じて更新します。
     *
     * @param response Response object
     * @return  True if a field is modified. フィールドが更新された場合はtrue。
     */
    public boolean storeResponseHeaders(Response response) {
        for (Header header : response.getHeaders()) {
            String name = header.getName();
            if (name != null && name.toLowerCase().equals("server")) {
                String value = header.getValue();
                if (value != null) {
                    return parseServerValue(value);
                }
            }
        }
        return false;
    }

    /**
     * Fetch modelName and firmwareVersion.
     * modelNameとfirmwareVersionを取得します。
     */
    public void fetchModelInfo() {
        fetchModelInfo(0);
    }

    /**
     * Fetch modelName and firmwareVersion.
     * modelNameとfirmwareVersionを取得します。
     *
     * @param retryCount Max retry count. 最大リトライ数。
     */
    public void fetchModelInfo(final int retryCount) {
        if (!this.isLocalAddressResolved()) {
            Log.e(TAG, "fetchModelInfo: local address isn't resolved");
            if (listener != null) {
                listener.onErrorFetchingModelInfo("network error");
            }
            return;
        }
        if (retryCount > 5) {
            Log.e(TAG, "fetchModelInfo: exceeded max retry count");
            if (listener != null) {
                listener.onErrorFetchingModelInfo("error");
            }
            return;
        }
        IRHTTPClient httpClient = IRKit.sharedInstance().getHTTPClient();
        httpClient.setDeviceAPIEndpoint("http://" + this.host.getHostAddress() + ":" + this.port);
        IRDeviceAPIService deviceAPIService = httpClient.getDeviceAPIService();
        deviceAPIService.getMessages(new Callback<IRDeviceAPIService.GetMessagesResponse>() {
            @Override
            public void success(IRDeviceAPIService.GetMessagesResponse getMessagesResponse, Response response) {
                // fetchModelInfo success
                if (storeResponseHeaders(response)) {
                    IRKit.sharedInstance().peripherals.save();
                }
                if (listener != null) {
                    listener.onFetchModelInfoSuccess();
                }
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "device getMessages failure: " + error.getMessage());
                fetchModelInfo(retryCount + 1);
            }
        });
    }

    /**
     * Fetch deviceid by calling POST /keys.
     * POST /keys を呼んでdeviceidを取得します。
     *
     * @see <a href="http://getirkit.com/#toc_3">POST /keys</a>
     */
    public void fetchDeviceId() {
        fetchDeviceId(0);
    }

    /**
     * Fetch deviceid by calling POST /keys.
     * POST /keys を呼んでdeviceidを取得します。
     *
     * @param retryCount Current retry count. 現在のリトライ数。
     * @see <a href="http://getirkit.com/#toc_3">POST /keys</a>
     */
    private void fetchDeviceId(final int retryCount) {
        if (!isLocalAddressResolved()) {
            Log.e(TAG, "fetchDeviceId: local address isn't resolved");
            if (listener != null) {
                isFetchingDeviceId = false;
                listener.onErrorFetchingDeviceId("network error");
            }
            return;
        }
        if (retryCount > 5) {
            Log.e(TAG, "fetchDeviceId exceeded max retry count");
            if (listener != null) {
                isFetchingDeviceId = false;
                listener.onErrorFetchingDeviceId("network error");
            }
            return;
        }
        if (isFetchingDeviceId) {  // already fetching device id
            return;
        }
        isFetchingDeviceId = true;
        if (listener != null) {
            listener.onDeviceIdStatusChange();
        }
        IRHTTPClient.sharedInstance().setDeviceAPIEndpoint("http://" + this.host.getHostAddress() + ":" + this.port);
        IRDeviceAPIService deviceAPIService = IRHTTPClient.sharedInstance().getDeviceAPIService();
        deviceAPIService.postKeys(new Callback<IRDeviceAPIService.PostKeysResponse>() {
            @Override
            public void success(IRDeviceAPIService.PostKeysResponse postKeysResponse, Response response) {
                if (storeResponseHeaders(response)) {
                    IRKit.sharedInstance().peripherals.save();
                }

                HashMap<String, String> params = new HashMap<>();
                params.put("clienttoken", postKeysResponse.clienttoken);
                params.put("clientkey", IRHTTPClient.sharedInstance().getClientKey());
                IRInternetAPIService internetAPIService = IRHTTPClient.sharedInstance().getInternetAPIService();
                internetAPIService.postKeys(params, new Callback<IRInternetAPIService.PostKeysResponse>() {
                    @Override
                    public void success(IRInternetAPIService.PostKeysResponse postKeysResponse, Response response) {
                        // Assigned a device id
                        IRPeripheral.this.setDeviceId(postKeysResponse.deviceid);
                        IRKit.sharedInstance().peripherals.save();
                        if (listener != null) {
                            listener.onFetchDeviceIdSuccess();
                        }
                        isFetchingDeviceId = false;
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Log.e(TAG, "internet postKeys failure: " + error.getMessage());
                        isFetchingDeviceId = false;
                    }
                });
            }

            @Override
            public void failure(RetrofitError error) {
                // Retry
                Log.w(TAG, "local postkeys failure: message=" + error.getMessage() +
                        " kind=" + error.getKind() + "; retrying");
                isFetchingDeviceId = false;
                fetchDeviceId(retryCount + 1);
            }
        });
    }

    /**
     * Return a local network endpoint for this IRKit device.
     * このIRKitデバイスにローカルネットワーク内で接続するためのエンドポイントを返します。
     *
     * @return A string like "http://host:port", or null if this peripheral is not found on local network.
     *         "http://host:port" のような文字列。またはIRKitがローカルネットワーク内に見つからない場合はnull。
     */
    public String getDeviceAPIEndpoint() {
        if ( isLocalAddressResolved() ) {
            return "http://" + host.getHostAddress() + ":" + port;
        } else {
            return null;
        }
    }

    /**
     * Return whether this IRKit is found on local network.
     * IRKitがローカルネットワーク内に見つかっているかどうかを返します。
     *
     * @return True if IRKit is found on local network. IRKitがローカルネットワーク内に検出済みの場合はtrue。
     */
    public boolean isLocalAddressResolved() {
        return host != null;
    }

    /**
     * Test whether this IRKit is reachable on local network. Blocks up to 100 milliseconds.
     * IRKitへローカルネットワーク内で到達可能かどうかをテストします。最大で100ミリ秒ブロックします。
     *
     * @return True if IRKit is reachable. IRKitに到達可能な場合はtrue。
     */
    public boolean isReachable() {
        if ( isLocalAddressResolved() ) {
            try {
                return host.isReachable(100);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Callback interface for testReachability().
     * testReachability()で使用するコールバック用インタフェースです。
     *
     * @see IRPeripheral#testReachability(ReachabilityResult)
     */
    public interface ReachabilityResult {
        /**
         * Called when IRKit is reachable on local network. Device HTTP API is available.
         * IRKitがローカルネットワーク内で到達可能な時に呼ばれます。この場合はDevice HTTP APIを利用できます。
         */
        public void reachable();

        /**
         * Called when IRKit is not reachable on local network. Device HTTP API is unavailable.
         * IRKitがローカルネットワーク内で到達できない時に呼ばれます。この場合はDevice HTTP APIを利用できません。
         */
        public void notReachable();
    }

    /**
     * Test asynchronously whether this IRKit is reachable on local network.
     * IRKitにローカルネットワーク内で到達可能かどうかを非同期にテストします。
     *
     * @param result ReachabilityResult object
     */
    public void testReachability(final ReachabilityResult result) {
        if ( !isLocalAddressResolved() ) {
            result.notReachable();
            return;
        }

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    if (host.isReachable(100)) {
                        result.reachable();
                        return null;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                result.notReachable();
                return null;
            }
        }.execute();
    }

    /**
     * Call this method when IRKit is no longer reachable on local network.
     * IRKitにローカルネットワーク内で到達できなくなった際にこのメソッドを呼びます。
     */
    public void lostLocalAddress() {
        this.host = null;
        this.port = 0;
    }

    public JSONObject toJSONObject() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("deviceid", deviceId);
            obj.put("customizedName", customizedName);
            obj.put("hostname", hostname);
            obj.put("foundDate", foundDate.getTime() / 1000);
            obj.put("version", firmwareVersion);
            obj.put("modelName", modelName);
            obj.put("regdomain", IRKit.getRegDomainForDefaultLocale());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(hostname);
        out.writeString(customizedName);
        out.writeSerializable(foundDate);
        out.writeString(deviceId);
        out.writeString(modelName);
        out.writeString(firmwareVersion);
        out.writeSerializable(host);
        out.writeInt(port);
        out.writeByte((byte) (isFetchingDeviceId ? 1 : 0));
    }

    public static final Creator<IRPeripheral> CREATOR = new Creator<IRPeripheral>() {
        @Override
        public IRPeripheral createFromParcel(Parcel in) {
            return new IRPeripheral(in);
        }

        @Override
        public IRPeripheral[] newArray(int size) {
            return new IRPeripheral[size];
        }
    };

    private IRPeripheral(Parcel in) {
        hostname = in.readString();
        customizedName = in.readString();
        foundDate = (Date) in.readSerializable();
        deviceId = in.readString();
        modelName = in.readString();
        firmwareVersion = in.readString();
        host = (InetAddress) in.readSerializable();
        port = in.readInt();
        isFetchingDeviceId = in.readByte() != 0;
    }
}
