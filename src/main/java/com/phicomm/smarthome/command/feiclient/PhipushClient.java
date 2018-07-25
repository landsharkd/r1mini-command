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
 * 微服务调用phipush的客户端.
 *
 */
@FeignClient(name = "push.messagepush")
public interface PhipushClient {

    @RequestMapping(value = "/monitor/push", method = RequestMethod.GET)
    public String getServerMonitor();
}
