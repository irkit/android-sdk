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
 * IRKitデバイスを表す。
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
     * IRKitのモデル名。Device HTTP APIのServerヘッダに含まれる。
     */
    private String modelName;

    /**
     * IRKit firmware version provided by Server header (e.g. "2.0.2.0.g838e0ea").
     * ファームウェアバージョン。Device HTTP APIのServerヘッダに含まれる。
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
     * Device HTTP APIのレスポンスに含まれるServerヘッダの値を解釈する。
     * modelNameとfirmwareVersionの値が変化している場合はフィールドに保存する。
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
     * Device HTTP APIのレスポンスヘッダを解釈してフィールドを必要に応じて更新する。
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
     * Fetch modelName and firmwareVersion using Device HTTP API.
     * modelNameとfirmwareVersionをDevice HTTP APIを利用して取得する。
     */
    public void fetchModelInfo() {
        fetchModelInfo(0);
    }

    /**
     * Fetch modelName and firmwareVersion using Device HTTP API.
     * modelNameとfirmwareVersionをDevice HTTP APIを利用して取得する。
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

    public void fetchDeviceId() {
        fetchDeviceId(0);
    }

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

    public String getDeviceAPIEndpoint() {
        if ( isLocalAddressResolved() ) {
            return "http://" + host.getHostAddress() + ":" + port;
        } else {
            return null;
        }
    }

    public boolean isLocalAddressResolved() {
        return host != null;
    }

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

    public interface ReachabilityResult {
        public void reachable();
        public void notReachable();
    }

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
