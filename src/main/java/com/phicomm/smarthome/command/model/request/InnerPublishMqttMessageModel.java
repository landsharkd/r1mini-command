package com.phicomm.smarthome.command.model.request;

public class InnerPublishMqttMessageModel {
    private String topic;

    private String body;

    private int priority = 0;

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

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("topic [").append(topic).append("] ");
        sb.append("body [").append(body).append("] ");
        sb.append("priority [").append(priority).append("]");

        return sb.toString();
    }
}
