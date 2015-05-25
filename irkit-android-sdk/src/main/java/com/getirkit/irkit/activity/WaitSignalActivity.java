package com.getirkit.irkit.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.getirkit.irkit.IRKit;
import com.getirkit.irkit.IRSignal;
import com.getirkit.irkit.R;
import com.getirkit.irkit.net.IRAPICallback;
import com.getirkit.irkit.net.IRInternetAPIService;

import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * 赤外線信号を学習します。
 * Learn an IR signal.
 */
public class WaitSignalActivity extends AppCompatActivity {
    public static final String TAG = WaitSignalActivity.class.getName();

    private static final int REQUEST_SIGNAL_DETAIL = 1;

    private String apiKey;
    private boolean isErrorOccurred;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("apiKey", apiKey);
        outState.putBoolean("isErrorOccurred", isErrorOccurred);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        setContentView(R.layout.activity_wait_signal);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        Bundle args = intent.getExtras();

        if (savedInstanceState != null) {
            args = savedInstanceState;
        }

        if (args != null) {
            apiKey = args.getString("apiKey");
            isErrorOccurred = args.getBoolean("isErrorOccurred");
        } else {
            isErrorOccurred = false;
        }
        IRKit irkit = IRKit.sharedInstance();
        irkit.init(getApplicationContext());
        // If apiKey argument is not provided (== null), it will be read from AndroidManifest.xml
        irkit.registerClient(apiKey);

        TextView notWorkingTextView = (TextView) findViewById(R.id.activity_wait_signal__not_working);
        notWorkingTextView.setMovementMethod(LinkMovementMethod.getInstance());

        Button closeButton = (Button) findViewById(R.id.activity_wait_signal__error_close_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        if (isErrorOccurred) {
            showError();
        }
    }

    private void showError() {
        ViewGroup normalViewGroup = (ViewGroup) findViewById(R.id.activity_wait_signal__scrollview);
        normalViewGroup.setVisibility(View.GONE);
        ViewGroup errorViewGroup = (ViewGroup) findViewById(R.id.activity_wait_signal__error);
        errorViewGroup.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isErrorOccurred) {
            IRKit.sharedInstance().getHTTPClient().waitForSignal(new NewSignalCallback<IRInternetAPIService.GetMessagesResponse>(), true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        IRKit.sharedInstance().getHTTPClient().cancelRequests();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    class NewSignalCallback<T> implements IRAPICallback<IRInternetAPIService.GetMessagesResponse> {
        @Override
        public void success(IRInternetAPIService.GetMessagesResponse getMessagesResponse, Response response) {
            IRSignal registeringSignal = new IRSignal();
            registeringSignal.setDeviceId(getMessagesResponse.deviceid);
            registeringSignal.setFrequency((float) getMessagesResponse.message.freq);
            registeringSignal.setFormat(getMessagesResponse.message.format);
            registeringSignal.setData(getMessagesResponse.message.data);

            // Start SignalActivity
            Bundle args = new Bundle();
            args.putParcelable("signal", registeringSignal);
            args.putInt("mode", SignalActivity.MODE_NEW);
            Intent intent = new Intent(WaitSignalActivity.this, SignalActivity.class);
            intent.putExtras(args);
            startActivityForResult(intent, REQUEST_SIGNAL_DETAIL);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                // Change the activity transition animation
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
            }
        }

        @Override
        public void failure(RetrofitError error) {
            Log.e(TAG, "waitforsignal failure: " + error.getMessage());
            isErrorOccurred = true;
            showError();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SIGNAL_DETAIL) {
            if (resultCode == RESULT_OK) {
                Intent resultIntent = new Intent();
                resultIntent.putExtras(data.getExtras());
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                if (data != null) {
                    boolean gotoHome = data.getBooleanExtra("back_to_home", false);
                    if (gotoHome) {
                        finish();
                    }
                }
            }
        }
    }
}
