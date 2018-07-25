package com.phicomm.smarthome.command.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeviceInfo {
    @JsonProperty("device_id")
    private String deviceId;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
