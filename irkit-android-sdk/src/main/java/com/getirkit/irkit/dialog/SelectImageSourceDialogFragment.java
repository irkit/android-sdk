package com.getirkit.irkit.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import com.getirkit.R;

/**
 * Dialog for selecting image source.
 */
public class SelectImageSourceDialogFragment extends DialogFragment {
    public static final String TAG = SelectImageSourceDialogFragment.class.getSimpleName();

    public interface SelectImageSourceDialogFragmentListener {
        public void onRequestSelectFromPreset();
        public void onRequestTakePhoto();
        public void onRequestSelectPhotoFromLibrary();
        public void onCancel();
    }

    private SelectImageSourceDialogFragmentListener selectImageSourceDialogFragmentListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Verify that the host activity implements the callback interface
        try {
            selectImageSourceDialogFragmentListener = (SelectImageSourceDialogFragmentListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement SelectImageSourceDialogFragmentListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        final CharSequence[] items = {getString(R.string.select_image_source__select_icon_from_list),
                getString(R.string.select_image_source__take_photo),
                getString(R.string.select_image_source__pick_image_from_gallery),
                getString(R.string.select_image_source__cancel)
        };

        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                switch (item) {
                    case 0: { // Select icon from list
                        dialog.dismiss();
                        if (selectImageSourceDialogFragmentListener != null) {
                            selectImageSourceDialogFragmentListener.onRequestSelectFromPreset();
                        }
                        break;
                    }
                    case 1: { // Take photo
                        dialog.dismiss();
                        if (selectImageSourceDialogFragmentListener != null) {
                            selectImageSourceDialogFragmentListener.onRequestTakePhoto();
                        }
                        break;
                    }
                    case 2: { // Pick image from Gallery
                        dialog.dismiss();
                        if (selectImageSourceDialogFragmentListener != null) {
                            selectImageSourceDialogFragmentListener.onRequestSelectPhotoFromLibrary();
                        }
                        break;
                    }
                    case 3: { // Cancel
                        dialog.dismiss();
                        if (selectImageSourceDialogFragmentListener != null) {
                            selectImageSourceDialogFragmentListener.onCancel();
                        }
                        break;
                    }
                }
            }
        });

        return builder.create();
    }
}
