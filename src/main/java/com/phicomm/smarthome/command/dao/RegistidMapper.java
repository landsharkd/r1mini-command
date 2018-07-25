package com.phicomm.smarthome.command.dao;

import com.phicomm.smarthome.command.model.dao.RegistidModel;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

public interface RegistidMapper {
    @Select("select * from ph_registration where uid=#{uid}")
    @Results({@Result(property = "id", column = "id"),
            @Result(property = "uid", column = "uid"),
            @Result(property = "platform", column = "platform"),
            @Result(property = "registid", column = "registration_id"),
            @Result(property = "osType", column = "os_type")})
    public List<RegistidModel> queryRegistidByUid(@Param("uid") String uid);
}
