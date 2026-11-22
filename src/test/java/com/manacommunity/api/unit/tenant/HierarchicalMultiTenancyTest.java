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
import com.manacommunity.api.user.security.UserPrincipal;
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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.Arrays;
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

    /**
     * These tests pin the fix for the tenant-header vulnerability.
     *
     * <p>The filter used to populate {@link TenantContext} straight from the client-supplied
     * {@code X-Community-Id} / {@code X-Tenant-ID} headers, so any caller could read and write
     * another community's data by changing a header. The tests here previously asserted that
     * behaviour, i.e. they asserted the bug.
     *
     * <p>Current contract: the community comes from the authenticated principal; the headers
     * are honoured only for {@code ROLE_SUPER_ADMIN}; for anyone else a header that disagrees
     * with the principal is discarded (and logged at WARN); an unauthenticated request leaves
     * the community null.
     */
    @Nested
    @DisplayName("2. TenantFilter derives the community from the principal, never from a header")
    class TenantFilterTests {

        private TenantFilter filter;

        @BeforeEach
        void setUp() {
            filter = new TenantFilter();
            SecurityContextHolder.clearContext();
        }

        @AfterEach
        void tearDown() {
            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("REGRESSION: a resident of community A sending X-Community-Id: B resolves to A")
        void headerCannotOverridePrincipalCommunity() throws ServletException, IOException {
            authenticateAs(7L, 100L, "ROLE_USER");

            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/finance/invoices");
            request.addHeader(TenantFilter.COMMUNITY_HEADER, "200");

            // Before the fix this captured 200 — the caller read community 200's data.
            assertThat(runFilter(request)).isEqualTo(100L);
            assertThat(TenantContext.getCommunityId()).isNull();
        }

        @Test
        @DisplayName("REGRESSION: the legacy X-Tenant-ID header cannot override the principal either")
        void legacyHeaderCannotOverridePrincipalCommunity() throws ServletException, IOException {
            authenticateAs(7L, 100L, "ROLE_USER");

            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/finance/invoices");
            request.addHeader(TenantFilter.LEGACY_TENANT_HEADER, "community_654");

            assertThat(runFilter(request)).isEqualTo(100L);
        }

        @Test
        @DisplayName("Community comes from the principal when no header is sent at all")
        void populatesCommunityFromPrincipal() throws ServletException, IOException {
            authenticateAs(7L, 350L, "ROLE_USER");

            assertThat(runFilter(new MockHttpServletRequest())).isEqualTo(350L);
        }

        @Test
        @DisplayName("A header that agrees with the principal changes nothing")
        void matchingHeaderIsHarmless() throws ServletException, IOException {
            authenticateAs(7L, 350L, "ROLE_USER");

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(TenantFilter.COMMUNITY_HEADER, "350");

            assertThat(runFilter(request)).isEqualTo(350L);
        }

        @Test
        @DisplayName("A principal with no community stays null however the header is set")
        void principalWithoutCommunityStaysNull() throws ServletException, IOException {
            authenticateAs(7L, null, "ROLE_USER");

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(TenantFilter.COMMUNITY_HEADER, "9");

            assertThat(runFilter(request)).isNull();
        }

        @Test
        @DisplayName("SUPER_ADMIN may target another community with X-Community-Id")
        void superAdminMayUseCommunityHeader() throws ServletException, IOException {
            authenticateAs(1L, 100L, "ROLE_SUPER_ADMIN");

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(TenantFilter.COMMUNITY_HEADER, "200");

            assertThat(runFilter(request)).isEqualTo(200L);
        }

        @Test
        @DisplayName("SUPER_ADMIN may target another community with the legacy X-Tenant-ID header")
        void superAdminMayUseLegacyHeader() throws ServletException, IOException {
            authenticateAs(1L, 100L, "ROLE_SUPER_ADMIN");

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(TenantFilter.LEGACY_TENANT_HEADER, "community_654");

            assertThat(runFilter(request)).isEqualTo(654L);
        }

        @Test
        @DisplayName("SUPER_ADMIN without a header keeps their own community")
        void superAdminWithoutHeaderKeepsOwnCommunity() throws ServletException, IOException {
            authenticateAs(1L, 100L, "ROLE_SUPER_ADMIN");

            assertThat(runFilter(new MockHttpServletRequest())).isEqualTo(100L);
        }

        @Test
        @DisplayName("An unauthenticated request leaves the community null, header or not")
        void unauthenticatedRequestHasNoCommunity() throws ServletException, IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(TenantFilter.COMMUNITY_HEADER, "350");
            request.addHeader(TenantFilter.LEGACY_TENANT_HEADER, "community_654");

            assertThat(runFilter(request)).isNull();
        }

        @Test
        @DisplayName("An anonymous authentication is treated as unauthenticated")
        void anonymousAuthenticationHasNoCommunity() throws ServletException, IOException {
            SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                    "key", "anonymousUser",
                    List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(TenantFilter.COMMUNITY_HEADER, "350");

            assertThat(runFilter(request)).isNull();
        }

        @Test
        @DisplayName("X-Organization-Id is still read from the header and defaults when absent")
        void organizationHeaderStillHonoured() throws ServletException, IOException {
            // organizationId is not carried on the principal and is not used for data
            // scoping (see TenantFilter's note); it must not be used for authorization.
            MockHttpServletRequest withHeader = new MockHttpServletRequest();
            withHeader.addHeader(TenantFilter.ORG_HEADER, "200");

            AtomicReference<Long> orgId = new AtomicReference<>();
            filter.doFilter(withHeader, new MockHttpServletResponse(),
                    (req, res) -> orgId.set(TenantContext.getOrganizationId()));
            assertThat(orgId.get()).isEqualTo(200L);

            filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
                    (req, res) -> orgId.set(TenantContext.getOrganizationId()));
            assertThat(orgId.get()).isEqualTo(TenantContext.DEFAULT_ORG_ID);
        }

        /** Runs the filter and returns the community id seen downstream of it. */
        private Long runFilter(MockHttpServletRequest request) throws ServletException, IOException {
            AtomicReference<Long> captured = new AtomicReference<>();
            FilterChain chain = (req, res) -> captured.set(TenantContext.getCommunityId());
            filter.doFilter(request, new MockHttpServletResponse(), chain);
            // The filter must always clean up after itself.
            assertThat(TenantContext.getCommunityId()).isNull();
            return captured.get();
        }

        /** Puts a real {@link UserPrincipal} in the SecurityContext, as JwtAuthenticationFilter does. */
        private void authenticateAs(Long userId, Long communityId, String... authorities) {
            Community community = communityId == null
                    ? null
                    : Community.builder().id(communityId).name("Community " + communityId).build();
            AppUser user = AppUser.builder().id(userId).email("u" + userId + "@example.com")
                    .community(community).build();
            List<SimpleGrantedAuthority> granted = Arrays.stream(authorities)
                    .map(SimpleGrantedAuthority::new)
                    .toList();
            UserPrincipal principal = new UserPrincipal(userId, user.getEmail(), "n/a", granted, user);
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, "n/a", granted));
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
