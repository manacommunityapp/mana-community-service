package com.manacommunity.api.unit.tenant;

import com.manacommunity.api.config.tenant.TenantContext;
import com.manacommunity.api.config.tenant.TenantFilter;
import com.manacommunity.api.controller.OrganizationController;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.Organization;
import com.manacommunity.api.model.common.TenantBaseEntity;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.repository.OrganizationRepository;
import com.manacommunity.api.user.model.AppUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Hierarchical Multi-Tenancy (Organization -> Community) Test Suite")
class HierarchicalMultiTenancyTest {

    @BeforeEach
    @AfterEach
    void cleanTenantContext() {
        TenantContext.clear();
    }

    @Nested
    @DisplayName("1. TenantContext Hierarchy & Lifecycle")
    class TenantContextTests {

        @Test
        @DisplayName("Stores and returns hierarchical tenancy context")
        void setsAndGetsTenantContext() {
            TenantContext.setTenant(101L, 502L);

            assertThat(TenantContext.getOrganizationId()).isEqualTo(101L);
            assertThat(TenantContext.getCommunityId()).isEqualTo(502L);
            assertThat(TenantContext.getTenantId()).isEqualTo("community_502");
        }

        @Test
        @DisplayName("Returns default organization ID and schema when empty")
        void returnsDefaultsWhenUnset() {
            TenantContext.clear();

            assertThat(TenantContext.getOrganizationId()).isEqualTo(TenantContext.DEFAULT_ORG_ID);
            assertThat(TenantContext.getCommunityId()).isNull();
            assertThat(TenantContext.getTenantId()).isEqualTo(TenantContext.DEFAULT_SCHEMA);
        }

        @Test
        @DisplayName("Parses legacy community_X tenant string")
        void parsesLegacyTenantFormat() {
            TenantContext.setTenantId("community_777");

            assertThat(TenantContext.getCommunityId()).isEqualTo(777L);
            assertThat(TenantContext.getTenantId()).isEqualTo("community_777");
        }

        @Test
        @DisplayName("AutoCloseable clears context automatically")
        void autoCloseableCleansUpContext() {
            try (TenantContext ctx = TenantContext.builder().organizationId(888L).communityId(999L).build()) {
                TenantContext.setContext(ctx);
                assertThat(TenantContext.getOrganizationId()).isEqualTo(888L);
                assertThat(TenantContext.getCommunityId()).isEqualTo(999L);
            }

            assertThat(TenantContext.getCommunityId()).isNull();
            assertThat(TenantContext.getOrganizationId()).isEqualTo(TenantContext.DEFAULT_ORG_ID);
        }
    }

    @Nested
    @DisplayName("2. TenantFilter HTTP Header Parsing")
    class TenantFilterTests {

        private TenantFilter filter;

        @BeforeEach
        void setUp() {
            filter = new TenantFilter();
        }

        @Test
        @DisplayName("Populates context from X-Organization-Id and X-Community-Id headers")
        void extractsHierarchicalHeaders() throws ServletException, IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(TenantFilter.ORG_HEADER, "200");
            request.addHeader(TenantFilter.COMMUNITY_HEADER, "350");
            MockHttpServletResponse response = new MockHttpServletResponse();

            AtomicReference<Long> capturedOrgId = new AtomicReference<>();
            AtomicReference<Long> capturedCommunityId = new AtomicReference<>();

            FilterChain chain = (req, res) -> {
                capturedOrgId.set(TenantContext.getOrganizationId());
                capturedCommunityId.set(TenantContext.getCommunityId());
            };

            filter.doFilter(request, response, chain);

            assertThat(capturedOrgId.get()).isEqualTo(200L);
            assertThat(capturedCommunityId.get()).isEqualTo(350L);
            // Verify context cleaned up after filter execution
            assertThat(TenantContext.getCommunityId()).isNull();
        }

        @Test
        @DisplayName("Falls back to legacy X-Tenant-ID header when X-Community-Id is absent")
        void fallsBackToLegacyHeader() throws ServletException, IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(TenantFilter.LEGACY_TENANT_HEADER, "community_654");
            MockHttpServletResponse response = new MockHttpServletResponse();

            AtomicReference<Long> capturedCommunityId = new AtomicReference<>();

            FilterChain chain = (req, res) -> {
                capturedCommunityId.set(TenantContext.getCommunityId());
            };

            filter.doFilter(request, response, chain);

            assertThat(capturedCommunityId.get()).isEqualTo(654L);
            assertThat(TenantContext.getCommunityId()).isNull();
        }
    }

    @Nested
    @DisplayName("3. TenantBaseEntity Automatic Context Population")
    class TenantBaseEntityTests {

        static class SampleEntity extends TenantBaseEntity {
            private String name;
        }

        @Test
        @DisplayName("@PrePersist populates organizationId and communityId from TenantContext")
        void prePersistPopulatesTenantContext() {
            TenantContext.setTenant(55L, 77L);

            SampleEntity entity = new SampleEntity();
            assertThat(entity.getOrganizationId()).isNull();
            assertThat(entity.getCommunityId()).isNull();

            entity.populateTenantContext();

            assertThat(entity.getOrganizationId()).isEqualTo(55L);
            assertThat(entity.getCommunityId()).isEqualTo(77L);
        }

        @Test
        @DisplayName("@PrePersist preserves explicitly assigned IDs")
        void prePersistPreservesExplicitIds() {
            TenantContext.setTenant(55L, 77L);

            SampleEntity entity = new SampleEntity();
            entity.setOrganizationId(999L);
            entity.setCommunityId(888L);

            entity.populateTenantContext();

            assertThat(entity.getOrganizationId()).isEqualTo(999L);
            assertThat(entity.getCommunityId()).isEqualTo(888L);
        }
    }

    @Nested
    @DisplayName("4. Organization & Community Domain Models")
    class DomainModelTests {

        @Test
        @DisplayName("Organization domain model supports builder, defaults and community association")
        void organizationModelStructure() {
            Organization org = Organization.builder()
                    .name("Prestige Group")
                    .code("PRESTIGE")
                    .businessRegistrationNo("CIN-123456")
                    .contactEmail("contact@prestige.com")
                    .tier("ENTERPRISE")
                    .active(true)
                    .build();

            assertThat(org.getName()).isEqualTo("Prestige Group");
            assertThat(org.getCode()).isEqualTo("PRESTIGE");
            assertThat(org.getTier()).isEqualTo("ENTERPRISE");
            assertThat(org.getActive()).isTrue();
            assertThat(org.getCommunities()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Community and AppUser link to organizationId")
        void communityAndUserOrganizationLink() {
            Community community = Community.builder()
                    .name("Prestige Falcon City")
                    .organizationId(10L)
                    .active(true)
                    .build();

            assertThat(community.getOrganizationId()).isEqualTo(10L);

            AppUser user = AppUser.builder()
                    .email("resident@prestige.com")
                    .organizationId(10L)
                    .build();

            assertThat(user.getOrganizationId()).isEqualTo(10L);
        }
    }

    @Nested
    @DisplayName("5. Organization REST Controller")
    class OrganizationControllerTests {

        @Mock
        private OrganizationRepository orgRepo;

        @Mock
        private CommunityRepository communityRepo;

        @InjectMocks
        private OrganizationController controller;

        @Test
        @DisplayName("POST /api/v1/organizations creates a new organization")
        void createOrganizationSuccess() {
            OrganizationController.CreateOrganizationRequest req = new OrganizationController.CreateOrganizationRequest();
            req.setName("Godrej Properties");
            req.setCode("GODREJ");
            req.setTier("ENTERPRISE");

            when(orgRepo.existsByCodeIgnoreCase("GODREJ")).thenReturn(false);
            when(orgRepo.save(any(Organization.class))).thenAnswer(inv -> {
                Organization o = inv.getArgument(0);
                o.setId(12L);
                return o;
            });

            ResponseEntity<?> response = controller.createOrganization(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            Organization created = (Organization) response.getBody();
            assertThat(created).isNotNull();
            assertThat(created.getId()).isEqualTo(12L);
            assertThat(created.getCode()).isEqualTo("GODREJ");
        }

        @Test
        @DisplayName("POST /api/v1/organizations rejects duplicate organization code")
        void createOrganizationDuplicateCode() {
            OrganizationController.CreateOrganizationRequest req = new OrganizationController.CreateOrganizationRequest();
            req.setName("Godrej Properties");
            req.setCode("GODREJ");

            when(orgRepo.existsByCodeIgnoreCase("GODREJ")).thenReturn(true);

            ResponseEntity<?> response = controller.createOrganization(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            verify(orgRepo, never()).save(any());
        }

        @Test
        @DisplayName("GET /api/v1/organizations/{id}/communities returns communities for organization")
        void getOrganizationCommunities() {
            Community c1 = Community.builder().name("Godrej Woods").organizationId(12L).build();
            Community c2 = Community.builder().name("Godrej Platinum").organizationId(12L).build();

            when(orgRepo.existsById(12L)).thenReturn(true);
            when(communityRepo.findByOrganizationId(12L)).thenReturn(List.of(c1, c2));

            ResponseEntity<List<Community>> response = controller.getOrganizationCommunities(12L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
            assertThat(response.getBody()).extracting("name").containsExactly("Godrej Woods", "Godrej Platinum");
        }
    }
}
