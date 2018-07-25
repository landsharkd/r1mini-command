package com.phicomm.smarthome.command.model.common;

import com.phicomm.smarthome.util.MyResponseutils;

/**
 * 返回给设备的基本Json模型类，比如设备绑定的时候需要返回给设备的结果.
 * @author huangrongwei
 *
 */
public class DeviceMqttBaseResponseModel {
    private int status;
    private String message;
    private long timestamp;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void intent() {
        message = MyResponseutils.parseMsg(status);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{").
            append("\"").append("code").append("\"").append(":").append(status).append(",").
            append("\"").append("message").append("\"").append(":").append(message).append(",").
            append("\"").append("timestamp").append(":").append("\"").append(timestamp).
            append("}");
        return sb.toString();
    }
}
