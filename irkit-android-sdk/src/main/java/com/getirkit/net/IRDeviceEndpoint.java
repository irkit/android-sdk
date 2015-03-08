package com.getirkit.net;

import retrofit.Endpoint;

/**
 * Local endpoint for IRPeripheral.
 */
public class IRDeviceEndpoint implements Endpoint {
    public static final String TAG = IRDeviceEndpoint.class.getSimpleName();

    private String url;

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getName() {
        return IRDeviceEndpoint.class.getSimpleName();
    }
}
