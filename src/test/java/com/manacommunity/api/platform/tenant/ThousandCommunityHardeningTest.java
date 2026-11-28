package com.manacommunity.api.platform.tenant;

import com.manacommunity.api.config.tenant.HibernateTenantFilterAspect;
import com.manacommunity.api.config.tenant.TenantContext;
import com.manacommunity.api.dto.CommunityModuleBulkRequest;
import com.manacommunity.api.dto.CommunityModuleResponse;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.Role;
import com.manacommunity.api.model.TenantResourceQuota;
import com.manacommunity.api.model.common.TenantBaseEntity;
import com.manacommunity.api.platform.tenant.provisioning.CommunityProvisionRequest;
import com.manacommunity.api.platform.tenant.provisioning.CommunityProvisionResponse;
import com.manacommunity.api.platform.tenant.provisioning.CommunityProvisioningService;
import com.manacommunity.api.platform.tenant.provisioning.TenantTier;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.repository.RoleRepository;
import com.manacommunity.api.repository.TenantResourceQuotaRepository;
import com.manacommunity.api.service.CommunityBlockConfigService;
import com.manacommunity.api.service.CommunityModuleService;
import com.manacommunity.api.service.impl.CommunityRoleInitializer;
import jakarta.persistence.EntityManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThousandCommunityHardeningTest {

    @Mock private CommunityRepository communityRepository;
    @Mock private CommunityRoleInitializer communityRoleInitializer;
    @Mock private CommunityModuleService communityModuleService;
    @Mock private CommunityBlockConfigService blockConfigService;
    @Mock private RoleRepository roleRepository;
    @Mock private TenantResourceQuotaRepository quotaRepository;

    private TenantRateLimitService rateLimitService;
    private TenantConnectionQuotaManager connectionQuotaManager;
    private TenantQueryTimeoutService queryTimeoutService;
    private CommunityProvisioningService provisioningService;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        TenantContextHolder.clear();
        rateLimitService = new TenantRateLimitService(null, 600);
        connectionQuotaManager = new TenantConnectionQuotaManager(12, 50);
        queryTimeoutService = new TenantQueryTimeoutService(5000L);
        provisioningService = new CommunityProvisioningService(
                communityRepository,
                communityRoleInitializer,
                communityModuleService,
                blockConfigService,
                roleRepository,
                quotaRepository,
                rateLimitService,
                connectionQuotaManager,
                queryTimeoutService
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        TenantContextHolder.clear();
    }

    @Test
    void testOneCallCommunityProvisioningSeedsAllTenantLayers() {
        when(communityRepository.findByInviteCode(anyString())).thenReturn(Optional.empty());
        when(communityRepository.save(any(Community.class))).thenAnswer(inv -> {
            Community c = inv.getArgument(0);
            c.setId(501L);
            return c;
        });
        when(roleRepository.findByCommunityId(501L)).thenReturn(List.of(
                Role.builder().id(1L).name("ADMIN").communityId(501L).build(),
                Role.builder().id(2L).name("USER").communityId(501L).build()
        ));
        when(communityModuleService.getModulesForCommunity(501L)).thenReturn(List.of(
                new CommunityModuleResponse(1L, 501L, "VISITORS", "Visitors", false, 1),
                new CommunityModuleResponse(2L, 501L, "POLLS", "Polls", false, 2)
        ));
        when(communityModuleService.getEnabledModuleKeys(501L)).thenReturn(List.of("VISITORS", "POLLS"));
        when(quotaRepository.findByCommunityIdAndResourceType(eq(501L), anyString())).thenReturn(Optional.empty());
        when(quotaRepository.save(any(TenantResourceQuota.class))).thenAnswer(inv -> inv.getArgument(0));

        CommunityProvisionRequest request = CommunityProvisionRequest.builder()
                .name("My Home Bhooja")
                .type("APARTMENT")
                .city("Hyderabad")
                .state("Telangana")
                .tenantTier("MEGA_TOWNSHIP")
                .enabledModules(List.of("VISITORS", "POLLS"))
                .build();

        CommunityProvisionResponse response = provisioningService.provisionCommunity(request);

        assertEquals(501L, response.getCommunityId());
        assertEquals("PROVISIONED", response.getStatus());
        assertEquals(TenantTier.MEGA_TOWNSHIP, response.getTenantTier());
        assertNotNull(response.getInviteCode());
        assertEquals(2, response.getRolesSeededCount());
        assertEquals(4, response.getQuotasSeededCount());
        assertEquals(4000, response.getRateLimitPerMinute());
        assertEquals(35, response.getConnectionPoolQuota());
        assertEquals(10000L, response.getStatementTimeoutMs());

        verify(communityRoleInitializer).initializeCommunityRoles(argThat(c -> c.getId().equals(501L)));
        verify(communityModuleService).initializeModulesForCommunity(501L);
        verify(communityModuleService).bulkUpdate(any(CommunityModuleBulkRequest.class));
        verify(blockConfigService).seedDefaultBlocks(501L);
        verify(quotaRepository, times(4)).save(any(TenantResourceQuota.class));

        // Verify live runtime guardrails took effect immediately
        assertEquals(35, connectionQuotaManager.getMaxAllowed(501L));
        assertEquals(10000L, queryTimeoutService.getStatementTimeoutMs(501L));
        assertEquals(4000, rateLimitService.checkTenant(501L).limit());
    }

    @Test
    void testProvisioningIdempotencyOnExistingInviteCode() {
        Community existing = Community.builder()
                .id(777L)
                .organizationId(1L)
                .name("Aparna Sarovar")
                .type("APARTMENT")
                .inviteCode("APAR-2026")
                .active(true)
                .build();
        when(communityRepository.findByInviteCode("APAR-2026")).thenReturn(Optional.of(existing));
        when(roleRepository.findByCommunityId(777L)).thenReturn(List.of());
        when(communityModuleService.getEnabledModuleKeys(777L)).thenReturn(List.of());

        CommunityProvisionRequest request = CommunityProvisionRequest.builder()
                .name("Aparna Sarovar")
                .inviteCode("apar-2026")
                .build();

        CommunityProvisionResponse response = provisioningService.provisionCommunity(request);

        assertEquals(777L, response.getCommunityId());
        assertEquals("ALREADY_EXISTS", response.getStatus());
        verify(communityRepository, never()).save(any());
    }

    @Test
    void testQueryTimeoutClampingAndPerTenantIsolation() {
        queryTimeoutService.setTenantStatementTimeout(101L, 120_000L); // exceeds max 30s -> clamped to 30000ms
        queryTimeoutService.setTenantStatementTimeout(102L, 100L);     // below min 500ms -> clamped to 500ms

        assertEquals(30_000L, queryTimeoutService.getStatementTimeoutMs(101L));
        assertEquals(500L, queryTimeoutService.getStatementTimeoutMs(102L));
        assertEquals(5000L, queryTimeoutService.getStatementTimeoutMs(103L));
    }

    @Test
    void testHibernateAspectResolvesContextHolderFallbackAndAppliesRlsAndTimeout() throws Throwable {
        EntityManager em = mock(EntityManager.class);
        Session session = mock(Session.class);
        Filter filter = mock(Filter.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);

        when(em.isOpen()).thenReturn(true);
        when(em.unwrap(Session.class)).thenReturn(session);
        when(session.enableFilter(TenantBaseEntity.TENANT_COMMUNITY_FILTER)).thenReturn(filter);
        when(session.getEnabledFilter(TenantBaseEntity.TENANT_COMMUNITY_FILTER)).thenReturn(filter);
        when(pjp.proceed()).thenReturn("OK");

        queryTimeoutService.setTenantStatementTimeout(888L, 6500L);

        HibernateTenantFilterAspect aspect = new HibernateTenantFilterAspect();
        aspect.setEntityManager(em);
        aspect.setJdbcTemplate(jdbc);
        aspect.setQueryTimeoutService(queryTimeoutService);

        // Bind ONLY TenantContextHolder (no TenantContext) to prove fallback works
        TenantContextHolder.set(888L);
        Object result = aspect.applyTenantFilterAroundTransaction(pjp);

        assertEquals("OK", result);
        verify(filter).setParameter("communityId", 888L);
        verify(jdbc).update("SELECT set_config('app.current_community_id', ?, true)", "888");
        verify(jdbc).update("SELECT set_config('statement_timeout', ?, true)", "6500");
        verify(session).disableFilter(TenantBaseEntity.TENANT_COMMUNITY_FILTER);
    }

    @Test
    void testIsolationStatusDiagnosticReturnsTenantLimits() {
        Community c = Community.builder().id(301L).name("Prestige Lakes").active(true).build();
        when(communityRepository.findById(301L)).thenReturn(Optional.of(c));
        when(quotaRepository.findByCommunityIdAndActiveTrue(301L)).thenReturn(List.of(
                TenantResourceQuota.builder()
                        .community(c)
                        .resourceType("GENERAL")
                        .rateLimitPerMinute(1500)
                        .maxConcurrentQueries(10)
                        .maxRowsPerExport(50000)
                        .priorityWeight(10)
                        .build()
        ));
        connectionQuotaManager.setTenantQuota(301L, 20);
        queryTimeoutService.setTenantStatementTimeout(301L, 7500L);

        Map<String, Object> status = provisioningService.getIsolationStatus(301L);
        assertEquals(301L, status.get("communityId"));
        assertEquals(20, status.get("maxConnectionLeases"));
        assertEquals(7500L, status.get("statementTimeoutMs"));
        assertEquals(1, ((List<?>) status.get("dbResourceQuotas")).size());
    }
}
