package com.phicomm.smarthome.command.cmd;

import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.phicomm.smarthome.cache.Cache;
import com.phicomm.smarthome.command.PhiHomeCommandMain;
import com.phicomm.smarthome.command.cmd.model.MqttRedisCmdModel;
import com.phicomm.smarthome.command.cmd.topic.Topic;
import com.phicomm.smarthome.command.mqtt.mqttpool.RocketmqConfig;
import com.phicomm.smarthome.command.util.StringUtil;
import com.phicomm.smarthome.consts.Const.ResponseStatus;

/**
 * mqtt监听类的入口，从这里初始化command 后台所有的 mqtt的topic监听.
 *
 * @author huangrongwei
 *
 */
@Component
public class BaseMqttTopicsInitlization {
    private static Logger logger = LogManager.getLogger(BaseMqttTopicsInitlization.class);

    //消息处理最大延迟时间
    private static final long MESSAGE_MAX_COMSUME_TIME_DELAY = 3 * 60 * 1000;

    /** 实体对象. */
    public static BaseMqttTopicsInitlization instance;

    @Autowired
    private Cache cache;

    @Autowired
    public RedisTemplate<String, String> redisTemplate;

    private static final String KEY_ROCKETMQ_MESSAGEIDS = "key_from_mqtt_mq_messageids";
    private static final int MAX_MESSAGES = 2000;

    private static final Topic TOPIC_CLIENT_BIND = new Topic("$events/client/+/bind/+");
    private static final Topic TOPIC_CLIENT_UNBIND = new Topic("$events/client/+/unbind/+");
    private static final Topic TOPIC_BROKER_CONNECTED = new Topic("$events/broker/+/connected");
    private static final Topic TOPIC_BROKER_DISCONNECTED = new Topic("$events/broker/+/disconnected");
    private static final Topic TOPIC_CLIENT_TIMESYNC = new Topic("$events/client/+/timesync/+");
    private static final Topic TOPIC_CLIENT_UPGRADED = new Topic("$events/client/+/upgraded");
    private static final Topic TOPIC_CLIENT_NOTIFY = new Topic("$events/client/+/notify");
    private static final Topic TOPIC_CLIENT_POWER_UPLOAD = new Topic("device/+/power/comsumption/upload");

    private static final Topic TOPIC_CLIENT_UPDATE_SHADOW = new Topic("$phihome/shadow/+/+/+/update");
    private static final Topic TOPIC_CLIENT_GET_SHADOW = new Topic("$phihome/shadow/+/+/+/get");

    //$events/client/{DEVICE_ID}/response/factory_reset/${message_id}
    private static final Topic TOPIC_CLIENT_FEEDBACK = new Topic("$events/client/+/response/+/+");

    //mqtt请求id字段名称
    public static final String REQUEST_ID = "client_token";

    /**
     * 自动调用的初始化方法.
     */
    @PostConstruct
    public void init() {
        instance = this;
    }

    /**
     * 初始化入口.
     *
     * @see PhiHomeCommandMain
     */
    public void start() {
        logger.debug("Base mqtt listener init starting...");
        CommonHandler.instance.onStart();
        AppCmdHandler.instance.onStart();
        OtherCmdHandler.instance.onStart();

        Thread thread = new Thread(this::startReadMqttRedisChannel);
        thread.start();

    }

    private boolean handleOnpublishMessage(String topic, String body, long storeTime) {
        logger.debug("base publish callback topic [{}]", topic);
        if (StringUtil.isNullOrEmpty(topic)) {
            logger.info("base callback topic is null");
            return true;
        }

        TopicType topicType = parseTopicType(topic);
        logger.info("parsed topicType [{}], topic [{}] body [{}]", topicType, topic, body);

        switch (topicType) {
            case TYPE_CLIENT_UPDATE_SHADOW:
                if (filterMsgBeforeDispatch(storeTime)) {
                    return true;
                }
                AppCmdHandler.instance.updatePublishByInterface(topic, body);
                break;
            case TYPE_CLIENT_GET_SHADOW:
                if (filterMsgBeforeDispatch(storeTime)) {
                    return true;
                }
                AppCmdHandler.instance.getPublishByInteface(topic, body);
                break;
            case TYPE_CLIENT_BIND:
                if (filterMsgBeforeDispatch(storeTime)) {
                    return true;
                }
                CommonHandler.instance.onBind(topic, body);
                break;
            case TYPE_CLIENT_UNBIND:
                if (filterMsgBeforeDispatch(storeTime)) {
                    return true;
                }
                CommonHandler.instance.onUnbind(topic, body);
                break;
            case TYPE_CLIENT_TIMESYNC:
                if (filterMsgBeforeDispatch(storeTime)) {
                    return true;
                }
                CommonHandler.instance.onTimeSync(topic, body);
                break;
            case TYPE_BROKER_CONNECTED:
                if (filterMsgBeforeDispatch(storeTime)) {
                    return true;
                }
                AppCmdHandler.instance.onConnectTopic(topic, body);
                break;
            case TYPE_BROKER_DISCONNECTED:
                if (filterMsgBeforeDispatch(storeTime)) {
                    return true;
                }
                AppCmdHandler.instance.onDisConnectTopic(topic, body);
                break;
            case TYPE_CLIENT_NOTIFY:
                if (filterMsgBeforeDispatch(storeTime)) {
                    return true;
                }
                OtherCmdHandler.instance.onDevicePushMessage(topic, body);
                break;
            case TYPE_CLIENT_POWER_UPLOAD:
                return PowerCmdHandler.instance.historyPowerStatistics(topic, body) == ResponseStatus.STATUS_OK;
            case TYPE_CLIENT_UPGRADED:
                return OtherCmdHandler.instance.onDeviceFwupdateResponse(topic, body) == ResponseStatus.STATUS_OK;
            case TYPE_CLIENT_FEEDBACK:
                redisTemplate.convertAndSend(topic, body);
            default:
                break;
        }
        return true;
    }

    private boolean filterMsgBeforeDispatch(long storeTimeStamp) {
        if (System.currentTimeMillis() - storeTimeStamp <= MESSAGE_MAX_COMSUME_TIME_DELAY) {
            return false;
        }
        logger.info("Drop message storeTimeStamp [{}]", storeTimeStamp);
        return true;
    }

    /**
     * 接收从mqtt服务器插件来的mqtt消息 { "topic":"", "body":"" }.
     */
    private void startReadMqttRedisChannel() {
        /*
         * Instantiate with specified consumer group name.
         */
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("group_dispatch_from_mqtt");
        logger.info("nameSer [{}]", RocketmqConfig.getNameser());
        consumer.setNamesrvAddr(RocketmqConfig.getNameser());

        /*
         * Specify where to start in case the specified consumer group is a
         * brand new one.
         */
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);

        /*
         * Subscribe one more more topics to consume.
         */
        try {
            consumer.subscribe("dispatch_mqtt_message", "*");
        } catch (MQClientException e) {
            e.printStackTrace();
        }

        /*
         * Register callback to execute on arrival of messages fetched from
         * brokers.
         */
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                if (msgs == null || msgs.isEmpty()) {
                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                }

                for (MessageExt m : msgs) {
                    if (m == null || m.getBody() == null) {
                        continue;
                    }
                    logger.debug("Not order message id [{}] ", m.getMsgId());
                    try {
                        if (isDupMessage(m.getMsgId())) {
                            logger.info("dup message id [{}]", m);
                            continue;
                        } else {
                            logger.debug("not dup message [{}]", m);
                        }
                        MqttRedisCmdModel model = JSON.parseObject(m.getBody(), MqttRedisCmdModel.class);
                        if (handleOnpublishMessage(model.getTopic(), model.getBody(), m.getBornTimestamp())) {
                            logger.debug("consume ok msg topic [{}], body [{}]", model.getTopic(), model.getBody());
                            putMsgIdIntoCache(m.getMsgId());
                        } else {
                            logger.info("consume failed msg topic [{}], body [{}]", model.getTopic(), model.getBody());
                            return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                        }
                    } catch (Exception e) {
                        logger.error("Not order message [{}] dispatch failed", m, e);
                    }

                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });

        /*
         * Launch the consumer instance.
         */
        try {
            consumer.start();
        } catch (MQClientException e) {
            e.printStackTrace();
        }
        logger.debug("command rocketmq reader Started");
    }

  

    private boolean isDupMessage(String messageId) {
        try {
            if (StringUtil.isNullOrEmpty(messageId)) {
                return false;
            }

            if (cache.isSetContains(KEY_ROCKETMQ_MESSAGEIDS, messageId)) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void putMsgIdIntoCache(String messageId) {
        long size = cache.getSetSize(KEY_ROCKETMQ_MESSAGEIDS);
        if (size >= MAX_MESSAGES) {
            cache.popSetValue(KEY_ROCKETMQ_MESSAGEIDS);
        }

        cache.putSetValue(KEY_ROCKETMQ_MESSAGEIDS, messageId);
    }

    /**
     * 解析topic，根据topic字符串解析出对应的类型，以便于调用不同的处理类来处理.
     *
     * @param topic
     *            收到的消息主题
     * @return topic类型
     */
    private static TopicType parseTopicType(String topic) {
        Topic comingTopic = new Topic(topic);

        if (comingTopic.match(TOPIC_CLIENT_BIND)) {
            return TopicType.TYPE_CLIENT_BIND;
        } else if (comingTopic.match(TOPIC_CLIENT_UNBIND)) {
            return TopicType.TYPE_CLIENT_UNBIND;
        } else if (comingTopic.match(TOPIC_BROKER_CONNECTED)) {
            return TopicType.TYPE_BROKER_CONNECTED;
        } else if (comingTopic.match(TOPIC_BROKER_DISCONNECTED)) {
            return TopicType.TYPE_BROKER_DISCONNECTED;
        } else if (comingTopic.match(TOPIC_CLIENT_TIMESYNC)) {
            return TopicType.TYPE_CLIENT_TIMESYNC;
        } else if (comingTopic.match(TOPIC_CLIENT_UPGRADED)) {
            return TopicType.TYPE_CLIENT_UPGRADED;
        } else if (comingTopic.match(TOPIC_CLIENT_NOTIFY)) {
            return TopicType.TYPE_CLIENT_NOTIFY;
        } else if (comingTopic.match(TOPIC_CLIENT_UPDATE_SHADOW)) {
            return TopicType.TYPE_CLIENT_UPDATE_SHADOW;
        } else if (comingTopic.match(TOPIC_CLIENT_GET_SHADOW)) {
            return TopicType.TYPE_CLIENT_GET_SHADOW;
        } else if (comingTopic.match(TOPIC_CLIENT_POWER_UPLOAD)) {
            return TopicType.TYPE_CLIENT_POWER_UPLOAD;
        } else if (comingTopic.match(TOPIC_CLIENT_FEEDBACK)) {
            return TopicType.TYPE_CLIENT_FEEDBACK;
        }
        return TopicType.TYPE_DEFAULT;
    }
}
