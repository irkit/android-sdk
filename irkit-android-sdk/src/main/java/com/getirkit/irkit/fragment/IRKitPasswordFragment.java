package com.getirkit.irkit.fragment;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.getirkit.irkit.R;

/**
 * View for entering IRKit Wi-Fi password.
 */
public class IRKitPasswordFragment extends Fragment {
    private CheckBox showPasswordCheckBox;
    private EditText passwordEditText;

    public interface IRKitPasswordFragmentListener {
        public void onClickOK(String password);
    }

    private IRKitPasswordFragmentListener irkitPasswordFragmentListener;

    public IRKitPasswordFragmentListener getIRKitPasswordFragmentListener() {
        return irkitPasswordFragmentListener;
    }

    public void setIRKitPasswordFragmentListener(IRKitPasswordFragmentListener irkitPasswordFragmentListener) {
        this.irkitPasswordFragmentListener = irkitPasswordFragmentListener;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (passwordEditText != null) {
            outState.putString("password", passwordEditText.getText().toString());
        }
        if (showPasswordCheckBox != null) {
            outState.putBoolean("showPassword", showPasswordCheckBox.isChecked());
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
        View rootView = inflater.inflate(R.layout.fragment_irkit_password, container, false);

        passwordEditText = (EditText) rootView.findViewById(R.id.irkitpassword_password_field);
        // Don't call passwordEditText.requestFocus(), as it makes
        // keyboard difficult to appear on Android 2.3

        showPasswordCheckBox = (CheckBox) rootView.findViewById(R.id.irkitpassword__show_password);
        if (args != null) {
            String password = args.getString("password");
            if (password != null) {
                passwordEditText.setText(password);
            }
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

        Button okButton = (Button) rootView.findViewById(R.id.irkitpassword_ok);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String password = passwordEditText.getText().toString();

                // Hide the virtual keyboard
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(passwordEditText.getWindowToken(), 0);

                if (irkitPasswordFragmentListener != null) {
                    irkitPasswordFragmentListener.onClickOK(password);
                }
            }
        });

        return rootView;
    }
}
