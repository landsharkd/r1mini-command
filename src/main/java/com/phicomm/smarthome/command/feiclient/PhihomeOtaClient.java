/**
 *
 */
package com.phicomm.smarthome.command.feiclient;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author rongwei.huang
 *
 * 微服务调用PhihomeOTA的客户端，比如客户端升级结果回调.
 *
 */
@FeignClient(name = "smarthome-phihome-ota")
public interface PhihomeOtaClient {

    @RequestMapping(value = "/monitor/ota", method = RequestMethod.GET)
    public String getServerMonitor();
}
