package com.manacommunity.api.config;

import com.manacommunity.api.config.tenant.TenantAwareTaskDecorator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Arrays;
import java.util.concurrent.Executor;

@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private final TenantAwareTaskDecorator tenantAwareTaskDecorator;

    public AsyncConfig(TenantAwareTaskDecorator tenantAwareTaskDecorator) {
        this.tenantAwareTaskDecorator = tenantAwareTaskDecorator;
    }

    /** Default async executor — used by @Async methods without a qualifier. */
    @Override
    public Executor getAsyncExecutor() {
        return smsAsyncExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            log.error("[Async] Uncaught exception in async method '{}' with parameters {}: {}",
                    method.toGenericString(), Arrays.toString(params), throwable.getMessage(), throwable);
        };
    }

    @Bean(name = "smsAsyncExecutor")
    public Executor smsAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("sms-async-");
        executor.setTaskDecorator(tenantAwareTaskDecorator);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("notif-async-");
        executor.setTaskDecorator(tenantAwareTaskDecorator);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
        return executor;
    }
}
