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
 * IRSignalを格納するArrayListです。
 * ArrayList that holds IRSignal.
 */
public class IRSignals extends ArrayList<IRSignal> implements Parcelable {
    // Never change this or you'll get InvalidClassException!
    private static final long serialVersionUID = 1L;

    public static final String TAG = "IRSignals";
    public static final String PREFS_KEY = "signals";

    /**
     * コンストラクタです。
     * Constructor.
     */
    public IRSignals() {
    }

    /**
     * idが一致するIRSignalインスタンスを返します。
     * Return IRSignal instance that matches id.
     *
     * @param id IRSignal id
     * @return 一致したIRSignal。一致しない場合はnull。
     *         Matched IRSignal, or null if not matched.
     */
    public IRSignal getSignal(String id) {
        for (IRSignal signal : this) {
            if (signal.getId().equals(id)) {
                return signal;
            }
        }
        return null;
    }

    /**
     * 同じidを持つ信号が存在するかどうかを返します。
     * Return whether some signals share the same id.
     *
     * @return 同じidを持つ信号が存在すればtrue。 True if some signals share the same id.
     */
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

    /**
     * viewPositionが無効な信号を削除します。
     * Remove signals which have invalid viewPosition.
     */
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
     * IRSignalのidに使うためのユニークなidを生成して返します。
     * Returns a unique ID which is not used by current set of signals.
     *
     * @return
     */
    public String getNewId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * SharedPreferencesにデータを保存します。
     * Save data to SharedPreferences.
     */
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

    /**
     * SharedPreferencesからこのインスタンスにデータを読み込みます。
     * Load data from SharedPreferences into this intance.
     */
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

    /**
     * deviceidが一致する信号をすべて削除します。
     * Remove signals that matches deviceid.
     *
     * @param deviceId deviceid
     */
    public void removeIRSignalsForDeviceId(String deviceId) {
        if (deviceId == null) {
            return;
        }
        for (Iterator<IRSignal> iter = this.iterator(); iter.hasNext(); ) {
            IRSignal signal = iter.next();
            if (signal.getDeviceId().equals(deviceId)) {
                iter.remove();
            }
        }
    }

    /**
     * deviceidが一致する信号のリストを返します。
     * Return list of signals that matches deviceid.
     *
     * @param deviceId deviceid
     * @return 信号のリスト。 List of signals.
     */
    public IRSignals getIRSignalsByDeviceId(String deviceId) {
        IRSignals signals = new IRSignals();
        if (deviceId != null) {
            for (IRSignal signal : this) {
                if (signal.getDeviceId().equals(deviceId)) {
                    signals.add(signal);
                }
            }
        }
        return signals;
    }

    /**
     * <p class="ja">
     * 各信号について、imageResourceNameを元にimageResourceIdを更新します。
     * </p>
     *
     * <p class="en">
     * Update each signal's imageResourceId out of imageResourceName.
     *
     * We should not use resource id as a persistent pointer to a drawable, as it is
     * determined at compile time and may change when a drawable is added or removed.
     * </p>
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
