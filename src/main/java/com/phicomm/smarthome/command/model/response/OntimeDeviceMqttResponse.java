package com.phicomm.smarthome.command.model.response;

import com.phicomm.smarthome.command.model.common.DeviceMqttBaseResponseModel;

public class OntimeDeviceMqttResponse extends DeviceMqttBaseResponseModel {
    private String timezone;

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
}
