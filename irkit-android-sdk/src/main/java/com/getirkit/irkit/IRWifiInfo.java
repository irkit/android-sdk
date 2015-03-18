package com.getirkit.irkit;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Encapsulates Wi-Fi network information.
 */
public class IRWifiInfo implements Parcelable {
    public static final String TAG = IRWifiInfo.class.getSimpleName();

    protected static final byte CRC8INIT = 0x00;
    protected static final byte CRC8POLY = 0x31; // = X^8+X^5+X^4+X^0

    // These constants need to match R.string.wifi_security_array
    public static final int SECURITY_WPA_WPA2 = 0;
    public static final int SECURITY_WEP = 1;
    public static final int SECURITY_NONE = 2;

    private String ssid;
    private int security;
    private String password;

    public IRWifiInfo() {
    }

    public IRWifiInfo(String ssid, int security, String password) {
        this.ssid = ssid;
        this.security = security;
        this.password = password;
    }

    public static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append( String.format(Locale.US, "%02x", b) );
        }
        return sb.toString().toUpperCase();
    }

    public static String toHexString(String str) {
        byte[] bytes;
        try {
            bytes = str.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
        return toHexString(bytes);
    }

    public static byte crc8(byte[] data, int size) {
        return crc8(data, size, CRC8INIT);
    }

    public static byte crc8(byte[] data, int size, byte crcinit) {
        byte crc = crcinit;
        int dataLength = data.length;
        for (int i = 0; i < size; i++) {
            if (i < dataLength) {
                crc ^= data[i];
            }
            for (int j = 0; j < 8; j++) {
                if ((crc & 0x80) != 0x00) {
                    crc = (byte) ((crc << 1) ^ CRC8POLY);
                } else {
                    crc <<= 1;
                }
            }
        }
        return crc;
    }

    public static byte securityToAPIByte(int security) {
        byte value;
        if (security == SECURITY_NONE) {
            value = 0;
        } else if (security == SECURITY_WEP) {
            value = 2;
        } else if (security == SECURITY_WPA_WPA2) {
            value = 8;
        } else {
            throw new IllegalArgumentException("invalid security: " + security);
        }
        return value;
    }

    public static String securityToAPIString(int security) {
        return Byte.toString( securityToAPIByte(security) );
    }

    public String getPasswordInIRKitFormat() {
        if (security == SECURITY_WEP) {
            int passwordLength = password.length();
            if (passwordLength == 5 || passwordLength == 13) {
                return toHexString(password);
            }
        }
        return password;
    }

    /**
     * [0248]/#{SSID}/#{Password}/#{Key}/#{RegDomain}//////#{CRC}
     *
     * @return
     */
    public String createMorseString(String deviceKey) {
        byte[] ssidBytes;
        byte[] passwordBytes;
        byte[] deviceKeyBytes;
        String password;
        if (security == SECURITY_NONE) {
            password = "";
        } else {
            password = getPasswordInIRKitFormat();
        }
        try {
            ssidBytes = ssid.getBytes("UTF-8");
            passwordBytes = password.getBytes("UTF-8");
            deviceKeyBytes = deviceKey.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }

        String ssidHex = toHexString(ssidBytes);
        String passwordHex = toHexString(passwordBytes);

        byte crc = crc8(new byte[]{ securityToAPIByte(security) }, 1);
        crc = crc8(ssidBytes, 33, crc);
        crc = crc8(passwordBytes, 64, crc);
        crc = crc8(new byte[]{ 1 }, 1, crc); // wifi_is_set == true
        crc = crc8(new byte[]{ 0 }, 1, crc); // wifi_was_valid == false
        crc = crc8(deviceKeyBytes, 33, crc);
        String crcString = String.format(Locale.US, "%02x", crc).toUpperCase();

        return securityToAPIString(security) + "/" + ssidHex + "/" + passwordHex +
                "/" + deviceKey + "/" + IRKit.getRegDomainForDefaultLocale() + "//////" + crcString;
    }

    public String getSSID() {
        return ssid;
    }

    public void setSSID(String ssid) {
        this.ssid = ssid;
    }

    public int getSecurity() {
        return security;
    }

    public void setSecurity(int security) {
        this.security = security;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "IRWifiInfo [SSID=" + ssid + " security=" + security + "]";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(ssid);
        parcel.writeInt(security);
        parcel.writeString(password);
    }

    public static final Creator<IRWifiInfo> CREATOR = new Creator<IRWifiInfo>() {
        @Override
        public IRWifiInfo createFromParcel(Parcel in) {
            return new IRWifiInfo(in);
        }

        @Override
        public IRWifiInfo[] newArray(int size) {
            return new IRWifiInfo[size];
        }
    };

    private IRWifiInfo(Parcel in) {
        ssid = in.readString();
        security = in.readInt();
        password = in.readString();
    }

    public static boolean isIRKitWifiSSID(String ssid) {
        if (ssid == null) {
            return false;
        }
        Pattern pattern = Pattern.compile("^\"(.*)\"$");
        Matcher matcher = pattern.matcher(ssid);
        if ( matcher.matches() ) {
            ssid = matcher.group(1);
        }
        // SSIDs are case-sensitive
        return ssid.startsWith("IRKit");
    }

    public static boolean isIRKitWifi(WifiInfo wifiInfo) {
        return isIRKitWifiSSID(wifiInfo.getSSID());
    }

    public static boolean isIRKitWifi(WifiConfiguration wifiConfig) {
        return isIRKitWifiSSID(wifiConfig.SSID);
    }

    public static boolean isIRKitWifi(ScanResult scanResult) {
        return isIRKitWifiSSID(scanResult.SSID);
    }

    public static String getRawSSID(String ssid) {
        Pattern pattern = Pattern.compile("^\"(.*)\"$");
        Matcher matcher = pattern.matcher(ssid);
        if (matcher.matches()) {
            ssid = matcher.group(1);
        }
        return ssid;
    }
}
