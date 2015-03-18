package com.getirkit.example.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.getirkit.irkit.IRKit;
import com.getirkit.example.R;
import com.getirkit.example.activity.MainActivity;
import com.getirkit.example.adapter.SignalListAdapter;

/**
 * List of signals
 */
public class SignalsFragment extends Fragment {
    public static final String TAG = SignalsFragment.class.getSimpleName();

    public interface SignalsFragmentListener {
        public void onSignalClick(int position);
    }

    private SignalsFragmentListener listener;

    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static SignalsFragment newInstance(int sectionNumber) {
        SignalsFragment fragment = new SignalsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public SignalsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_signals, container, false);
        ListView signalsListView = (ListView) rootView.findViewById(R.id.fragment_signals__listview);
        MainActivity mainActivity = (MainActivity) getActivity();
        SignalListAdapter signalListAdapter = new SignalListAdapter(getActivity(), IRKit.sharedInstance().signals);
        signalsListView.setAdapter(signalListAdapter);
        mainActivity.setSignalListAdapter(signalListAdapter);
        signalsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (listener != null) {
                    listener.onSignalClick(position);
                }
            }
        });

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));

        try {
            listener = (SignalsFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(("Activity must implement SignalsFragmentListener"));
        }
    }
}
