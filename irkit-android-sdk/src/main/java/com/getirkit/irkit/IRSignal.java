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
 * 赤外線信号を表します。
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

    /**
     * Constructor.
     * コンストラクタ。
     */
    public IRSignal() {
    }

    /**
     * Return the data array of this IR signal.
     * 赤外線信号のdataを返します。
     *
     * @return IR signal data. 赤外線信号データ。
     */
    public int[] getData() {
        return data;
    }

    /**
     * Set the data array for this IR signal.
     * 赤外線信号のdataをセットします。
     *
     * @param data IR signal data. 赤外線信号データ。
     */
    public void setData(int[] data) {
        this.data = data;
    }

    /**
     * Return the format of this IR signal.
     * 赤外線信号のformatを返します。
     *
     * @return IR signal format. 赤外線信号フォーマット。
     */
    public String getFormat() {
        return format;
    }

    /**
     * Set the format for this IR signal.
     * 赤外線信号のformatをセットします。
     *
     * @param format IR signal format. 赤外線信号フォーマット。
     */
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * Return the carrier frequency of this IR signal.
     * 赤外線信号のfreq（キャリア周波数）を返します。
     *
     * @return Carrier frequency. キャリア周波数。
     */
    public float getFrequency() {
        return frequency;
    }

    /**
     * Set the carrier frequency for this IR signal.
     * 赤外線信号のfreq（キャリア周波数）をセットします。
     *
     * @param frequency Carrier frequency. キャリア周波数。
     */
    public void setFrequency(float frequency) {
        this.frequency = frequency;
    }

    /**
     * Return the name of this IR signal.
     * 赤外線信号の名前を返します。
     *
     * @return IR signal name. 赤外線信号の名前。
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name for this IR signal.
     * 赤外線信号の名前をセットします。
     *
     * @param name IR signal name. 赤外線信号の名前。
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the resource id of the icon image.
     * アイコンのリソースIDを返します。
     *
     * @return Resource id. リソースID。
     */
    public int getImageResourceId() {
        return imageResourceId;
    }

    /**
     * Set the icon image using resource id. imageResourceName will not be updated.
     * リソースIDを使ってアイコンをセットします。imageResourceNameは更新されません。
     *
     * @param imageResourceId Resource id. リソースID。
     */
    public void setImageResourceId(int imageResourceId) {
        this.imageResourceId = imageResourceId;
    }

    /**
     * Set the icon image using resource id. imageResourceName will be updated as well.
     * リソースIDを使ってアイコンをセットします。imageResourceNameも同時に更新されます。
     *
     * @param imageResourceId Resource id. リソースID。
     * @param res Resources object
     */
    public void setImageResourceId(int imageResourceId, Resources res) {
        setImageResourceId(imageResourceId);
        onUpdateImageResourceId(res);
    }

    /**
     * Return the resource name of the icon image.
     * アイコンのリソース名を返します。
     *
     * @return Resource name. リソース名。
     */
    public String getImageResourceName() {
        return imageResourceName;
    }

    /**
     * Set the resource name for the icon image. imageResourceId will not be updated.
     * アイコンのリソース名をセットします。imageResourceIdは更新されません。
     *
     * @param imageResourceName Resource name. リソース名。
     */
    public void setImageResourceName(String imageResourceName) {
        this.imageResourceName = imageResourceName;
    }

    /**
     * Return the filename of the icon image.
     * アイコンのファイル名を返します。
     *
     * @return Filename. ファイル名。
     */
    public String getImageFilename() {
        return imageFilename;
    }

    /**
     * Set the filename of the icon image. Used for bitmap image.
     * アイコンの画像ファイル名をセットします。ビットマップ画像をアイコンとして使う際に用います。
     *
     * @param imageFilename
     */
    public void setImageFilename(String imageFilename) {
        this.imageFilename = imageFilename;
        if (imageFilename != null) {
            imageResourceId = 0;
            imageResourceName = null;
        }
    }

    /**
     * Return the deviceid.
     * deviceidを返します。
     *
     * @return deviceid
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * Set the deviceid.
     * deviceidをセットします。
     *
     * @param deviceId deviceid
     */
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * Return the position in view.
     * ビューに表示する際の位置を返します。
     *
     * @return Position in view. ビュー内の位置。
     */
    public int getViewPosition() {
        return viewPosition;
    }

    /**
     * Set the position in view.
     * ビューに表示する際の位置をセットします。
     *
     * @param viewPosition Position in view. ビュー内の位置。
     */
    public void setViewPosition(int viewPosition) {
        this.viewPosition = viewPosition;
    }

    /**
     * Return the id of this IR signal.
     * 赤外線信号のidを返します。
     *
     * @return id
     */
    public String getId() {
        return id;
    }

    /**
     * Set the id for this IR signal.
     * 赤外線信号のidをセットします。
     *
     * @param id id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Return the suggested filename of the icon image.
     * アイコン画像の保存場所として推奨されるファイル名を返します。
     *
     * @return Filename. ファイル名。
     */
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
     * ビットマップ画像のファイル名を推奨されたファイル名に変更します。
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
     * APIのリクエストパラメータに使用するためのJSON文字列を返します。
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
     * 引数のHashMapにformatとfreqを追加します。
     *
     * @param map HashMap object
     */
    public void export(HashMap<String, String> map) {
        map.put("format", format);
        map.put("freq", String.valueOf(frequency));
    }

    /**
     * Returns whether a bitmap image is associated with this instance.
     * ビットマップ画像を持っているかどうかを返します。
     *
     * @return True if this instance has a bitmap. ビットマップ画像を持っていればtrue。
     */
    public boolean hasBitmapImage() {
        return imageFilename != null;
    }

    /**
     * Unset the bitmap icon image.
     * アイコンのビットマップ画像を削除します。
     */
    public void removeBitmapImage() {
        removeBitmapImage(null);
    }

    /**
     * Unset the bitmap icon image and delete the associated file.
     * アイコンのビットマップ画像とファイルを削除します。
     *
     * @param context Context object
     */
    public void removeBitmapImage(Context context) {
        if (imageFilename != null && context != null) {
            if (!context.deleteFile(imageFilename)) {
                Log.e(TAG, "Failed to delete bitmap file");
            }
        }
        imageFilename = null;
    }

    /**
     * Update imageResourceName based on current imageResourceId.
     * 現在のimageResourceIdを元にimageResourceNameを更新します。
     *
     * @param resources Resources object
     */
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
