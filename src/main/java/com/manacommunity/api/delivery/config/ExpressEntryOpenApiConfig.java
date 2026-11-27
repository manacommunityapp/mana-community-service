package com.manacommunity.api.delivery.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExpressEntryOpenApiConfig {

    @Bean
    public GroupedOpenApi expressEntryPartnerApiGroup() {
        return GroupedOpenApi.builder()
                .group("Express Entry Partner API")
                .pathsToMatch("/api/v1/partners/delivery/**")
                .build();
    }
}
