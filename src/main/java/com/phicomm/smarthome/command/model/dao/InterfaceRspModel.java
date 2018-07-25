package com.phicomm.smarthome.command.model.dao;

/**
 * 对应接口调用的Model
 *
 * @author chao03.li
 * @date 2017年10月12日
 */
public class InterfaceRspModel {

    private int status;

    private String message;

    private String result;

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

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
