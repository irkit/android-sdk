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
 * 赤外線信号を表します。
 * An IR signal.
 */
public class IRSignal implements Serializable, Parcelable {
    public static final String TAG = IRSignal.class.getSimpleName();

    public static final int VIEW_POSITION_INVALID = -1;

    // Never change this or you'll get InvalidClassException!
    private static final long serialVersionUID = 1L;

    /**
     * 信号データ（2Mhzクロックでのon/off時間の配列）。
     * Signal data (array of on/off time periods in 2MHz clock).
     */
    private int[] data;

    /**
     * フォーマット。現状 "raw" のみサポートされています。
     * Signal format. Only "raw" is supported at this time.
     */
    private String format = "raw";

    /**
     * キャリア周波数（kHz）。
     * Carrier frequency in kHz.
     */
    private float frequency = 38;

    /**
     * 信号の名前。
     * Name of this signal.
     */
    private String name;

    /**
     * アイコン画像のリソースID。アイコンがDrawableリソースの場合のみ使用されます。
     * Resource id of the icon image. Used only if the icon is a drawable resource.
     */
    private transient int imageResourceId;

    /**
     * アイコン画像のリソース名。アイコンがDrawableリソースの場合のみ使用されます。
     * Resource name of the icon image. Used only if the icon is a drawable resource.
     */
    private String imageResourceName;

    /**
     * アイコン画像のファイル名。アイコンがビットマップ画像の場合のみ使用されます。
     * Filename of the icon image. Used only if the icon is a bitmap image.
     */
    private String imageFilename;

    /**
     * この信号の送信対象となるIRKitのdeviceid。
     * deviceid of IRKit which this signal will be sent from.
     */
    private String deviceId;

    /**
     * View内の位置。不要な場合はセットしなくてOK。
     * Position in a view. Optional.
     */
    private int viewPosition;

    /**
     * 端末上でIRSignalをユニークに識別するためのID。
     * ID which uniquely identifies this signal on the device.
     */
    private String id;

    /**
     * コンストラクタ。
     * Constructor.
     */
    public IRSignal() {
    }

    /**
     * 赤外線信号のdataを返します。
     * Return the data array of this IR signal.
     *
     * @return 赤外線信号データ。 IR signal data.
     */
    public int[] getData() {
        return data;
    }

    /**
     * 赤外線信号のdataをセットします。
     * Set the data array for this IR signal.
     *
     * @param data 赤外線信号データ。 IR signal data.
     */
    public void setData(int[] data) {
        this.data = data;
    }

    /**
     * 赤外線信号のformatを返します。
     * Return the format of this IR signal.
     *
     * @return 赤外線信号フォーマット。 IR signal format.
     */
    public String getFormat() {
        return format;
    }

    /**
     * 赤外線信号のformatをセットします。
     * Set the format for this IR signal.
     *
     * @param format 赤外線信号フォーマット。 IR signal format.
     */
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * 赤外線信号のfreq（キャリア周波数）を返します。
     * Return the carrier frequency of this IR signal.
     *
     * @return キャリア周波数。 Carrier frequency.
     */
    public float getFrequency() {
        return frequency;
    }

    /**
     * 赤外線信号のfreq（キャリア周波数）をセットします。
     * Set the carrier frequency for this IR signal.
     *
     * @param frequency キャリア周波数。 Carrier frequency.
     */
    public void setFrequency(float frequency) {
        this.frequency = frequency;
    }

    /**
     * 赤外線信号の名前を返します。
     * Return the name of this IR signal.
     *
     * @return 赤外線信号の名前。 IR signal name.
     */
    public String getName() {
        return name;
    }

    /**
     * 赤外線信号の名前をセットします。
     * Set the name for this IR signal.
     *
     * @param name 赤外線信号の名前。 IR signal name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * アイコンのリソースIDを返します。
     * Returns the resource id of the icon image.
     *
     * @return リソースID。 Resource id.
     */
    public int getImageResourceId() {
        return imageResourceId;
    }

    /**
     * リソースIDを使ってアイコンをセットします。imageResourceNameは更新されません。
     * Set the icon image using resource id. imageResourceName will not be updated.
     *
     * @param imageResourceId リソースID。 Resource id.
     */
    public void setImageResourceId(int imageResourceId) {
        this.imageResourceId = imageResourceId;
    }

    /**
     * リソースIDを使ってアイコンをセットします。imageResourceNameも同時に更新されます。
     * Set the icon image using resource id. imageResourceName will be updated as well.
     *
     * @param imageResourceId リソースID。 Resource id.
     * @param res Resources object
     */
    public void setImageResourceId(int imageResourceId, Resources res) {
        setImageResourceId(imageResourceId);
        onUpdateImageResourceId(res);
    }

    /**
     * アイコンのリソース名を返します。
     * Return the resource name of the icon image.
     *
     * @return リソース名。 Resource name.
     */
    public String getImageResourceName() {
        return imageResourceName;
    }

    /**
     * アイコンのリソース名をセットします。imageResourceIdは更新されません。
     * Set the resource name for the icon image. imageResourceId will not be updated.
     *
     * @param imageResourceName リソース名。 Resource name.
     */
    public void setImageResourceName(String imageResourceName) {
        this.imageResourceName = imageResourceName;
    }

    /**
     * アイコンのファイル名を返します。
     * Return the filename of the icon image.
     *
     * @return ファイル名。 Filename.
     */
    public String getImageFilename() {
        return imageFilename;
    }

    /**
     * アイコンの画像ファイル名をセットします。ビットマップ画像をアイコンとして使う際に用います。
     * Set the filename of the icon image. Used for bitmap image.
     *
     * @param imageFilename 内部ストレージ上の画像ファイル名。 Filename of the image on internal storage.
     */
    public void setImageFilename(String imageFilename) {
        this.imageFilename = imageFilename;
        if (imageFilename != null) {
            imageResourceId = 0;
            imageResourceName = null;
        }
    }

    /**
     * deviceidを返します。
     * Return the deviceid.
     *
     * @return deviceid
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * deviceidをセットします。
     * Set the deviceid.
     *
     * @param deviceId deviceid
     */
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * ビューに表示する際の位置を返します。
     * Return the position in view.
     *
     * @return ビュー内の位置。セットされていない場合はVIEW_POSITION_INVALID。
     *         Position in view, or VIEW_POSITION_INVALID if position is not set.
     */
    public int getViewPosition() {
        return viewPosition;
    }

    /**
     * ビューに表示する際の位置をセットします。
     * Set the position in view.
     *
     * @param viewPosition ビュー内の位置。 Position in view.
     */
    public void setViewPosition(int viewPosition) {
        this.viewPosition = viewPosition;
    }

    /**
     * 赤外線信号のidを返します。
     * Return the id of this IR signal.
     *
     * @return id
     */
    public String getId() {
        return id;
    }

    /**
     * 赤外線信号のidをセットします。
     * Set the id for this IR signal.
     *
     * @param id id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * アイコン画像の保存場所として推奨されるファイル名を返します。
     * Return the suggested filename of the icon image.
     *
     * @return ファイル名。 Filename.
     */
    public String getSuggestedImageFilename() {
        return id + ".png";
    }

    /**
     * 引数のIRSignalの内容をこのインスタンスにコピーします。
     * Copy contents from the given IRSignal to this instance.
     *
     * @param signal コピー元の信号。 Source.
     * @param context Context object
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
     * ビットマップ画像のファイル名を推奨されたファイル名に変更します。
     * Rename the filename of the bitmap image to a suggested one.
     *
     * @param context Context object
     * @return ファイル名変更が成功した場合はtrue。 True if the renaming succeeded.
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
     * APIのリクエストパラメータに使用するためのJSON文字列を返します。
     * Returns JSON string which will be used for API parameters.
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
     * 引数のHashMapにformatとfreqを追加します。
     * Export format and freq to the given HashMap.
     *
     * @param map HashMap object
     */
    public void export(HashMap<String, String> map) {
        map.put("format", format);
        map.put("freq", String.valueOf(frequency));
    }

    /**
     * ビットマップ画像を持っているかどうかを返します。
     * Returns whether a bitmap image is associated with this instance.
     *
     * @return ビットマップ画像がセットされている場合はtrue。 True if this instance has a bitmap.
     */
    public boolean hasBitmapImage() {
        return imageFilename != null;
    }

    /**
     * アイコンのビットマップ画像を削除します。
     * Unset the bitmap icon image.
     */
    public void removeBitmapImage() {
        removeBitmapImage(null);
    }

    /**
     * アイコンのビットマップ画像とファイルを削除します。
     * Unset the bitmap icon image and delete the associated file.
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
     * 現在のimageResourceIdを元にimageResourceNameを更新します。
     * Update imageResourceName based on current imageResourceId.
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
