package com.getirkit.irkit;

import android.content.Context;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.os.Handler;
import android.util.Log;

import com.getirkit.irkit.net.IRAPICallback;
import com.getirkit.irkit.net.IRAPIResult;
import com.getirkit.irkit.net.IRDeviceAPIService;
import com.getirkit.irkit.net.IRInternetAPIService;

import java.util.Timer;
import java.util.TimerTask;

import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * IRKitのセットアップを担うクラスです。
 * Handles setting up an IRKit.
 */
class IRKitSetupManager implements IRKitEventListener {
    public static final String TAG = IRKitSetupManager.class.getName();

    private Context context;
    private boolean isSettingUpIRKit = false;
    private IRKit.IRKitConnectWifiListener irKitConnectWifiListener;
    private IRWifiInfo irWifiInfo;
    private String irkitWifiPassword;
    private WifiConfiguration normalWifiConfiguration;
    private int irkitWifiNetworkId = -1;
    private boolean isOriginallyWifiEnabled;
    private String setupDeviceId;
    private String irkitWifiSSID;
    private boolean isPostDoorSuccess = false;
    private boolean isIRKitFound = false;
    private IRState checkConnectivityState;
    private IRAPIResult discoveryResult;

    public boolean isActive() {
        return isSettingUpIRKit;
    }

    public IRKitSetupManager(Context context) {
        this.context = context;
    }

    public void setIrKitConnectWifiListener(IRKit.IRKitConnectWifiListener listener) {
        this.irKitConnectWifiListener = listener;
    }

    public void fetchDeviceKey() {
        irKitConnectWifiListener.onStatus(context.getString(R.string.setup_status__obtaining_device_key));

        // Timeout handler
        // We should avoid using a Handler because this might be
        // executed on a background thread.
        final IRState state = new IRState();
        final Timer timeoutTimer = new Timer();
        timeoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (state) {
                    if (!state.isFinished()) {
                        state.finish();
                        if (isSettingUpIRKit) {
                            isSettingUpIRKit = false;
                            irKitConnectWifiListener.onError(context.getString(R.string.setup_status__error_timeout_obtain_device_key));
                        }
                    }
                }
            }
        }, 30000);

        // Obtain device key and device id
        IRKit.sharedInstance().getHTTPClient().obtainDeviceKey(new IRAPICallback<IRInternetAPIService.PostDevicesResponse>() {
            @Override
            public void success(IRInternetAPIService.PostDevicesResponse postDevicesResponse, Response response) {
                synchronized (state) {
                    if (!state.isFinished()) {
                        state.finish();
                        timeoutTimer.cancel();
                        if (isSettingUpIRKit) {
                            setupDeviceId = postDevicesResponse.deviceid;
                            scanIRKitWifi();
                        }
                    }
                }
            }

            @Override
            public void failure(RetrofitError error) {
                synchronized (state) {
                    if (!state.isFinished()) {
                        state.finish();
                        timeoutTimer.cancel();
                        Log.e(TAG, "Failed to get device key");
                        if (isSettingUpIRKit) {
                            isSettingUpIRKit = false;
                            irKitConnectWifiListener.onError(context.getString(R.string.setup_status__error_obtain_device_key));
                        }
                    }
                }
            }
        });
    }

    public void start(String apiKey, IRWifiInfo connectDestination, String irkitWifiPassword) {
        if (isSettingUpIRKit) {  // Already setting up IRKit
            return;
        }

        isSettingUpIRKit = true;
        IRKit irkit = IRKit.sharedInstance();
        irkit.unforceIRKitWifi();
        isOriginallyWifiEnabled = irkit.isWifiEnabled();
        this.irWifiInfo = connectDestination;
        this.irkitWifiPassword = irkitWifiPassword;
        this.normalWifiConfiguration = irkit.getCurrentWifiConfiguration();

        if (normalWifiConfiguration != null && IRWifiInfo.isIRKitWifi(normalWifiConfiguration)) {
            // Disconnect from IRKit Wi-Fi
            irkit.disconnectFromCurrentWifi();
            normalWifiConfiguration = null;
        }

        irKitConnectWifiListener.onStatus(context.getString(R.string.setup_status__obtaining_client_key));
        irkit.getHTTPClient().ensureRegisteredAndCall(apiKey, new IRAPICallback<IRInternetAPIService.PostClientsResponse>() {
            @Override
            public void success(IRInternetAPIService.PostClientsResponse postClientsResponse, Response response) {
                fetchDeviceKey();
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "Failed to get clientkey: " + error.getMessage());
                isSettingUpIRKit = false;
                irKitConnectWifiListener.onError(context.getString(R.string.setup_status__error_no_client_key));
            }
        });
    }

    private void scanIRKitWifi() {
        if (!isSettingUpIRKit) {
            return;
        }
        irKitConnectWifiListener.onStatus(context.getString(R.string.setup_status__scanning_for_irkit_wifi));

        // Timeout handler
        // We should avoid using a Handler because this might be
        // executed on a background thread.
        final IRState state = new IRState();
        final Timer timeoutTimer = new Timer();
        timeoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (state) {
                    if (!state.isFinished()) {
                        state.finish();
                        if (isSettingUpIRKit) {
                            isSettingUpIRKit = false;
                            IRKit.sharedInstance().stopWifiStateListener();
                            IRKit.sharedInstance().stopWifiScan();
                            irKitConnectWifiListener.onError(context.getString(R.string.setup_status__error_irkit_wifi_not_found));
                        }
                    }
                }
            }
        }, 50000);

        IRKit.sharedInstance().scanIRKitWifi(new IRKit.IRKitWifiScanResultListener() {
            @Override
            public void onIRKitWifiFound(ScanResult result) {
                synchronized (state) {
                    if (!state.isFinished()) {
                        state.finish();
                        timeoutTimer.cancel();
                        if (isSettingUpIRKit) {
                            irkitWifiSSID = result.SSID;
                            changeToIRKitWifi();
                        }
                    }
                }
            }
        });
    }

    private void removeIRKitWifiConfiguration() {
        if (irkitWifiNetworkId != -1) {
            IRKit.sharedInstance().removeWifiConfiguration(irkitWifiNetworkId);
        }
    }

    private void changeToIRKitWifi() {
        if (irkitWifiPassword == null) {
            throw new IllegalStateException("IRKit wifi password isn't set");
        }
        if (!isSettingUpIRKit) {
            return;
        }
        IRWifiInfo info = new IRWifiInfo();
        info.setSSID(irkitWifiSSID);
        info.setSecurity(IRWifiInfo.SECURITY_WPA_WPA2);
        info.setPassword(irkitWifiPassword);
        irKitConnectWifiListener.onStatus(context.getString(R.string.setup_status__connecting_to_irkit_wifi));
        irkitWifiNetworkId = IRKit.sharedInstance().connectToIRKitWifi(info, new IRKit.WifiConnectionChangeListener() {
            @Override
            public void onTargetWifiConnected(WifiInfo wifiInfo, NetworkInfo networkInfo) {
                if (isSettingUpIRKit) {
                    // Connected to IRKit Wi-Fi. Wait to settle.
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            IRKit.sharedInstance().forceIRKitWifi(new IRCallback() {
                                @Override
                                public void onSuccess() {
                                    if (isSettingUpIRKit) {
                                        connectIRKitToWifi();
                                    }
                                }

                                @Override
                                public void onError(Exception ex) {
                                    // will not be called
                                }

                                @Override
                                public void onTimeout() {

                                }
                            });
                        }
                    }, 2000);
                }
            }

            @Override
            public void onError(String reason) {
                Log.e(TAG, "connectToIRKitWifi error: " + reason);
                if (isSettingUpIRKit) {
                    isSettingUpIRKit = false;
                    revertToNormalWifi();
                    irKitConnectWifiListener.onError(context.getString(R.string.setup_status__error_connect_to_irkit_wifi));
                }
            }

            @Override
            public void onTimeout() {
                Log.e(TAG, "connectToIRKitWifi timed out");
                if (isSettingUpIRKit) {
                    isSettingUpIRKit = false;
                    revertToNormalWifi();
                    irKitConnectWifiListener.onError(context.getString(R.string.setup_status__error_timeout_connect_to_irkit_wifi));
                }
            }
        });
    }

    private void connectIRKitToWifi() {
        connectIRKitToWifi(0);
    }

    private void connectIRKitToWifi(final int retryCount) {
        if (!isSettingUpIRKit) {
            return;
        }
        if (retryCount >= 5) {
            Log.e(TAG, "connectIRKitToWifi: too many retries");
            if (isSettingUpIRKit) {
                isSettingUpIRKit = false;
                irKitConnectWifiListener.onError(context.getString(R.string.setup_status__error_connect_irkit_to_wifi));
                revertToNormalWifi();
            }
            return;
        }

        irKitConnectWifiListener.onStatus(context.getString(R.string.setup_status__connecting_irkit_to_wifi));

        // timeout handler
        final Handler handler = new Handler();
        final IRState state = new IRState();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                synchronized (state) {
                    if (!state.isFinished()) {
                        state.finish();
                        if (isSettingUpIRKit) {
                            isSettingUpIRKit = false;
                            irKitConnectWifiListener.onError(context.getString(R.string.setup_status__error_timeout_connect_to_wifi));
                        }
                    }
                }
            }
        };
        handler.postDelayed(runnable, 30000);

        IRKit.sharedInstance().getHTTPClient().connectDeviceToWifi(irWifiInfo, new IRAPICallback<IRDeviceAPIService.PostWifiResponse>() {
            @Override
            public void success(IRDeviceAPIService.PostWifiResponse postWifiResponse, Response response) {
                synchronized (state) {
                    if (!state.isFinished()) {
                        state.finish();
                        handler.removeCallbacks(runnable);
                        if (isSettingUpIRKit) {
                            changeToNormalWifi();
                        }
                    }
                }
            }

            @Override
            public void failure(RetrofitError error) {
                synchronized (state) {
                    if (!state.isFinished()) {
                        state.finish();
                        handler.removeCallbacks(runnable);
                        if (isSettingUpIRKit) {
                            Log.e(TAG, "connectIRKitToWifi failure: " + error.getMessage() + "; retrying");

                            if (error.getMessage().equals("Machine is not on the network")) {
                                // IRKit Wi-Fi is no longer available (maybe success)
                                if (isSettingUpIRKit) {
                                    changeToNormalWifi();
                                }
                            } else {
                                // Wait 1000ms before retry
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        connectIRKitToWifi(retryCount + 1);
                                    }
                                }, 1000);
                            }
                        }
                    }
                }
            }
        });
    }

    private void revertToNormalWifi() {
        IRKit.sharedInstance().unforceIRKitWifi();
        if (normalWifiConfiguration != null) {
            IRKit.sharedInstance().connectToNormalWifi(normalWifiConfiguration, new IRKit.WifiConnectionChangeListener() {
                @Override
                public void onTargetWifiConnected(WifiInfo wifiInfo, NetworkInfo networkInfo) {
                    // Reverted to normal Wi-Fi
                }

                @Override
                public void onError(String reason) {
                    Log.e(TAG, "revertToNormalWifi error: " + reason);
                }

                @Override
                public void onTimeout() {
                    // this method should never be called
                    Log.e(TAG, "revertToNormalWifi timeout");
                }
            });
        } else {
            IRKit.sharedInstance().disconnectFromCurrentWifi();
            if (!isOriginallyWifiEnabled) {
                IRKit.sharedInstance().setWifiEnabled(false);
            }
        }
        removeIRKitWifiConfiguration();
    }

    private void changeToNormalWifi() {
        if (!isSettingUpIRKit) {
            return;
        }

        IRKit.sharedInstance().unforceIRKitWifi();
        if (normalWifiConfiguration != null) {
            irKitConnectWifiListener.onStatus(context.getString(R.string.setup_status__connecting_to_normal_wifi));
            IRKit.sharedInstance().connectToNormalWifi(normalWifiConfiguration, new IRKit.WifiConnectionChangeListener() {
                @Override
                public void onTargetWifiConnected(WifiInfo wifiInfo, NetworkInfo networkInfo) {
                    // TODO: Is 500 ms delay enough?
                    if (isSettingUpIRKit) {
                        // Changed to normal Wi-Fi. Wait to settle.
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                checkConnectivity();
                            }
                        }, 500);
                    }
                }

                @Override
                public void onError(String reason) {
                    Log.e(TAG, "changeToNormalWifi error: " + reason);
                    if (isSettingUpIRKit) {
                        isSettingUpIRKit = false;
                        irKitConnectWifiListener.onError(context.getString(R.string.setup_status__error_connect_to_normal_wifi) + reason);
                    }
                }

                @Override
                public void onTimeout() {
                    // this method should never be called
                    Log.e(TAG, "changeToNormalWifi timeout");
                    if (isSettingUpIRKit) {
                        isSettingUpIRKit = false;
                        irKitConnectWifiListener.onError(context.getString(R.string.setup_status__error_timeout_connect_to_normal_wifi));
                    }
                }
            });
        } else {
            irKitConnectWifiListener.onStatus(context.getString(R.string.setup_status__disconnecting_from_irkit_wifi));
            IRKit.sharedInstance().disconnectFromCurrentWifi();
            if (!isOriginallyWifiEnabled) {
                IRKit.sharedInstance().setWifiEnabled(false);
            }
            // Wait 500 ms to settle
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkConnectivity();
                }
            }, 500);
        }
        removeIRKitWifiConfiguration();
    }

    private void checkConnectivity() {
        checkConnectivity(0);
    }

    private void checkConnectivity(final int retryCount) {
        if (!isSettingUpIRKit) {
            return;
        }
        irKitConnectWifiListener.onStatus(context.getString(R.string.setup_status__waiting_for_postdoor));

        // timeout handler
        final Handler handler = new Handler();
        checkConnectivityState = new IRState();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                synchronized (checkConnectivityState) {
                    if (!checkConnectivityState.isFinished()) {
                        checkConnectivityState.finish();
                        if (isSettingUpIRKit) {
                            Log.e(TAG, "checkConnectivity timed out");
                            isSettingUpIRKit = false;
                            IRKit.sharedInstance().getHTTPClient().cancelPostDoor();
                            irKitConnectWifiListener.onError(context.getString(R.string.setup_status__error_timeout_postdoor));
                        }
                    }
                }
            }
        };
        handler.postDelayed(runnable, 30000);

        // We do not perform Bonjour discovery
        isIRKitFound = true;

//            isIRKitFound = false;
//            startIRKitDiscovery(new IRAPIResult() {
//                @Override
//                public void onSuccess() {
//                    if (isSettingUpIRKit) {
//                        isIRKitFound = true;
//                        checkBothDoorAndLocal();
//                    }
//                }
//
//                @Override
//                public void onError(IRAPIError error) {
//                    Log.e(TAG, "startIRKitDiscovery error: " + error.message);
//                    synchronized (checkConnectivityState) {
//                        if (!checkConnectivityState.isFinished()) {
//                            checkConnectivityState.finish();
//                            if (isSettingUpIRKit) {
//                                isSettingUpIRKit = false;
//                                IRKit.sharedInstance().getHTTPClient().cancelPostDoor();
//                                irKitConnectWifiListener.onError(context.getString(R.string.setup_status__error_bonjour_discovery));
//                            }
//                        }
//                    }
//                }
//
//                @Override
//                public void onTimeout() {
//                    synchronized (checkConnectivityState) {
//                        if (!checkConnectivityState.isFinished()) {
//                            checkConnectivityState.finish();
//                            if (isSettingUpIRKit) {
//                                isSettingUpIRKit = false;
//                                IRKit.sharedInstance().getHTTPClient().cancelPostDoor();
//                                irKitConnectWifiListener.onError(context.getString(R.string.setup_status__error_timeout_bonjour_discovery));
//                            }
//                        }
//                    }
//                }
//            });

        IRKit.sharedInstance().getHTTPClient().waitForDoor(setupDeviceId, new IRAPICallback<IRInternetAPIService.PostDoorResponse>() {
            @Override
            public void success(IRInternetAPIService.PostDoorResponse postDoorResponse, Response response) {
                if (isSettingUpIRKit) {
                    isPostDoorSuccess = true;
                    if (!isIRKitFound) {
                        irKitConnectWifiListener.onStatus(context.getString(R.string.setup_status__waiting_for_bonjour_discovery));
                    }
                    checkBothDoorAndLocal();
                }
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "postdoor failure: " + error.getMessage());
                if (!isSettingUpIRKit) {
                    return;
                }
                if (error.getKind() == RetrofitError.Kind.NETWORK && retryCount < 1) {
                    // Wait 1000ms and retry
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            checkConnectivity(retryCount + 1);
                        }
                    }, 1000);
                } else {
                    if (isSettingUpIRKit) {
                        isSettingUpIRKit = false;
                        irKitConnectWifiListener.onError(context.getString(R.string.setup_status__error_postdoor));
                    }
                }
            }
        });
    }

//        private void startIRKitDiscovery(final IRAPIResult result) {
//            startIRKitDiscovery(result, 0);
//        }
//
//        private void startIRKitDiscovery(final IRAPIResult result, final int retryCount) {
//            discoveryResult = result;
//            if (retryCount > 5) {
//                result.onTimeout();
//                return;
//            }
//            IRKit irkit = IRKit.sharedInstance();
//            irkit.setIRKitEventListener(this);
//            irkit.stopServiceDiscovery();
//            irkit.startServiceDiscovery();
//            final Handler handler = new Handler();
//            final Runnable runnable = new Runnable() {
//                @Override
//                public void run() {
//                    if (isSettingUpIRKit && !isIRKitFound) {
//                        startIRKitDiscovery(result, retryCount + 1);
//                    }
//                }
//            };
//            handler.postDelayed(runnable, 10000);
//        }

    public void cancel() {
        if (isSettingUpIRKit) {
            isSettingUpIRKit = false;
            IRKit.sharedInstance().getHTTPClient().cancelPostDoor();
            revertToNormalWifi();
        }
        IRKit.sharedInstance().unforceIRKitWifi();
    }

    private void checkBothDoorAndLocal() {
        if (isIRKitFound && isPostDoorSuccess) {
            synchronized (checkConnectivityState) {
                if (!checkConnectivityState.isFinished()) {
                    checkConnectivityState.finish();
                    if (isSettingUpIRKit) {
                        isSettingUpIRKit = false;

                        String irkitHostname = IRWifiInfo.getRawSSID(irkitWifiSSID);
                        IRPeripherals peripherals = IRKit.sharedInstance().peripherals;
                        IRPeripheral peripheral = peripherals.getPeripheral(irkitHostname);
                        if (peripheral == null) {  // new IRKit
                            peripheral = new IRPeripheral();
                            peripheral.setHostname(irkitHostname);
                            peripheral.setCustomizedName(irkitHostname);
                            peripherals.add(peripheral);
                        }
                        peripheral.setDeviceId(setupDeviceId);
                        peripherals.save();

                        irKitConnectWifiListener.onComplete();
                    }
                }
            }
        }
    }

    @Override
    public void onNewIRKitFound(IRPeripheral peripheral) {
        if (peripheral.getHostname().toLowerCase().equals(IRWifiInfo.getRawSSID(irkitWifiSSID).toLowerCase())) {
            peripheral.setDeviceId(setupDeviceId);
            IRKit.sharedInstance().peripherals.save();

            onDiscoverIRKit(peripheral);
        }
    }

    @Override
    public void onExistingIRKitFound(IRPeripheral peripheral) {
        if (peripheral.getHostname().toLowerCase().equals(IRWifiInfo.getRawSSID(irkitWifiSSID).toLowerCase())) {
            // IRKit already exists in peripherals. Renew the device id.
            peripheral.setDeviceId(setupDeviceId);
            IRKit.sharedInstance().peripherals.save();

            onDiscoverIRKit(peripheral);
        }
    }

    private void onDiscoverIRKit(final IRPeripheral peripheral) {
        if (!isSettingUpIRKit) {
            return;
        }

        if (peripheral.hasModelInfo()) {
            discoveryResult.onSuccess();
            return;
        }

        peripheral.setListener(new IRPeripheral.IRPeripheralListener() {
            @Override
            public void onErrorFetchingDeviceId(String message) {
                // This method should never be called
                Log.e(TAG, "onDiscoverIRKit: onErrorFetchingDeviceId: " + message);
            }

            @Override
            public void onDeviceIdStatusChange() {
                // This method should never be called
                Log.e(TAG, "onDiscoverIRKit: onDeviceIdStatusChange");
            }

            @Override
            public void onFetchDeviceIdSuccess() {
                // This method should never be called
                Log.e(TAG, "onDiscoverIRKit: onFetchDeviceIdSuccess");
            }

            @Override
            public void onFetchModelInfoSuccess() {
                if (isSettingUpIRKit) {
                    discoveryResult.onSuccess();
                }
            }

            @Override
            public void onErrorFetchingModelInfo(String message) {
                if (isSettingUpIRKit) {
                    isSettingUpIRKit = false;
                    Log.e(TAG, "error fetching model info: " + message);
                    IRKit.sharedInstance().getHTTPClient().cancelPostDoor();
                    irKitConnectWifiListener.onError("Error occurred during setting up IRKit");
                }
            }
        });

        // Do not call fetchModelInfo() here, as it will be
        // called in serviceResolved().
    }
}
