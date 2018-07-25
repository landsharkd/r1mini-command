package com.phicomm.smarthome.command.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.phicomm.smarthome.command.controller.common.BaseController;
import com.phicomm.smarthome.command.model.common.PhiHomeBaseResponse;
import com.phicomm.smarthome.command.model.request.InnerPatchMqttMessageModel;
import com.phicomm.smarthome.command.model.request.InnerPublishMqttMessageModel;
import com.phicomm.smarthome.command.mqtt.mqttpool.MQTTFactory;
import com.phicomm.smarthome.command.util.MqttFeekbackUtil;
import com.phicomm.smarthome.command.util.StringUtil;
import com.phicomm.smarthome.consts.PhihomeConst.ResponseStatus;
import com.phicomm.smarthome.consts.R1miniTopics;
import com.phicomm.smarthome.r1mini.model.InnerPublishAttributeMsgModel;
import com.phicomm.smarthome.r1mini.model.InnerPublishR1miniMqttModel;
import com.phicomm.smarthome.redisfeedback.InvokeRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author rongwei.huang
 *
 *         2017年8月11日
 */
@RestController
public class PublishMqttMessageController extends BaseController {

    private static final Logger LOGGER = LogManager.getLogger(PublishMqttMessageController.class);

    private static final int TIMEOUT = 4000;

    /**
     * 请求发送mqtt消息接口.
     *
     * @return 请求结果
     */
    @RequestMapping(value = "/inner/publishmqtt", method = RequestMethod.POST, produces = { "application/json" })
    public PhiHomeBaseResponse<Object> publishMqttMsg(@RequestBody InnerPublishMqttMessageModel requestParas) {
        LOGGER.info("publishMqttMsg start requestParas [{}]", requestParas);
        PhiHomeBaseResponse<Object> rspObj = new PhiHomeBaseResponse<Object>();
        int result = dispatchInner(requestParas);
        if (result != ResponseStatus.STATUS_OK) {
            return errorResponse(result);
        }
        LOGGER.debug("publishMqttMsg successfully.");
        return successResponse(rspObj);
    }

    /**
     * 请求批量发送mqtt消息接口.
     * @param requestParas  请求参数
     * @return 请求结果
     */
    @RequestMapping(value = "/inner/publish_patch_mqtt", method = RequestMethod.POST, produces = { "application/json" })
    public PhiHomeBaseResponse<Object> publishPatchMqttMsg(@RequestBody InnerPatchMqttMessageModel requestParas) {
        LOGGER.info("publishPatchMqttMsg start.");
        if (requestParas == null || requestParas.getDatas() == null) {
            LOGGER.info("no request params");
            return errorResponse(ResponseStatus.STATUS_NO_PARA_IN_REQUEST);
        }
        LOGGER.info("publish patch mqtt message size [{}]", requestParas.getDatas().size());
        PhiHomeBaseResponse<Object> rspObj = new PhiHomeBaseResponse<Object>();
        for (InnerPublishMqttMessageModel item : requestParas.getDatas()) {
            dispatchInner(item);
        }
        LOGGER.debug("publishPatchMqttMsg successfully.");
        return successResponse(rspObj);
    }

    @RequestMapping(value = "/inner/publishmqtt/attibute", method = RequestMethod.POST, produces = {
            "application/json" })
    public PhiHomeBaseResponse<Object> publishR1miniAttributeMsg(
            @RequestBody InnerPublishAttributeMsgModel requestModel) {
        LOGGER.info("publishR1miniAttributeMsg start requestParas [{}]", JSON.toJSONString(requestModel));
        // 参数判断
        if (StringUtil.isNullOrEmpty(requestModel.getDeviceId()) || StringUtil.isNullOrEmpty(requestModel.getData())) {
            LOGGER.error("no enough parameter.");
            return errorResponse(ResponseStatus.STATUS_NO_PARA_IN_REQUEST);
        }

        PhiHomeBaseResponse<Object> rspObj = new PhiHomeBaseResponse<Object>();

        String requestTopic = "$events/broker/" + requestModel.getDeviceId() + "/attributes/set/request";
        // mqtt payload
        JSONObject jsonObject = new JSONObject();
        String messageId = StringUtil.initMessageId();
        String responseTopic = "$events/client/" + requestModel.getDeviceId() + "/response/attributes_set/" + messageId;
        jsonObject.put("response_topic", responseTopic);
        jsonObject.put("data", requestModel.getData());

        return MqttFeekbackUtil.sendMqttByInvokeSync(requestTopic, jsonObject.toJSONString(), responseTopic, TIMEOUT,
                worker);
    }

    @RequestMapping(value = "/inner/publishmqtt/need_rsp", method = RequestMethod.POST, produces = {
            "application/json" })
    public PhiHomeBaseResponse<Object> publishR1miniMqttMsg(
            @RequestBody InnerPublishR1miniMqttModel requestModel) {
        LOGGER.info("publishR1miniMqttMsg start requestParas [{}]", JSON.toJSONString(requestModel));
        // 参数判断
        if (StringUtil.isNullOrEmpty(requestModel.getDeviceId()) || StringUtil.isNullOrEmpty(requestModel.getTopic())) {
            LOGGER.error("no enough parameter.");
            return errorResponse(ResponseStatus.STATUS_NO_PARA_IN_REQUEST);
        }
        // 判断是否支持
        if (StringUtil.isNullOrEmpty(R1miniTopics.RESPONSE_TOPICS.get(requestModel.getTopic()))) {
            LOGGER.error("no match response topic.");
            return errorResponse(ResponseStatus.STATUS_INVAID_PARA);
        }

        PhiHomeBaseResponse<Object> rspObj = new PhiHomeBaseResponse<Object>();

        String requestTopic = String.format(requestModel.getTopic(), requestModel.getDeviceId());
        String messageId = StringUtil.initMessageId();
        String responseTopic = String.format(R1miniTopics.RESPONSE_TOPICS.get(requestModel.getTopic()),
                requestModel.getDeviceId(), messageId);
        // mqtt payload
        JSONObject jsonObject = null;
        if (StringUtil.isNullOrEmpty(requestModel.getBody())) {
            jsonObject = new JSONObject();
        } else {
            jsonObject = JSONObject.parseObject(requestModel.getBody());
        }
        jsonObject.put("response_topic", responseTopic);

        return MqttFeekbackUtil.sendMqttByInvokeSync(requestTopic, jsonObject.toJSONString(), responseTopic, TIMEOUT,
                worker);
    }

    private InvokeRequest.Worker worker = new InvokeRequest.Worker() {
        @Override
        public boolean doWork(InvokeRequest rq) {
            InnerPublishMqttMessageModel requestParas = new InnerPublishMqttMessageModel();
            requestParas.setTopic(rq.getTopic());
            requestParas.setBody(rq.getBody());
            int result = dispatchInner(requestParas);
            if (result != ResponseStatus.STATUS_OK) {
                return false;
            }
            return true;
        }
    };

    private int dispatchInner(InnerPublishMqttMessageModel requestParas) {
        if (requestParas == null || StringUtil.isNullOrEmpty(requestParas.getTopic())) {
            return ResponseStatus.STATUS_NO_PARA_IN_REQUEST;
        }

        LOGGER.debug("dispatch topic [{}], body [{}]", requestParas.getTopic(), requestParas.getBody());

        if (MQTTFactory.publish(requestParas.getTopic(), requestParas.getBody(), requestParas.getPriority())) {
            return ResponseStatus.STATUS_OK;
        } else {
            return ResponseStatus.STATUS_COMMON_FAILED;
        }
    }

}
