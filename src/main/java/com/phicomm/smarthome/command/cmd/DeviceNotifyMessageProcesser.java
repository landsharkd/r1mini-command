package com.phicomm.smarthome.command.cmd;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.netflix.discovery.EurekaClient;
import com.phicomm.smarthome.command.util.InnerServiceUtil;
import com.phicomm.smarthome.command.util.StringUtil;
import com.phicomm.smarthome.consts.Const.ResponseStatus;

public class DeviceNotifyMessageProcesser {
    private static Logger logger = LogManager.getLogger(DeviceNotifyMessageProcesser.class);

    private static final int EVENTTYPE_OUTLETS_TIMING = 200;//智能排插定时器推送消息类型

    private static final String EVENT_OUTLETS_TIMING_ON = "%s已开启，定时已完成";
    private static final String EVENT_OUTLETS_TIMING_OFF = "%s已关闭，定时已完成";

    private static final String TOPIC_TC1_GET_SHADOW = "$phihome/shadow/outlet_tc1/%s/OutletStatus/get";

    private RestTemplate restTemplate = new RestTemplate();

    private DeviceNotifyMessageProcesser() {
    }

    private static class Holder {
        private static DeviceNotifyMessageProcesser process = new DeviceNotifyMessageProcesser();
    }

    public static DeviceNotifyMessageProcesser getInstance() {
        return Holder.process;
    }

    /**
     * 处理排插设备特殊上报信息.
     *
     * @param body mqtt message body
     * @return 处理结果
     */
    public String processEventType(String topic, String body, EurekaClient discoveryClient,
            Map<String, String> portNameMap) {
        if (StringUtil.isNullOrEmpty(topic) || StringUtil.isNullOrEmpty(body)) {
            return body;
        }

        String[] items = topic.split("/");
        if (items == null || items.length < 3) {
            return body;
        }
        String deviceId = items[2];

        JSONObject model = JSON.parseObject(body, JSONObject.class);
        if (!model.containsKey("eventType")) {
            return body;
        }

        int eventType = model.getIntValue("eventType");
        switch (eventType) {
            case EVENTTYPE_OUTLETS_TIMING:
                String outletIndex = model.getString("outletIndex");

                //存储插排号取代直接存储名字，在取消息时再做转换
                String transName = "[" + outletIndex + "]";
                logger.debug("outletIndex [{}]", outletIndex);

                //获取deviceId的每个插口名字
                String name1 = transSlotName(deviceId, "S1", discoveryClient);
                String name2 = transSlotName(deviceId, "S2", discoveryClient);
                String name3 = transSlotName(deviceId, "S3", discoveryClient);
                String name4 = transSlotName(deviceId, "S4", discoveryClient);
                String name5 = transSlotName(deviceId, "S5", discoveryClient);
                String name6 = transSlotName(deviceId, "S6", discoveryClient);
                portNameMap.put("S1", name1);
                portNameMap.put("S2", name2);
                portNameMap.put("S3", name3);
                portNameMap.put("S4", name4);
                portNameMap.put("S5", name5);
                portNameMap.put("S6", name6);

                //String transName = transSlotName(deviceId, outletIndex, discoveryClient);
                //logger.debug("outletIndex [{}], transName [{}]", outletIndex, transName);
                int status = model.getIntValue("status");
                if (status == 0) {
                    model.put("message", String.format(EVENT_OUTLETS_TIMING_OFF, transName));
                } else {
                    model.put("message", String.format(EVENT_OUTLETS_TIMING_ON, transName));
                }
                return JSON.toJSONString(model);
            default:
                return "";
        }
    }

    private String transSlotName(String deviceId, String outletIndex, EurekaClient discoveryClient) {
        String topic = String.format(TOPIC_TC1_GET_SHADOW, deviceId);
        String response = getPublishByInteface(topic, "", discoveryClient);

        if (StringUtil.isNullOrEmpty(response)) {
            return outletIndex;
        }

        JSONObject obj = JSON.parseObject(response, JSONObject.class);
        if (obj == null) {
            return outletIndex;
        }

        if (obj.getInteger("status") != ResponseStatus.STATUS_OK) {
            return outletIndex;
        }

        JSONObject message = obj.getJSONObject("message");
        if (message == null) {
            return outletIndex;
        }

        JSONObject state = message.getJSONObject("state");
        if (state == null) {
            return outletIndex;
        }
        JSONObject reported = state.getJSONObject("reported");
        if (reported == null) {
            return outletIndex;
        }
        JSONObject switchName = reported.getJSONObject("switchName");
        if (switchName == null) {
            return outletIndex;
        }
        String transName = switchName.getString(outletIndex.toLowerCase());
        if (StringUtil.isNullOrEmpty(transName)) {
            transName = switchName.getString(outletIndex.toUpperCase());
        }
        if (StringUtil.isNotEmpty(transName)) {
            return transName;
        }

        return outletIndex;
    }

    public String getPublishByInteface(String topic, String body, EurekaClient discoveryClient) {
        try {
            Map<String, Object> uriVariables = new HashMap<String, Object>();
            uriVariables.put("topic", topic);
            uriVariables.put("body", body);
            return restTemplate.getForObject(InnerServiceUtil.getPhihomeDataProcessUrl(discoveryClient, "shadow/info")
                    + "?topic={topic}&body={body}", String.class, uriVariables);
        } catch (Exception e) {
            logger.error("Post get event to dataprocess error", e);
            return null;
        }
    }
}
