package com.getirkit.irkit.activity;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;

import com.getirkit.irkit.IRKit;
import com.getirkit.irkit.IRWifiInfo;
import com.getirkit.irkit.R;
import com.getirkit.irkit.fragment.IRKitPasswordFragment;
import com.getirkit.irkit.fragment.TurnOnIRKitFragment;
import com.getirkit.irkit.fragment.WifiConnectFragment;
import com.getirkit.irkit.fragment.WifiInputFragment;

/**
 * 新しいIRKitデバイスをユーザにセットアップさせるための画面を表示します。
 * IRKitが家のWi-Fi接続情報を記録している状態ではセットアップできません。
 *
 * Show UI for setting up a new IRKit. IRKit cannot be set up if
 * it previously connected to a Wi-Fi.
 */
public class IRKitSetupActivity extends ActionBarActivity {
    public static final String TAG = IRKitSetupActivity.class.getSimpleName();

    private int currentScreen = 0;
    private IRWifiInfo irWifiInfo;
    private String irkitWifiPassword;
    private String apiKey;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("currentScreen", currentScreen);
        outState.putParcelable("irWifiInfo", irWifiInfo);
        outState.putString("irkitWifiPassword", irkitWifiPassword);
        outState.putString("apiKey", apiKey);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            currentScreen = savedInstanceState.getInt("currentScreen");
            irWifiInfo = savedInstanceState.getParcelable("irWifiInfo");
            irkitWifiPassword = savedInstanceState.getString("irkitWifiPassword");
        }

        setResult(RESULT_CANCELED);
        setContentView(R.layout.activity_irkitsetup);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        Bundle args = intent.getExtras();

        if (savedInstanceState != null) {
            args = savedInstanceState;
        }

        if (args != null) {
            apiKey = args.getString("apiKey");
        }
        IRKit irkit = IRKit.sharedInstance();

        // If apiKey argument is not provided, read it from AndroidManifest.xml
        if (apiKey == null) {
            apiKey = irkit.getIRKitAPIKey();
        }

        irkit.init(getApplicationContext());
        irkit.registerClient(apiKey);

        if (currentScreen == 3) {
            goToScreen(currentScreen, true, true, false);
        } else {
            goToScreen(currentScreen, true, false, false);
        }
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu items for use in the action bar
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.irkit_setup_activity_actions, menu);
//        if (currentScreen == 3) {
//            menu.findItem(R.id.activity_irkit_setup__action_next).setEnabled(false);
//            menu.findItem(R.id.activity_irkit_setup__action_next).setVisible(false);
//        } else {
//            menu.findItem(R.id.activity_irkit_setup__action_next).setEnabled(true);
//            menu.findItem(R.id.activity_irkit_setup__action_next).setVisible(true);
//        }
//        return super.onCreateOptionsMenu(menu);
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            IRKit.sharedInstance().cancelIRKitSetup();
            finish();
            return true;
//        } else if (id == R.id.activity_irkit_setup__action_next) {
//            goToNextScreen();
//            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void goToPreviousScreen() {
        if (currentScreen == 0) {
            finish();
        } else {
            if (currentScreen == 3) {
                IRKit.sharedInstance().cancelIRKitSetup();
            }
            goToScreen(--currentScreen, false, true, true);
        }
    }

    private void goToNextScreen() {
        goToScreen(++currentScreen, false);
    }

    private void goToScreen(int position, boolean isBack) {
        goToScreen(position, false, false, isBack);
    }

    private void goToScreen(int position, boolean noAnimation, boolean skipWifiCheck, boolean isBack) {
        Fragment fragment = null;
        String tag = "";
        if (position == 0) {
            tag = "TurnOnIRKitFragment";
            fragment = getSupportFragmentManager().findFragmentByTag(tag);
            if (fragment == null) {
                fragment = new TurnOnIRKitFragment();
            }
            ((TurnOnIRKitFragment) fragment).setTurnOnIRKitFragmentListener(new TurnOnIRKitFragment.TurnOnIRKitFragmentListener() {
                @Override
                public void onClickNext() {
                    goToNextScreen();
                }
            });
        } else if (position == 1) {
            tag = "WifiInputFragment";
            // First we have to try to get existing fragment, otherwise
            // onCreateView() of the fragment gets called twice.
            fragment = getSupportFragmentManager().findFragmentByTag(tag);
            if (fragment == null) {
                fragment = new WifiInputFragment();
                Bundle args = new Bundle();
                if (irWifiInfo != null) {
                    args.putString("ssid", irWifiInfo.getSSID());
                    args.putString("password", irWifiInfo.getPassword());
                    args.putInt("security", irWifiInfo.getSecurity());
                }
                args.putString("apiKey", apiKey);
                fragment.setArguments(args);
            }
            ((WifiInputFragment) fragment).setWifiInputFragmentListener(new WifiInputFragment.WifiInputFragmentListener() {
                @Override
                public void onClickOK(IRWifiInfo wifiInfo) {
                    irWifiInfo = wifiInfo;
                    goToNextScreen();
                }
            });
        } else if (position == 2) {
            tag = "IRKitPasswordFragment";
            fragment = getSupportFragmentManager().findFragmentByTag(tag);
            if (fragment == null) {
                fragment = new IRKitPasswordFragment();
                if (irkitWifiPassword != null) {
                    Bundle args = new Bundle();
                    args.putString("password", irkitWifiPassword);
                    fragment.setArguments(args);
                }
            }
            ((IRKitPasswordFragment) fragment).setIRKitPasswordFragmentListener(new IRKitPasswordFragment.IRKitPasswordFragmentListener() {
                @Override
                public void onClickOK(String password) {
                    irkitWifiPassword = password;
                    goToNextScreen();
                }
            });
        } else if (position == 3) {
            // Request that the system call onCreateOptionsMenu()
            // (though documentation says it is onPrepareOptionsMenu())
//                    supportInvalidateOptionsMenu();

            tag = "WifiConnectFragment";
            fragment = getSupportFragmentManager().findFragmentByTag(tag);
            if (fragment == null) {
                fragment = new WifiConnectFragment();
                Bundle args = new Bundle();
                args.putParcelable("connectDestination", irWifiInfo);
                args.putString("irkitWifiPassword", irkitWifiPassword);
                args.putString("apiKey", apiKey);
                fragment.setArguments(args);
            }
            WifiConnectFragment wifiConnectFragment = (WifiConnectFragment) fragment;
            wifiConnectFragment.setWifiConnectFragmentListener(new WifiConnectFragment.WifiConnectFragmentListener() {
                @Override
                public void onSuccess() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setResult(RESULT_OK);
                            finish();
                        }
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            goToPreviousScreen();
                        }
                    });
                }
            });
        }
        if (fragment != null) {
            // update the main content by replacing fragments
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            if (!noAnimation) {
                if (isBack) {
                    transaction.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                } else {
                    transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
                }
            }
            transaction.replace(R.id.irkitsetup__fragment_container, fragment, tag)
                    .commit();
        } else {
            Log.e(TAG, "fragment is null");
        }
    }

    @Override
    public void onBackPressed() {
        goToPreviousScreen();
    }
}
