package com.manacommunity.config.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.stereotype.Component;

@Component
public class ConfigServerHealthIndicator implements HealthIndicator {

    private final EnvironmentRepository environmentRepository;

    public ConfigServerHealthIndicator(EnvironmentRepository environmentRepository) {
        this.environmentRepository = environmentRepository;
    }

    @Override
    public Health health() {
        try {
            environmentRepository.findOne("application", "default", null);
            return Health.up()
                    .withDetail("configServer", "available")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("configServer", "unavailable")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
