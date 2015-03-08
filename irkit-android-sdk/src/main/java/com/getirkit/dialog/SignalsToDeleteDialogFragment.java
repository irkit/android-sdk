package com.getirkit.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.getirkit.IRSignals;
import com.getirkit.IRViewUtils;
import com.getirkit.adapter.DeleteSignalsAdapter;
import com.getirkit.R;

/**
 * Dialog for warning a user that some signals will also be deleted if they remove the device.
 */
public class SignalsToDeleteDialogFragment extends DialogFragment {
    public static final String TAG = SignalsToDeleteDialogFragment.class.getSimpleName();

    public interface SignalsToDeleteDialogFragmentListener {
        public void onClickDeleteAllSignals();
        public void onClickCancelDeleteSignals();
    }

    private SignalsToDeleteDialogFragmentListener signalsToDeleteDialogFragmentListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Verify that the host activity implements the callback interface
        try {
            signalsToDeleteDialogFragmentListener = (SignalsToDeleteDialogFragmentListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement SignalsToDeleteDialogFragmentListener");
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
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.dialog_signals_to_delete, null);

        ListView listView = (ListView) rootView.findViewById(R.id.dialog_signals_to_delete__listview);
        IRSignals signalsToDelete = getArguments().getParcelable("irsignals");
        DeleteSignalsAdapter deleteSignalsAdapter = new DeleteSignalsAdapter(getActivity(), signalsToDelete);
        listView.setAdapter(deleteSignalsAdapter);

        TextView textView = (TextView) rootView.findViewById(R.id.dialog_signals_to_delete__text);
        int numSignals = signalsToDelete.size();
        textView.setText(
                getResources().getQuantityString(R.plurals.dialog_signals_to_delete__text, numSignals, numSignals)
        );

        IRViewUtils.applyDialogStyle(rootView);

        builder.setView(rootView)
                .setTitle( getResources().getQuantityString(R.plurals.signals_to_delete__title, numSignals, numSignals) )
                .setPositiveButton(R.string.signals_to_delete__delete_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (signalsToDeleteDialogFragmentListener != null) {
                            signalsToDeleteDialogFragmentListener.onClickDeleteAllSignals();
                        }
                    }
                })
                .setNegativeButton(R.string.signals_to_delete__cancel_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (signalsToDeleteDialogFragmentListener != null) {
                            signalsToDeleteDialogFragmentListener.onClickCancelDeleteSignals();
                        }
                    }
                });
        return builder.create();
    }
}