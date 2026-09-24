package com.manacommunity.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;

/**
 * HTTP client and async executor configuration for the push notification module.
 *
 * Two concerns:
 *  1. RestTemplate with timeouts tuned for the Expo Push API
 *     (Expo SLA: p99 < 500ms, so 5s connect + 8s read is conservative)
 *  2. A dedicated async thread pool for MobilePushEventListener
 *     so push latency never blocks Tomcat request threads
 */
@Configuration
@EnableAsync
public class RestClientConfig {

    /**
     * General-purpose RestTemplate used by ExpoPushServiceImpl.
     * If the project already declares a RestTemplate @Bean elsewhere,
     * rename this to pushRestTemplate and inject it with @Qualifier("pushRestTemplate")
     * in ExpoPushServiceImpl.
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);   // 5s to establish TCP connection to Expo
        factory.setReadTimeout(8_000);      // 8s to receive full response
        return new RestTemplate(factory);
    }

    /**
     * Thread pool for @Async event listeners.
     * Named "taskExecutor" so Spring's @Async picks it up automatically.
     *
     * Sizing rationale for a community app:
     *  - Core: 4  — always-warm threads for immediate dispatch
     *  - Max:  16 — burst headroom for high-activity periods
     *  - Queue: 200 — buffer announcements during traffic spikes without dropping them
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("push-async-");
        executor.setRejectedExecutionHandler(
            (runnable, pool) ->
                org.slf4j.LoggerFactory.getLogger(RestClientConfig.class)
                    .warn("Push async queue full — notification dropped. Consider increasing queueCapacity.")
        );
        executor.initialize();
        return executor;
    }
}
