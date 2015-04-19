package com.getirkit.irkit;

import android.content.Context;
import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;

/**
 * An IR signal.
 * 赤外線信号を表す。
 */
public class IRSignal implements Serializable, Parcelable {
    public static final String TAG = IRSignal.class.getSimpleName();

    public static final int VIEW_POSITION_INVALID = -1;

    // Never change this or you'll get InvalidClassException!
    private static final long serialVersionUID = 1L;

    // signal data (array of on/off time periods in 2MHz clock)
    private int[] data;

    // only "raw" is allowed
    private String format = "raw";

    // carrier frequency in kHz
    private float frequency = 38;

    // name of this command
    private String name;

    // icon image resource id (if it's selected from preset images)
    private transient int imageResourceId;

    // icon image resource name (if it's selected from preset images)
    private String imageResourceName;

    // icon image filename (if it's stored on internal storage)
    private String imageFilename;

    // associated device id
    private String deviceId;

    // position in view
    private int viewPosition;

    // id which uniquely identifies this signal on this device
    private String id;

    public IRSignal() {
    }

    public int[] getData() {
        return data;
    }

    public void setData(int[] data) {
        this.data = data;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public float getFrequency() {
        return frequency;
    }

    public void setFrequency(float frequency) {
        this.frequency = frequency;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getImageResourceId() {
        return imageResourceId;
    }

    public void setImageResourceId(int imageResourceId) {
        this.imageResourceId = imageResourceId;
    }

    public void setImageResourceId(int imageResourceId, Resources res) {
        setImageResourceId(imageResourceId);
        onUpdateImageResourceId(res);
    }

    public String getImageResourceName() {
        return imageResourceName;
    }

    public void setImageResourceName(String imageResourceName) {
        this.imageResourceName = imageResourceName;
    }

    public String getImageFilename() {
        return imageFilename;
    }

    public void setImageFilename(String imageFilename) {
        this.imageFilename = imageFilename;
        if (imageFilename != null) {
            imageResourceId = 0;
            imageResourceName = null;
        }
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public int getViewPosition() {
        return viewPosition;
    }

    public void setViewPosition(int viewPosition) {
        this.viewPosition = viewPosition;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSuggestedImageFilename() {
        return id + ".png";
    }

    /**
     * Copy contents from the given IRSignal to this instance.
     * 引数のIRSignalの内容をこのインスタンスにコピーします。
     *
     * @param signal Source. コピー元の信号。
     * @param context Context
     */
    public void copyFrom(IRSignal signal, Context context) {
        setName( signal.getName() );
        if (signal.hasBitmapImage()) {
            if (!signal.renameToSuggestedImageFilename(context)) {
                Log.e(TAG, "Failed to rename bitmap file");
            }
            setImageFilename( signal.getImageFilename() );
        } else {
            setImageResourceId(signal.getImageResourceId(), context.getResources());
            removeBitmapImage(context);
        }
    }

    /**
     * Rename the filename of the bitmap image to a suggested one.
     * ビットマップ画像のファイル名を推奨されたファイル名に変更する。
     *
     * @param context Context
     * @return True if the renaming succeeded. ファイル名変更が成功した場合はtrue。
     */
    public boolean renameToSuggestedImageFilename(Context context) {
        if (imageFilename == null) {
            throw new IllegalStateException("imageFilename is null");
        }
        File from = new File(context.getFilesDir(), imageFilename);
        String suggestedFilename = getSuggestedImageFilename();
        File to = new File(context.getFilesDir(), suggestedFilename);
        boolean success = from.renameTo(to);
        imageFilename = suggestedFilename;
        return success;
    }

    /**
     * Returns JSON string which will be used for API parameters.
     * APIのリクエストパラメータに使用するためのJSON文字列を返す。
     *
     * @return JSON string
     */
    public String toJson() {
        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj.put("format", format);
            jsonObj.put("freq", frequency);
            if (data != null) {
                JSONArray dataArray = new JSONArray();
                for (int value : data) {
                    dataArray.put(value);
                }
                jsonObj.put("data", dataArray);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return jsonObj.toString();
    }

    /**
     * Export format and freq to the given HashMap.
     * 引数のHashMapにformatとfreqを追加する。
     *
     * @param map HashMap object
     */
    public void export(HashMap<String, String> map) {
        map.put("format", format);
        map.put("freq", String.valueOf(frequency));
    }

    /**
     * Returns whether a bitmap image is associated with this instance.
     * ビットマップ画像を持っているかどうかを返す。
     *
     * @return True if this instance has a bitmap. ビットマップ画像を持っていればtrue。
     */
    public boolean hasBitmapImage() {
        return imageFilename != null;
    }

    public void removeBitmapImage() {
        removeBitmapImage(null);
    }

    public void removeBitmapImage(Context context) {
        if (imageFilename != null && context != null) {
            if (!context.deleteFile(imageFilename)) {
                Log.e(TAG, "Failed to delete bitmap file");
            }
        }
        imageFilename = null;
    }

    public void onUpdateImageResourceId(Resources resources) {
        try {
            String name = resources.getResourceName(this.getImageResourceId());
            this.setImageResourceName(name);
        } catch (Resources.NotFoundException ex) {
            Log.e(TAG, "Resource not found for id: " + this.getImageResourceId());
        }
    }

    @Override
    public String toString() {
        return "IRSignal[name=" + name + ";deviceId=" + deviceId + ";viewPosition=" + viewPosition + ";imageResourceName=" + imageResourceName + ";imageFilename=" + imageFilename + "]";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        if (data == null) {
            out.writeInt(-1);
        } else {
            out.writeInt(data.length);
            out.writeIntArray(data);
        }
        out.writeString(format);
        out.writeFloat(frequency);
        out.writeString(name);
        out.writeInt(imageResourceId);
        out.writeString(imageResourceName);
        out.writeString(imageFilename);
        out.writeString(deviceId);
        out.writeInt(viewPosition);
        out.writeString(id);
    }

    public static final Creator<IRSignal> CREATOR = new Creator<IRSignal>() {
        @Override
        public IRSignal createFromParcel(Parcel in) {
            return new IRSignal(in);
        }

        @Override
        public IRSignal[] newArray(int size) {
            return new IRSignal[size];
        }
    };

    private IRSignal(Parcel in) {
        int dataLength = in.readInt();
        if (dataLength == -1) {
            data = null;
        } else {
            data = new int[dataLength];
            in.readIntArray(data);
        }
        format = in.readString();
        frequency = in.readFloat();
        name = in.readString();
        imageResourceId = in.readInt();
        imageResourceName = in.readString();
        imageFilename = in.readString();
        deviceId = in.readString();
        viewPosition = in.readInt();
        id = in.readString();
    }
}
