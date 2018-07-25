package com.phicomm.smarthome.command.controller.common;

import com.phicomm.smarthome.command.model.common.PhiHomeBaseResponse;
import com.phicomm.smarthome.consts.PhihomeConst.ResponseStatus;
import com.phicomm.smarthome.util.MyResponseutils;
import com.phicomm.smarthome.util.StringUtil;

/**
 * Phihome Controller基类, 提供了一些返回的基本功能.
 *
 * @author huangrongwei
 *
 */
public abstract class BaseController {

    public static PhiHomeBaseResponse<Object> geResponse(Object result) {
        PhiHomeBaseResponse<Object> smartHomeResponseT = new PhiHomeBaseResponse<Object>();
        smartHomeResponseT.setResult(result);
        return smartHomeResponseT;
    }

    protected PhiHomeBaseResponse<Object> errorResponse(int errCode) {
        String errMsg = MyResponseutils.parseMsg(errCode);
        return errorResponse(errCode, errMsg);
    }

    protected PhiHomeBaseResponse<Object> errorResponse(int errCode, String errMsg) {
        PhiHomeBaseResponse<Object> response = geResponse(null);
        response.setStatus(errCode);
        if (StringUtil.isNullOrEmpty(errMsg)) {
            response.setMessage(MyResponseutils.parseMsg(errCode));
        } else {
            response.setMessage(errMsg);
        }
        return response;
    }

    protected PhiHomeBaseResponse<Object> successResponse(Object obj) {
        PhiHomeBaseResponse<Object> response = (PhiHomeBaseResponse<Object>) obj;
        response.setStatus(ResponseStatus.STATUS_OK);
        response.setMessage(MyResponseutils.parseMsg(ResponseStatus.STATUS_OK));
        return response;
    }
}
