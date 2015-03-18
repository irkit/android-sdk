package com.getirkit.irkit.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.getirkit.R;

/**
 * View to tell the user to turn on IRKit.
 */
public class TurnOnIRKitFragment extends Fragment {
    public static final String TAG = TurnOnIRKitFragment.class.getSimpleName();

    public interface TurnOnIRKitFragmentListener {
        public void onClickNext();
    }

    private TurnOnIRKitFragmentListener turnOnIRKitFragmentListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_turn_on_irkit, container, false);
        Button nextButton = (Button) rootView.findViewById(R.id.turnonirkit__next_button);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (turnOnIRKitFragmentListener != null) {
                    turnOnIRKitFragmentListener.onClickNext();
                }
            }
        });

        TextView purchaseTextView = (TextView) rootView.findViewById(R.id.turnonirkit__purchase);
        purchaseTextView.setMovementMethod(LinkMovementMethod.getInstance());
        return rootView;
    }

    public TurnOnIRKitFragmentListener getTurnOnIRKitFragmentListener() {
        return turnOnIRKitFragmentListener;
    }

    public void setTurnOnIRKitFragmentListener(TurnOnIRKitFragmentListener turnOnIRKitFragmentListener) {
        this.turnOnIRKitFragmentListener = turnOnIRKitFragmentListener;
    }
}
