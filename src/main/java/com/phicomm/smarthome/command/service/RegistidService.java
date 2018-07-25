package com.phicomm.smarthome.command.service;

import java.util.List;

import com.phicomm.smarthome.command.model.dao.RegistidModel;

public interface RegistidService {
    List<RegistidModel> queryByuid(String uid);
}
