package com.phicomm.smarthome.command.util;

import com.alibaba.fastjson.JSONObject;
import com.phicomm.smarthome.command.model.common.PhiHomeBaseResponse;
import com.phicomm.smarthome.consts.Const;
import com.phicomm.smarthome.redisfeedback.FeedbackManager;
import com.phicomm.smarthome.redisfeedback.InvokeRequest;
import com.phicomm.smarthome.util.StringUtil;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *@author qiangbin.wei
 *
 *2018年7月6日
 */
public class MqttFeekbackUtil {

    private static final Logger LOGGER = LogManager.getLogger(MqttFeekbackUtil.class);

    /**
     * 封装好topic、body、responseTopic之后调用mqtt 同步阻塞方法
     *
     * @param topic
     * @param body
     * @param responseTopic
     * @param timeout
     * @param worker
     * @return
     */
    public static PhiHomeBaseResponse<Object> sendMqttByInvokeSync(String topic, String body, String responseTopic,
            int timeout, InvokeRequest.Worker worker) {
        PhiHomeBaseResponse<Object> phbr = new PhiHomeBaseResponse<>();
        InvokeRequest invokeRequest = InvokeRequest.create(topic, body, responseTopic, timeout, worker);
        String resultStr = FeedbackManager.invokeSync(invokeRequest);
        Object rt = null;
        if (StringUtil.isNullOrEmpty(resultStr) || "error".equals(resultStr)) {
            phbr.setStatus(Const.ResponseStatus.SEND_MQTT_BY_INVOKE_SYNC_IS_FAIL);
            phbr.setMessage(Const.ResponseStatus.SEND_MQTT_BY_INVOKE_SYNC_IS_FAIL_STR);
            LOGGER.error("publish mqtt by interface is fail, topic is [{}], body is [{}],resultStr is [{}] ", topic,
                    body, resultStr);
        } else {
            rt = JSONObject.parse(resultStr);
            phbr.setStatus(Const.ResponseStatus.STATUS_OK);
            phbr.setMessage(Const.ResponseStatus.STATUS_OK_STR);
        }
        phbr.setResult(rt);
        return phbr;
    }
}

