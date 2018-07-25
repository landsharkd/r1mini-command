package com.phicomm.smarthome.command.consts;

public interface Const {
    interface JpushPhihome {
        String APPKEY = "0e621306bf07eb4eefc49e66";
        String MASTER_KEY = "9a3869c9d2fec8333db84a2e";
        String REGISTER_ID = "13065ffa4e3dbec64dc";
    }

    interface Mqtt {
        String TOPIC_DEV_ONTIME_RSP_PREFIX = "$events/broker/%s/timesynced/%s";
    }

    public static final int INNER_SERVICE_REQUEST_READ_TIMEOUT = 10000;
    public static final int INNER_SERVICE_REQUEST_CONNECT_TIMEOUT = 10000;
}
