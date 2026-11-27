package com.manacommunity.api.unit.service;

import com.manacommunity.api.config.WebSocketConfig;
import com.manacommunity.api.security.JwtTokenProvider;
import com.manacommunity.api.security.StompSubscriptionInterceptor;
import com.manacommunity.api.sync.SyncChangeLog;
import com.manacommunity.api.sync.SyncEnums.*;
import com.manacommunity.api.sync.dto.SyncDtos.ClientMutationItem;
import com.manacommunity.api.sync.engine.DeltaSyncEngine;
import com.manacommunity.platform.websocket.StompSecurityChannelInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.SimpleBrokerRegistration;
import org.springframework.scheduling.TaskScheduler;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketAndSyncResilienceTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private Environment environment;
    @Mock
    private StompSubscriptionInterceptor stompSubscriptionInterceptor;
    @Mock
    private StompSecurityChannelInterceptor stompSecurityChannelInterceptor;

    @Test
    @DisplayName("Should configure 10s STOMP heartbeats and TaskScheduler to detect zombie sockets on Wi-Fi/Cellular switch")
    void shouldConfigureStompHeartbeatAndScheduler() {
        WebSocketConfig config = new WebSocketConfig(
                jwtTokenProvider, environment, stompSubscriptionInterceptor, stompSecurityChannelInterceptor);

        MessageBrokerRegistry registry = mock(MessageBrokerRegistry.class);
        SimpleBrokerRegistration registration = mock(SimpleBrokerRegistration.class);
        when(registry.enableSimpleBroker("/topic", "/queue")).thenReturn(registration);
        when(registration.setTaskScheduler(any(TaskScheduler.class))).thenReturn(registration);
        when(registration.setHeartbeatValue(any(long[].class))).thenReturn(registration);

        config.configureMessageBroker(registry);

        verify(registration).setTaskScheduler(any(TaskScheduler.class));
        verify(registration).setHeartbeatValue(new long[]{10000L, 10000L});
    }

    @Test
    @DisplayName("DeltaSyncEngine should deduplicate duplicate offline mutations with identical clientMutationId")
    void shouldDeduplicateRepeatedOfflineMutation() {
        DeltaSyncEngine engine = new DeltaSyncEngine();

        ClientMutationItem mutation = ClientMutationItem.builder()
                .clientMutationId("mut_guard_tablet_001")
                .entityType(SyncEntityType.EXPRESS_DELIVERY)
                .entityId(501L)
                .operation(SyncOperation.UPDATE)
                .payloadJson("{\"passCode\":\"EXP-4521\",\"status\":\"CHECKED_IN\"}")
                .clientTimestamp(LocalDateTime.now().minusSeconds(15))
                .build();

        SyncChangeLog recorded = SyncChangeLog.builder()
                .id(88L)
                .entityId(501L)
                .status(SyncStatus.APPLIED)
                .build();

        var decision = engine.evaluateMutation(
                mutation,
                Optional.of(recorded),
                Optional.empty(),
                ConflictResolutionStrategy.SERVER_WINS
        );

        assertThat(decision.status()).isEqualTo(SyncStatus.APPLIED);
        assertThat(decision.serverChangeId()).isEqualTo(88L);
        assertThat(decision.reason()).contains("Idempotent repeat");
    }

    @Test
    @DisplayName("DeltaSyncEngine should apply SERVER_WINS if server record updated after offline tablet mutation")
    void shouldEnforceServerWinsOnStaleOfflineMutation() {
        DeltaSyncEngine engine = new DeltaSyncEngine();
        LocalDateTime clientTime = LocalDateTime.now().minusMinutes(2);
        LocalDateTime serverUpdate = LocalDateTime.now().minusSeconds(10);

        ClientMutationItem mutation = ClientMutationItem.builder()
                .clientMutationId("mut_guard_tablet_002")
                .entityType(SyncEntityType.VISITOR_PASS)
                .entityId(77L)
                .operation(SyncOperation.UPDATE)
                .payloadJson("{\"status\":\"CHECKED_IN\"}")
                .clientTimestamp(clientTime)
                .build();

        var decision = engine.evaluateMutation(
                mutation,
                Optional.empty(),
                Optional.of(serverUpdate),
                ConflictResolutionStrategy.SERVER_WINS
        );

        assertThat(decision.status()).isEqualTo(SyncStatus.CONFLICT_SERVER_WINS);
    }
}
