package com.phicomm.smarthome.command.mqtt.mqttpool;

import com.alibaba.fastjson.JSON;
import com.phicomm.smarthome.command.util.StringUtil;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.common.RemotingHelper;


public class MQTTFactory {
    private static Logger logger = LogManager.getLogger(MQTTFactory.class);

    private static final String MQTT_PUBLISH_CHANNEL = "publish_mqtt_msg";

    private static final String HP_MQTT_PUBLISH_CHANNEL = "hp_publish_mqtt_msg"; //high priority message queue

    private static MQTTFactory instance;

    private DefaultMQProducer producer;

    private DefaultMQProducer hpProducer;

    private MQTTFactory(String nameSer) {
        producer = new DefaultMQProducer("group_push_to_mqtt");
        producer.setNamesrvAddr(nameSer);
        try {
            producer.start();
        } catch (MQClientException e) {
            logger.error("", e);
        }


        hpProducer = new DefaultMQProducer("hp_group_push_to_mqtt");
        hpProducer.setNamesrvAddr(nameSer);
        try {
            hpProducer.start();
        } catch (MQClientException e) {
            logger.error("", e);
        }
    }

    private static MQTTFactory getIns() {
        if (instance == null) {
            syncInit();
        }
        logger.debug("name config : " + RocketmqConfig.getNameser());
        return instance;
    }

    private static synchronized void syncInit() {
        if (instance == null) {
            instance = new MQTTFactory(RocketmqConfig.getNameser());
        }
    }

    /**
     * 消息发布.
     * @param topic topic
     * @param msg msg
     * @return result
     */
    public static boolean publish(String topic, String msg) {
        return publish(topic, msg, 0);
    }

    /**
     * 带消息权限的发布.
     * @param topic topic
     * @param msg msg
     * @param priority priority
     * @return result send result
     */
    public static boolean publish(String topic, String msg, int priority) {
        MqttMsgModel model = new MqttMsgModel();
        model.setTopic(topic);
        model.setBody(msg);

        String outString = JSON.toJSONString(model);
        logger.debug("publish to mqtt server [{}]", outString);
        if (StringUtil.isNullOrEmpty(outString)) {
            logger.info("publish to mqtt server with null, topic [{}], msg [{}]", topic, msg);
            return false;
        }

        return getIns().sendInner(outString, priority);
    }

    private boolean sendInner(String content, int priority) {
        String mqTtopic = (priority == 0) ? MQTT_PUBLISH_CHANNEL : HP_MQTT_PUBLISH_CHANNEL;
        try {
            Message msg = new Message(mqTtopic,
                    "TagA",
                    content.getBytes(RemotingHelper.DEFAULT_CHARSET)
            );
            if (priority == 0) {
                producer.send(msg);
            } else {
                hpProducer.send(msg);
            }
            logger.debug("send rocketmq message [{}]", msg);
        } catch (Exception e) {
            logger.error("send message to mq error ", e);
            return false;
        }
        return true;
    }

    /**
     * 消息订阅.
     */
    public static int subscribe(String[] topics) {

        return 1;
    }

    /**
     * 取消消息订阅.
     */
    public static int unSubscribe(String[] topics) {
        return 1;
    }

    /**
     * 发送给mqtt服务器插件的消息模型.
     * @author huangrongwei
     */
    static class MqttMsgModel {
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
}
