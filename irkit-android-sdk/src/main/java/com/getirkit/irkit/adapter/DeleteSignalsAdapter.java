package com.getirkit.irkit.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.getirkit.irkit.IRSignal;
import com.getirkit.irkit.R;

import java.io.File;
import java.util.ArrayList;

/**
 * Adapter for listing signals in SignalsToDeleteDialogFragment
 */
public class DeleteSignalsAdapter extends BaseAdapter {
    public static final String TAG = DeleteSignalsAdapter.class.getSimpleName();

    private Activity activity;
    private ArrayList data;

    public DeleteSignalsAdapter(Activity activity, ArrayList data) {
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

    @Override
    public boolean areAllItemsEnabled() {
        // Make items not clickable
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        // Make items not clickable
        return false;
    }

    public static class ViewHolder {
        public ImageView image;
        public TextView name;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewHolder holder;

        IRSignal signal = (IRSignal) data.get(position);

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.list_item_signal_to_delete, parent, false);

            holder = new ViewHolder();
            holder.image = (ImageView) view.findViewById(R.id.list_item_signal_to_delete__image);
            holder.name = (TextView) view.findViewById(R.id.list_item_signal_to_delete__name);

            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        if (signal.hasBitmapImage()) {
            String filename = new File(activity.getFilesDir(), signal.getImageFilename()).getAbsolutePath();
            Bitmap bitmap = BitmapFactory.decodeFile(filename);
            holder.image.setImageBitmap(bitmap);
        } else {
            holder.image.setImageResource(signal.getImageResourceId());
        }

        holder.name.setText(signal.getName());

        return view;
    }
}
