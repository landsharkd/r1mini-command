package com.phicomm.smarthome.command.redis;

import com.phicomm.smarthome.command.dao.DevicesMapper;
import com.phicomm.smarthome.command.model.dao.DeviceDaoModel;
import com.phicomm.smarthome.command.util.StringUtil;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DevicesRedisImpl {
    private final Logger logger = LogManager.getLogger(getClass());

    @Autowired
    DevicesMapper mapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public List<DeviceDaoModel> queryDeviceByUid(String uid) {
        return mapper.queryDevicesByUid(uid);
    }

    /**
     * 根据mac查询，比如绑定的时候查看此mac是否已经被绑定过 如果此设备已经被人绑定过，那么就返回提示已经绑定，请先解绑.
     */
    public List<DeviceDaoModel> queryDevicesByDeviceMac(String deviceId) {
        return mapper.queryDevicesByDeviceMac(deviceId);
    }

    public List<DeviceDaoModel> queryDevices() {
        return mapper.queryDevices();
    }

    public long updateDevice(DeviceDaoModel device) {
        mapper.updateDevice(device);
        return 0;
    }

    /**
     * 更新设备在线状态.
     * @param deviceId 设备唯一标示
     * @param isOnline 0: 不在线 1:在线
     * @return 更新结果
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public long updateDeviceOnlineStatus(String deviceId, int isOnline) {
        int affected = 0;
        DeviceDaoModel daoModel = mapper.queryDeviceBindUid(deviceId);
        if (daoModel == null) {
            logger.error("can not find bind deviceid: [{}]", deviceId);
            return affected;
        }
        String uid = daoModel.getUid();
        String fid = daoModel.getFid();
        if (StringUtil.isNullOrEmpty(uid)) {
            logger.error("deviceid:[{}] can not find bind uid:[{}]", deviceId, uid);
            return affected;
        }

        affected = (int) mapper.updateDeviceOnlineStatus(deviceId, isOnline);
        try {
            String redisKey = uid + "_msg";
            String hashKey = fid + "_devices";
            redisTemplate.opsForHash().delete(redisKey, hashKey);
        } catch (Exception e) {
            logger.error(e);
            throw new RuntimeException();
        }
        return affected;
    }
}
