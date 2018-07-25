package com.phicomm.smarthome.command.feiclient;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author rongwei.huang
 *
 * 调用data-process的接口的FeignClient客户端.
 */
@FeignClient(name = "smarthome-phihome-data-process")
public interface DataProcessClient {

    @RequestMapping(value = "/monitor/data-process", method = RequestMethod.GET)
    public String getServerMonitor();
}
