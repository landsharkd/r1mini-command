package com.phicomm.smarthome.command.dao;

import com.phicomm.smarthome.command.model.dao.DeviceDaoModel;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.UpdateProvider;


/**
 * ph_device表方法入口.
 * @author huangrongwei
 *
 */
public interface DevicesMapper {
    /**
     * 根据uid查询device表.
     * @param uid 用户 id
     * @return 查询结果
     */
    @Select("select * from ph_device where uid=#{uid} and status=0")
    @Results({ @Result(property = "id", column = "id"), @Result(property = "uid", column = "uid"),
            @Result(property = "fid", column = "fid"), @Result(property = "ridDeviceidId", column = "rid_deviceid_id"),
            @Result(property = "rid", column = "rid"), @Result(property = "model", column = "model"),
            @Result(property = "hardwareVersion", column = "hardware_version"),
            @Result(property = "romVersion", column = "rom_version"),
            @Result(property = "deviceId", column = "device_id"), @Result(property = "name", column = "name"),
            @Result(property = "pic", column = "pic"), @Result(property = "position", column = "position"),
            @Result(property = "createTime", column = "create_time"),
            @Result(property = "updateTime", column = "update_time"), @Result(property = "status", column = "status"),
            @Result(property = "deviceType", column = "device_type") })
    public List<DeviceDaoModel> queryDevicesByUid(@Param("uid") String uid);

    /**
     * 根据mac地址/device id查询device表.
     * @param deviceId 设备device id
     * @return 查询结果
     */
    @Select("select * from ph_device where device_id=#{deviceId} and status=0")
    @Results({ @Result(property = "id", column = "id"), @Result(property = "uid", column = "uid"),
            @Result(property = "fid", column = "fid"), @Result(property = "ridDeviceidId", column = "rid_deviceid_id"),
            @Result(property = "rid", column = "rid"), @Result(property = "model", column = "model"),
            @Result(property = "hardwareVersion", column = "hardware_version"),
            @Result(property = "romVersion", column = "rom_version"),
            @Result(property = "deviceId", column = "device_id"), @Result(property = "name", column = "name"),
            @Result(property = "pic", column = "pic"), @Result(property = "position", column = "position"),
            @Result(property = "createTime", column = "create_time"),
            @Result(property = "updateTime", column = "update_time"),
            @Result(property = "status", column = "status"),
            @Result(property = "deviceType", column = "device_type"),
            @Result(property = "taskRemind", column = "task_remind"),
            @Result(property = "onlineStatus", column = "online_status")
        })
    public List<DeviceDaoModel> queryDevicesByDeviceMac(@Param("deviceId") String deviceId);

    /**
     * 查询所有的生效的device.
     * @return 查询结果
     */
    @Select("select * from ph_device where status=0")
    @Results({ @Result(property = "id", column = "id"), @Result(property = "uid", column = "uid"),
            @Result(property = "fid", column = "fid"), @Result(property = "ridDeviceidId", column = "rid_deviceid_id"),
            @Result(property = "rid", column = "rid"), @Result(property = "model", column = "model"),
            @Result(property = "hardwareVersion", column = "hardware_version"),
            @Result(property = "romVersion", column = "rom_version"),
            @Result(property = "deviceId", column = "device_id"), @Result(property = "name", column = "name"),
            @Result(property = "pic", column = "pic"), @Result(property = "position", column = "position"),
            @Result(property = "createTime", column = "create_time"),
            @Result(property = "updateTime", column = "update_time"), @Result(property = "status", column = "status"),
            @Result(property = "deviceType", column = "device_type") })
    public List<DeviceDaoModel> queryDevices();

    /**
     * 更新device数据库.
     * @param deviceDaoModel device实体类
     * @return 更新结果
     */
    @UpdateProvider(type = DeviceUpdateProvider.class, method = "updateDevice")
    long updateDevice(DeviceDaoModel deviceDaoModel);

    /**
     * 更具deivce id更新device 表.
     * @param deviceId device id
     * @param isOnline online
     * @return 更新结果
     */
    @Update("update ph_device set online_status=#{isOnline} where device_id=#{deviceId} and status=0")
    long updateDeviceOnlineStatus(@Param("deviceId") String deviceId, @Param("isOnline") int isOnline);

    /**
     * 根据device id查询device 表.
     * @param deviceId device id
     * @return 查询结果
     */
    @Select("select * from ph_device where device_id=#{deviceId} and status=0 limit 1")
    DeviceDaoModel queryDeviceBindUid(@Param("deviceId") String deviceId);
}
