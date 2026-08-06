package com.cpos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Community Property Operating System (CPOS)
 * Enterprise Digital Property OS — Multi-Tenant Community Super App
 *
 * Architecture: Clean Architecture + Domain-Driven Design + Event-Driven
 * Version: 1.0.0
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
public class CposApplication {

    public static void main(String[] args) {
        SpringApplication.run(CposApplication.class, args);
    }
}
