package com.phicomm.smarthome.command.cmd;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.netflix.discovery.EurekaClient;
import com.phicomm.smarthome.cache.Cache;
import com.phicomm.smarthome.command.consts.Const;
import com.phicomm.smarthome.command.jsonstring.IJsonStringHandler;
import com.phicomm.smarthome.command.jsonstring.SimpleField;
import com.phicomm.smarthome.command.jsonstring.SimpleField.FieldOpt;
import com.phicomm.smarthome.command.model.common.DeviceMqttBaseResponseModel;
import com.phicomm.smarthome.command.model.common.PhiHomeBaseResponse;
import com.phicomm.smarthome.command.model.dao.InterfaceRspModel;
import com.phicomm.smarthome.command.model.dao.InvokeInterfaceDaoModel;
import com.phicomm.smarthome.command.mqtt.mqttpool.MQTTFactory;
import com.phicomm.smarthome.command.service.impl.BindDeviceWithAccountImpl;
import com.phicomm.smarthome.command.util.InnerServiceUtil;
import com.phicomm.smarthome.command.util.StringUtil;
import com.phicomm.smarthome.consts.PhihomeConst;
import com.phicomm.smarthome.consts.PhihomeConst.ResponseStatus;
import com.phicomm.smarthome.phihome.Topic;
import com.phicomm.smarthome.skill.SkillConst;
import com.phicomm.smarthome.util.MyResponseutils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.collections.CollectionUtils;
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
 * 接收来自App命令，主要是更新shadow,get shadow,设备在线和离线.
 */
@Component
public class AppCmdHandler {
    private static Logger logger = LogManager.getLogger(AppCmdHandler.class);

    private static final Topic SKILL_RESPONSE_CONTROLL_OUTLET_SWITCH = new Topic("$phihome/shadow/outlet_tc1/+/OutletStatus/update");

    /**
     * 实体变量，初始化时使用.
     */
    public static AppCmdHandler instance;

    private RestTemplate restTemplate;

    @Autowired
    private BindDeviceWithAccountImpl deviceImpl;

    @Autowired
    private EurekaClient discoveryClient;

    @Autowired
    private Cache cache;
    
    @Autowired
    private IJsonStringHandler jsonStringHandler;
    
    

    /**
     * 初始化方法，自动调用.
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
     * called when init.
     */
    public void onStart() {
    }

    /**
     * 通过调用data-process项目中的接口实现shadowInfo的更新.
     *
     * @param topic mqtt message topic
     * @param body mqtt message body
     */
    public void updatePublishByInterface(String topic, String body) {
        logger.debug("update shadow topic callback topic [{}] body [{}]", topic, body);
        
        /**
         * start : 去除requestId并记录(如果有)
         */
        SimpleField delSf = new SimpleField(FieldOpt.DEL);
        delSf.setName(BaseMqttTopicsInitlization.REQUEST_ID);
        body = jsonStringHandler.handle(body, delSf, "");
        List<SimpleField> addsfList = null;
        if(delSf.isSuccess()) {
            addsfList = new ArrayList<>();
            addsfList.add(new SimpleField(delSf.getName(), delSf.getVal(), FieldOpt.ADD));
        }
        if(logger.isDebugEnabled()) {
            logger.debug("removed requestId: update shadow topic callback topic [{}] body [{}]", topic, body);
        }
        /**
         * end : 去除requestId并记录(如果有)
         */
        
        try {
            HttpHeaders headers = new HttpHeaders();
            MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
            headers.setContentType(type);

            InvokeInterfaceDaoModel invokeDaoModel = new InvokeInterfaceDaoModel();
            invokeDaoModel.setTopic(topic);
            invokeDaoModel.setBody(body);
            String contentInvoke = JSONObject.toJSONString(invokeDaoModel);

            HttpEntity<String> formEntity = new HttpEntity<String>(contentInvoke, headers);
            String rspMsg = restTemplate.postForObject(
                    InnerServiceUtil.getPhihomeDataProcessUrl(discoveryClient, "r1mini/shadow/info/update"),
                    formEntity,
                    String.class);
            InterfaceRspModel rspModel = JSON.parseObject(rspMsg, InterfaceRspModel.class);
            logger.info("post from dataprocess rsp status [{}] message [{}]", rspModel.getStatus(), rspModel.getMessage());
            if (rspModel.getStatus() == ResponseStatus.STATUS_OK) {
                dispatchEvent(true, topic, body, addsfList);
                notifySkillIfPossible(true, topic);
            } else {
                dispatchEvent(false, topic, rspModel.getStatus(),addsfList);
                notifySkillIfPossible(false, topic);
            }
        } catch (Exception e) {
            logger.error("Post update event to dataprocess error", e);
            dispatchEvent(false, topic, PhihomeConst.ResponseStatus.STATUS_COMMON_FAILED, addsfList);
        }
    }

    private void notifySkillIfPossible(boolean updateShadowSuccess, String topic) {
        Topic tp = new Topic(topic);
        if (tp.match(SKILL_RESPONSE_CONTROLL_OUTLET_SWITCH)) {
            String[] items = topic.split("/");
            String reponseTopic = String.format(SkillConst.RESPONSE_CONTROLL_OUTLET_SWITCH, items[3]);
            logger.debug("notifySkillIfPossible topic [{}]", reponseTopic);

//            cache.pushListValue(reponseTopic, String.valueOf(updateShadowSuccess));
//            cache.setExpire(reponseTopic, 30, TimeUnit.SECONDS);
            cache.put(reponseTopic, String.valueOf(updateShadowSuccess), 30);
        }
    }

    /**
     * 调用dataprocess中的查询shadow接口.
     *
     * @param topic mqtt topic
     * @param body mqtt message
     */
    public void getPublishByInteface(String topic, String body) {
        logger.debug("get shadow topic callback topic [{}] body [{}]", topic, body);
        
        /**
         * start : 去除requestId并记录(如果有)
         */
        SimpleField delSf = new SimpleField(FieldOpt.DEL);
        delSf.setName(BaseMqttTopicsInitlization.REQUEST_ID);
        body = jsonStringHandler.handle(body, delSf, "");
        List<SimpleField> addsfList = null;
        if(delSf.isSuccess()) {
            addsfList = new ArrayList<>();
            addsfList.add(new SimpleField(delSf.getName(), delSf.getVal(), FieldOpt.ADD));
        }
        if(logger.isDebugEnabled()) {
            logger.debug("removed requestId: update shadow topic callback topic [{}] body [{}]", topic, body);
        }
        /**
         * end : 去除requestId并记录(如果有)
         */
        
        try {
            Map<String, Object> uriVariables = new HashMap<String, Object>();
            uriVariables.put("topic", topic);
            uriVariables.put("body", body);
            String getRspMsg = restTemplate.getForObject(
                    InnerServiceUtil.getPhihomeDataProcessUrl(discoveryClient, "shadow/info") + "?topic={topic}&body={body}",
                            String.class,
                            uriVariables);
            InterfaceRspModel rspModel = JSON.parseObject(getRspMsg, InterfaceRspModel.class);
            logger.info("post from dataprocess rsp status [{}] message [{}]", rspModel.getStatus(), rspModel.getMessage());
            if (rspModel.getStatus() == ResponseStatus.STATUS_OK) {
                dispatchEvent(true, topic, rspModel.getMessage(), addsfList);
            } else {
                PhiHomeBaseResponse<Object> rspBodyObj = new PhiHomeBaseResponse<>();
                rspBodyObj.setStatus(rspModel.getStatus());
                rspBodyObj.setMessage(MyResponseutils.parseMsg(rspModel.getStatus()));
                rspBodyObj.setResult(null);

                dispatchEvent(false, topic, JSON.toJSONString(rspBodyObj),addsfList);
            }
        } catch (Exception e) {
            logger.error("Post get event to dataprocess error", e);
        }
    }

    private String getResponseBody(int result) {
        DeviceMqttBaseResponseModel model = new DeviceMqttBaseResponseModel();
        model.setStatus(result);
        model.setTimestamp(System.currentTimeMillis() / 1000);
        model.intent();

        return JSON.toJSONString(model);
    }

    /**
     * 连接类通知的回调处理，比如设备绑定，解绑，对时等.
     * @param topic 消息主题
     * @param body 消息内容
     */
    public void onConnectTopic(String topic, String body) {
        logger.debug("onPublish callback topic:[{}] body:[{}]", topic, body);
        String deviceId = parseConnectDeviceId(topic);
        if (StringUtil.isNullOrEmpty(deviceId)) {
            logger.info("onDisconnect topic[{}] body is:[{}]", topic, body);
            return;
        }
        // 将连接对应的broker标识保存到redis中, 过期时间30分钟
        String key = deviceId + "_connected";
        cache.put(key, body, 1800);
        // 更改数据库设备在线状态
        deviceImpl.updateDeviceOnlineStatus(deviceId, PhihomeConst.DeviceOnlineStatus.ONLINE);
    }

    /**
     * 设备断连时收到的消息(设备主动掉线和被动掉线).
     * @param topic 消息主题
     * @param body 消息内容
     */
    public void onDisConnectTopic(String topic, String body) {
        logger.debug("onPublish callback topic:[{}] body:[{}]", topic, body);
        // 设备状态,更新表状态
        String deviceId = parseConnectDeviceId(topic);
        if (StringUtil.isNullOrEmpty(deviceId)) {
            logger.info("onDisconnect topic[{}] body is:[{}]", topic, body);
            return;
        }
        String key = deviceId + "_connected";
        String connectedBroker = (String) cache.get(key);
        // 连接和断开是同一个broker发起，则进行更改数据库操作（broker为null时，为同一个broker操作）
        if (connectedBroker == null || body.equals(connectedBroker)) {
            deviceImpl.updateDeviceOnlineStatus(deviceId, PhihomeConst.DeviceOnlineStatus.INLINE);
            cache.delete(key);
        }
    }

    private String parseConnectDeviceId(String topic) {
        if (StringUtil.isNullOrEmpty(topic)) {
            return null;
        }
        String[] items = topic.split("/");
        if (items == null || items.length < 3) {
            return null;
        }
        return items[2];
    }

    /**
     * 分发事件，包含两种失败或者成功，发送给app和设备.
     * @param success 存储shadow是否成功
     * @param tp 消息主题
     * @param body 消息内容
     */
    private void dispatchEvent(boolean success, String tp, int rspCode, List<SimpleField> sfList) {
        String body = getResponseBody(rspCode);
        dispatchEvent(success, tp, body, sfList);
    }

    private void dispatchEvent(boolean success, String tp, String body, List<SimpleField> sfList) {
        if (success) {
            tp = tp + "/accepted";
        } else {
            tp = tp + "/rejected";
        }
        
        /**
         * start : 返回结果中加入已经记录的requestId(如果有)
         */
        if(logger.isDebugEnabled()) {
            logger.debug("dispatchEvent -  response topic callback topic [{}] body [{}]", tp, body);
        }
        if(CollectionUtils.isNotEmpty(sfList)) {
            body = jsonStringHandler.handle(body, sfList);
        }
        if(logger.isDebugEnabled()) {
            logger.debug("dispatchEvent - add requestId: response topic callback topic [{}] body [{}]", tp, body);
        }
        /**
         * end : 返回结果中加入已经记录的requestId(如果有)
         */
        
        MQTTFactory.publish(tp, body);
    }
}
