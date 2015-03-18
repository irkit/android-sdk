package com.getirkit.irkit;

import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

/**
 * List of IRPeripheral.
 */
public class IRPeripherals extends ArrayList<IRPeripheral> {
    // Never change this or you'll get InvalidClassException!
    private static final long serialVersionUID = 1L;

    public static final String TAG = "IRPeripherals";
    public static final String PREFS_KEY = "peripherals";

    public IRPeripheral addPeripheral(String hostname) {
        IRPeripheral peripheral = new IRPeripheral();
        peripheral.setHostname(hostname);
        peripheral.setCustomizedName(hostname);
        this.add(peripheral);
        this.save();
        return peripheral;
    }

    public IRPeripheral getPeripheralByDeviceId(String deviceId) {
        for (IRPeripheral peripheral : this) {
            if (peripheral.getDeviceId().equals(deviceId)) {
                return peripheral;
            }
        }
        return null;
    }

    public IRPeripheral getPeripheral(String name) {
        if (name == null) {
            return null;
        }
        name = name.toLowerCase();
        for (IRPeripheral peripheral : this) {
            if (peripheral.getHostname().toLowerCase().equals(name)) {
                return peripheral;
            }
        }
        return null;
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
                IRPeripherals peripherals = (IRPeripherals)ois.readObject();
                for (Object obj : peripherals) {
                    IRPeripheral peripheral = (IRPeripheral)obj;
                    this.add(peripheral);
                }
                ois.close();
            } catch (Exception ex) {
                Log.e(TAG, "Failed to load peripherals");
                ex.printStackTrace();
            }
        }
    }

    public JSONArray toJSONArray() {
        JSONArray arr = new JSONArray();
        for (IRPeripheral peripheral : this) {
            arr.put(peripheral.toJSONObject());
        }
        return arr;
    }
}
