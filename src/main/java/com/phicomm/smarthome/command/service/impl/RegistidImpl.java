package com.phicomm.smarthome.command.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.phicomm.smarthome.command.dao.RegistidMapper;
import com.phicomm.smarthome.command.model.dao.RegistidModel;
import com.phicomm.smarthome.command.service.RegistidService;

@Service
public class RegistidImpl implements RegistidService {

    @Autowired
    RegistidMapper mapper;

    @Override
    public List<RegistidModel> queryByuid(String uid) {
        return mapper.queryRegistidByUid(uid);
    }
}
