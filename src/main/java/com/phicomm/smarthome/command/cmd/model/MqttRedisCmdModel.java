package com.phicomm.smarthome.command.cmd.model;

/**
 * mqtt服务器传过来的消息，通过redis管道.
 * @author huangrongwei
 *
 */
public class MqttRedisCmdModel {
    private String topic;

    private String body;

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
