package com.getirkit.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.lang.ref.WeakReference;
import com.getirkit.R;

/**
 * Adapter for listing signals in SignalImageDialogFragment
 */
public class SignalImageGridAdapter extends BaseAdapter {
    public static final String TAG = SignalImageGridAdapter.class.getSimpleName();

    private Context mContext;
//    private LruCache<String, Bitmap> mMemoryCache;
    private Bitmap mPlaceHolderBitmap;

    public SignalImageGridAdapter(Context c) {
        super();
        mContext = c;
    }

    @Override
    public int getCount() {
        return mThumbIds.length;
    }

    @Override
    public Object getItem(int position) {
        return mThumbIds[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {  // if it's not recycled, initialize some attributes
            imageView = new ImageView(mContext);
            int size = (int) parent.getResources().getDimension(R.dimen.signal_image_dialog__grid_image_size);
            imageView.setLayoutParams(new GridView.LayoutParams(size, size));
            imageView.setPadding(8, 8, 8, 8);
        } else {
            imageView = (ImageView) convertView;
        }

        loadBitmap(mThumbIds[position], imageView);
        return imageView;
    }

    // http://developer.android.com/training/displaying-bitmaps/display-bitmap.html
    public void loadBitmap(int resId, ImageView imageView) {
        if (mPlaceHolderBitmap == null) {
            mPlaceHolderBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.btn_icon_256_blank);
        }
        if (cancelPotentialWork(resId, imageView)) {
            final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
            final AsyncDrawable asyncDrawable =
                    new AsyncDrawable(mContext.getResources(), mPlaceHolderBitmap, task);
            imageView.setImageDrawable(asyncDrawable);
            task.execute(resId);
        }
    }

//    protected void loadBitmap(int resId, ImageView imageView) {
//        final String imageKey = String.valueOf(resId);
//        final Bitmap bitmap = mMemoryCache.get(imageKey);
//        if (bitmap != null) {
//            imageView.setImageBitmap(bitmap);
//        } else {
//            imageView.setImageResource(R.drawable.btn_icon_256_0);
//            BitmapWorkerTask task = new BitmapWorkerTask(imageView);
//            task.execute(resId);
//        }
//    }

    // http://developer.android.com/training/displaying-bitmaps/display-bitmap.html
    static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap,
                             BitmapWorkerTask bitmapWorkerTask) {
            super(res, bitmap);
            bitmapWorkerTaskReference =
                    new WeakReference<>(bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }

    // http://developer.android.com/training/displaying-bitmaps/display-bitmap.html
    public static boolean cancelPotentialWork(int data, ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final int bitmapData = bitmapWorkerTask.data;
            if (bitmapData != data) {
                // Cancel previous task
                bitmapWorkerTask.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }
        // No task associated with the ImageView, or an existing task was cancelled
        return true;
    }

    // http://developer.android.com/training/displaying-bitmaps/process-bitmap.html
    private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    // references to our images
    final private Integer[] mThumbIds = {
            R.drawable.btn_icon_256_0,
            R.drawable.btn_icon_256_1,
            R.drawable.btn_icon_256_2,
            R.drawable.btn_icon_256_3,
            R.drawable.btn_icon_256_4,
            R.drawable.btn_icon_256_5,
            R.drawable.btn_icon_256_6,
            R.drawable.btn_icon_256_7,
            R.drawable.btn_icon_256_8,
            R.drawable.btn_icon_256_9,
            R.drawable.btn_icon_256_10,
            R.drawable.btn_icon_256_11,
            R.drawable.btn_icon_256_12,
            R.drawable.btn_icon_256_aircon,
            R.drawable.btn_icon_256_down,
            R.drawable.btn_icon_256_fan,
            R.drawable.btn_icon_256_fastfoward,
            R.drawable.btn_icon_256_light,
            R.drawable.btn_icon_256_minus,
            R.drawable.btn_icon_256_next,
            R.drawable.btn_icon_256_pause,
            R.drawable.btn_icon_256_play,
            R.drawable.btn_icon_256_pluss,
            R.drawable.btn_icon_256_power,
            R.drawable.btn_icon_256_prev,
            R.drawable.btn_icon_256_rewind,
            R.drawable.btn_icon_256_signal,
            R.drawable.btn_icon_256_stop,
            R.drawable.btn_icon_256_time,
            R.drawable.btn_icon_256_tv,
            R.drawable.btn_icon_256_up,
    };

    // http://developer.android.com/training/displaying-bitmaps/process-bitmap.html
    class BitmapWorkerTask extends AsyncTask<Integer, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;
        private int data = 0;

        public BitmapWorkerTask(ImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<>(imageView);
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(Integer... params) {
            data = params[0];
            int size = (int) mContext.getResources().getDimension(R.dimen.signal_image_dialog__grid_image_size);
            return decodeSampledBitmapFromResource(mContext.getResources(), data, size, size);
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                bitmap = null;
            }

            if (bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
                if (this == bitmapWorkerTask && imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }

    // http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    // http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
                                                         int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }
}