package com.getirkit;

import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

/**
 * View utilities.
 */
public class IRViewUtils {
    public static final String TAG = IRViewUtils.class.getName();

    /**
     * Apply styles to a dialog's root view for legacy platforms.
     *
     * Workaround for bug where Theme.AppCompat.Light makes dialog's
     * text and background black on pre-Honeycomb.
     *
     * @param viewGroup
     */
    public static void applyDialogStyle(ViewGroup viewGroup) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return;
        }

        viewGroup.setBackgroundColor(Color.BLACK);

        // Theme.AppCompat.Light makes all text and background black
        final int childCount = viewGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childView = viewGroup.getChildAt(i);
            if (childView instanceof ListView) {
                childView.setBackgroundColor(Color.LTGRAY);
            } else if (childView instanceof ViewGroup) {
                applyDialogStyle((ViewGroup) childView);
            } else if (childView instanceof TextView) {
                ((TextView) childView).setTextColor(Color.WHITE);
            }
        }
    }
}
