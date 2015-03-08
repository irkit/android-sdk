package com.getirkit.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import com.getirkit.IRViewUtils;
import com.getirkit.adapter.SignalImageGridAdapter;
import com.getirkit.R;

/**
 * Dialog for selecting image from list.
 */
public class SignalImageDialogFragment extends DialogFragment {
    public static final String TAG = SignalImageDialogFragment.class.getSimpleName();

    public interface SignalImageListener {
        public void onSignalImageSelect(int resourceId);
        public void onSignalImageCancel();
    }

    private SignalImageListener listener;
    private SignalImageGridAdapter signalImageGridAdapter;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Verify that the host activity implements the callback interface
        try {
            listener = (SignalImageListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement SignalImageListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        final ViewGroup view = (ViewGroup) inflater.inflate(R.layout.dialog_signal_image, null);

        GridView gridView = (GridView) view.findViewById(R.id.signal_image_gridview);
        Activity activity = getActivity();
        if (activity == null) {
            Log.e(TAG, "getActivity() returned null");
        }
        signalImageGridAdapter = new SignalImageGridAdapter(activity);
        gridView.setAdapter(signalImageGridAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Integer resourceId = (Integer) signalImageGridAdapter.getItem(i);
                if (listener != null) {
                    listener.onSignalImageSelect(resourceId);
                } else {
                    Log.e(TAG, "SignalImageListener is null");
                }
                dismiss();
            }
        });

        IRViewUtils.applyDialogStyle(view);

        builder.setView(view);
        // Create the AlertDialog object and return it
        return builder.create();
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        if (listener != null) {
            listener.onSignalImageCancel();
        }
    }
}
