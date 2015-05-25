package com.getirkit.example.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.getirkit.irkit.IRKit;
import com.getirkit.irkit.IRKitEventListener;
import com.getirkit.irkit.IRPeripheral;
import com.getirkit.irkit.IRPeripherals;
import com.getirkit.irkit.IRSignal;
import com.getirkit.irkit.activity.DeviceActivity;
import com.getirkit.irkit.activity.IRKitSetupActivity;
import com.getirkit.irkit.activity.SignalActivity;
import com.getirkit.irkit.activity.WaitSignalActivity;
import com.getirkit.example.R;
import com.getirkit.example.adapter.DeviceListAdapter;
import com.getirkit.example.adapter.SignalListAdapter;
import com.getirkit.example.fragment.DevicesFragment;
import com.getirkit.example.fragment.NavigationDrawerFragment;
import com.getirkit.example.fragment.SelectSignalActionDialogFragment;
import com.getirkit.example.fragment.SignalsFragment;
import com.getirkit.irkit.net.IRAPIError;
import com.getirkit.irkit.net.IRAPIResult;

public class MainActivity extends AppCompatActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks, IRKitEventListener, DevicesFragment.DevicesFragmentListener,
        SelectSignalActionDialogFragment.SelectSignalActionDialogFragmentListener, SignalsFragment.SignalsFragmentListener {
    public static final String TAG = MainActivity.class.getSimpleName();

    // Activity request codes
    private static final int REQUEST_IRKIT_SETUP = 1;
    private static final int REQUEST_SIGNAL_DETAIL = 2;
    private static final int REQUEST_WAIT_SIGNAL = 3;
    private static final int REQUEST_DEVICE_DETAIL = 4;

    private int currentSection;
    private int editingPeripheralPosition = -1;
    private int selectedSignalPosition = -1;
    private SignalListAdapter signalListAdapter;
    private DeviceListAdapter deviceListAdapter;

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        if (mTitle == null) {
            mTitle = getTitle();
        }

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        if (savedInstanceState != null) {
            editingPeripheralPosition = savedInstanceState.getInt("editingPeripheralPosition");
            selectedSignalPosition = savedInstanceState.getInt("selectedSignalPosition");
        }

        IRKit irkit = IRKit.sharedInstance();
        irkit.setIRKitEventListener(this);

        // Set context, then initialize if not initialized yet.
        // contextをセットして初期化する。すでに初期化済みの場合は
        // contextのセットのみ行うので何回呼んでもOK。
        irkit.init(getApplicationContext());

        // Show dialog after orientation change (support library bug?)
        showSelectSignalActionDialogIfNeeded();
    }

    /**
     * Show SelectSignalActionDialog if the dialog exists in fragment manager.
     */
    private void showSelectSignalActionDialogIfNeeded() {
        final SelectSignalActionDialogFragment dialog = (SelectSignalActionDialogFragment) getSupportFragmentManager().findFragmentByTag("SelectSignalActionDialogFragment");
        if (dialog != null) {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    dialog.show(getSupportFragmentManager(), "SelectSignalActionDialogFragment");
                }
            });
        }
    }

    /**
     * Save UI state
     *
     * @param outState
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Save UI state changes to the outState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        outState.putInt("editingPeripheralPosition", editingPeripheralPosition);
        outState.putInt("selectedSignalPosition", selectedSignalPosition);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IRKit irkit = IRKit.sharedInstance();
        irkit.setIRKitEventListener(this);

        // Start Bonjour discovery
        // Bonjour検索を開始
        irkit.startServiceDiscovery();

        // Watch Wi-Fi state change
        // Wi-Fi状態の変化を監視する
        irkit.registerWifiStateChangeListener();

        // Get clientkey if we have not received it yet
        // clientkeyをまだ取得していない場合は取得する
        irkit.registerClient();
    }

    @Override
    protected void onPause() {
        super.onPause();
        IRKit irkit = IRKit.sharedInstance();

        // Stop Bonjour discovery
        // Bonjour検索を停止
        irkit.stopServiceDiscovery();

        // Unset listener
        // リスナーを解除
        irkit.setIRKitEventListener(null);

        // Unwatch Wi-Fi state change
        // Wi-Fi状態の変化の監視をやめる
        irkit.unregisterWifiStateChangeListener();
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        String tag = String.valueOf(position + 1);
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
        if (fragment == null) {
            if (position == 0) {  // Signals (Buttons)
                fragment = SignalsFragment.newInstance(position + 1);
            } else if (position == 1) {  // Devices
                fragment = DevicesFragment.newInstance(position + 1);
            } else {
                throw new IllegalStateException("Unknown drawer item position: " + position);
            }
        }

        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, fragment, tag)
                .commit();
    }

    public void onSectionAttached(int number) {
        currentSection = number;
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_signals);
                break;
            case 2:
                mTitle = getString(R.string.title_devices);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            if (currentSection == 1) {  // Signals (Buttons)
                getMenuInflater().inflate(R.menu.signals, menu);
            } else if (currentSection == 2) {  // Devices
                getMenuInflater().inflate(R.menu.devices, menu);
            }
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.menu_signals__add) {
            // Start WaitSignalActivity
            Intent intent = new Intent(this, WaitSignalActivity.class);
            startActivityForResult(intent, REQUEST_WAIT_SIGNAL);
            return true;
        } else if (id == R.id.menu_devices__add) {
            // Start IRKitSetupActivity
            Intent intent = new Intent(this, IRKitSetupActivity.class);
            startActivityForResult(intent, REQUEST_IRKIT_SETUP);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IRKIT_SETUP) {  // Returned from IRKitSetupActivity
            if (resultCode == RESULT_OK) {
                if (deviceListAdapter != null) {
                    deviceListAdapter.notifyDataSetChanged();
                }
            }
        } else if (requestCode == REQUEST_WAIT_SIGNAL) {  // Returned from WaitSignalActivity
            if (resultCode == RESULT_OK) {
                Bundle args = data.getExtras();
                IRSignal signal = args.getParcelable("signal");
                if (signal == null) {
                    Log.e(TAG, "failed to receive signal");
                    return;
                }
                Log.d(TAG, "received signal: " + signal);
                IRKit irkit = IRKit.sharedInstance();
                signal.setId(irkit.signals.getNewId());

                if (signal.hasBitmapImage()) {
                    // Do not call renameToSuggestedImageFilename before
                    // assigning an ID to the signal.
                    if (!signal.renameToSuggestedImageFilename(this)) {
                        Log.e(TAG, "Failed to rename bitmap file");
                    }
                }

                // Add and save the signal
                irkit.signals.add(signal);
                irkit.signals.save();
                if (signalListAdapter != null) {
                    signalListAdapter.notifyDataSetChanged();
                }
            }
        } else if (requestCode == REQUEST_DEVICE_DETAIL) {  // Returned from DeviceActivity
            if (resultCode == RESULT_OK) {
                Bundle args = data.getExtras();
                String action = args.getString("action");
                IRPeripheral peripheral = args.getParcelable("peripheral");
                switch (action) {
                    case "save":
                        saveEditingPeripheral(peripheral);
                        break;
                    case "delete":
                        deleteEditingPeripheral();
                        break;
                    default:
                        Log.e(TAG, "unknown action: " + action);
                        break;
                }
            }
            editingPeripheralPosition = -1;
        } else if (requestCode == REQUEST_SIGNAL_DETAIL) {
            if (resultCode == RESULT_OK) {
                Bundle args = data.getExtras();
                String action = args.getString("action");
                IRSignal signal = args.getParcelable("signal");
                IRKit irkit = IRKit.sharedInstance();
                switch (action) {
                    case "save":
                        if (selectedSignalPosition != -1) {
                            IRSignal signalToEdit = irkit.signals.get(selectedSignalPosition);
                            if (signalToEdit != null) {
                                signalToEdit.copyFrom(signal, this);
                            }
                            irkit.signals.save();
                            if (signalListAdapter != null) {
                                signalListAdapter.notifyDataSetChanged();
                            }
                        }
                        break;
                    case "delete":
                        if (selectedSignalPosition != -1) {
                            irkit.signals.remove(selectedSignalPosition);
                            irkit.signals.save();
                            if (signalListAdapter != null) {
                                signalListAdapter.notifyDataSetChanged();
                            }
                        }
                        break;
                    default:
                        Log.e(TAG, "unknown action: " + action);
                        break;
                }
            }
            selectedSignalPosition = -1;
        } else {
            Log.e(TAG, "unknown requestCode: " + requestCode);
        }
    }

    private void saveEditingPeripheral(IRPeripheral peripheral) {
        if (editingPeripheralPosition == -1) {
            Log.e(TAG, "invalid editingPeripheral");
            return;
        }
        IRPeripherals peripherals = IRKit.sharedInstance().peripherals;
        IRPeripheral editingPeripheral = peripherals.get(editingPeripheralPosition);
        if (editingPeripheral == null) {
            Log.e(TAG, "editingPeripheral is null");
            return;
        }
        editingPeripheral.setCustomizedName(peripheral.getCustomizedName());
        peripherals.save();
        if (deviceListAdapter != null) {
            deviceListAdapter.notifyDataSetChanged();
        }
    }

    private void deleteEditingPeripheral() {
        if (editingPeripheralPosition == -1) {
            Log.e(TAG, "Invalid editingPeripheral");
            return;
        }
        IRPeripheral peripheral = IRKit.sharedInstance().peripherals.get(editingPeripheralPosition);
        if (peripheral == null) {
            Log.e(TAG, "editingPeripheral is null");
            return;
        }

        // Remove device and also signals tied to the device
        IRKit irkit = IRKit.sharedInstance();
        IRPeripheral removedPeripheral = irkit.peripherals.remove(editingPeripheralPosition);
        irkit.peripherals.save();
        irkit.signals.removeIRSignalsForDeviceId(removedPeripheral.getDeviceId());
        irkit.signals.save();
        if (deviceListAdapter != null) {
            deviceListAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Called when a new IRKit is found.
     * 新しいIRKitが見つかったときに呼ばれるメソッド。
     *
     * @param peripheral
     */
    @Override
    public void onNewIRKitFound(IRPeripheral peripheral) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, R.string.new_irkit_found, Toast.LENGTH_SHORT).show();
                if (deviceListAdapter != null) {
                    deviceListAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    /**
     * Called when an existing IRKit is found.
     * 既存のIRKitが見つかったときに呼ばれるメソッド。
     *
     * @param peripheral
     */
    @Override
    public void onExistingIRKitFound(IRPeripheral peripheral) {
    }

    @Override
    public void onDeviceClick(int position) {
        final IRPeripheral peripheral = IRKit.sharedInstance().peripherals.get(position);

        // Start DeviceActivity
        Bundle args = new Bundle();
        args.putParcelable("peripheral", peripheral);
        editingPeripheralPosition = position;
        Intent intent = new Intent(this, DeviceActivity.class);
        intent.putExtras(args);
        startActivityForResult(intent, MainActivity.REQUEST_DEVICE_DETAIL);
    }

    @Override
    public void onSelectSignalActionSend() {
        if (selectedSignalPosition == -1) {
            return;
        }
        final IRSignal signal = IRKit.sharedInstance().signals.get(selectedSignalPosition);
        if (signal != null) {
            IRKit.sharedInstance().sendSignal(signal, new IRAPIResult() {
                @Override
                public void onSuccess() {
                }

                @Override
                public void onError(IRAPIError error) {
                    String msg = "Error sending " + signal.getName() + ": " + error.message;
                    Log.e(TAG, msg);
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onTimeout() {
                    String msg = "Error sending " + signal.getName() + ": timeout";
                    Log.e(TAG, msg);
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onSelectSignalActionEdit() {
        if (selectedSignalPosition == -1) {
            return;
        }
        final IRSignal signal = IRKit.sharedInstance().signals.get(selectedSignalPosition);
        if (signal != null) {
            // Start SignalActivity
            Bundle args = new Bundle();
            args.putInt("mode", SignalActivity.MODE_EDIT);
            args.putParcelable("signal", signal);
            Intent intent = new Intent(this, SignalActivity.class);
            intent.putExtras(args);
            startActivityForResult(intent, REQUEST_SIGNAL_DETAIL);
        }
    }

    @Override
    public void onSignalClick(int position) {
        selectedSignalPosition = position;
        SelectSignalActionDialogFragment dialog = new SelectSignalActionDialogFragment();
        dialog.show(getSupportFragmentManager(), "SelectSignalActionDialogFragment");
    }

    public SignalListAdapter getSignalListAdapter() {
        return signalListAdapter;
    }

    public void setSignalListAdapter(SignalListAdapter signalListAdapter) {
        this.signalListAdapter = signalListAdapter;
    }

    public DeviceListAdapter getDeviceListAdapter() {
        return deviceListAdapter;
    }

    public void setDeviceListAdapter(DeviceListAdapter deviceListAdapter) {
        this.deviceListAdapter = deviceListAdapter;
    }
}
