package com.manacommunity.discovery.config;

import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Configuration
public class EurekaInstanceConfig {

    @Bean
    @Profile("kubernetes")
    public EurekaInstanceConfigBean eurekaInstanceConfigBean(InetUtils inetUtils, Environment env) {
        EurekaInstanceConfigBean config = new EurekaInstanceConfigBean(inetUtils);
        String podName = env.getProperty("HOSTNAME", "discovery-service");
        config.setHostname(podName + ".discovery-service-headless.default.svc.cluster.local");
        config.setPreferIpAddress(false);
        config.setNonSecurePort(8761);
        return config;
    }
}
