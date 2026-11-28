package com.manacommunity.api.unit.service;

import com.manacommunity.api.platform.tenant.TenantContextHolder;
import com.manacommunity.api.rbac.service.CrossSocietyTenantGuard;
import com.manacommunity.api.rbac.service.SocietyPersonaRbacService;
import com.manacommunity.api.rbac.service.SocietyPersonaRbacService.ActorContext;
import com.manacommunity.api.rbac.service.SocietyPersonaRbacService.PersonaCapability;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SocietyRbacAndCrossTenantIsolationTest {

    private final SocietyPersonaRbacService rbac = new SocietyPersonaRbacService();
    private final CrossSocietyTenantGuard tenantGuard = new CrossSocietyTenantGuard();

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void enforcesLeastPrivilegeMatrixAcrossAllSixSocietyPersonas() {
        ActorContext owner = ActorContext.builder().userId(1L).rawRole("FLAT_OWNER").communityId(101L).flatId(501L).build();
        ActorContext tenant = ActorContext.builder().userId(2L).rawRole("TENANT").communityId(101L).flatId(501L).build();
        ActorContext family = ActorContext.builder().userId(3L).rawRole("FAMILY_MEMBER").communityId(101L).flatId(501L).build();
        ActorContext guard = ActorContext.builder().userId(4L).rawRole("GUARD").communityId(101L).build();
        ActorContext manager = ActorContext.builder().userId(5L).rawRole("ESTATE_MANAGER").communityId(101L).build();
        ActorContext mcAdmin = ActorContext.builder().userId(6L).rawRole("MC_ADMIN").communityId(101L).build();

        // 1. Flat Owner: full flat rights including AGM voting & occupancy management
        assertThat(rbac.isAuthorized(owner, 101L, 501L, PersonaCapability.VOTE_OWNER_AGM)).isTrue();
        assertThat(rbac.isAuthorized(owner, 101L, 501L, PersonaCapability.PAY_MAINTENANCE_DUES)).isTrue();
        assertThat(rbac.isAuthorized(owner, 101L, 501L, PersonaCapability.MANAGE_SOCIETY_GOVERNANCE)).isFalse();

        // 2. Tenant: can approve visitors/book/pay own dues, BLOCKED from owner-only AGM voting & lease management
        assertThat(rbac.isAuthorized(tenant, 101L, 501L, PersonaCapability.BOOK_AMENITY)).isTrue();
        assertThat(rbac.isAuthorized(tenant, 101L, 501L, PersonaCapability.PAY_MAINTENANCE_DUES)).isTrue();
        assertThat(rbac.isAuthorized(tenant, 101L, 501L, PersonaCapability.VOTE_OWNER_AGM)).isFalse();
        assertThat(rbac.isAuthorized(tenant, 101L, 501L, PersonaCapability.MANAGE_FLAT_OCCUPANCY)).isFalse();

        // 3. Family Member: visitor approval + amenity + complaint only; BLOCKED from financial dues & AGM voting
        assertThat(rbac.isAuthorized(family, 101L, 501L, PersonaCapability.APPROVE_VISITOR)).isTrue();
        assertThat(rbac.isAuthorized(family, 101L, 501L, PersonaCapability.VIEW_OWN_DUES)).isFalse();
        assertThat(rbac.isAuthorized(family, 101L, 501L, PersonaCapability.VOTE_OWNER_AGM)).isFalse();

        // 4. Guard: gate check-ins only; BLOCKED from resident financials, SLA admin, and governance
        assertThat(rbac.isAuthorized(guard, 101L, null, PersonaCapability.GATE_CHECKIN_SCAN)).isTrue();
        assertThat(rbac.isAuthorized(guard, 101L, 501L, PersonaCapability.VIEW_OWN_DUES)).isFalse();
        assertThat(rbac.isAuthorized(guard, 101L, null, PersonaCapability.MANAGE_HELPDESK_SLA)).isFalse();

        // 5. Estate Manager: operational SLA/gate dispatch; BLOCKED from MC governance rules
        assertThat(rbac.isAuthorized(manager, 101L, null, PersonaCapability.MANAGE_HELPDESK_SLA)).isTrue();
        assertThat(rbac.isAuthorized(manager, 101L, null, PersonaCapability.MANAGE_SOCIETY_GOVERNANCE)).isFalse();

        // 6. MC Admin: full control INSIDE Society 101
        assertThat(rbac.isAuthorized(mcAdmin, 101L, null, PersonaCapability.MANAGE_SOCIETY_GOVERNANCE)).isTrue();
        assertThat(rbac.isAuthorized(mcAdmin, 101L, null, PersonaCapability.CROSS_SOCIETY_PLATFORM_ACCESS)).isFalse();
    }

    @Test
    void blocksAllCrossSocietyAndCrossFlatDataLeaksIncludingMcAdminAndRuntimeTenantGuard() {
        ActorContext mcAdminSoc101 = ActorContext.builder().userId(6L).rawRole("MC_ADMIN").communityId(101L).build();
        ActorContext managerSoc101 = ActorContext.builder().userId(5L).rawRole("ESTATE_MANAGER").communityId(101L).build();
        ActorContext ownerFlat501 = ActorContext.builder().userId(1L).rawRole("FLAT_OWNER").communityId(101L).flatId(501L).build();

        // Cross-society attempts (Society 101 -> Society 202) MUST fail for EVERY community role
        assertThat(rbac.isAuthorized(mcAdminSoc101, 202L, null, PersonaCapability.MANAGE_SOCIETY_GOVERNANCE)).isFalse();
        assertThat(rbac.isAuthorized(managerSoc101, 202L, null, PersonaCapability.MANAGE_HELPDESK_SLA)).isFalse();
        assertThatThrownBy(() -> rbac.assertAuthorized(ownerFlat501, 202L, 501L, PersonaCapability.VIEW_OWN_DUES))
                .isInstanceOf(AccessDeniedException.class);

        // Cross-flat isolation inside the SAME society (Flat 501 cannot view Flat 502 dues or vote on Flat 502)
        assertThat(rbac.isAuthorized(ownerFlat501, 101L, 502L, PersonaCapability.VIEW_OWN_DUES)).isFalse();
        assertThat(rbac.isAuthorized(ownerFlat501, 101L, 502L, PersonaCapability.VOTE_OWNER_AGM)).isFalse();

        // Controller runtime TenantContextHolder guard blocks IDOR across societies
        TenantContextHolder.set(101L);
        tenantGuard.assertMatchesActiveTenant(101L);
        assertThatThrownBy(() -> tenantGuard.assertMatchesActiveTenant(202L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Cross-society data access denied");
    }
}
