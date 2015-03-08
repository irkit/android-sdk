package com.getirkit.example.adapter;

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

import com.getirkit.IRSignal;
import com.getirkit.example.R;
import com.getirkit.example.view.AsyncDrawable;
import com.getirkit.example.view.BitmapWorkerTask;

import java.io.File;
import java.util.ArrayList;

/**
 * Adapter for listing signals
 */
public class SignalListAdapter extends BaseAdapter {
    private Activity activity;
    private ArrayList data;
    private Bitmap mPlaceHolderBitmap;

    public SignalListAdapter(Activity activity, ArrayList data) {
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
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.list_item_signal, parent, false);

            holder = new ViewHolder();
            holder.image = (ImageView) view.findViewById(R.id.item_image);
            holder.name = (TextView) view.findViewById(R.id.item_name);

            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        IRSignal signal = (IRSignal) data.get(position);
        setSignalImage(holder.image, signal);
        holder.name.setText(signal.getName());

        return view;
    }

    /**
     * Set signal icon to ImageView
     *
     * @param imageView
     * @param signal
     */
    private void setSignalImage(ImageView imageView, IRSignal signal) {
        if (signal.hasBitmapImage()) {
            if (mPlaceHolderBitmap == null) {
                mPlaceHolderBitmap = BitmapFactory.decodeResource(activity.getResources(), R.drawable.btn_icon_256_blank);
            }
            if (activity != null) {
                String filename = new File(activity.getFilesDir(), signal.getImageFilename()).getAbsolutePath();

                // Cancel ongoing BitmapWorkerTask for the same ImageView
                BitmapWorkerTask.cancelWork(imageView);

                final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
                final AsyncDrawable asyncDrawable = new AsyncDrawable(activity.getResources(), mPlaceHolderBitmap, task);
                imageView.setImageDrawable(asyncDrawable);
                task.execute(filename);
            }
        } else {
            BitmapWorkerTask.cancelWork(imageView);
            imageView.setImageResource(signal.getImageResourceId());
        }
    }
}
