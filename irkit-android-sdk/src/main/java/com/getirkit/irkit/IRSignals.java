package com.getirkit.irkit;

import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

/**
 * List of IRSignal.
 */
public class IRSignals extends ArrayList<IRSignal> implements Parcelable {
    // Never change this or you'll get InvalidClassException!
    private static final long serialVersionUID = 1L;

    public static final String TAG = "IRSignals";
    public static final String PREFS_KEY = "signals";

    public IRSignals() {
    }

    public IRSignal getSignal(String id) {
        for (IRSignal signal : this) {
            if (signal.getId().equals(id)) {
                return signal;
            }
        }
        return null;
    }

    public boolean checkIdOverlap() {
        HashMap<String, Boolean> ids = new HashMap<>();
        for (IRSignal signal : this) {
            Object previousValue = ids.put(signal.getId(), true);
            if (previousValue != null) {
                return false;
            }
        }
        return true;
    }

    public void removeInvalidSignals() {
        for (Iterator<IRSignal> iter = this.iterator(); iter.hasNext(); ) {
            IRSignal signal = iter.next();
            if (signal.getViewPosition() == IRSignal.VIEW_POSITION_INVALID) {
                iter.remove();
                Log.w(TAG, "Removed invalid signal: " + signal);
            }
        }
    }

    /**
     * Returns a unique ID which is not used by current set of signals
     *
     * @return
     */
    public String getNewId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public void save() {
        String serializedStr;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this);
            oos.close();
            serializedStr = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }
        IRKit.sharedInstance().savePreference(PREFS_KEY, serializedStr);
    }

    public void load() {
        this.clear();

        String jsonStr = IRKit.sharedInstance().getPreference(PREFS_KEY);
        if (jsonStr != null) {
            try {
                byte[] data = Base64.decode(jsonStr, Base64.DEFAULT);
                ObjectInputStream ois = new ObjectInputStream(
                        new ByteArrayInputStream(data)
                );
                IRSignals signals = (IRSignals)ois.readObject();
                for (Object obj : signals) {
                    IRSignal signal = (IRSignal)obj;
                    this.add(signal);
                }
                ois.close();
            } catch (IOException | ClassNotFoundException ex) {
                Log.e(TAG, "Failed to load signals");
                ex.printStackTrace();
            }
        }
    }

    public void removeIRSignalsForDeviceId(String deviceId) {
        for (Iterator<IRSignal> iter = this.iterator(); iter.hasNext(); ) {
            IRSignal signal = iter.next();
            if (signal.getDeviceId().equals(deviceId)) {
                iter.remove();
            }
        }
    }

    public IRSignals getIRSignalsByDeviceId(String deviceId) {
        IRSignals signals = new IRSignals();
        for (IRSignal signal : this) {
            if (signal.getDeviceId().equals(deviceId)) {
                signals.add(signal);
            }
        }
        return signals;
    }

    /**
     * We should not use resource id as a persistent pointer to a drawable, as it is
     * determined at compile time and may change when a drawable is added or removed.
     *
     * @param resources
     */
    public void updateImageResourceIdFromName(Resources resources) {
        for (IRSignal signal : this) {
            String name = signal.getImageResourceName();
            if (name != null) {
                int resourceId = resources.getIdentifier(signal.getImageResourceName(), null, null);
                if (resourceId != 0) {
                    signal.setImageResourceId(resourceId);
                } else {
                    Log.e(TAG, "Failed to resolve resource id for name: " + signal.getImageResourceName());
                }
            } else if (signal.getImageFilename() == null) {
                Log.e(TAG, "Both resource name and image filename are null: " + signal);
            }
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeList(this);
    }

    public static final Creator<IRSignals> CREATOR = new Creator<IRSignals>() {
        @Override
        public IRSignals createFromParcel(Parcel in) {
            return new IRSignals(in);
        }

        @Override
        public IRSignals[] newArray(int size) {
            return new IRSignals[size];
        }
    };

    private IRSignals(Parcel in) {
        in.readList(this, null);
    }
}
