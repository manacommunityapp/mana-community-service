package com.manacommunity.api.unit.tenant;

import com.manacommunity.api.config.tenant.TenantAwareTaskDecorator;
import com.manacommunity.api.config.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Tenant Async Propagation and Thread Pool Isolation Test")
class TenantAsyncPropagationTest {

    private TenantAwareTaskDecorator taskDecorator;
    private ThreadPoolTaskExecutor executor;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        taskDecorator = new TenantAwareTaskDecorator();
        executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("test-async-");
        executor.setTaskDecorator(taskDecorator);
        executor.initialize();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        executor.shutdown();
    }

    @Test
    @DisplayName("Async task receives TenantContext from submitting thread")
    void asyncTaskReceivesCallerTenantContext() throws Exception {
        TenantContext context = TenantContext.builder()
                .organizationId(10L)
                .communityId(101L)
                .userId(5L)
                .build();
        TenantContext.setContext(context);

        AtomicReference<Long> asyncCommunityId = new AtomicReference<>();
        AtomicReference<Long> asyncOrgId = new AtomicReference<>();
        AtomicReference<Long> asyncUserId = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        executor.execute(() -> {
            asyncCommunityId.set(TenantContext.getCommunityId());
            asyncOrgId.set(TenantContext.getOrganizationId());
            TenantContext raw = TenantContext.getRawContext();
            if (raw != null) {
                asyncUserId.set(raw.getUserId());
            }
            latch.countDown();
        });

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        assertThat(asyncCommunityId.get()).isEqualTo(101L);
        assertThat(asyncOrgId.get()).isEqualTo(10L);
        assertThat(asyncUserId.get()).isEqualTo(5L);
    }

    @Test
    @DisplayName("Pooled thread cleans up TenantContext in finally block and never leaks to subsequent tasks")
    void pooledThreadCleansUpAndNeverLeaksStaleContext() throws Exception {
        // Task 1: Runs with Community 200
        TenantContext ctx1 = TenantContext.builder().organizationId(20L).communityId(200L).userId(2L).build();
        TenantContext.setContext(ctx1);

        CountDownLatch latch1 = new CountDownLatch(1);
        executor.execute(() -> {
            assertThat(TenantContext.getCommunityId()).isEqualTo(200L);
            latch1.countDown();
        });
        latch1.await(5, TimeUnit.SECONDS);

        // Task 2: Submitting thread has NO tenant context
        TenantContext.clear();

        AtomicReference<Long> task2CommunityId = new AtomicReference<>();
        AtomicReference<TenantContext> task2RawContext = new AtomicReference<>();
        CountDownLatch latch2 = new CountDownLatch(1);

        executor.execute(() -> {
            task2CommunityId.set(TenantContext.getCommunityId());
            task2RawContext.set(TenantContext.getRawContext());
            latch2.countDown();
        });
        latch2.await(5, TimeUnit.SECONDS);

        // Task 2 must NOT see Community 200 from Task 1
        assertThat(task2CommunityId.get()).isNull();
        assertThat(task2RawContext.get()).isNull();
    }

    @Test
    @DisplayName("ThreadLocal does not leak across manually spawned threads without explicit propagation")
    void threadLocalDoesNotLeakToRawChildThreads() throws Exception {
        TenantContext ctx = TenantContext.builder().organizationId(50L).communityId(500L).build();
        TenantContext.setContext(ctx);

        AtomicReference<TenantContext> childThreadContext = new AtomicReference<>();
        Thread childThread = new Thread(() -> {
            childThreadContext.set(TenantContext.getRawContext());
        });
        childThread.start();
        childThread.join(5000);

        // Standard ThreadLocal ensures child thread does NOT inherit parent's mutable context
        assertThat(childThreadContext.get()).isNull();
    }
}
