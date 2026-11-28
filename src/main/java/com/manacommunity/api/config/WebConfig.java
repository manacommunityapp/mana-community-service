package com.manacommunity.api.config;

import com.manacommunity.api.platform.idempotency.IdempotencyInterceptor;
import com.manacommunity.api.versioning.AppVersionGatingInterceptor;
import com.manacommunity.api.versioning.DeprecationHeaderInterceptor;
import com.manacommunity.platform.ratelimit.RateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    private final IdempotencyInterceptor idempotencyInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;
    private final DeprecationHeaderInterceptor deprecationHeaderInterceptor;
    private final AppVersionGatingInterceptor appVersionGatingInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .toArray(String[]::new);
        // Swagger / OpenAPI docs — same origins as the API, not a blanket wildcard
        registry.addMapping("/swagger-ui.html").allowedOrigins(origins);
        registry.addMapping("/v3/api-docs/**").allowedOrigins(origins);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(idempotencyInterceptor).addPathPatterns("/api/**");
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/api/**");
        registry.addInterceptor(deprecationHeaderInterceptor).addPathPatterns("/api/**");
        registry.addInterceptor(appVersionGatingInterceptor).addPathPatterns("/api/**");
    }
}
