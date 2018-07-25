package com.phicomm.smarthome.command.service;

import java.util.List;

import com.phicomm.smarthome.command.model.dao.DeviceDaoModel;

public interface BindDeviceWithAccountService {
    List<DeviceDaoModel> queryDeviceByUid(String uid);

    List<DeviceDaoModel> queryDevicesByDeviceMac(String deviceMac);

    List<DeviceDaoModel> queryDevices();

    long updateDevice(DeviceDaoModel device);

    long updateDeviceOnlineStatus(String deviceId, int isOnline);
}
