package com.getirkit.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.getirkit.IRSignal;
import com.getirkit.dialog.SelectImageSourceDialogFragment;
import com.getirkit.dialog.SignalImageDialogFragment;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import com.getirkit.R;

/**
 * Show details of IRSignal.
 */
public class SignalActivity extends ActionBarActivity implements SignalImageDialogFragment.SignalImageListener, SelectImageSourceDialogFragment.SelectImageSourceDialogFragmentListener {
    public static final String TAG = SignalActivity.class.getSimpleName();

    public static final String TMP_BITMAP_FILENAME = "tmp_bitmap.png";

    public static final int MODE_NEW = 1;
    public static final int MODE_EDIT = 2;

    private static final int REQUEST_CAMERA = 1;
    private static final int SELECT_FILE = 2;

    private int mode;
    private IRSignal signal;
    private boolean showDetails = false;

    // temporary variables
    private Bitmap signalImageBitmap;
    private boolean isBitmapTemporary;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        EditText nameEditText = (EditText) findViewById(R.id.activity_signal__name_field);
        if (nameEditText != null) {
            outState.putString("name", nameEditText.getText().toString());
        }
        outState.putInt("mode", mode);
        outState.putParcelable("signal", signal);
        outState.putBoolean("showDetails", showDetails);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setResult(RESULT_CANCELED);

        Intent intent = getIntent();
        Bundle args = intent.getExtras();

        // Use savedInstanceState if it exists
        if (savedInstanceState != null) {
            args = savedInstanceState;
        }

        setContentView(R.layout.activity_signal);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ImageView imageView = (ImageView) findViewById(R.id.activity_signal__image);
        if (args == null) {
            throw new IllegalArgumentException("extras are not passed via Intent");
        }
        mode = args.getInt("mode", MODE_NEW);
        signal = args.getParcelable("signal");
        if (signal == null) {
            throw new IllegalArgumentException("signal attribute is not passed via Intent");
        }
        showDetails = args.getBoolean("showDetails", false);
        if (showDetails) {
            updateShowDetails();
        }

        signalImageBitmap = null;
        isBitmapTemporary = false;
        if (signal.hasBitmapImage()) {
            FileInputStream fis;
            try {
                fis = openFileInput(signal.getImageFilename());
                signalImageBitmap = BitmapFactory.decodeStream(fis);
                imageView.setImageBitmap(signalImageBitmap);
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            isBitmapTemporary = signal.getImageFilename().equals(TMP_BITMAP_FILENAME);
        }
        if (signalImageBitmap == null) {
            int resId = signal.getImageResourceId();
            if (resId == 0) {
                resId = R.drawable.btn_icon_256_0;
                signal.setImageResourceId(resId);
            }
            imageView.setImageResource(resId);
        }

        if (mode == MODE_NEW) {
            setTitle(R.string.activity_signal__title_new);
        } else {
//            setTitle(signal.getName());
            setTitle(R.string.activity_signal__title_edit);
        }

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImage();
            }
        });

        EditText nameEditText = (EditText) findViewById(R.id.activity_signal__name_field);
        if (args.containsKey("name")) {
            String name = args.getString("name");
            if (name != null) {
                nameEditText.setText(name);
            }
        } else {
            nameEditText.setText(signal.getName());
        }

        TextView deviceIdTextView = (TextView) findViewById(R.id.activity_signal__device_id_value);
        String deviceId = signal.getDeviceId();
        if (deviceId != null) {
            deviceIdTextView.setText(deviceId);
        }

        TextView signalIdTextView = (TextView) findViewById(R.id.activity_signal__signal_id_value);
        String signalId = signal.getId();
        if (signalId != null) {
            signalIdTextView.setText(signalId);
        }
    }

    private void deleteTemporaryBitmap() {
        if (signal.hasBitmapImage() && isBitmapTemporary) {
            String bitmapFilename = signal.getImageFilename();
            if (!deleteFile(bitmapFilename)) {
                Log.e(TAG, "failed to delete file " + bitmapFilename);
            }
            signal.setImageFilename(null);
        }
    }

    private void deleteAndFinish() {
        Intent resultIntent = new Intent();
        Bundle args = new Bundle();
        args.putString("action", "delete");
        args.putParcelable("signal", signal);
        args.putInt("mode", mode);
        resultIntent.putExtras(args);
        setResult(RESULT_OK, resultIntent);
        deleteTemporaryBitmap();
        finish();
    }

    private void saveAndFinish() {
        EditText editText = (EditText) findViewById(R.id.activity_signal__name_field);
        Intent resultIntent = new Intent();
        Bundle args = new Bundle();
        args.putString("action", "save");
        signal.setName( editText.getText().toString() );
        args.putParcelable("signal", signal);
        args.putInt("mode", mode);
        resultIntent.putExtras(args);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        if (mode == MODE_EDIT) {
            inflater.inflate(R.menu.signal_activity_actions_edit, menu);
        } else {  // MODE_NEW
            inflater.inflate(R.menu.signal_activity_actions_new, menu);
        }
        if (showDetails) {
            menu.findItem(R.id.activity_signal__action_show_details).setChecked(true);
        }
        return super.onCreateOptionsMenu(menu);
    }

    private void updateShowDetails() {
        ViewGroup detailsViewGroup = (ViewGroup) findViewById(R.id.activity_signal__details);
        if (showDetails) {
            detailsViewGroup.setVisibility(View.VISIBLE);
        } else {
            detailsViewGroup.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            deleteTemporaryBitmap();
            finish();
            return true;
        } else if (id == R.id.activity_signal__action_save) {
            saveAndFinish();
            return true;
        } else if (id == R.id.activity_signal__action_delete) {
            deleteAndFinish();
            return true;
        } else if (id == R.id.activity_signal__action_show_details) {
            showDetails = !item.isChecked();
            item.setChecked(showDetails);
            updateShowDetails();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSignalImageSelect(int resourceId) {
        deleteTemporaryBitmap();
        signalImageBitmap = null;
        signal.setImageFilename(null);
        signal.setImageResourceId(resourceId);
        ImageView imageView = (ImageView) findViewById(R.id.activity_signal__image);
        imageView.setImageDrawable(getResources().getDrawable(resourceId));
    }

    @Override
    public void onSignalImageCancel() {
        // Do nothing
    }

    private boolean saveBitmap(Bitmap bitmap) {
        String filename = TMP_BITMAP_FILENAME;
        try {
            FileOutputStream fos = openFileOutput(filename, Context.MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, fos);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        signal.setImageFilename(filename);
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CAMERA) {
                Bundle bundle = data.getExtras();
                deleteTemporaryBitmap();
                signalImageBitmap = (Bitmap) bundle.get("data");
                if (saveBitmap(signalImageBitmap)) {
                    ImageView imageView = (ImageView) findViewById(R.id.activity_signal__image);
                    imageView.setImageBitmap(signalImageBitmap);
                    isBitmapTemporary = true;
                } else {
                    Log.e(TAG, "failed to save bitmap to file");
                }
            } else if (requestCode == SELECT_FILE) {
                Uri selectedImageUri = data.getData();

                InputStream is;
                try {
                    is = getContentResolver().openInputStream(selectedImageUri);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return;
                }
                Bitmap bm = BitmapFactory.decodeStream(is);
                if (bm == null) {
                    Log.e(TAG, "can't decode bitmap");
                } else {
                    deleteTemporaryBitmap();
                    // Create thumbnail
                    Matrix matrix = new Matrix();
                    matrix.setRectToRect(new RectF(0, 0, bm.getWidth(), bm.getHeight()),
                            new RectF(0, 0, 200, 200), Matrix.ScaleToFit.CENTER);
                    signalImageBitmap = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
                    if (saveBitmap(signalImageBitmap)) {
                        ImageView imageView = (ImageView) findViewById(R.id.activity_signal__image);
                        imageView.setImageBitmap(signalImageBitmap);
                        isBitmapTemporary = true;
                    } else {
                        Log.e(TAG, "failed to save bitmap to file");
                    }
                }
            }
        }
    }

    private void selectImage() {
        SelectImageSourceDialogFragment dialog = new SelectImageSourceDialogFragment();
        dialog.show(getSupportFragmentManager(), "SelectImageSourceDialogFragment");
    }

    @Override
    public void onRequestSelectFromPreset() {
        SignalImageDialogFragment dlg = new SignalImageDialogFragment();
        dlg.show(getSupportFragmentManager(), "SignalImageDialogFragment");
    }

    @Override
    public void onRequestTakePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    @Override
    public void onRequestSelectPhotoFromLibrary() {
        Intent intent = new Intent(Intent.ACTION_PICK);
//        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true); // restrict items in the local device
        intent.setType("image/*");
        startActivityForResult(intent, SELECT_FILE);
//        startActivityForResult(
//                Intent.createChooser(intent, "Select File"),
//                SELECT_FILE);
    }

    @Override
    public void onCancel() {
    }

    @Override
    public void onBackPressed() {
        deleteTemporaryBitmap();
        super.onBackPressed();
    }
}
