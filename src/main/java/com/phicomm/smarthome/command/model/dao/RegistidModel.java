package com.phicomm.smarthome.command.model.dao;

import com.phicomm.smarthome.command.model.common.BaseDaoModel;

public class RegistidModel extends BaseDaoModel {
    private String uid;

    private String platform;

    private String registid;

    private String osType;

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getRegistid() {
        return registid;
    }

    public void setRegistid(String registid) {
        this.registid = registid;
    }

    public String getOsType() {
        return osType;
    }

    public void setOsType(String osType) {
        this.osType = osType;
    }
}
