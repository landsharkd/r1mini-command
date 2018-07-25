package com.phicomm.smarthome.command.dao;

import com.phicomm.smarthome.model.MessagePushDaoModel;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;


/**
 * 设备推送消息表方法入口.
 * @author huangrongwei
 *
 */
public interface DevicePushMessageMapper {
    /**
     * 插入推送消息表.
     * @param dv 推送消息表实体类
     * @return 插入结果
     */
    @Insert("insert into ph_push_message (uid,device_id,title,message,type,create_time,update_time,status)"
            + " values(#{dv.uid},#{dv.deviceId},#{dv.title},#{dv.message},#{dv.type},#{dv.createTime},#{dv.updateTime},#{dv.status})")
    int insert(@Param("dv") MessagePushDaoModel dv);
}
