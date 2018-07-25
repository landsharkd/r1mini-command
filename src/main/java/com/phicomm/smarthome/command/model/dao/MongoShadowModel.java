package com.phicomm.smarthome.command.model.dao;

public class MongoShadowModel {
    private String deviceid;

    private String shadowname;

    private String shadowid;//deviceid和shadowname拼接的唯一key

    private Object shadow;

    private Long createtime;

    private Long updatetime;

    private Long version;

    public String getDeviceid() {
        return deviceid;
    }

    public void setDeviceid(String deviceid) {
        this.deviceid = deviceid;
    }

    public String getShadowname() {
        return shadowname;
    }

    public void setShadowname(String shadowname) {
        this.shadowname = shadowname;
    }

    public String getShadowid() {
        return shadowid;
    }

    public void setShadowid(String shadowid) {
        this.shadowid = shadowid;
    }

    public Object getShadow() {
        return shadow;
    }

    public void setShadow(Object shadow) {
        this.shadow = shadow;
    }

    public Long getCreatetime() {
        return createtime;
    }

    public void setCreatetime(Long createtime) {
        this.createtime = createtime;
    }

    public Long getUpdatetime() {
        return updatetime;
    }

    public void setUpdatetime(Long updatetime) {
        this.updatetime = updatetime;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("shadowid: ").append(shadowid).append(" shadow: ").append(shadow);

        return sb.toString();
    }
}
