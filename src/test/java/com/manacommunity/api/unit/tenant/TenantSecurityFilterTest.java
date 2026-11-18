package com.manacommunity.api.unit.tenant;

import com.manacommunity.api.config.tenant.TenantContext;
import com.manacommunity.api.config.tenant.TenantFilter;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.security.CommunityMembershipService;
import com.manacommunity.api.security.SecurityErrorWriter;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantSecurityFilter Authoritative Membership Resolution Test")
class TenantSecurityFilterTest {

    @Mock
    private CommunityMembershipService membershipService;

    @Mock
    private SecurityErrorWriter securityErrorWriter;

    @Mock
    private AppUserRepository userRepository;

    private TenantFilter filter;

    @BeforeEach
    void setUp() {
        filter = new TenantFilter(membershipService, securityErrorWriter, userRepository);
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private void authenticateUser(AppUser user) {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getEmail(), "password",
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())),
                user
        );
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Authorized resident requesting own community via header succeeds and sets TenantContext")
    void authorizedResidentRequestingOwnCommunitySucceeds() throws ServletException, IOException {
        Community comm = Community.builder().name("Palm Grove").organizationId(10L).build();
        comm.setId(101L);

        AppUser user = AppUser.builder()
                .id(1L)
                .email("resident@example.com")
                .role("USER")
                .community(comm)
                .organizationId(10L)
                .build();
        authenticateUser(user);

        when(membershipService.isUserAuthorizedForCommunity(eq(user), eq(101L))).thenReturn(true);
        when(membershipService.resolveOrganizationId(eq(101L), eq(user))).thenReturn(10L);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TenantFilter.COMMUNITY_HEADER, "101");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<Long> capturedCommunityId = new AtomicReference<>();
        AtomicReference<Long> capturedOrgId = new AtomicReference<>();
        AtomicReference<Long> capturedUserId = new AtomicReference<>();

        FilterChain chain = (req, res) -> {
            capturedCommunityId.set(TenantContext.getCommunityId());
            capturedOrgId.set(TenantContext.getOrganizationId());
            capturedUserId.set(TenantContext.getContext().getUserId());
        };

        filter.doFilter(request, response, chain);

        assertThat(capturedCommunityId.get()).isEqualTo(101L);
        assertThat(capturedOrgId.get()).isEqualTo(10L);
        assertThat(capturedUserId.get()).isEqualTo(1L);
        // Guaranteed cleanup after request
        assertThat(TenantContext.getCommunityId()).isNull();
    }

    @Test
    @DisplayName("Resident spoofing unauthorized community header is rejected with 403 Forbidden")
    void residentSpoofingUnauthorizedCommunityHeaderIsRejected() throws ServletException, IOException {
        Community comm = Community.builder().name("Palm Grove").organizationId(10L).build();
        comm.setId(101L);

        AppUser user = AppUser.builder()
                .id(1L)
                .email("resident@example.com")
                .role("USER")
                .community(comm)
                .organizationId(10L)
                .build();
        authenticateUser(user);

        // Community 999 is NOT authorized for this user
        when(membershipService.isUserAuthorizedForCommunity(eq(user), eq(999L))).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TenantFilter.COMMUNITY_HEADER, "999");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        // Filter chain must NOT proceed
        verify(chain, never()).doFilter(any(), any());
        verify(securityErrorWriter).write(eq(request), eq(response), eq(org.springframework.http.HttpStatus.FORBIDDEN),
                eq("TENANT_ACCESS_DENIED"), contains("999"));
        assertThat(TenantContext.getCommunityId()).isNull();
    }

    @Test
    @DisplayName("Authenticated user omitting community header automatically resolves to assigned community")
    void authenticatedUserOmittingHeaderResolvesAssignedCommunity() throws ServletException, IOException {
        Community comm = Community.builder().name("Palm Grove").organizationId(10L).build();
        comm.setId(101L);

        AppUser user = AppUser.builder()
                .id(1L)
                .email("resident@example.com")
                .role("USER")
                .community(comm)
                .organizationId(10L)
                .build();
        authenticateUser(user);

        when(membershipService.resolveDefaultCommunityId(eq(user))).thenReturn(101L);
        when(membershipService.resolveOrganizationId(eq(101L), eq(user))).thenReturn(10L);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<Long> capturedCommunityId = new AtomicReference<>();

        FilterChain chain = (req, res) -> {
            capturedCommunityId.set(TenantContext.getCommunityId());
        };

        filter.doFilter(request, response, chain);

        assertThat(capturedCommunityId.get()).isEqualTo(101L);
        assertThat(TenantContext.getCommunityId()).isNull();
    }

    @Test
    @DisplayName("Super admin can access any community via header")
    void superAdminCanAccessAnyCommunity() throws ServletException, IOException {
        AppUser admin = AppUser.builder()
                .id(99L)
                .email("superadmin@mana.com")
                .role("SUPER_ADMIN")
                .build();
        authenticateUser(admin);

        when(membershipService.isUserAuthorizedForCommunity(eq(admin), eq(555L))).thenReturn(true);
        when(membershipService.resolveOrganizationId(eq(555L), eq(admin))).thenReturn(20L);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TenantFilter.COMMUNITY_HEADER, "555");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<Long> capturedCommunityId = new AtomicReference<>();

        FilterChain chain = (req, res) -> {
            capturedCommunityId.set(TenantContext.getCommunityId());
        };

        filter.doFilter(request, response, chain);

        assertThat(capturedCommunityId.get()).isEqualTo(555L);
        assertThat(TenantContext.getCommunityId()).isNull();
    }

    @Test
    @DisplayName("User requesting unauthorized property/flat is rejected with 403 Forbidden")
    void userRequestingUnauthorizedPropertyIsRejected() throws ServletException, IOException {
        Community comm = Community.builder().name("Palm Grove").organizationId(10L).build();
        comm.setId(101L);

        AppUser user = AppUser.builder()
                .id(1L)
                .email("resident@example.com")
                .role("USER")
                .community(comm)
                .build();
        authenticateUser(user);

        when(membershipService.isUserAuthorizedForCommunity(eq(user), eq(101L))).thenReturn(true);
        when(membershipService.isUserAuthorizedForProperty(eq(user), eq(404L))).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TenantFilter.COMMUNITY_HEADER, "101");
        request.addHeader(TenantFilter.PROPERTY_HEADER, "404");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        verify(securityErrorWriter).write(eq(request), eq(response), eq(org.springframework.http.HttpStatus.FORBIDDEN),
                eq("PROPERTY_ACCESS_DENIED"), contains("404"));
        assertThat(TenantContext.getCommunityId()).isNull();
    }
}
