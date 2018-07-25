package com.phicomm.smarthome.command.cmd;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import com.alibaba.fastjson.JSON;
import com.netflix.discovery.EurekaClient;
import com.phicomm.smarthome.command.cmd.CommonHandler.PhihomeBindDeviceRspModel;
import com.phicomm.smarthome.command.consts.Const;
import com.phicomm.smarthome.command.model.common.PushMqttToOthersModel;
import com.phicomm.smarthome.command.model.dao.DeviceDaoModel;
import com.phicomm.smarthome.command.model.dao.RegistidModel;
import com.phicomm.smarthome.command.service.impl.BindDeviceWithAccountImpl;
import com.phicomm.smarthome.command.service.impl.DevicePushMessageImpl;
import com.phicomm.smarthome.command.service.impl.RegistidImpl;
import com.phicomm.smarthome.command.util.InnerServiceUtil;
import com.phicomm.smarthome.command.util.MyListUtils;
import com.phicomm.smarthome.command.util.StringUtil;
import com.phicomm.smarthome.consts.PhihomeConst;
import com.phicomm.smarthome.model.MessagePushDaoModel;


/**
 * 一些杂项handler,比如设备上报通知.
 * @author huangrongwei
 *
 */
@Component
public class OtherCmdHandler {
    private static Logger logger = LogManager.getLogger(OtherCmdHandler.class);

    @Autowired
    private DevicePushMessageImpl pushMessageImpl;

    @Autowired
    private BindDeviceWithAccountImpl deviceImpl;

    @Autowired
    private RegistidImpl registIdImpl;

    @Autowired
    private EurekaClient discoveryClient;

    /**
     * 实体变量，初始化时使用.
     */
    public static OtherCmdHandler instance;

    private RestTemplate restTemplate;

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
        logger.debug("OtherCmdHandler onStart");
    }

    /**
     * 收到设备上报的消息通知
     * 1. 存入数据库
     * 2. 调用其他系统发送给系统的第三方极光平台，然后推送给app
     * @param topic 消息topic
     * @param body 消息内容
     */
    public void onDevicePushMessage(String topic, String body) {
        logger.debug("OtherCmdHandler receive device notify topic [{}], body [{}]", topic, body);

        Map<String, String> portNameMap = new HashMap<>();
        String temp = DeviceNotifyMessageProcesser.getInstance().processEventType(topic, body, discoveryClient, portNameMap);

        if (StringUtil.isNullOrEmpty(temp)) {
            logger.info("cannot push device message body [{}]", body);
            return;
        }

        if (saveDb(topic, temp)) {
            //替换内容
            for (Map.Entry<String, String> entry : portNameMap.entrySet()) {
                temp = temp.replaceAll("\\[" + entry.getKey() + "\\]", entry.getValue());
            }
            pushToThirdParty(topic, temp);
        }
    }

    public int onDeviceFwupdateResponse(String topic, String body) {
        return pushToFwupdateServer(topic, body);
    }

    private int pushToFwupdateServer(String topic, String body) {
        //通过微服务调用把数据发送给升级后台
        try {
            PushMqttToOthersModel pushModel = new PushMqttToOthersModel(topic, body);
            String pushString = JSON.toJSONString(pushModel);

            HttpHeaders headers = new HttpHeaders();
            MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
            headers.setContentType(type);

            HttpEntity<String> formEntity = new HttpEntity<String>(pushString, headers);

            String rspBackend = restTemplate.postForObject(
                    InnerServiceUtil.getSmartHomeOtaUpgradeUrl(discoveryClient, "fw_update_report"),
                    formEntity,
                    String.class);
            if (StringUtil.isNotEmpty(rspBackend)) {
                PhihomeBindDeviceRspModel rspModel = JSON.parseObject(rspBackend, PhihomeBindDeviceRspModel.class);
                logger.info("post upgrade_report_topic [{}] body [{}] to ota, rsp status [{}]", topic, body, rspModel.getStatus());
                return rspModel.getStatus();
            }
            return PhihomeConst.ResponseStatus.STATUS_OK;
        } catch (Exception e) {
            logger.error("Post bind event to phihome error", e);
            return PhihomeConst.ResponseStatus.STATUS_COMMON_FAILED;
        }
    }

    private boolean saveDb(String topic, String body) {
        if (StringUtil.isNullOrEmpty(topic)) {
            logger.info("device notify message has no topic");
            return false;
        }
        if (StringUtil.isNullOrEmpty(body)) {
            logger.info("device notify message has no body");
            return false;
        }

        try {
            String deviceId = parseDeviceId(topic);
            if (StringUtil.isNullOrEmpty(deviceId)) {
                logger.info("push message parse device error");
                return false;
            }

            String uid = getUid(deviceId);
            if (StringUtil.isNullOrEmpty(uid)) {
                logger.info("push message find uid error by deviceid [{}]", deviceId);
                return false;
            }

            //正式解析publish消息带过来的Json数据
            MessagePushDaoModel model = JSON.parseObject(body, MessagePushDaoModel.class);
            if (StringUtil.isNullOrEmpty(model.getMessage())) {
                logger.info("push message has no message deviceId [{}]", deviceId);
                return false;
            }
            if (model.getTitle() == null) {
                model.setTitle("");
            }

            //from now on, all precheck passed!
            logger.debug("begin to save push message");
            model.setUid(uid);
            model.setDeviceId(deviceId);

            long current = System.currentTimeMillis() / 1000;
            model.setCreateTime(current);
            model.setUpdateTime(current);

            model.setType(PhihomeConst.DevicePushMessageType.TYPE_DEVICE_BUSI_MSG);
            model.setStatus(0);
            pushMessageImpl.insert(model);
        } catch (Exception e) {
            logger.error("insert notify message error ", e);
            return false;
        }

        return true;
    }

    private int pushToThirdParty(String topic, String body) {
        //通过微服务调用后台服务，从而推送给极光
        try {
            String deviceId = parseDeviceId(topic);

            //根据deviceId获取设备注册信息
            RegistidModel registidModel=findRegistIdbyDeviceId(deviceId);
            if(registidModel==null) {
                return 0;
            }
            String registId = registidModel.getRegistid();

            //-1 means user login out
            if (StringUtil.isNullOrEmpty(registId) || registId.equals("-1")) {
                logger.info("push message with null registid, return deviceId[{}]", deviceId);
                return 0;
            }

            body = fillBody(body, registId,registidModel.getOsType());

            HttpHeaders headers = new HttpHeaders();
            MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
            headers.setContentType(type);

            HttpEntity<String> formEntity = new HttpEntity<String>(body, headers);

            String rspPhihomePush = restTemplate.postForObject(
                    InnerServiceUtil.getPhiPushUrl(discoveryClient, "JPush/push/with_registid"),
                    formEntity,
                    String.class);
            logger.debug("push to phipush result [{}]", rspPhihomePush);
            return PhihomeConst.ResponseStatus.STATUS_OK;
        } catch (Exception e) {
            logger.error("Post bind event to phihome error", e);
            return PhihomeConst.ResponseStatus.STATUS_COMMON_FAILED;
        }
    }

    private RegistidModel findRegistIdbyDeviceId(String deviceId) {
        try {
            List<DeviceDaoModel> devices = deviceImpl.queryDevicesByDeviceMac(deviceId);
            if (!MyListUtils.isEmpty(devices)) {
                String uid = devices.get(0).getUid();

                if (StringUtil.isNullOrEmpty(uid)) {
                    logger.info("uid is null for deviceId [{}]", deviceId);
                    return null;
                }
                //0 means shutdown message pushing for this device.
                if (devices.get(0).getTaskRemind() == 0) {
                    logger.info("task remind is 0 for deviceId [{}]", deviceId);
                    return null;
                }

                List<RegistidModel> models = registIdImpl.queryByuid(uid);
                if (!MyListUtils.isEmpty(models)) {
                    return models.get(0);
                }
            }
        } catch (Exception e) {
            logger.error("db error", e);
        }
        return null;
    }

    private String fillBody(String body, String registId,String osType) {
        //find title, message
        MessageJPushModel model = JSON.parseObject(body, MessageJPushModel.class);

        //fill registration_id and osType
        model.setRegistrationId(registId);
        model.setOsType(osType);

        return JSON.toJSONString(model);
    }

    private String parseDeviceId(String topic) {
        String[] items = topic.split("/");
        if (items == null || items.length < 3) {
            logger.info("parse notify topic error topic [{}]", topic);
            return null;
        }

        return items[2];
    }

    private String getUid(String deviceId) {
        List<DeviceDaoModel> devices = deviceImpl.queryDevicesByDeviceMac(deviceId);
        if (devices != null && !devices.isEmpty()) {
            return devices.get(0).getUid();
        }
        return null;
    }

    public static class MessageJPushModel {
        private String title;

        private String message;

        private String registrationId;

        private String osType;

        public String getOsType() {
            return osType;
        }

        public void setOsType(String osType) {
            this.osType = osType;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getRegistrationId() {
            return registrationId;
        }

        public void setRegistrationId(String registrationId) {
            this.registrationId = registrationId;
        }
    }
}
