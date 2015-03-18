package com.getirkit.irkit.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.getirkit.irkit.IRKit;
import com.getirkit.irkit.IRWifiInfo;
import com.getirkit.R;

/**
 * View to showing the status of ongoing setup.
 */
public class WifiConnectFragment extends Fragment implements IRKit.IRKitConnectWifiListener {
    public interface WifiConnectFragmentListener {
        public void onSuccess();
        public void onError(String message);
    }

    public static final String TAG = WifiConnectFragment.class.getSimpleName();

    private String status;
    private boolean isErrorOccurred = false;
    private String errorMessage;

    private ProgressBar progressBar;
    private TextView statusTextView;
    private TextView tipsTextView;
    private ImageView imageView;
    private Button closeButton;
    private WifiConnectFragmentListener wifiConnectFragmentListener;

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("status", this.status);
        outState.putBoolean("isErrorOccurred", this.isErrorOccurred);
        outState.putString("errorMessage", this.errorMessage);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStatus(String status) {
        setStatus(status);
    }

    @Override
    public void onError(String message) {
        showError(message);
    }

    @Override
    public void onComplete() {
        setStatus(getString(R.string.wifi_connect__success));
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    imageView.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (wifiConnectFragmentListener != null) {
                                wifiConnectFragmentListener.onSuccess();
                            }
                        }
                    }, 1000);
                }
            });
        }
    }

    public WifiConnectFragmentListener getWifiConnectFragmentListener() {
        return wifiConnectFragmentListener;
    }

    public void setWifiConnectFragmentListener(WifiConnectFragmentListener wifiConnectFragmentListener) {
        this.wifiConnectFragmentListener = wifiConnectFragmentListener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_wifi_connect, container, false);

        progressBar = (ProgressBar) rootView.findViewById(R.id.wifi_connect__progressbar);

        statusTextView = (TextView) rootView.findViewById(R.id.wifi_connect__status);
        tipsTextView = (TextView) rootView.findViewById(R.id.wifi_connect__tips);
        tipsTextView.setMovementMethod(LinkMovementMethod.getInstance());

        imageView = (ImageView) rootView.findViewById(R.id.wifi_connect__imageview);
        closeButton = (Button) rootView.findViewById(R.id.wifi_connect__close_button);

        Bundle args = getArguments();
        IRWifiInfo irWifiInfo = args.getParcelable("connectDestination");
        String irkitWifiPassword = args.getString("irkitWifiPassword");
        String apiKey = args.getString("apiKey");

        if (savedInstanceState != null) {
            isErrorOccurred = savedInstanceState.getBoolean("isErrorOccurred");
            if (isErrorOccurred) {
                showError( savedInstanceState.getString("errorMessage") );
            } else {
                setStatus(savedInstanceState.getString("status"));
            }
        }
        if (!isErrorOccurred) {
            // IRKit instance is already initialized at IRKitSetupActivity.onCreate()
            IRKit.sharedInstance().setupIRKit(apiKey, irWifiInfo, irkitWifiPassword, this);
        }

        return rootView;
    }

    public void showError(final String message) {
        isErrorOccurred = true;
        errorMessage = message;
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    imageView.setImageResource(R.drawable.error);
                    imageView.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    statusTextView.setText(message);
                    tipsTextView.setVisibility(View.VISIBLE);
                    closeButton.setVisibility(View.VISIBLE);
                    closeButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (wifiConnectFragmentListener != null) {
                                wifiConnectFragmentListener.onError(message);
                            }
                        }
                    });
                }
            });
        }
    }

    public void setStatus(final String status) {
        this.status = status;
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    statusTextView.setText(status);
                }
            });
        }
    }
}
