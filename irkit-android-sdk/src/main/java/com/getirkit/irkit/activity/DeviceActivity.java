package com.getirkit.irkit.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.getirkit.irkit.IRKit;
import com.getirkit.irkit.IRPeripheral;
import com.getirkit.irkit.IRSignals;
import com.getirkit.irkit.R;
import com.getirkit.irkit.dialog.SignalsToDeleteDialogFragment;

import java.net.InetAddress;

/**
 * IRPeripheralの詳細を表示します。
 * Show details of IRPeripheral.
 */
public class DeviceActivity extends ActionBarActivity implements SignalsToDeleteDialogFragment.SignalsToDeleteDialogFragmentListener {
    public static final String TAG = DeviceActivity.class.getName();

    private IRPeripheral peripheral;
    private String apiKey;
    private boolean showDetails = false;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        EditText nameEditText = (EditText)findViewById(R.id.activity_device__name_field);
        if (nameEditText != null) {
            outState.putString("name", nameEditText.getText().toString());
        }
        outState.putParcelable("peripheral", peripheral);
        outState.putString("apiKey", apiKey);
        outState.putBoolean("showDetails", showDetails);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setResult(RESULT_CANCELED);
        setContentView(R.layout.activity_device);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Get the values from the intent
        Intent intent = getIntent();
        Bundle args = intent.getExtras();

        if (savedInstanceState != null) {
            args = savedInstanceState;
        }

        peripheral = args.getParcelable("peripheral");
        setTitle(peripheral.getCustomizedName());

        EditText nameEditText = (EditText) findViewById(R.id.activity_device__name_field);

        IRKit irkit = IRKit.sharedInstance();
        irkit.init(getApplicationContext());
        apiKey = args.getString("apiKey");
        // If apiKey argument is not provided (== null), it will be read from AndroidManifest.xml
        irkit.registerClient(apiKey);

        showDetails = args.getBoolean("showDetails", false);
        if (showDetails) {
            updateShowDetails();
        }

        if (args.containsKey("name")) {
            String name = args.getString("name");
            if (name != null) {
                nameEditText.setText(name);
            }
        } else {
            nameEditText.setText(peripheral.getCustomizedName());
        }

        TextView ipTextView = (TextView) findViewById(R.id.activity_device__ipaddress_field);
        InetAddress host = peripheral.getHost();
        if (host != null) {
            ipTextView.setText(host.getHostAddress());
        } else {
            ipTextView.setText(getString(R.string.activity_device__ipaddress_not_available));
        }

        TextView modelNameTextView = (TextView) findViewById(R.id.activity_device__modelname_field);
        modelNameTextView.setText(peripheral.getModelName());

        TextView firmwareVersionTextView = (TextView) findViewById(R.id.activity_device__firmwareversion_field);
        firmwareVersionTextView.setText(peripheral.getFirmwareVersion());

        if (peripheral.getDeviceId() != null) {
            TextView deviceIdTextView = (TextView) findViewById(R.id.activity_device__device_id_field);
            deviceIdTextView.setText(peripheral.getDeviceId());
        }

        TextView hostnameTextView = (TextView) findViewById(R.id.activity_device__hostname_field);
        hostnameTextView.setText( peripheral.getHostname() );

        TextView clientKeyTextView = (TextView) findViewById(R.id.activity_device__client_key_value);
        String clientKey = IRKit.sharedInstance().getHTTPClient().getClientKey();
        if (clientKey != null) {
            clientKeyTextView.setText(clientKey);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.device_activity_actions, menu);
        if (showDetails) {
            menu.findItem(R.id.activity_device__action_show_details).setChecked(true);
        }
        return super.onCreateOptionsMenu(menu);
    }

    private void updateShowDetails() {
        ViewGroup detailsViewGroup = (ViewGroup) findViewById(R.id.activity_device__details);
        if (showDetails) {
            detailsViewGroup.setVisibility(View.VISIBLE);
        } else {
            detailsViewGroup.setVisibility(View.GONE);
        }
    }

    private void saveAndFinish() {
        EditText nameEditText = (EditText)findViewById(R.id.activity_device__name_field);
        Intent resultIntent = new Intent();
        Bundle args = new Bundle();
        peripheral.setCustomizedName( nameEditText.getText().toString() );
        args.putParcelable("peripheral", peripheral);
        args.putString("action", "save");
        resultIntent.putExtras(args);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void deleteAndFinish() {
        Intent resultIntent = new Intent();
        Bundle args = new Bundle();
        args.putString("action", "delete");
        args.putParcelable("peripheral", peripheral);
        resultIntent.putExtras(args);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.activity_device__action_save) {
            saveAndFinish();
            return true;
        } else if (id == R.id.activity_device__action_delete) {
            IRSignals signalsToDelete = IRKit.sharedInstance().signals.getIRSignalsByDeviceId(
                    peripheral.getDeviceId()
            );
            if (signalsToDelete.size() > 0) {
                // warn user
                SignalsToDeleteDialogFragment dialog2 = new SignalsToDeleteDialogFragment();
                Bundle args = new Bundle();
                args.putParcelable("irsignals", signalsToDelete);
                dialog2.setArguments(args);
                dialog2.show(getSupportFragmentManager(), "SignalsToDeleteDialog");
            } else {
                deleteAndFinish();
            }
            return true;
        } else if (id == R.id.activity_device__action_show_details) {
            showDetails = !item.isChecked();
            item.setChecked(showDetails);
            updateShowDetails();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClickDeleteAllSignals() {
        deleteAndFinish();
    }

    @Override
    public void onClickCancelDeleteSignals() {
    }
}
