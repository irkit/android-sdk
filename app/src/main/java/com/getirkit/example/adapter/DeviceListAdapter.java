package com.getirkit.example.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.getirkit.IRPeripheral;
import com.getirkit.example.R;

import java.util.ArrayList;

/**
 * Adapter for listing devices
 */
public class DeviceListAdapter extends BaseAdapter {
    private Activity activity;
    private ArrayList data;

    public DeviceListAdapter(Activity activity, ArrayList data) {
        this.activity = activity;
        this.data = data;
    }

    public int getCount() {
        return data.size();
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    public static class ViewHolder {
        public ImageView image;
        public TextView name;
        public TextView details;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.list_item_device, parent, false);

            holder = new ViewHolder();
            holder.image = (ImageView) view.findViewById(R.id.item_image);
            holder.name = (TextView) view.findViewById(R.id.item_name);
            holder.details = (TextView) view.findViewById(R.id.item_details);

            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        IRPeripheral peripheral = (IRPeripheral) data.get(position);
        holder.name.setText(peripheral.getCustomizedName());
        holder.details.setText( peripheral.getHostname() );

        return view;
    }
}
