package com.phicomm.smarthome.command.model.request;

import java.util.List;

public class InnerPatchMqttMessageModel {

    private List<InnerPublishMqttMessageModel> datas;

    public List<InnerPublishMqttMessageModel> getDatas() {
        return datas;
    }

    public void setDatas(List<InnerPublishMqttMessageModel> datas) {
        this.datas = datas;
    }
}
