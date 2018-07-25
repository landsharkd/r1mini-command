package com.phicomm.smarthome.command.cmd;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.netflix.discovery.EurekaClient;
import com.phicomm.smarthome.command.consts.Const;
import com.phicomm.smarthome.command.model.common.PushMqttToOthersModel;
import com.phicomm.smarthome.command.model.response.OntimeDeviceMqttResponse;
import com.phicomm.smarthome.command.mqtt.mqttpool.MQTTFactory;
import com.phicomm.smarthome.command.util.InnerServiceUtil;
import com.phicomm.smarthome.command.util.StringUtil;
import com.phicomm.smarthome.consts.PhihomeConst;
import com.phicomm.smarthome.util.MyResponseutils;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * 通用初始化，接受设备绑定，取消绑定，对时等等 .
 */
@Component
public class CommonHandler {
    private static Logger logger = LogManager.getLogger(CommonHandler.class);

    private static final String MQTT_BIND_RSP_TOPIC = "$events/broker/%s/binded/%s";

    private RestTemplate restTemplate;

    /**
     * 初始化的实体对象.
     */
    public static CommonHandler instance;

    @Autowired
    private EurekaClient discoveryClient;

    /**
     * 自动调用的初始化方法.
     */
    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setReadTimeout(Const.INNER_SERVICE_REQUEST_READ_TIMEOUT);
        requestFactory.setConnectTimeout(Const.INNER_SERVICE_REQUEST_CONNECT_TIMEOUT);
        restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(requestFactory);
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler());

        instance = this;
    }

    /**
     * 初始化时调用的方法.
     *
     * @see BaseMqttTopicsInitlization
     *            监听的topic回调接口
     */
    public void onStart() {
        logger.debug("Common init starting...");
    }

    /**
     * 对时topic回调.
     */
    private void handleOnTimeRsp(String deviceId, String rspId) {
        logger.debug("handle timesync response message topic [{}]", rspId);
        OntimeDeviceMqttResponse model = new OntimeDeviceMqttResponse();
        model.setStatus(PhihomeConst.ResponseStatus.STATUS_OK);
        model.setTimestamp(System.currentTimeMillis() / 1000);
        model.intent();
        model.setTimezone("UTC-8"); // TODO 先返回东八区，后面需要根据设备的ip地址计算对应时区返回

        String rspJson = JSON.toJSONString(model);

        MQTTFactory.publish(String.format(Const.Mqtt.TOPIC_DEV_ONTIME_RSP_PREFIX, deviceId, rspId), rspJson);
    }

    /**
     * 设备解绑，删除对应的绑定信息，通过微服务调用phihome模块删除.
     *
     * @param body mqtt body
     * @return unbind result
     */
    private int unBindDevice(String topic, String body) {
        // 通过微服务插入数据库，解除绑定
        try {
            logger.debug("unbind device, body [{}]", body);
            PushMqttToOthersModel pushModel = new PushMqttToOthersModel(topic, body);
            String pushString = JSON.toJSONString(pushModel);

            HttpHeaders headers = new HttpHeaders();
            MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
            headers.setContentType(type);

            HttpEntity<String> formEntity = new HttpEntity<String>(pushString, headers);

            String rspPhihome = restTemplate.postForObject(
                    InnerServiceUtil.getPhihomeCommonServiceUrl(discoveryClient, "user/device/unbind/mqtt"),
                    formEntity,
                    String.class);
            if (StringUtil.isNotEmpty(rspPhihome)) {
                PhihomeBindDeviceRspModel rspModel = JSON.parseObject(rspPhihome, PhihomeBindDeviceRspModel.class);
                logger.info("unbind post from phihome rsp status [{}]", rspModel.getStatus());
                return rspModel.getStatus();
            }
            logger.debug("unbind return from phihome");
            return PhihomeConst.ResponseStatus.STATUS_OK;
        } catch (Exception e) {
            logger.error("Post bind event to phihome error", e);
            return PhihomeConst.ResponseStatus.STATUS_COMMON_FAILED;
        }
    }

    /**
     * 绑定设备,通过调用phihome微服务来绑定,可能要关联房间等消息.
     * @param topic topic
     * @param body
     *            传过来的消息内容
     * @return 绑定结果
     */
    private int bindDevice(String topic, String body) {
        // 通过微服务插入数据库，绑定
        try {
            PushMqttToOthersModel pushModel = new PushMqttToOthersModel(topic, body);
            String pushString = JSON.toJSONString(pushModel);

            HttpHeaders headers = new HttpHeaders();
            MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
            headers.setContentType(type);

            HttpEntity<String> formEntity = new HttpEntity<String>(pushString, headers);

            String rspPhihome = restTemplate.postForObject(
                    InnerServiceUtil.getPhihomeCommonServiceUrl(discoveryClient, "user/device/bind/mqtt"),
                    formEntity,
                    String.class);
            if (StringUtil.isNotEmpty(rspPhihome)) {
                PhihomeBindDeviceRspModel rspModel = JSON.parseObject(rspPhihome, PhihomeBindDeviceRspModel.class);
                logger.info("post from phihome rsp status [{}]", rspModel.getStatus());
                return rspModel.getStatus();
            }
            logger.debug("bind phihome result [{}]", rspPhihome);
            return PhihomeConst.ResponseStatus.STATUS_OK;
        } catch (Exception e) {
            logger.error("Post bind event to phihome error", e);
            return PhihomeConst.ResponseStatus.STATUS_COMMON_FAILED;
        }
    }

    /**
     * 绑定回调.
     * @param topic mqtt message topic
     * @param body mqtt message body
     */
    public void onBind(String topic, String body) {
        logger.debug("bind receive topic [{}] body [{}]", topic, body);

        String[] items = topic.split("/");
        if (items == null || items.length < 5) {
            logger.info("parse error bind topic [{}]", topic); //should not be here.
            return;
        }
        String deviceId = items[2];
        String rspOperateId = items[4];

        if (StringUtil.isNullOrEmpty(rspOperateId)) {
            logger.info("bind error with no response topic [{}]", topic);
            return;
        }
        if (StringUtil.isNullOrEmpty(deviceId)) {
            logger.info("bind with no device id topic [{}]", topic);
            return;
        }
        int bindStatus = bindDevice(topic, body);
        //绑定失败，发送mqtt通知；成功不需要，因为common-service会去发送mqtt消息
        if (bindStatus != PhihomeConst.ResponseStatus.STATUS_OK) {
            String erroRspTopic = String.format(MQTT_BIND_RSP_TOPIC, deviceId, rspOperateId);
            JSONObject rspObj = new JSONObject();
            rspObj.put("status", bindStatus);
            rspObj.put("message", MyResponseutils.parseMsg(bindStatus));
            rspObj.put("timestamp", System.currentTimeMillis() / 1000);
            rspObj.put("result", null);

            MQTTFactory.publish(erroRspTopic, rspObj.toJSONString());
        }
    }

    /**
     * unbind interface.
     * @param topic mqtt message topic
     * @param body mqtt message body
     */
    public void onUnbind(String topic, String body) {
        logger.debug("unbind receive topic [{}] body [{}]", topic, body);

        String[] items = topic.split("/");
        if (items == null || items.length < 5) {
            logger.info("parse error unbind topic [{}]", topic); //should not be here.
            return;
        }
        unBindDevice(topic, body);
    }

    /**
     * 对时回调.
     * @param topic mqtt message topic
     * @param body mqtt message body
     */
    public void onTimeSync(String topic, String body) {
        logger.debug("timesync receive topic [{}] body [{}]", topic, body);
        String[] items = topic.split("/");
        if (items == null || items.length < 5) {
            logger.info("parse error timesync topic [{}]", topic); //should not be here.
            return;
        }
        String deviceId = items[2];
        String rspOperateId = items[4];
        handleOnTimeRsp(deviceId, rspOperateId);
    }

    /**
     * 从phihome微服务返回的Json解析类.
     *
     * @author huangrongwei
     *
     */
    public static class PhihomeBindDeviceRspModel {
        private int status;
        private String message;

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
    }
}
