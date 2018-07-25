package com.phicomm.smarthome.command.cmd;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.netflix.discovery.EurekaClient;
import com.phicomm.smarthome.command.consts.Const;
import com.phicomm.smarthome.command.model.dao.InvokeInterfaceDaoModel;
import com.phicomm.smarthome.command.mqtt.mqttpool.MQTTFactory;
import com.phicomm.smarthome.command.util.InnerServiceUtil;
import com.phicomm.smarthome.command.util.StringUtil;
import com.phicomm.smarthome.consts.Const.ResponseStatus;
import com.phicomm.smarthome.util.MyResponseutils;
import com.phicomm.smarthome.consts.PhihomeConst;

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
 * 接收设备的电量上报信息 $events/presence/power/${deviceId}
 *
 * @author chao03.li
 * @date 2017年9月20日
 */
@Component
public class PowerCmdHandler {

    private static String SHADOW_UPDATE_ACCEPTED = "$phihome/shadow/outlet_tc1/%s/PowerConsumption/update/%s";

    private static final Logger LOGGER = LogManager.getLogger(PowerCmdHandler.class);

    // 初始化实体对象
    public static PowerCmdHandler instance;

    private RestTemplate restTemplate;

    @Autowired
    private EurekaClient discoveryClient;

    /**
     * 构造初始化对象.
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
     * 通过微服务调用data-process项目来完成历史电量统计操作.
     *
     * @param topic mqtt message topic
     * @param body mqtt message body
     */
    public int historyPowerStatistics(String topic, String body) {
        LOGGER.info("start statistics power info for new topic is [{}],body is[{}]", topic, body);
        String deviceId = parseDeviceId(topic);
        if (StringUtil.isNullOrEmpty(deviceId)) {
            LOGGER.error("powerInfo'topic type and length is error, topic[{}]", topic);
            return PhihomeConst.ResponseStatus.STATUS_COMMON_FAILED;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
            headers.setContentType(type);

            InvokeInterfaceDaoModel powerDaoModel = new InvokeInterfaceDaoModel();
            powerDaoModel.setTopic(topic);
            powerDaoModel.setBody(body);
            body = JSONObject.toJSONString(powerDaoModel);

            long start = System.currentTimeMillis();
            HttpEntity<String> formEntity = new HttpEntity<String>(body, headers);
            String rspPowerStatistics = restTemplate.postForObject(
                    InnerServiceUtil.getPhihomeDataProcessUrl(discoveryClient,
                            "powerInfo/statistics"),
                    formEntity,
                    String.class);
            PowerStatisticsRspModel rspModel = JSON.parseObject(rspPowerStatistics, PowerStatisticsRspModel.class);
            if (rspModel == null) {
                LOGGER.info("response from dataprocess model is null");
                sendUpdateShadowFailedTopic(deviceId, ResponseStatus.STATUS_COMMON_FAILED);
                return ResponseStatus.STATUS_COMMON_FAILED;
            }
            LOGGER.info("post from dataprocess rsp status [{}],rspMsg is [{}], cost [{}] ms", rspModel.getStatus(),
                    rspModel.getMessage(), (System.currentTimeMillis() - start));
            if (rspModel.getStatus() == ResponseStatus.STATUS_OK) {
                sendUpdateShadowTopic(deviceId, rspModel.getMessage(), true);
                return ResponseStatus.STATUS_OK;
            } else {
                sendUpdateShadowFailedTopic(deviceId, rspModel.getStatus());
                return rspModel.getStatus();
            }
        } catch (Exception e) {
            LOGGER.error("Post statistics event to phihome error", e);
            sendUpdateShadowFailedTopic(deviceId, ResponseStatus.STATUS_DATABASE_OPERATE_ERROR);
            return PhihomeConst.ResponseStatus.STATUS_DATABASE_OPERATE_ERROR;
        }
    }

    public static class PowerStatisticsRspModel {
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

    private void sendUpdateShadowFailedTopic(String deviceId, int errorCode) {
        PowerStatisticsRspModel errRsp = new PowerStatisticsRspModel();
        errRsp.setStatus(errorCode);
        errRsp.setMessage(MyResponseutils.parseMsg(errRsp.getStatus()));
        sendUpdateShadowTopic(deviceId, JSONObject.toJSONString(errRsp), false);
    }

    /**
     * 统计完电量信息之后发送topic给app.
     *
     * @param deviceId 设备唯一标示
     * @param body 发送mqtt 消息内容
     * @param sucess update shadow是否成功
     */
    private void sendUpdateShadowTopic(String deviceId, String body, boolean sucess) {
        String topic = "";
        if (sucess) {
            topic = String.format(SHADOW_UPDATE_ACCEPTED, deviceId, "accepted");
        } else {
            topic = String.format(SHADOW_UPDATE_ACCEPTED, deviceId, "rejected");
        }
        MQTTFactory.publish(topic, body);
    }

    /**
     * 从topic中拆分DeviceId.
     *
     * @param topic mqtt message topic
     * @return deviceid
     */
    private String parseDeviceId(String topic) {
        if (StringUtil.isNullOrEmpty(topic)) {
            return null;
        }

        String[] items = topic.split("/");
        if (items == null || items.length != 5) {
            LOGGER.error("电量统计的topic的格式不正确，请检查");
            return null;
        }
        LOGGER.info("deviceId in topic is [{}]", items[1]);
        return items[1];
    }
}
