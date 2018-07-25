package com.phicomm.smarthome.command.model.common;

/**
 * @author rongwei.huang
 *
 */
public class PhiHomeBaseResponse<T> {

    private int status;

    private String message;

    private Object result;

    public PhiHomeBaseResponse() {
        status = 200;
        message = "";
    }

    public PhiHomeBaseResponse(long timeStamp, int status, String error, String exception, String message,
            String path)
    {
        this.status = status;
        this.message = message;
    }

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

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
