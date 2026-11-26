package com.manacommunity.api.unit.service;

import com.manacommunity.api.common.lock.DistributedJobLockService;
import com.manacommunity.api.communityops.dashboard.CommunityOpsDashboardController;
import com.manacommunity.api.communityops.dashboard.CommunityOpsDashboardService;
import com.manacommunity.api.communityops.dashboard.CommunityOpsSnapshot;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.service.PermissionCheckService;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.platform.ops.PlatformAlertRepository;
import com.manacommunity.platform.ops.PlatformDeployRepository;
import com.manacommunity.platform.ops.PlatformMetricsRepository;
import com.manacommunity.platform.ops.PlatformOpsService;
import com.manacommunity.platform.websocket.WebSocketSessionTracker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpSubscription;
import org.springframework.messaging.simp.user.SimpSubscriptionMatcher;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("DistributedJobLockService, WebSocketSessionTracker, and Ops Broadcast Tests")
class SchedulerAndSessionCleanupTest {

    @Test
    @DisplayName("DistributedJobLockService passes java.sql.Timestamp instead of Instant to JdbcTemplate")
    void distributedJobLockPassesSqlTimestamp() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.update(anyString(), any(), any(), any(), any(), any())).thenReturn(1);

        DistributedJobLockService service = new DistributedJobLockService(jdbc);
        boolean acquired = service.acquireLock("daily_basket_cutoff_reminders", Duration.ofMinutes(5));

        assertTrue(acquired);
        ArgumentCaptor<Object> arg2 = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> arg3 = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> arg5 = ArgumentCaptor.forClass(Object.class);
        verify(jdbc).update(anyString(), eq("daily_basket_cutoff_reminders"),
                arg2.capture(), arg3.capture(), anyString(), arg5.capture());
        assertInstanceOf(Timestamp.class, arg2.getValue());
        assertInstanceOf(Timestamp.class, arg3.getValue());
        assertInstanceOf(Timestamp.class, arg5.getValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("WebSocketSessionTracker reaps orphaned dead-node session keys and deletes empty sets")
    void trackerReapsStaleDeadNodeSessions() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        SetOperations<String, String> setOps = mock(SetOperations.class);
        ValueOperations<String, String> valOps = mock(ValueOperations.class);
        when(redis.opsForSet()).thenReturn(setOps);
        when(redis.opsForValue()).thenReturn(valOps);

        when(redis.keys("ws:sessions:*")).thenReturn(Set.of("ws:sessions:11"), Set.of());
        when(setOps.members("ws:sessions:11")).thenReturn(Set.of("stale-sess-1"));
        when(valOps.get("ws:session-node:stale-sess-1")).thenReturn("dead-node-id");
        when(redis.hasKey("ws:node:dead-node-id")).thenReturn(false);
        when(setOps.size("ws:sessions:11")).thenReturn(0L);

        WebSocketSessionTracker tracker = new WebSocketSessionTracker(redis);
        assertFalse(tracker.hasActiveSessions());

        verify(setOps).remove("ws:sessions:11", "stale-sess-1");
        verify(redis).delete("ws:session-node:stale-sess-1");
        verify(redis).delete("ws:sessions:11");
    }

    @Test
    @DisplayName("PlatformOpsService.broadcastSnapshot skips DB work when 0 subscribers exist on /topic/platform/ops")
    void platformOpsSkipsWhenNoSubscribers() {
        PlatformMetricsRepository metricsRepo = mock(PlatformMetricsRepository.class);
        PlatformAlertRepository alertRepo = mock(PlatformAlertRepository.class);
        PlatformDeployRepository deployRepo = mock(PlatformDeployRepository.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        DataSource dataSource = mock(DataSource.class);
        SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
        SimpUserRegistry registry = mock(SimpUserRegistry.class);
        when(registry.findSubscriptions(any(SimpSubscriptionMatcher.class))).thenReturn(Set.of());

        PlatformOpsService ops = new PlatformOpsService(metricsRepo, alertRepo, deployRepo, jdbc, dataSource, messaging);
        ReflectionTestUtils.setField(ops, "simpUserRegistry", registry);

        ops.broadcastSnapshot();

        verifyNoInteractions(metricsRepo, alertRepo, deployRepo, jdbc, messaging);
    }

    @Test
    @DisplayName("CommunityOpsDashboardController pushes only when communities have active /ops subscribers")
    void communityOpsPushesOnlyForSubscribedCommunities() {
        CommunityOpsDashboardService dashboardService = mock(CommunityOpsDashboardService.class);
        SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
        CommunityRepository communityRepo = mock(CommunityRepository.class);
        LoggedInUserService userService = mock(LoggedInUserService.class);
        PermissionCheckService permService = mock(PermissionCheckService.class);
        SimpUserRegistry registry = mock(SimpUserRegistry.class);

        CommunityOpsDashboardController controller = new CommunityOpsDashboardController(
                dashboardService, messaging, communityRepo, userService, permService);
        ReflectionTestUtils.setField(controller, "simpUserRegistry", registry);

        // Case 1: 0 subscribers -> 0 DB queries
        when(registry.findSubscriptions(any(SimpSubscriptionMatcher.class))).thenReturn(Set.of());
        controller.pushSnapshotsToAll();
        verifyNoInteractions(communityRepo, dashboardService, messaging);

        // Case 2: subscriber on community 2 only -> skips community 1
        SimpSubscription sub2 = mock(SimpSubscription.class);
        when(sub2.getDestination()).thenReturn("/topic/community/2/ops");
        when(registry.findSubscriptions(any(SimpSubscriptionMatcher.class))).thenReturn(Set.of(sub2));

        Community c1 = Community.builder().id(1L).name("GENERAL").active(true).build();
        Community c2 = Community.builder().id(2L).name("Lakshmi's Emperia").active(true).build();
        when(communityRepo.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(c1, c2));
        CommunityOpsSnapshot snap = mock(CommunityOpsSnapshot.class);
        when(dashboardService.buildSnapshot(2L)).thenReturn(snap);

        controller.pushSnapshotsToAll();

        verify(dashboardService, never()).buildSnapshot(1L);
        verify(dashboardService).buildSnapshot(2L);
        verify(messaging).convertAndSend("/topic/community/2/ops", snap);
    }
}
