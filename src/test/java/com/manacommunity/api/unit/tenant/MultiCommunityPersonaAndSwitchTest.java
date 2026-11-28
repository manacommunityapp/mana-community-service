package com.manacommunity.api.unit.tenant;

import com.manacommunity.api.config.tenant.TenantContext;
import com.manacommunity.api.config.tenant.TenantFilter;
import com.manacommunity.api.marketplace.entity.MarketSellerProfile;
import com.manacommunity.api.marketplace.repository.MarketSellerRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.CommunityModule;
import com.manacommunity.api.platform.tenant.TenantContextHolder;
import com.manacommunity.api.platform.tenant.TenantSecurityFilter;
import com.manacommunity.api.repository.CommunityModuleRepository;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.repository.RolePermissionRepository;
import com.manacommunity.api.resident.model.CommunityFlat;
import com.manacommunity.api.resident.model.FlatMembership;
import com.manacommunity.api.resident.repository.FlatMembershipRepository;
import com.manacommunity.api.security.CommunityMembershipService;
import com.manacommunity.api.security.CookieAuthHelper;
import com.manacommunity.api.security.JwtAuthenticationFilter;
import com.manacommunity.api.security.JwtTokenProvider;
import com.manacommunity.api.security.SecurityErrorWriter;
import com.manacommunity.api.security.TokenBlacklistService;
import com.manacommunity.api.service.RolePermissionService;
import com.manacommunity.api.serviceplatform.entity.ServiceProvider;
import com.manacommunity.api.serviceplatform.entity.enums.VerificationStatus;
import com.manacommunity.api.serviceplatform.repository.ServiceProviderRepository;
import com.manacommunity.api.user.controller.MultiCommunityAuthController;
import com.manacommunity.api.user.dto.AuthResponse;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Multi-Community Persona Isolation & Switching (Owner A / Tenant B / Vendor C)")
class MultiCommunityPersonaAndSwitchTest {

    @Mock private AppUserRepository userRepository;
    @Mock private FlatMembershipRepository flatMembershipRepository;
    @Mock private CommunityRepository communityRepository;
    @Mock private ServiceProviderRepository serviceProviderRepository;
    @Mock private MarketSellerRepository marketSellerRepository;
    @Mock private SecurityErrorWriter securityErrorWriter;
    @Mock private RolePermissionRepository rolePermissionRepository;
    @Mock private CommunityModuleRepository communityModuleRepository;
    @Mock private Environment environment;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private RolePermissionService rolePermissionService;
    @Mock private CookieAuthHelper cookieAuthHelper;

    private CommunityMembershipService membershipService;
    private JwtTokenProvider jwtTokenProvider;
    private TenantFilter tenantFilter;
    private TenantSecurityFilter tenantSecurityFilter;
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    private MultiCommunityAuthController authController;

    private Community commA;
    private Community commB;
    private Community commC;
    private AppUser multiUser;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();

        membershipService = new CommunityMembershipService(userRepository, flatMembershipRepository, communityRepository);
        membershipService.setServiceProviderRepository(serviceProviderRepository);
        membershipService.setMarketSellerRepository(marketSellerRepository);

        jwtTokenProvider = new JwtTokenProvider(
                "test-secret-key-at-least-32-characters-long-123456",
                900000L, 604800000L, "mana-community", "mana-api", true);

        tenantFilter = new TenantFilter(membershipService, securityErrorWriter, userRepository);
        tenantSecurityFilter = new TenantSecurityFilter();
        tenantSecurityFilter.setMembershipService(membershipService);
        tenantSecurityFilter.setUserRepository(userRepository);

        jwtAuthenticationFilter = new JwtAuthenticationFilter(
                jwtTokenProvider, userRepository, rolePermissionRepository,
                communityModuleRepository, environment, tokenBlacklistService, rolePermissionService);
        jwtAuthenticationFilter.setCommunityMembershipService(membershipService);

        authController = new MultiCommunityAuthController(
                userRepository, membershipService, jwtTokenProvider,
                rolePermissionService, communityModuleRepository, cookieAuthHelper);

        commA = Community.builder().id(101L).name("Palm Meadows (A)").organizationId(1L).build();
        commB = Community.builder().id(202L).name("Cyber Heights (B)").organizationId(2L).build();
        commC = Community.builder().id(303L).name("Lakeview Towers (C)").organizationId(3L).build();

        multiUser = AppUser.builder()
                .id(42L)
                .fullName("Karthik Rao")
                .email("karthik@example.com")
                .passwordHash("encoded")
                .role("USER")
                .community(commA)
                .isActive(true)
                .organizationId(1L)
                .build();

        FlatMembership ownerA = FlatMembership.builder()
                .id(1L).user(multiUser)
                .flat(CommunityFlat.builder().id(1001L).community(commA).flatNumber("A-101").build())
                .residentType("OWNER").isPrimaryResident(true).appAccessStatus("FULL_ACCESS")
                .build();

        FlatMembership tenantB = FlatMembership.builder()
                .id(2L).user(multiUser)
                .flat(CommunityFlat.builder().id(2002L).community(commB).flatNumber("B-404").build())
                .residentType("TENANT").isPrimaryResident(false).appAccessStatus("FULL_ACCESS")
                .build();

        ServiceProvider vendorC = ServiceProvider.builder()
                .id(3L).user(multiUser).community(commC).businessName("Karthik Electricals")
                .verificationStatus(VerificationStatus.VERIFIED).build();

        MarketSellerProfile sellerC = MarketSellerProfile.builder()
                .id(4L).user(multiUser).community(commC).businessName("Karthik Kitchen")
                .isActive(true).build();

        lenient().when(userRepository.findById(42L)).thenReturn(Optional.of(multiUser));
        lenient().when(flatMembershipRepository.findByUserId(42L)).thenReturn(List.of(ownerA, tenantB));
        lenient().when(serviceProviderRepository.findAllByUserId(42L)).thenReturn(List.of(vendorC));
        lenient().when(marketSellerRepository.findByUserIdAndIsActiveTrue(42L)).thenReturn(List.of(sellerC));
        lenient().when(communityRepository.findById(101L)).thenReturn(Optional.of(commA));
        lenient().when(communityRepository.findById(202L)).thenReturn(Optional.of(commB));
        lenient().when(communityRepository.findById(303L)).thenReturn(Optional.of(commC));
        lenient().when(rolePermissionService.getEffectivePermissionsForUser(any())).thenReturn(List.of());
        lenient().when(communityModuleRepository.findEnabledByCommunityId(any())).thenReturn(List.of(
                CommunityModule.builder().moduleKey("GOVERNANCE").build()
        ));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Resolves community-scoped personas without leaking Owner/Tenant/Vendor roles across A, B, and C")
    void resolvesPerCommunityPersonasWithoutCrossCommunityBleed() {
        assertThat(membershipService.getAuthorizedCommunityIds(multiUser))
                .containsExactlyInAnyOrder(101L, 202L, 303L);

        assertThat(membershipService.isUserAuthorizedForCommunity(multiUser, 999L)).isFalse();

        assertThat(membershipService.resolveRolesForCommunity(multiUser, 101L))
                .containsExactlyInAnyOrder("USER", "OWNER", "RESIDENT");
        assertThat(membershipService.resolveRolesForCommunity(multiUser, 202L))
                .containsExactlyInAnyOrder("TENANT", "RESIDENT")
                .doesNotContain("OWNER", "VENDOR");
        assertThat(membershipService.resolveRolesForCommunity(multiUser, 303L))
                .containsExactlyInAnyOrder("SERVICE_PROVIDER", "MARKETPLACE_SELLER", "VENDOR")
                .doesNotContain("OWNER", "TENANT");

        var contexts = membershipService.getCommunityContexts(multiUser);
        assertThat(contexts).hasSize(3);
    }

    @Test
    @DisplayName("TenantFilter & TenantSecurityFilter allow switching A <-> B <-> C and reject unauthorized Community D")
    void filtersPermitAuthorizedSwitchingAndBlockUnauthorizedCommunity() throws Exception {
        UserPrincipal principal = new UserPrincipal(
                42L, "karthik@example.com", "encoded",
                List.of(new SimpleGrantedAuthority("ROLE_USER")), multiUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        // Switch from home Community A (101) to Community C (303 - Vendor)
        MockHttpServletRequest reqC = new MockHttpServletRequest("GET", "/api/service-platform/orders");
        reqC.addHeader("X-Community-ID", "303");
        MockHttpServletResponse resC = new MockHttpServletResponse();

        AtomicReference<Long> capturedTenantFilterCid = new AtomicReference<>();
        AtomicReference<Long> capturedSecurityFilterCid = new AtomicReference<>();

        FilterChain chainC = (req, res) -> {
            capturedTenantFilterCid.set(TenantContext.getCommunityId());
            tenantSecurityFilter.doFilter(req, res, (r2, s2) -> capturedSecurityFilterCid.set(TenantContextHolder.get()));
        };

        tenantFilter.doFilter(reqC, resC, chainC);
        assertThat(resC.getStatus()).isEqualTo(200);
        assertThat(capturedTenantFilterCid.get()).isEqualTo(303L);
        assertThat(capturedSecurityFilterCid.get()).isEqualTo(303L);

        // Unauthorized Community D (999) is rejected with 403
        MockHttpServletRequest reqD = new MockHttpServletRequest("GET", "/api/visitor/passes");
        reqD.addHeader("X-Community-ID", "999");
        MockHttpServletResponse resD = new MockHttpServletResponse();
        tenantSecurityFilter.doFilter(reqD, resD, (req, res) -> {});
        assertThat(resD.getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("Switch-community mints community-scoped JWT & JwtAuthenticationFilter scopes authorities per request")
    void switchCommunityMintsScopedTokenAndJwtFilterScopesAuthorities() throws Exception {
        UserPrincipal initialPrincipal = new UserPrincipal(
                42L, "karthik@example.com", "encoded",
                List.of(new SimpleGrantedAuthority("ROLE_OWNER")), multiUser, 101L);

        AuthResponse switchedToB = authController.switchCommunity(
                initialPrincipal,
                new MultiCommunityAuthController.SwitchCommunityRequest(202L),
                new MockHttpServletResponse()
        ).getBody();

        assertThat(switchedToB).isNotNull();
        assertThat(switchedToB.getCommunityId()).isEqualTo(202L);
        assertThat(switchedToB.getRoles()).containsExactlyInAnyOrder("TENANT", "RESIDENT");
        assertThat(jwtTokenProvider.getCommunityId(switchedToB.getToken())).isEqualTo(202L);

        // Authenticate subsequent request with switched JWT -> SecurityContext has ROLE_TENANT (no ROLE_OWNER!)
        MockHttpServletRequest reqB = new MockHttpServletRequest("GET", "/api/visitor/passes");
        reqB.addHeader("Authorization", "Bearer " + switchedToB.getToken());
        MockHttpServletResponse resB = new MockHttpServletResponse();

        AtomicReference<List<String>> capturedAuthorities = new AtomicReference<>();
        AtomicReference<Long> capturedPrincipalCid = new AtomicReference<>();
        jwtAuthenticationFilter.doFilter(reqB, resB, (req, res) -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            capturedAuthorities.set(auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
            capturedPrincipalCid.set(((UserPrincipal) auth.getPrincipal()).getCommunityId());
        });

        assertThat(capturedPrincipalCid.get()).isEqualTo(202L);
        assertThat(capturedAuthorities.get()).contains("ROLE_TENANT", "ROLE_RESIDENT")
                .doesNotContain("ROLE_OWNER", "ROLE_VENDOR");

        // Attempting to switch into unauthorized Community D throws AccessDeniedException
        assertThatThrownBy(() -> authController.switchCommunity(
                initialPrincipal,
                new MultiCommunityAuthController.SwitchCommunityRequest(999L),
                new MockHttpServletResponse()
        )).isInstanceOf(AccessDeniedException.class);
    }
}
