package com.phicomm.smarthome.command.service;

import com.phicomm.smarthome.model.MessagePushDaoModel;

public interface DevicePushMessageService {
    int insert(MessagePushDaoModel model);
}
