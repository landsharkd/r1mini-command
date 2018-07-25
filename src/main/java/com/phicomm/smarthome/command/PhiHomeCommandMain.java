package com.phicomm.smarthome.command;

import com.phicomm.smarthome.command.cmd.BaseMqttTopicsInitlization;
import com.phicomm.smarthome.command.mqtt.mqttpool.RocketmqConfig;
import com.phicomm.smarthome.redisfeedback.FeedbackManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.web.client.RestTemplate;

/**
 * 应用启动入口.
 * @author levins
 */
@SpringBootApplication(scanBasePackages = { "com.phicomm.smarthome.**" })
@PropertySources({ @PropertySource(value = "classpath:application.properties", ignoreResourceNotFound = true) })
@MapperScan("com.phicomm.smarthome.command.dao.**")
@EnableConfigurationProperties({ RocketmqConfig.class })
@EnableCircuitBreaker
@EnableDiscoveryClient
@EnableEurekaClient
@EnableFeignClients
public class PhiHomeCommandMain {
    private static Logger logger = LogManager.getLogger(PhiHomeCommandMain.class);

    /**
     * rest接口.
     * @return rest模板
     */
    @Bean
    @LoadBalanced
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * 项目启动方法入口.
     * @param args args
     */
    public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(PhiHomeCommandMain.class, args);

        RedisMessageListenerContainer container = ctx.getBean(RedisMessageListenerContainer.class);
        FeedbackManager.init(container);

        //初始化mqtt消息回调，初始化读取mqtt服务器消息插件的消息队列
        BaseMqttTopicsInitlization.instance.start();
        logger.info("startup success!");
    }

    @Bean
    RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }
}
