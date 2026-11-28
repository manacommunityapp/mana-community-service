package com.manacommunity.api.unit.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.config.tenant.HibernateTenantFilterAspect;
import com.manacommunity.api.config.tenant.TenantContext;
import com.manacommunity.api.iot.mesh.entity.IoTSensor;
import com.manacommunity.api.iot.mesh.repository.IoTSensorRepository;
import com.manacommunity.api.iot.mesh.service.IoTMeshService;
import com.manacommunity.api.model.common.TenantBaseEntity;
import com.manacommunity.api.platform.tenant.TenantContextHolder;
import com.manacommunity.api.platform.tenant.TenantGuard;
import com.manacommunity.api.rbac.service.CrossSocietyTenantGuard;
import com.manacommunity.api.social.privacy.model.ResidentDirectoryEntry;
import com.manacommunity.api.social.privacy.model.UserPrivacyMatrix;
import com.manacommunity.api.social.privacy.model.ViewerContext;
import com.manacommunity.api.social.privacy.model.VisibilityScope;
import com.manacommunity.api.social.privacy.service.DirectoryPrivacyEngineImpl;
import com.manacommunity.api.sports.dto.SportsDashboardResponse.UpcomingEvent;
import com.manacommunity.api.sports.dto.SportsLiveMatchStateResponse;
import com.manacommunity.api.sports.repository.SportsMatchEventOutboxRepository;
import com.manacommunity.api.sports.service.SportsDashboardCacheService;
import com.manacommunity.api.sports.service.SportsLiveScoreCacheService;
import jakarta.persistence.EntityManager;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.hibernate.annotations.FilterDef;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Full-Stack Tenant Isolation: Authorization, DB/ORM Filter & Cache Keys")
class TenantIsolationAuthDbCacheTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private SportsMatchEventOutboxRepository outboxRepository;
    @Mock private IoTSensorRepository iotSensorRepository;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private EntityManager entityManager;
    @Mock private Session hibernateSession;
    @Mock private Filter hibernateFilter;
    @Mock private Authentication auth;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        TenantContextHolder.clear();
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Auth Guards: TenantGuard and CrossSocietyTenantGuard block mismatched communities")
    void authGuardsBlockCrossCommunityAccess() {
        TenantContextHolder.set(101L);
        TenantGuard guard = new TenantGuard();
        assertThat(guard.canAccess(auth, 101L)).isTrue();
        assertThat(guard.canAccess(auth, 202L)).isFalse();

        CrossSocietyTenantGuard societyGuard = new CrossSocietyTenantGuard();
        societyGuard.assertMatchesActiveTenant(101L);
        assertThatThrownBy(() -> societyGuard.assertMatchesActiveTenant(202L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Cross-society data access denied");
    }

    @Test
    @DisplayName("DB/ORM Isolation: TenantBaseEntity declares Hibernate @Filter & aspect activates filter + PG RLS")
    void dbOrmHibernateFilterAndPostgresRlsContextAreEnabled() {
        FilterDef filterDef = TenantBaseEntity.class.getAnnotation(FilterDef.class);
        org.hibernate.annotations.Filter filterAnnotation =
                TenantBaseEntity.class.getAnnotation(org.hibernate.annotations.Filter.class);
        assertThat(filterDef).isNotNull();
        assertThat(filterDef.name()).isEqualTo(TenantBaseEntity.TENANT_COMMUNITY_FILTER);
        assertThat(filterAnnotation).isNotNull();
        assertThat(filterAnnotation.condition()).isEqualTo("community_id = :communityId");

        when(entityManager.isOpen()).thenReturn(true);
        when(entityManager.unwrap(Session.class)).thenReturn(hibernateSession);
        when(hibernateSession.enableFilter(TenantBaseEntity.TENANT_COMMUNITY_FILTER)).thenReturn(hibernateFilter);

        HibernateTenantFilterAspect aspect = new HibernateTenantFilterAspect();
        aspect.setEntityManager(entityManager);
        aspect.setJdbcTemplate(jdbcTemplate);

        assertThat(aspect.enableCommunityFilter(entityManager, 202L)).isTrue();
        verify(hibernateFilter).setParameter("communityId", 202L);

        aspect.applyPostgresRlsContext(202L);
        verify(jdbcTemplate).update("SELECT set_config('app.current_community_id', ?, true)", "202");
    }

    @Test
    @DisplayName("Cache Isolation: SportsDashboard, LiveScore, IoTMesh & DirectoryPrivacy caches are community-scoped")
    void cachesIsolateKeysByCommunityId() {
        ObjectMapper mapper = new ObjectMapper();
        SportsDashboardCacheService dashboardCache = new SportsDashboardCacheService(redisTemplate, mapper);

        // Multi-community user reads upcoming events in Community 101 then Community 202 -> distinct keys
        TenantContext.setTenant(1L, 101L);
        dashboardCache.getUpcomingEvents(42L, List::of);
        verify(valueOps).get("dashboard:v1:comm:101:user:42:upcoming-events");

        TenantContext.setTenant(2L, 202L);
        dashboardCache.getUpcomingEvents(42L, List::of);
        verify(valueOps).get("dashboard:v1:comm:202:user:42:upcoming-events");

        // SportsLiveScoreCacheService includes active community prefix
        SportsLiveScoreCacheService liveScoreCache = new SportsLiveScoreCacheService(redisTemplate, outboxRepository, mapper);
        SportsLiveMatchStateResponse liveSnapshot = new SportsLiveMatchStateResponse(
                77L, "LIVE", 1, null, null, null, null, null, null, null, null,
                List.of(), "Team A", "Team B", null, null, 1L, 2L, 120, 20, 4, 6, 1L, "BAT");
        liveScoreCache.setLiveSnapshot(77L, liveSnapshot);
        verify(valueOps).set(eq("comm:202:match:77:live"), anyString(), any());

        // IoTMeshService pollSensor blocks cross-community access & uses community-scoped Redis key
        IoTMeshService meshService = new IoTMeshService(iotSensorRepository, null, null, null, jdbcTemplate);
        ReflectionTestUtils.setField(meshService, "redisTemplate", redisTemplate);
        IoTSensor sensorInCommA = IoTSensor.builder().id(55L).communityId(101L).lastReading(new BigDecimal("42.5")).build();
        when(iotSensorRepository.findById(55L)).thenReturn(Optional.of(sensorInCommA));

        TenantContext.setTenant(2L, 202L);
        assertThatThrownBy(() -> meshService.pollSensor(55L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("does not belong to active community 202");

        TenantContext.setTenant(1L, 101L);
        assertThat(meshService.pollSensor(55L)).isEqualByComparingTo("42.5");
        verify(valueOps).get("iot:comm:101:sensor:55:last-reading");

        // DirectoryPrivacyEngine isolates per (communityId, userId) even for same user across two communities
        DirectoryPrivacyEngineImpl privacyEngine = new DirectoryPrivacyEngineImpl();
        privacyEngine.registerPrivacyMatrix(UserPrivacyMatrix.builder()
                .userId(42L).communityId(101L)
                .phoneScope(VisibilityScope.EVERYONE_IN_SOCIETY).vehicleScope(VisibilityScope.HIDDEN_COMPLETELY)
                .flatScope(VisibilityScope.EVERYONE_IN_SOCIETY).emailScope(VisibilityScope.HIDDEN_COMPLETELY).build());
        privacyEngine.registerPrivacyMatrix(UserPrivacyMatrix.builder()
                .userId(42L).communityId(202L)
                .phoneScope(VisibilityScope.HIDDEN_COMPLETELY).vehicleScope(VisibilityScope.EVERYONE_IN_SOCIETY)
                .flatScope(VisibilityScope.EVERYONE_IN_SOCIETY).emailScope(VisibilityScope.HIDDEN_COMPLETELY).build());

        ResidentDirectoryEntry targetA = ResidentDirectoryEntry.builder()
                .userId(42L).communityId(101L).residentName("Karthik").phoneNumber("+919876543210").flatNumber("A-101").build();
        ViewerContext viewerA = ViewerContext.builder().viewerUserId(99L).communityId(101L).build();
        assertThat(privacyEngine.sanitizeRecord(viewerA, targetA).getPhoneNumber()).isEqualTo("+919876543210");

        ResidentDirectoryEntry targetB = ResidentDirectoryEntry.builder()
                .userId(42L).communityId(202L).residentName("Karthik").phoneNumber("+919876543210").flatNumber("B-404").build();
        ViewerContext viewerB = ViewerContext.builder().viewerUserId(88L).communityId(202L).build();
        assertThat(privacyEngine.sanitizeRecord(viewerB, targetB).getPhoneNumber()).isNull();
    }
}
