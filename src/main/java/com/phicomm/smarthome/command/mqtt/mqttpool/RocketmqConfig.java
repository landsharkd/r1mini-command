package com.phicomm.smarthome.command.mqtt.mqttpool;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rocketmq")
public class RocketmqConfig {
    private static String nameser;

    public static String getNameser() {
        return nameser;
    }

    public static void setNameser(String nameser) {
        RocketmqConfig.nameser = nameser;
    }
}
