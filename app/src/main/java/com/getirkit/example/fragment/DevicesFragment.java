package com.getirkit.example.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.getirkit.IRKit;
import com.getirkit.example.R;
import com.getirkit.example.activity.MainActivity;
import com.getirkit.example.adapter.DeviceListAdapter;

/**
 * List of devices
 */
public class DevicesFragment extends Fragment {
    public static final String TAG = DevicesFragment.class.getSimpleName();

    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    public interface DevicesFragmentListener {
        public void onDeviceClick(int position);
    }

    private DevicesFragmentListener listener;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static DevicesFragment newInstance(int sectionNumber) {
        DevicesFragment fragment = new DevicesFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public DevicesFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_devices, container, false);
        ListView devicesListView = (ListView) rootView.findViewById(R.id.devices__listview);
        MainActivity mainActivity = (MainActivity) getActivity();
        DeviceListAdapter deviceListAdapter = new DeviceListAdapter(mainActivity, IRKit.sharedInstance().peripherals);
        devicesListView.setAdapter(deviceListAdapter);
        mainActivity.setDeviceListAdapter(deviceListAdapter);
        devicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (listener != null) {
                    listener.onDeviceClick(position);
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
            listener = (DevicesFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(("Activity must implement DevicesFragmentListener"));
        }
    }
}
