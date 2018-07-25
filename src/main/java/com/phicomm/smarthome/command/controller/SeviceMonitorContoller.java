package com.phicomm.smarthome.command.controller;

import com.alibaba.fastjson.JSON;
import com.jcabi.manifests.Manifests;
import com.phicomm.smarthome.cache.Cache;
import com.phicomm.smarthome.command.feiclient.CommonServiceClient;
import com.phicomm.smarthome.command.feiclient.DataProcessClient;
import com.phicomm.smarthome.command.feiclient.PhihomeOtaClient;
import com.phicomm.smarthome.command.feiclient.PhipushClient;
import com.phicomm.smarthome.command.mqtt.mqttpool.MQTTFactory;
import com.phicomm.smarthome.consts.PhihomeConst;
import com.phicomm.smarthome.model.SmartHomeResponse;
import com.phicomm.smarthome.model.status.StatusResponse;
import com.phicomm.smarthome.phihome.model.PhiHomeBaseResponse;
import com.phicomm.smarthome.util.MyResponseutils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author qiangbin.wei
 *
 *         2017年10月27日
 */
@RestController
public class SeviceMonitorContoller {

    private static final Logger LOGGER = LogManager.getLogger(SeviceMonitorContoller.class);

    private static final String COMMAND_SERVICE_HEALTH_CHECK = "command_service_health_check";

    @Autowired
    private Cache cache;

    @Autowired
    private DataProcessClient dataprodessClient;

    @Autowired
    private PhipushClient phipushClient;

    @Autowired
    private CommonServiceClient commonServiceClient;

    @Autowired
    private PhihomeOtaClient otaClient;

    /**
     * 服务监控.
     * @return 监控结果
     */
    @RequestMapping(value = "/monitor/command", produces = { "application/json" })
    public SmartHomeResponse<Object> getServiceMonitorStatus() {

        StatusResponse statusResponse = new StatusResponse();
        try {
            statusResponse.setGroupId(Manifests.read("Implementation-Vendor-Id"));
            statusResponse.setArtifactId(Manifests.read("Implementation-Title"));
            statusResponse.setBuildNum(Manifests.read("buildNumber"));
            statusResponse.setGitBranch(Manifests.read("gitBranch"));
            statusResponse.setGitCommit(Manifests.read("gitCommit"));
        } catch (Exception e) {
            LOGGER.error(e);
        }
        return new SmartHomeResponse<Object>(0, "MonitorMessage", statusResponse);
    }

    /**
     * 服务监控.
     *
     * @return 服务状态结果, status: 200 - OK，其他报错.message 错误提示，便于debug.
     */
    @RequestMapping(value = "/checkhealth", method = RequestMethod.GET, produces = { "application/json" })
    public PhiHomeBaseResponse<Object> getServiceHealth() {
        PhiHomeBaseResponse<Object> rsp = new PhiHomeBaseResponse<>();

        //检查rocketmq
        if (!MQTTFactory.publish("checkhealth", "")) {
            //发送rocketmq消息失败
            LOGGER.info("send test message to Rocketmq failed");
            rsp.setCode(PhihomeConst.ResponseStatus.STATUS_COMMON_FAILED);
            rsp.setMessage("smarthome-command send msg to rocketmq failed");
            return rsp;
        }

        //检查redis
        try {
            cache.put(COMMAND_SERVICE_HEALTH_CHECK, COMMAND_SERVICE_HEALTH_CHECK);
            cache.get(COMMAND_SERVICE_HEALTH_CHECK);
            cache.delete(COMMAND_SERVICE_HEALTH_CHECK);
        } catch (Exception e) {
            LOGGER.error(e);
            wrappErrorRsp(rsp, e, "redis");
            return rsp;
        }

        //检查OTA
        try {
            String rspJson = otaClient.getServerMonitor();
            LOGGER.debug("ota rspJson [{}]", rspJson);
            SmartHomeResponse model = JSON.parseObject(rspJson, SmartHomeResponse.class);
            if (model.getErrCode() != 0) {
                rsp.setCode(PhihomeConst.ResponseStatus.STATUS_COMMON_FAILED);
                rsp.setMessage("smarthome-command invoke phihome-ota failed, errorCode: " + model.getErrCode());
                return rsp;
            }
        } catch (Exception e) {
            LOGGER.error(e);
            wrappErrorRsp(rsp, e, "phihome-ota");
            return rsp;
        }

        //检查phipush
        try {
            String rspJson = phipushClient.getServerMonitor();
            SmartHomeResponse model = JSON.parseObject(rspJson, SmartHomeResponse.class);
            if (model.getErrCode() != 0) {
                rsp.setCode(PhihomeConst.ResponseStatus.STATUS_COMMON_FAILED);
                rsp.setMessage("smarthome-command invoke smarthome-push failed, errorCode: " + model.getErrCode());
                return rsp;
            }
        } catch (Exception e) {
            LOGGER.error(e);
            wrappErrorRsp(rsp, e, "smarthome-push");
            return rsp;
        }

        //检查common-service
        try {
            String rspJson = commonServiceClient.getServerMonitor();
            SmartHomeResponse model = JSON.parseObject(rspJson, SmartHomeResponse.class);
            if (model.getErrCode() != 0) {
                rsp.setCode(PhihomeConst.ResponseStatus.STATUS_COMMON_FAILED);
                rsp.setMessage("smarthome-command invoke common-service failed, errorCode: " + model.getErrCode());
                return rsp;
            }
        } catch (Exception e) {
            LOGGER.error(e);
            wrappErrorRsp(rsp, e, "common-service");
            return rsp;
        }

        //检查dateprocess
        try {
            String rspJson = dataprodessClient.getServerMonitor();
            SmartHomeResponse model = JSON.parseObject(rspJson, SmartHomeResponse.class);
            if (model.getErrCode() != 0) {
                rsp.setCode(PhihomeConst.ResponseStatus.STATUS_COMMON_FAILED);
                rsp.setMessage("smarthome-command invoke data-process failed, errorCode: " + model.getErrCode());
                return rsp;
            }
        } catch (Exception e) {
            LOGGER.error(e);
            wrappErrorRsp(rsp, e, "data-process");
            return rsp;
        }

        rsp.setCode(PhihomeConst.ResponseStatus.STATUS_OK);
        rsp.setMessage(MyResponseutils.parseMsg(PhihomeConst.ResponseStatus.STATUS_OK));
        return rsp;
    }

    private void wrappErrorRsp(PhiHomeBaseResponse<Object> rsp, Exception e, String server) {
        rsp.setCode(PhihomeConst.ResponseStatus.STATUS_COMMON_FAILED);
        StringBuilder sb = new StringBuilder();
        sb.append("smarthome-command invoke ").append(server).append(" exception: ");
        if (e != null) {
            sb.append(e.getMessage());
        }else {
            sb.append("Null");
        }
        rsp.setMessage(sb.toString());
    }
}
