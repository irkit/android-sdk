package com.getirkit.irkit;

import android.app.backup.BackupManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import com.getirkit.irkit.net.IRAPICallback;
import com.getirkit.irkit.net.IRAPIError;
import com.getirkit.irkit.net.IRAPIResult;
import com.getirkit.irkit.net.IRHTTPClient;
import com.getirkit.irkit.net.IRInternetAPIService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * IRKit library main class
 */
public class IRKit {
    public String TAG;
    public static final String SERVICE_TYPE = "_irkit._tcp.local.";
    public static final String PREF_KEY_BONJOUR_HOSTNAME = "debuginfo.bonjour.hostname";
    public static final String PREF_KEY_BONJOUR_RESOLVED_AT = "debuginfo.bonjour.resolved_at";
    private static final int SEND_SIGNAL_LOCAL_TIMEOUT_MS = 3000;

    /**
     * IRPeripherals instance which holds existing IRPeripheral instances.
     * 既存のIRPeripheralインスタンスが格納されたIRPeripheralsインスタンス。
     */
    public IRPeripherals peripherals;

    /**
     * IRSignals instance which holds existing IRSignal instances.
     * 既存のIRSignalインスタンスが格納されたIRSignalsインスタンス。
     */
    public IRSignals signals;

    private Context context;
    private IRKitEventListener irkitEventListener;
    private IRHTTPClient httpClient;
    private boolean isDataLoaded = false;
    private WifiManager wifiManager;
    private ScanResultReceiver scanResultReceiver;
    private WifiEnableEventReceiver wifiEnableEventReceiver;
    private boolean isDiscovering = false;
    private LinkedList<Boolean> discoveryQueue;
    private boolean isProcessingBonjour = false;
    private boolean isInitialized = false;
    private IRKitSetupManager setupManager;
    private NetworkStateChangeReceiver networkStateChangeReceiver;

    // For JmDNS
    private WifiManager.MulticastLock multicastLock;
    private JmDNS jmdns;
    private BonjourServiceListener bonjourServiceListener;

    // singleton
    private static IRKit ourInstance = new IRKit();

    /**
     * Returns a singleton instance.
     * singletonのインスタンスを返します。
     *
     * @return
     */
    public static IRKit sharedInstance() {
        return ourInstance;
    }

    public IRHTTPClient getHTTPClient() {
        return httpClient;
    }

    private ArrayDeque<SendSignalItem> sendSignalQueue = new ArrayDeque<>();

    private IRKit() {
        httpClient = IRHTTPClient.sharedInstance();
        TAG = IRKit.class.getSimpleName() + ":" + this.hashCode();
    }

    /**
     * Load data from shared preferences
     */
    public void loadData() {
        if (!isDataLoaded) {
            peripherals = new IRPeripherals();
            peripherals.load();

            signals = new IRSignals();
            signals.load();
            signals.updateImageResourceIdFromName(context.getResources());
            if (!signals.checkIdOverlap()) {
                Log.e(TAG, "there are some signals that share the same id");
                // TODO: reassign ids?
            }
            signals.removeInvalidSignals();

            isDataLoaded = true;
        }
    }

    public void cancelIRKitSetup() {
        if (setupManager != null) {
            setupManager.cancel();
            setupManager = null;
        }
        stopWifiStateListener();
        stopWifiScan();
        getHTTPClient().cancelPostDoor();
        httpClient.clearDeviceKeyCache();
    }

    public void setupIRKit(String apiKey, IRWifiInfo connectDestination, String irkitWifiPassword, IRKitConnectWifiListener listener) {
        if (setupManager != null && setupManager.isActive()) {  // There is an active setup
            setupManager.setIrKitConnectWifiListener(listener);
            return;
        }
//        cancelIRKitSetup(); // TODO: Do we need this?
        setupManager = new IRKitSetupManager(context);
        setupManager.setIrKitConnectWifiListener(listener);
        setupManager.start(apiKey, connectDestination, irkitWifiPassword);
    }

    /**
     * Watch Wi-Fi state change. When Wi-Fi is enabled, Bonjour discovery
     * will automatically start. When Wi-Fi is disabled, Bonjour discovery
     * will automatically stop.
     * Wi-Fi状態の変化を監視し、Wi-Fiが有効になったらBonjour検索を
     * 開始し、Wi-Fiが無効になったらBonjour検索を停止します。
     */
    public void registerWifiStateChangeListener() {
        if (networkStateChangeReceiver == null) {
            networkStateChangeReceiver = new NetworkStateChangeReceiver();
        }
        context.registerReceiver(networkStateChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    /**
     * Unwatch Wi-Fi state change.
     * Wi-Fi状態の監視を停止します。
     */
    public void unregisterWifiStateChangeListener() {
        if (networkStateChangeReceiver != null) {
            context.unregisterReceiver(networkStateChangeReceiver);
            networkStateChangeReceiver = null;
        }
    }

    /**
     * Start Bonjour discovery for IRKit.
     * IRKitのBonjour検索を開始します。
     */
    public void startServiceDiscovery() {
        if (!isWifiConnected()) {
            // Wi-Fi is not connected. We don't initiate service discovery.
            return;
        }
        synchronized (this) {
            if (isDiscovering) {
                return;
            }
            isDiscovering = true;
        }
        startBonjourDiscovery();
    }

    /**
     * Stop Bonjour discovery for IRKit.
     * IRKitのBonjour検索を停止します。
     */
    public void stopServiceDiscovery() {
        synchronized (this) {
            if (!isDiscovering) {
                return;
            }
            isDiscovering = false;
        }
        stopBonjourDiscovery();
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public void addExampleDataIfEmpty() {
        if (peripherals.isEmpty()) {
            addExamplePeripheral();
        }
        if (signals.isEmpty()) {
            addExampleSignal();
        }
    }

    public void addExamplePeripheral() {
        IRPeripheral peripheral = new IRPeripheral();
        peripheral.setHostname("IRKit1234");
        peripheral.setCustomizedName("IRKit1234");
        peripheral.setFoundDate(new Date());
        peripheral.setDeviceId("testdeviceid");
        peripheral.setModelName("IRKit");
        peripheral.setFirmwareVersion("2.0.2.0.g838e0ea");
        peripherals.add(peripheral);
        peripherals.save();
    }

    public void addExampleSignal() {
        // debug (add signal)
        IRSignal testSignal = new IRSignal();
        testSignal.setData(new int[]{
                18031, 8755, 1190, 1190, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 1190, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 3341, 1190, 3341, 1190, 1190, 1190, 3341, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 65535, 0, 9379, 18031, 4400, 1190
        });
        testSignal.setFormat("raw");
        testSignal.setFrequency(38.0f);
//        testSignal.setName("Warm");
        testSignal.setName("暖房");
        testSignal.setId(signals.getNewId());
        testSignal.setImageResourceId(R.drawable.btn_icon_256_aircon, context.getResources());
        testSignal.setDeviceId("testdeviceid");
        testSignal.setViewPosition(0);
        signals.add(testSignal);

        testSignal = new IRSignal();
        testSignal.setData(new int[]{
                18031, 8755, 1190, 1190, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 1190, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 3341, 1190, 3341, 1190, 1190, 1190, 3341, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 65535, 0, 9379, 18031, 4400, 1190
        });
        testSignal.setFormat("raw");
        testSignal.setFrequency(38.0f);
//        testSignal.setName("Cool");
        testSignal.setName("冷房");
        testSignal.setId(signals.getNewId());
        testSignal.setImageResourceId(R.drawable.btn_icon_256_aircon, context.getResources());
        testSignal.setDeviceId("testdeviceid");
        testSignal.setViewPosition(1);
        signals.add(testSignal);

        testSignal = new IRSignal();
        testSignal.setData(new int[]{
                18031, 8755, 1190, 1190, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 1190, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 3341, 1190, 3341, 1190, 1190, 1190, 3341, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 65535, 0, 9379, 18031, 4400, 1190
        });
        testSignal.setFormat("raw");
        testSignal.setFrequency(38.0f);
        testSignal.setName("\uD83D\uDCA4");
        testSignal.setId(signals.getNewId());
        testSignal.setImageResourceId(R.drawable.btn_icon_256_aircon, context.getResources());
        testSignal.setDeviceId("testdeviceid");
        testSignal.setViewPosition(2);
        signals.add(testSignal);

        testSignal = new IRSignal();
        testSignal.setData(new int[]{
                18031, 8755, 1190, 1190, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 1190, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 3341, 1190, 3341, 1190, 1190, 1190, 3341, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 65535, 0, 9379, 18031, 4400, 1190
        });
        testSignal.setFormat("raw");
        testSignal.setFrequency(38.0f);
//        testSignal.setName("\uD83C\uDFE0"); // house building
//        testSignal.setName("Living Room");
        testSignal.setName("リビング");
        testSignal.setId(signals.getNewId());
        testSignal.setImageResourceId(R.drawable.btn_icon_256_light, context.getResources());
        testSignal.setDeviceId("testdeviceid");
        testSignal.setViewPosition(3);
        signals.add(testSignal);

        signals.save();
    }

    /**
     * Set context, and initialize IRKit instance if it is not initialized yet.
     * contextをセットし、またIRKitインスタンスが初期化されていない場合は初期化します。
     *
     * @param context Application context
     */
    public void init(Context context) {
        setContext(context);
        if (!isInitialized) {
            isInitialized = true;
            discoveryQueue = new LinkedList<>();
            loadData();
        }
    }

    public void savePreference(String key, String value) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(
                context.getString(R.string.preferences_file_key), Context.MODE_PRIVATE
        );
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(key, value);
        editor.apply();

        this.requestBackup();
    }

    public String getPreference(String key) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(
                context.getString(R.string.preferences_file_key), Context.MODE_PRIVATE
        );
        return sharedPrefs.getString(key, null);
    }

    public void requestBackup() {
        BackupManager bm = new BackupManager(context);
        bm.dataChanged();
    }

    /**
     * Returns the global IRKit API key.
     *
     * @return
     */
    public String getIRKitAPIKey() {
        if (context == null) {
            throw new IllegalStateException("Context is not set. Have you called IRKit.sharedInstance().init(context)?");
        }

        // Fetch IRKit api key in whatever way you want
        ApplicationInfo ai = null;
        try {
            ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to get apikey. Have you add com.getirkit.IRKIT_API_KEY to AndroidManifest.xml?");
            return null;
        }
        Bundle metaData = ai.metaData;
        if (metaData == null) {
            Log.e(TAG, "Failed to get apikey. Have you add com.getirkit.IRKIT_API_KEY to AndroidManifest.xml?");
            return null;
        }
        return metaData.getString("com.getirkit.IRKIT_API_KEY");
    }

    /**
     * If we have not received clientkey yet, fetch it using apikey.
     * If apikey is null, it is read from AndroidManifest.xml.
     * clientkeyが未取得の場合はapikeyを使って取得します。apikeyがnullの場合は
     * AndroidManifest.xmlで指定されたapikeyを使用します。
     */
    public void registerClient(String apikey) {
        if (apikey == null) {
            apikey = getIRKitAPIKey();
        }
        if (apikey != null) {
            IRHTTPClient.sharedInstance().ensureRegisteredAndCall(apikey, new IRAPICallback<IRInternetAPIService.GetClientsResponse>() {
                @Override
                public void success(IRInternetAPIService.GetClientsResponse getClientsResponse, Response response) {
                    // Client has been registered or already registered
                }

                @Override
                public void failure(RetrofitError error) {
                    Log.e(TAG, "Failed to register client: " + error.getMessage());
                }
            });
        }
    }

    /**
     * Fetch clientkey if we have not received it yet.
     * clientkeyが未取得の場合は取得します。
     */
    public void registerClient() {
        registerClient(null);
    }

    public void removeWifiConfiguration(int networkId) {
        if (networkId != -1) {
            fetchWifiManager();
            wifiManager.removeNetwork(networkId);
        }
    }

    public void disconnectFromCurrentWifi() {
        fetchWifiManager();
        wifiManager.disconnect();
    }

    public WifiConfiguration getCurrentWifiConfiguration() {
        fetchWifiManager();

        WifiInfo currentWifiInfo = getCurrentWifiInfo();
        if (currentWifiInfo == null) {
            return null;
        }

        List<WifiConfiguration> networks = wifiManager.getConfiguredNetworks();
        if (networks == null) {
            return null;
        }
        for (WifiConfiguration config : networks) {
            if (config.networkId == currentWifiInfo.getNetworkId()) {
                return config;
            }
        }
        return null;
    }

    public WifiInfo getCurrentWifiInfo() {
        if (context == null) {
            Log.e(TAG, "getCurrentWifiInfo: context is null");
            return null;
        }

        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo.isConnected()) {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            return wifiManager.getConnectionInfo(); // returns WifiInfo instance
        } else {  // No internet connection
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            return wifiManager.getConnectionInfo(); // returns WifiInfo instance
        }
    }

    private void fetchWifiManager() {
        if (wifiManager == null) {
            wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        }
    }

    public void stopWifiStateListener() {
        if (wifiEnableEventReceiver != null) {
            wifiEnableEventReceiver.stop();
            context.getApplicationContext().unregisterReceiver(wifiEnableEventReceiver);
            wifiEnableEventReceiver = null;
        }
    }

    public void setWifiEnabled(boolean set) {
        fetchWifiManager();
        wifiManager.setWifiEnabled(set);
    }

    public boolean isWifiEnabled() {
        fetchWifiManager();
        return wifiManager.isWifiEnabled();
    }

    public void scanIRKitWifi(final IRKitWifiScanResultListener listener) {
        fetchWifiManager();

        if (scanResultReceiver == null) {
            scanResultReceiver = new ScanResultReceiver(wifiManager);
            scanResultReceiver.setIRKitWifiScanResultListener(new IRKitWifiScanResultListener() {
                @Override
                public void onIRKitWifiFound(ScanResult result) {
                    stopWifiScan();
                    listener.onIRKitWifiFound(result);
                }
            });
            context.getApplicationContext().registerReceiver(scanResultReceiver,
                    new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        }

        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
            wifiEnableEventReceiver = new WifiEnableEventReceiver();
            wifiEnableEventReceiver.setListener(new WifiEnableEventReceiver.WifiEnableEventReceiverListener() {
                @Override
                public void onWifiEnabled() {
                    stopWifiStateListener();
                    wifiManager.startScan();

                    final Timer t = new Timer();
                    t.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if (scanResultReceiver == null) {
                                // cancel timer
                                t.cancel();
                            } else {
                                // perform another scan
                                wifiManager.startScan();
                            }
                        }
                    }, 10000, 10000);
                }
            });
            context.getApplicationContext().registerReceiver(wifiEnableEventReceiver,
                    new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
        } else {
            wifiManager.startScan();
        }
    }

    public interface WifiConnectionChangeListener {
        public void onTargetWifiConnected(WifiInfo wifiInfo, NetworkInfo networkInfo);

        public void onError(String reason);

        public void onTimeout();
    }

    public interface IRKitWifiScanResultListener {
        public void onIRKitWifiFound(ScanResult result);
    }

    public void stopWifiScan() {
        if (scanResultReceiver != null) {
            scanResultReceiver.stopScan();
            context.getApplicationContext().unregisterReceiver(scanResultReceiver);
            scanResultReceiver = null;
        }
    }

    // Delete IRKit Wi-Fi configuration from Android
    public void clearIRKitWifiConfigurations() {
        fetchWifiManager();

        List<WifiConfiguration> configs = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration config : configs) {
            if (IRWifiInfo.isIRKitWifi(config)) {
                wifiManager.removeNetwork(config.networkId);
            }
        }
    }

    public WifiConfiguration getWifiConfigurationBySSID(String ssid) {
        List<WifiConfiguration> configs = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration config : configs) {
            if (config.SSID.equals(ssid)) {
                return config;
            }
        }
        return null;
    }

    public void connectToNormalWifi(WifiConfiguration wifiConfig, final WifiConnectionChangeListener listener) {
        fetchWifiManager();
        final WifiConnectionChangeReceiver wifiConnectionChangeReceiver = new WifiConnectionChangeReceiver(wifiManager, wifiConfig);
        wifiConnectionChangeReceiver.setWifiConnectionChangeListener(new WifiConnectionChangeListener() {
            @Override
            public void onTargetWifiConnected(WifiInfo wifiInfo, NetworkInfo networkInfo) {
                wifiConnectionChangeReceiver.cancel();
                context.getApplicationContext().unregisterReceiver(wifiConnectionChangeReceiver);
                listener.onTargetWifiConnected(wifiInfo, networkInfo);
            }

            @Override
            public void onError(String reason) {
                Log.e(TAG, "connectToNormalWifi error: " + reason);
                wifiConnectionChangeReceiver.cancel();
                context.getApplicationContext().unregisterReceiver(wifiConnectionChangeReceiver);
                listener.onError(reason);
            }

            @Override
            public void onTimeout() {
                Log.e(TAG, "connectToNormalWifi timeout");
                wifiConnectionChangeReceiver.cancel();
                context.getApplicationContext().unregisterReceiver(wifiConnectionChangeReceiver);
                listener.onTimeout();
            }
        });

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        context.getApplicationContext().registerReceiver(wifiConnectionChangeReceiver,
                intentFilter);

        wifiManager.enableNetwork(wifiConfig.networkId, true);
    }

    public int connectToIRKitWifi(IRWifiInfo irWifiInfo, final WifiConnectionChangeListener listener) {
        final String irkitSSID = "\"" + irWifiInfo.getSSID() + "\"";

        WifiConfiguration wifiConfig = getWifiConfigurationBySSID(irkitSSID);
        int irkitWifiNetworkId;
        if (wifiConfig != null) {
            // Update existing IRKit Wi-Fi configuration
            wifiConfig.preSharedKey = "\"" + irWifiInfo.getPassword() + "\"";
            irkitWifiNetworkId = wifiManager.updateNetwork(wifiConfig);
            if (irkitWifiNetworkId == -1) {
                Log.e(TAG, "Failed to update network configuration");
                listener.onError("Failed to update network configuration");
                return irkitWifiNetworkId;
            }
        } else {
            // Add IRKit Wi-Fi configuration
            wifiConfig = new WifiConfiguration();
            wifiConfig.SSID = irkitSSID;
            wifiConfig.preSharedKey = "\"" + irWifiInfo.getPassword() + "\"";
            irkitWifiNetworkId = wifiManager.addNetwork(wifiConfig);
            if (irkitWifiNetworkId == -1) {
                Log.e(TAG, "Failed to add network configuration");
                listener.onError("Failed to add network configuration");
                return irkitWifiNetworkId;
            }
        }

        long irkitWifiConnectionTimeout = 30000;

        fetchWifiManager();
        final WifiConnectionChangeReceiver wifiConnectionChangeReceiver = new WifiConnectionChangeReceiver(
                wifiManager, wifiConfig, WifiConnectionChangeReceiver.FLAG_FULL_MATCH, irkitWifiConnectionTimeout);
        wifiConnectionChangeReceiver.setWifiConnectionChangeListener(new WifiConnectionChangeListener() {
            @Override
            public void onTargetWifiConnected(WifiInfo wifiInfo, NetworkInfo networkInfo) {
                wifiConnectionChangeReceiver.cancel();
                context.getApplicationContext().unregisterReceiver(wifiConnectionChangeReceiver);

                getHTTPClient().setDeviceAPIEndpoint(IRHTTPClient.DEVICE_API_ENDPOINT_IRKITWIFI);
                listener.onTargetWifiConnected(wifiInfo, networkInfo);
            }

            @Override
            public void onError(String reason) {
                Log.e(TAG, "connectToIRKitWifi error: " + reason);
                wifiConnectionChangeReceiver.cancel();
                context.getApplicationContext().unregisterReceiver(wifiConnectionChangeReceiver);
                listener.onError(reason);
            }

            @Override
            public void onTimeout() {
                Log.e(TAG, "connectToIRKitWifi timeout");
                wifiConnectionChangeReceiver.cancel();
                context.getApplicationContext().unregisterReceiver(wifiConnectionChangeReceiver);
                listener.onTimeout();
            }
        });

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        context.getApplicationContext().registerReceiver(wifiConnectionChangeReceiver,
                intentFilter);

        if (irkitWifiNetworkId != -1) {
            if (!wifiManager.enableNetwork(irkitWifiNetworkId, true)) {
                Log.e(TAG, "enableNetwork failed");
            }
        } else {
            Log.e(TAG, "Invalid network id: " + irkitWifiNetworkId);
        }

        return irkitWifiNetworkId;
    }

    public String getDebugInfo() {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        try {
            JSONObject debugInfo = new JSONObject();

            JSONObject os = new JSONObject();
            os.put("manufacturer", Build.MANUFACTURER);
            os.put("model", Build.MODEL);
            os.put("version", Build.VERSION.RELEASE);
            os.put("sdk_int", Build.VERSION.SDK_INT);
            os.put("fingerprint", Build.FINGERPRINT); // BRAND/PRODUCT/DEVICE:VERSION.RELEASE/ID/VERSION.INCREMENTAL:TYPE/TAGS
            JSONObject screen = new JSONObject();
            screen.put("width", metrics.widthPixels);
            screen.put("height", metrics.heightPixels);
            os.put("screen", screen);
            debugInfo.put("system", os);

            debugInfo.put("peripherals", peripherals.toJSONArray());

            JSONObject bonjour = new JSONObject();
            bonjour.put("hostname", getPreference(PREF_KEY_BONJOUR_HOSTNAME));
            bonjour.put("resolved_at", getPreference(PREF_KEY_BONJOUR_RESOLVED_AT));

            debugInfo.put("bonjour", bonjour);

            debugInfo.put("version", BuildConfig.VERSION_NAME);
            debugInfo.put("platform", "android");

            return debugInfo.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

//        String osCodename = Build.VERSION.CODENAME;
//        String osIncremental = Build.VERSION.INCREMENTAL;
//        String osRelease = Build.VERSION.RELEASE;
//        int sdkVersion = Build.VERSION.SDK_INT;
//        Log.d(TAG, "VERSION.CODENAME: " + osCodename);
//        Log.d(TAG, "VERSION.INCREMENTAL: " + osIncremental);
//        Log.d(TAG, "VERSION.RELEASE: " + osRelease);
//        Log.d(TAG, "VERSION.SDK_INT: " + sdkVersion);
//        String deviceManufacturer = Build.MANUFACTURER;
//        String deviceModel = Build.MODEL;
//        Log.d(TAG, "MANUFACTURER: " + deviceManufacturer);
//        Log.d(TAG, "MODEL: " + deviceModel);
//        String buildId = Build.DISPLAY;
//        Log.d(TAG, "DISPLAY (build ID): " + buildId);
//        Log.d(TAG, "FINGERPRINT: " + Build.FINGERPRINT);
//        Log.d(TAG, "HARDWARE: " + Build.HARDWARE);
//        Log.d(TAG, "ID (changelist number or label): " + Build.ID);
//        Log.d(TAG, "PRODUCT: " + Build.PRODUCT);
//        Log.d(TAG, "radioVersion: " + Build.getRadioVersion());
//        Log.d(TAG, "SERIAL: " + Build.SERIAL);
//        Log.d(TAG, "TAGS: " + Build.TAGS);
//        Log.d(TAG, "TYPE: " + Build.TYPE);
//        Log.d(TAG, "DEVICE: " + Build.DEVICE);
//        Log.d(TAG, "BRAND: " + Build.BRAND);
//        Log.d(TAG, "BOOTLOADER: " + Build.BOOTLOADER);
//        Log.d(TAG, "BOARD: " + Build.BOARD);
    }

    public static int getRegDomainForDefaultLocale() {
        return getRegDomainForLocale(Locale.getDefault());
    }

    public static int getRegDomainForLocale(Locale locale) {
        String countryCode = locale.getCountry();
        int regdomain;
        String fccPattern = "^(?:CA|MX|US|AU|HK|IN|MY|NZ|PH|TW|RU|AR|BR|CL|CO|CR|DO|DM|EC|PA|PY|PE|PR|VE)$";
        if (countryCode.equals("JP")) {
            regdomain = 2; // TELEC
        } else if (countryCode.matches(fccPattern)) {
            regdomain = 0; // FCC
        } else {
            regdomain = 1; // ETSI
        }
        return regdomain;
    }

    public InetAddress getWifiIPv4Address() {
        fetchWifiManager();
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

        // http://stackoverflow.com/questions/16730711/get-my-wifi-ip-address-android

        // Convert little-endian to big-endian if needed
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getByAddress(ipByteArray);
        } catch (UnknownHostException ex) {
            Log.e(TAG, "Failed to get Wi-Fi IP address");
        }

        return inetAddress;
    }

    class BonjourServiceListener implements ServiceListener {
        @Override
        public void serviceAdded(ServiceEvent serviceEvent) {
            jmdns.requestServiceInfo(serviceEvent.getType(), serviceEvent.getName());
        }

        @Override
        public void serviceRemoved(ServiceEvent serviceEvent) {
            String serviceName = serviceEvent.getName();
            IRPeripheral peripheral = peripherals.getPeripheral(serviceName);
            if (peripheral != null) {
                peripheral.lostLocalAddress();
            }
        }

        @Override
        public void serviceResolved(ServiceEvent serviceEvent) {
            ServiceInfo serviceInfo = serviceEvent.getInfo();
            if (serviceInfo == null) {
                Log.e(TAG, "serviceResolved: service info is null");
                return;
            }
            Inet4Address[] addresses = serviceInfo.getInet4Addresses();
            if (addresses.length == 0) {
                Log.e(TAG, "serviceResolved: no address");
                return;
            }
            Inet4Address host = addresses[0];
            int port = serviceInfo.getPort();

            String serviceName = serviceEvent.getName();
//            Log.d(TAG, "resolve success: name=" + serviceName + " host=" + host + " port=" + port);
            savePreference(PREF_KEY_BONJOUR_HOSTNAME, serviceEvent.getInfo().getQualifiedName());
            savePreference(PREF_KEY_BONJOUR_RESOLVED_AT, String.valueOf(new Date().getTime() / 1000));
            IRPeripheral peripheral = peripherals.getPeripheral(serviceName);
            if (peripheral == null) { // Found new IRKit
                peripheral = peripherals.addPeripheral(serviceName);
                if (irkitEventListener != null) {
                    irkitEventListener.onNewIRKitFound(peripheral);
                }
            } else {
                if (irkitEventListener != null) {
                    irkitEventListener.onExistingIRKitFound(peripheral);
                }
            }
            peripheral.setHost(host);
            peripheral.setPort(port);
            final IRPeripheral p = peripheral;
            if (!peripheral.hasDeviceId()) {
                // Wait 2000 ms to settle.
                // We can't use Handler nor AsyncTask because we aren't on the UI thread.
                Timer t = new Timer();
                t.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        p.fetchDeviceId();
                    }
                }, 2000);
            } else if (!peripheral.hasModelInfo()) {
                // Wait 500 ms to settle.
                // We can't use Handler nor AsyncTask because we aren't on the UI thread.
                Timer t = new Timer();
                t.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        p.fetchModelInfo();
                    }
                }, 500);
            }
        }
    }

    private void pushDiscoveryQueue(boolean startDiscovery) {
        int originalSize;
        synchronized (discoveryQueue) {
            originalSize = discoveryQueue.size();
            while (discoveryQueue.size() >= 1) {
                boolean set = discoveryQueue.peekLast();
                if (set == startDiscovery) {
                    return;
                } else {
                    if (discoveryQueue.size() >= 2) {
                        discoveryQueue.removeLast();
                        continue;
                    }
                }
                break;
            }
            discoveryQueue.add(startDiscovery);
        }

        if (originalSize == 0 && !isProcessingBonjour) {
            nextDiscoveryQueue();
        }
    }

    private void nextDiscoveryQueue() {
        boolean startDiscovery;
        synchronized (discoveryQueue) {
            if (discoveryQueue.isEmpty()) {
                return;
            }
            startDiscovery = discoveryQueue.pop();
        }
        if (startDiscovery) {
            _startBonjourDiscovery();
        } else {
            _stopBonjourDiscovery();
        }
    }

    private void _startBonjourDiscovery() {
        if (isProcessingBonjour) {
            Log.e(TAG, "isProcessingBonjour is true");
            return;
        }
        isProcessingBonjour = true;
        // Do network tasks in background. We can't use Handler here.
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                fetchWifiManager();

                WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                multicastLock = wifi.createMulticastLock(getClass().getName());
                multicastLock.setReferenceCounted(true);
                multicastLock.acquire();

                InetAddress deviceIPAddress = getWifiIPv4Address();
                try {
                    // Do not use default constructor i.e. JmDNS.create()
                    jmdns = JmDNS.create(deviceIPAddress);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (jmdns != null) {
                    // Started zeroconf probe
                    bonjourServiceListener = new BonjourServiceListener();
                    jmdns.addServiceListener(SERVICE_TYPE, bonjourServiceListener);
                }
                isProcessingBonjour = false;
                nextDiscoveryQueue();
                return null;
            }
        }.execute();
    }

    public void startBonjourDiscovery() {
        pushDiscoveryQueue(true);
    }

    public void stopBonjourDiscovery() {
        pushDiscoveryQueue(false);
    }

    private void _stopBonjourDiscovery() {
        if (isProcessingBonjour) {
            Log.e(TAG, "isProcessingBonjour is true");
            return;
        }
        isProcessingBonjour = true;
        // Do network tasks in another thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (jmdns != null) {
                    if (bonjourServiceListener != null) {
                        jmdns.removeServiceListener(SERVICE_TYPE, bonjourServiceListener);
                        bonjourServiceListener = null;
                    }
                    try {
                        jmdns.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    jmdns = null;
                }
                if (multicastLock != null) {
                    multicastLock.release();
                    multicastLock = null;
                }
                // Stopped zeroconf probe
                isProcessingBonjour = false;
                nextDiscoveryQueue();
            }
        }).start();
    }

    /**
     * Call _sendSignal() one by one using queue
     *
     * IRKit panics when received parallel requests from local network.
     *
     * @param signal
     * @param callback
     */
    public void sendSignal(IRSignal signal, IRAPIResult callback) {
        synchronized (sendSignalQueue) {
            sendSignalQueue.add(new SendSignalItem(signal, callback));
            if (sendSignalQueue.size() == 1) {
                // Do it now
                _sendSignal(signal, callback);
            }
        }
    }

    private void consumeNextSendSignal() {
        synchronized (sendSignalQueue) {
            sendSignalQueue.removeFirst();
            SendSignalItem sendSignalItem = sendSignalQueue.peek();
            if (sendSignalItem != null) {
                // Consume the next signal
                _sendSignal(sendSignalItem.signal, sendSignalItem.callback);
            }
        }
    }

    private void _sendSignal(final IRSignal signal, final IRAPIResult callback) {
        String deviceId = signal.getDeviceId();
        if (deviceId == null) {
            // This shouldn't happen under normal circumstances
            Log.e(TAG, "sendSignal: deviceId is null");
            if (callback != null) {
                callback.onError(new IRAPIError("deviceId is null"));
            }
            consumeNextSendSignal();
            return;
        }
        final IRPeripheral peripheral = peripherals.getPeripheralByDeviceId(deviceId);

        // If a peripheral is registered twice, its deviceId is overwritten.
        // But we still try to send those signals over Internet API.

        final IRAPICallback<IRInternetAPIService.PostMessagesResponse> internetAPICallback = new IRAPICallback<IRInternetAPIService.PostMessagesResponse>() {
            @Override
            public void success(IRInternetAPIService.PostMessagesResponse postMessagesResponse, Response response) {
                if (callback != null) {
                    callback.onSuccess();
                }
                consumeNextSendSignal();
            }

            @Override
            public void failure(RetrofitError error) {
                if (callback != null) {
                    callback.onError(new IRAPIError(error.getLocalizedMessage()));
                }
                consumeNextSendSignal();
            }
        };

        if ( peripheral != null && peripheral.isLocalAddressResolved() ) {
            httpClient.setDeviceAPIEndpoint(peripheral.getDeviceAPIEndpoint());
            httpClient.sendSignalOverLocalNetwork(signal, new IRAPIResult() {
                @Override
                public void onSuccess() {
                    if (callback != null) {
                        callback.onSuccess();
                    }
                    consumeNextSendSignal();
                }

                @Override
                public void onError(IRAPIError error) {
                    // Try to send signal over Internet
                    httpClient.sendSignalOverInternet(signal, internetAPICallback);
                }

                @Override
                public void onTimeout() {
                    peripheral.lostLocalAddress();
                    // Try to send signal over Internet
                    httpClient.sendSignalOverInternet(signal, internetAPICallback);
                }
            }, SEND_SIGNAL_LOCAL_TIMEOUT_MS);
        } else {  // Local address isn't resolved
            httpClient.sendSignalOverInternet(signal, internetAPICallback);
        }
    }

    public boolean isWifiConnected() {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return mWifi.isConnected();
    }

    // Getters and setters

    public IRKitEventListener getIRKitEventListener() {
        return irkitEventListener;
    }

    /**
     * Set an IRKitEventListener to this instance.
     * IRKitEventListenerをセットします。nullが渡された場合はリスナーを解除します。
     *
     * @param listener IRKitEventListener instance. Pass null to unset listener.
     */
    public void setIRKitEventListener(IRKitEventListener listener) {
        this.irkitEventListener = listener;
    }

    public void setContext(Context c) {
        context = c;
        if (!httpClient.hasClientKey()) {
            httpClient.setClientKey(this.getPreference("clientkey"));
        }
    }

    public Context getContext() {
        return context;
    }

    public boolean isDataLoaded() {
        return isDataLoaded;
    }

    // Interfaces

    public interface IRKitConnectWifiListener {
        public void onStatus(String status);
        public void onError(String message);
        public void onComplete();
    }

    // Inner classes

    private static class SendSignalItem {
        public IRSignal signal;
        public IRAPIResult callback;

        public SendSignalItem(IRSignal signal, IRAPIResult callback) {
            this.signal = signal;
            this.callback = callback;
        }
    }

    private static class NetworkStateChangeReceiver extends BroadcastReceiver {
        public static final String TAG = NetworkStateChangeReceiver.class.getName();

        /**
         * Called when connectivity has been changed
         *
         * @param context
         * @param intent
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = conMan.getActiveNetworkInfo();
            IRKit irkit = IRKit.sharedInstance();
            if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                // Wi-Fi connection is available
                irkit.startServiceDiscovery();
            } else {
                // No Wi-Fi connection
                irkit.stopServiceDiscovery();
            }
        }
    }

    private static class WifiConnectionChangeReceiver extends BroadcastReceiver {
        public static final String TAG = WifiConnectionChangeReceiver.class.getSimpleName();

        public static final int FLAG_FULL_MATCH = 1;
        public static final int FLAG_STARTS_WITH = 2;

        private WifiConnectionChangeListener wifiConnectionChangeListener;
        private String targetSSID;
        private int flag;
        private WifiConfiguration targetWifiConfiguration;
        private String lastSeenSSID;
        private long startTime;
        private static final int ERROR_TIME_THRESHOLD = 3000; // milliseconds
        private boolean isFinished = false;
        private int authFailedCount = 0;
        private WifiManager wifiManager;
        private boolean isCanceled = false;
        private Handler timeoutHandler;
        private Runnable timeoutRunnable;

        public WifiConnectionChangeReceiver(WifiManager wifiManager, WifiConfiguration wifiConfig, int flag, long timeoutMs) {
            super();
            this.wifiManager = wifiManager;
            targetWifiConfiguration = wifiConfig;
            targetSSID = targetWifiConfiguration.SSID;
            this.flag = flag;
            startTime = System.currentTimeMillis();

            if (timeoutMs != 0) {
                timeoutHandler = new Handler();
                timeoutRunnable = new Runnable() {
                    @Override
                    public void run() {
                        synchronized (this) {
                            if (!isCanceled && !isFinished) {
                                isFinished = true;
                                if (wifiConnectionChangeListener != null) {
                                    wifiConnectionChangeListener.onTimeout();
                                }
                            }
                            timeoutRunnable = null;
                            timeoutHandler = null;
                        }
                    }
                };
                timeoutHandler.postDelayed(timeoutRunnable, timeoutMs);
            }
        }

        public WifiConnectionChangeReceiver(WifiManager wifiManager, WifiConfiguration wifiConfig) {
            this(wifiManager, wifiConfig, FLAG_FULL_MATCH, 0);
        }

        public void cancel() {
            synchronized (this) {
                isCanceled = true;
                if (timeoutHandler != null) {
                    timeoutHandler.removeCallbacks(timeoutRunnable);
                    timeoutRunnable = null;
                    timeoutHandler = null;
                }
            }
        }

        private boolean matchesSSID(String currentSSID, String targetSSID) {
            if (currentSSID == null || targetSSID == null) {
                return false;
            }
            Pattern pattern = Pattern.compile("^\"(.*)\"$");
            Matcher matcher = pattern.matcher(currentSSID);
            if ( matcher.matches() ) {
                currentSSID = matcher.group(1);
            }
            matcher = pattern.matcher(targetSSID);
            if ( matcher.matches() ) {
                targetSSID = matcher.group(1);
            }

            if (flag == FLAG_FULL_MATCH) {
                return currentSSID.equals(targetSSID);
            } else if (flag == FLAG_STARTS_WITH) {
                return currentSSID.startsWith(targetSSID);
            } else {
                throw new IllegalStateException("unknown flag: " + flag);
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (isCanceled) {
                return;
            }
            final String action = intent.getAction();
            if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
//                WifiInfo wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
//                NetworkInfo.DetailedState state = info.getDetailedState();
//                String extraInfo = info.getExtraInfo();
//                Log.d(TAG, "state changed: isConnected=" + info.isConnected() + " detailedState=" + state + " extra=" + extraInfo + " wifiInfo=" + wifiInfo + " networkInfo=" + info);
                lastSeenSSID = info.getExtraInfo();
                if (info.isConnected()) {
                    if (wifiInfo != null) {
                        int ipaddr = wifiInfo.getIpAddress();
//                        String ipaddrString = String.format("%d.%d.%d.%d",
//                                (ipaddr & 0xff), (ipaddr >> 8 & 0xff), (ipaddr >> 16 & 0xff), (ipaddr >> 24 & 0xff));
//                        Log.d(TAG, "ip address: " + ipaddrString);
                        if (ipaddr == 0) {
                            // Empty IP address. Still wait.
                            return;
                        }
                    }
                    if (wifiInfo != null) {
                        String currentSSID = wifiInfo.getSSID();
                        if (matchesSSID(currentSSID, targetSSID)) { // Connected to target Wi-Fi
                            synchronized (this) {
                                if (!isCanceled && !isFinished) {
                                    isFinished = true;
                                    if (timeoutHandler != null) {
                                        timeoutHandler.removeCallbacks(timeoutRunnable);
                                        timeoutRunnable = null;
                                        timeoutHandler = null;
                                    }
                                    if (wifiConnectionChangeListener != null) {
                                        wifiConnectionChangeListener.onTargetWifiConnected(wifiInfo, info);
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (action.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
                int errorCode = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, 0);
//                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
//                WifiInfo wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
//                Log.d(TAG, "Supplicant state change: errorCode=" + errorCode + " ERROR_AUTHENTICATING=" + WifiManager.ERROR_AUTHENTICATING);
                if (errorCode == WifiManager.ERROR_AUTHENTICATING) {
                    if (matchesSSID(lastSeenSSID, targetSSID)) {
                        long diff = System.currentTimeMillis() - startTime;
                        Log.e(TAG, "Wifi authentication failed (count=" + authFailedCount + " diff=" + diff + "ms)");
                        if (++authFailedCount >= 2) {
                            synchronized (this) {
                                if (!isCanceled && !isFinished) {
                                    isFinished = true;
                                    if (timeoutHandler != null) {
                                        timeoutHandler.removeCallbacks(timeoutRunnable);
                                        timeoutRunnable = null;
                                        timeoutHandler = null;
                                    }
                                    if (wifiConnectionChangeListener != null) {
                                        wifiConnectionChangeListener.onError("Authentication failed");
                                    }
                                }
                            }
                        }
                        if (diff >= ERROR_TIME_THRESHOLD) {
                            synchronized (this) {
                                if (!isCanceled && !isFinished) {
                                    isFinished = true;
                                    if (timeoutHandler != null) {
                                        timeoutHandler.removeCallbacks(timeoutRunnable);
                                        timeoutRunnable = null;
                                        timeoutHandler = null;
                                    }
                                    if (wifiConnectionChangeListener != null) {
                                        wifiConnectionChangeListener.onError("Authentication failed");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        public WifiConnectionChangeListener getWifiConnectionChangeListener() {
            return wifiConnectionChangeListener;
        }

        public void setWifiConnectionChangeListener(WifiConnectionChangeListener wifiConnectionChangeListener) {
            this.wifiConnectionChangeListener = wifiConnectionChangeListener;
        }
    }

    private static class WifiEnableEventReceiver extends BroadcastReceiver {
        public static final String TAG = WifiEnableEventReceiver.class.getSimpleName();

        private interface WifiEnableEventReceiverListener {
            public void onWifiEnabled();
        }

        private WifiEnableEventReceiverListener listener;
        private boolean isStopped = false;

        public WifiEnableEventReceiverListener getListener() {
            return listener;
        }

        public void setListener(WifiEnableEventReceiverListener listener) {
            this.listener = listener;
        }

        public void stop() {
            isStopped = true;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (isStopped) {
                return;
            }

            int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
            if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
                if (listener != null) {
                    listener.onWifiEnabled();
                }
            }
        }
    }

    private static class ScanResultReceiver extends BroadcastReceiver {
        public static final String TAG = ScanResultReceiver.class.getSimpleName();

        private boolean isStopped;
        private WifiManager wifiManager;
        private IRKitWifiScanResultListener irkitWifiScanResultListener;

        public ScanResultReceiver(WifiManager wifiManager) {
            this.wifiManager = wifiManager;
            isStopped = false;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (isStopped) {
                return;
            }
            List<ScanResult> results = wifiManager.getScanResults();
            if (results != null) {
                for (ScanResult result : results) {
                    if (IRWifiInfo.isIRKitWifi(result)) {
                        if (irkitWifiScanResultListener != null) {
                            irkitWifiScanResultListener.onIRKitWifiFound(result);
                            return;
                        }
                    }
                }
            }
            if (!isStopped) {
                wifiManager.startScan();
            }
        }

        public void stopScan() {
            isStopped = true;
        }

        public IRKitWifiScanResultListener getIRKitWifiScanResultListener() {
            return irkitWifiScanResultListener;
        }

        public void setIRKitWifiScanResultListener(IRKitWifiScanResultListener irKitWifiScanResultListener) {
            this.irkitWifiScanResultListener = irKitWifiScanResultListener;
        }
    }
}
