package com.phicomm.smarthome.command.model.common;

/**
 * 转发mqtt消息到其他服务的实体类，比如转发设备升级消息到管理后台.
 * @author huangrongwei。
 */
public class PushMqttToOthersModel {
    private String topic;

    private String body;

    public PushMqttToOthersModel() {

    }

    public PushMqttToOthersModel(String topic, String body) {
        this.topic = topic;
        this.body = body;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
