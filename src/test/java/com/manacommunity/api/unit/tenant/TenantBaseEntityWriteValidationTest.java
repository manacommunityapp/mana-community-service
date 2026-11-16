package com.manacommunity.api.unit.tenant;

import com.manacommunity.api.config.tenant.TenantContext;
import com.manacommunity.api.model.common.TenantBaseEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TenantBaseEntity Mandatory Tenant Identity and Cross-Tenant Write Validation Test")
class TenantBaseEntityWriteValidationTest {

    static class TestTenantEntity extends TenantBaseEntity {
        private String title;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
    }

    @BeforeEach
    @AfterEach
    void cleanContext() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Write with active TenantContext automatically populates communityId and organizationId")
    void writeWithActiveContextPopulatesTenantFields() {
        TenantContext.setTenant(12L, 345L);

        TestTenantEntity entity = new TestTenantEntity();
        entity.setTitle("Tennis Court Maintenance");
        entity.populateTenantContext();

        assertThat(entity.getCommunityId()).isEqualTo(345L);
        assertThat(entity.getOrganizationId()).isEqualTo(12L);
    }

    @Test
    @DisplayName("Write without active TenantContext throws IllegalStateException (mandatory tenant identity)")
    void writeWithoutTenantContextThrowsException() {
        TenantContext.clear();

        TestTenantEntity entity = new TestTenantEntity();
        entity.setTitle("Orphan Entity");

        assertThatThrownBy(entity::populateTenantContext)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("mandatory for tenant-owned write");
    }

    @Test
    @DisplayName("Write attempting to spoof a different communityId throws IllegalStateException (cross-tenant write rejection)")
    void writeWithMismatchedCommunityIdThrowsException() {
        TenantContext.setTenant(10L, 100L); // Current context: Community 100

        TestTenantEntity entity = new TestTenantEntity();
        entity.setCommunityId(200L); // Malicious payload tries to write into Community 200

        assertThatThrownBy(entity::populateTenantContext)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cross-tenant write rejected")
                .hasMessageContaining("does not match active tenant context");
    }

    @Test
    @DisplayName("Write with matching explicit communityId and organizationId succeeds")
    void writeWithMatchingExplicitIdsSucceeds() {
        TenantContext.setTenant(10L, 100L);

        TestTenantEntity entity = new TestTenantEntity();
        entity.setOrganizationId(10L);
        entity.setCommunityId(100L);
        entity.populateTenantContext();

        assertThat(entity.getCommunityId()).isEqualTo(100L);
        assertThat(entity.getOrganizationId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Update attempting to mutate communityId across tenant boundary is rejected")
    void updateAttemptingToMutateCommunityIdThrowsException() {
        TenantContext.setTenant(10L, 100L); // Active context is Community 100

        TestTenantEntity entity = new TestTenantEntity();
        entity.setCommunityId(200L); // Entity belongs to Community 200

        assertThatThrownBy(entity::validateTenantContextOnUpdate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cross-tenant update rejected");
    }
}
