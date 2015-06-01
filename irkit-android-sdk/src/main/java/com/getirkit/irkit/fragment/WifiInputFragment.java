package com.getirkit.irkit.fragment;

import android.graphics.Typeface;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.getirkit.irkit.R;
import com.getirkit.irkit.net.IRHTTPClient;
import com.getirkit.irkit.IRKit;
import com.getirkit.irkit.IRWifiInfo;
import com.getirkit.irkit.net.IRAPICallback;
import com.getirkit.irkit.net.IRInternetAPIService;

import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * View for entering Wi-Fi info which will be used by IRKit.
 */
public class WifiInputFragment extends Fragment {
    public static final String TAG = WifiInputFragment.class.getName();
    private View rootView;
    private WifiInputFragmentListener wifiInputFragmentListener;

    public interface WifiInputFragmentListener {
        void onClickOK(IRWifiInfo irWifiInfo);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (rootView != null) {
            CheckBox showPasswordCheckBox = (CheckBox) rootView.findViewById(R.id.wifi__showpassword);
            outState.putBoolean("showPassword", showPasswordCheckBox.isChecked());
            EditText ssidEditText = (EditText) rootView.findViewById(R.id.wifi__ssid_field);
            outState.putString("ssid", ssidEditText.getText().toString());
            EditText passwordEditText = (EditText) rootView.findViewById(R.id.wifi__password_field);
            outState.putString("password", passwordEditText.getText().toString());
            Spinner securitySpinner = (Spinner) rootView.findViewById(R.id.wifi__security_spinner);
            int security = securitySpinner.getSelectedItemPosition();
            outState.putInt("security", security);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle args = savedInstanceState;
        if (args == null) {
            args = getArguments();
        }

        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_wifi_input, container, false);
        final EditText ssidEditText = (EditText) rootView.findViewById(R.id.wifi__ssid_field);
        CheckBox showPasswordCheckBox = (CheckBox) rootView.findViewById(R.id.wifi__showpassword);
        final EditText passwordEditText = (EditText) rootView.findViewById(R.id.wifi__password_field);
        final Spinner securitySpinner = (Spinner) rootView.findViewById(R.id.wifi__security_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.wifi_security_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        securitySpinner.setAdapter(adapter);
        securitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long rowId) {
                if (position == IRWifiInfo.SECURITY_NONE) {
                    hidePasswordForm();
                } else {
                    showPasswordForm();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                showPasswordForm();
            }
        });

        int security = args.getInt("security");  // default is 0
        securitySpinner.setSelection(security);

        String ssid = args.getString("ssid");
        if (ssid == null) {
            if (IRKit.sharedInstance().isWifiConnected()) {
                // Fill SSID and security using current Wi-Fi configuration

                WifiConfiguration wifiConfig = IRKit.sharedInstance().getCurrentWifiConfiguration();
                if (wifiConfig != null && !IRWifiInfo.isIRKitWifi(wifiConfig)) {
                    // Get SSID
                    ssidEditText.setText(IRWifiInfo.getRawSSID(wifiConfig.SSID));

                    // Get security
                    // NOTE: We never can read the actual password.
                    //       preSharedKey and wepKeys[0] will return just "*" if they are set.
                    if (wifiConfig.preSharedKey != null) {
                        // Detected WPA
                        securitySpinner.setSelection(IRWifiInfo.SECURITY_WPA_WPA2);
                    } else if (wifiConfig.wepKeys != null && wifiConfig.wepKeys[0] != null) {
                        // Detected WEP
                        securitySpinner.setSelection(IRWifiInfo.SECURITY_WEP);
                    } else {
                        // Detected open network (no password)
                        securitySpinner.setSelection(IRWifiInfo.SECURITY_NONE);
                    }
                }
            }
        } else {
            ssidEditText.setText(ssid);
        }
        String password = args.getString("password");
        if (password != null) {
            passwordEditText.setText(password);
        }

        // Don't call passwordEditText.requestFocus(), as it makes
        // keyboard difficult to appear on Android 2.3

        if (args != null) {
            showPasswordCheckBox.setChecked(args.getBoolean("showPassword", false));
        } else {
            showPasswordCheckBox.setChecked(false);
        }
        if (showPasswordCheckBox.isChecked()) {
            // show password
            passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT);
            passwordEditText.setTypeface(Typeface.MONOSPACE);
        } else {
            // hide password
            passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            passwordEditText.setTypeface(Typeface.MONOSPACE);
        }
        showPasswordCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean checked = ((CheckBox) view).isChecked();
                if (checked) {
                    // show password
                    passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT);
                    passwordEditText.setTypeface(Typeface.MONOSPACE);
                } else {
                    // hide password
                    passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    passwordEditText.setTypeface(Typeface.MONOSPACE);
                }
            }
        });

        Button okButton = (Button) rootView.findViewById(R.id.wifi__ok_button);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (wifiInputFragmentListener != null) {
                    IRWifiInfo wifiInfo = new IRWifiInfo();
                    wifiInfo.setSSID(ssidEditText.getText().toString());
                    int security = securitySpinner.getSelectedItemPosition();
                    wifiInfo.setSecurity(security);
                    if (security != IRWifiInfo.SECURITY_NONE) {
                        String password = passwordEditText.getText().toString();
                        if (password.equals("")) {
                            // Error: password is empty
                            Toast.makeText(getActivity(), R.string.wifi_input__enter_wifi_password, Toast.LENGTH_SHORT).show();
                            return;
                        } else {
                            wifiInfo.setPassword(password);
                        }
                    }
                    wifiInputFragmentListener.onClickOK(wifiInfo);
                }
            }
        });

        String apiKey = getArguments().getString("apiKey");
        final IRHTTPClient httpClient = IRKit.sharedInstance().getHTTPClient();
        httpClient.ensureRegisteredAndCall(apiKey, new IRAPICallback<IRInternetAPIService.PostClientsResponse>() {
            @Override
            public void success(IRInternetAPIService.PostClientsResponse postClientsResponse, Response response) {
                httpClient.obtainDeviceKey(new IRAPICallback<IRInternetAPIService.PostDevicesResponse>() {
                    @Override
                    public void success(IRInternetAPIService.PostDevicesResponse postDevicesResponse, Response response) {
                        // nop
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Log.e(TAG, "Failed to get devicekey: " + error.getMessage());
                        // nop
                    }
                });
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "Failed to get clientkey: " + error.getMessage());
            }
        });

        return rootView;
    }

    private void hidePasswordForm() {
        setPasswordFormVisibility(false);
    }

    private void showPasswordForm() {
        setPasswordFormVisibility(true);
    }

    private void setPasswordFormVisibility(boolean isVisible) {
        TextView passwordLabel = (TextView) rootView.findViewById(R.id.wifi__password_label);
        EditText passwordEditText = (EditText) rootView.findViewById(R.id.wifi__password_field);
        CheckBox showPasswordCheckBox = (CheckBox) rootView.findViewById(R.id.wifi__showpassword);
        int visibility;
        if (isVisible) {
            visibility = View.VISIBLE;
        } else {
            visibility = View.GONE;
        }
        passwordLabel.setVisibility(visibility);
        passwordEditText.setVisibility(visibility);
        showPasswordCheckBox.setVisibility(visibility);
        rootView.invalidate();  // this is necessary
    }

    public WifiInputFragmentListener getWifiInputFragmentListener() {
        return wifiInputFragmentListener;
    }

    public void setWifiInputFragmentListener(WifiInputFragmentListener wifiInputFragmentListener) {
        this.wifiInputFragmentListener = wifiInputFragmentListener;
    }
}
