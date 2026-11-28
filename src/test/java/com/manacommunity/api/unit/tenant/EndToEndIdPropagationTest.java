package com.manacommunity.api.unit.tenant;

import com.manacommunity.api.config.tenant.TenantAwareTaskDecorator;
import com.manacommunity.api.config.tenant.TenantContext;
import com.manacommunity.api.config.tenant.TenantFilter;
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
import com.manacommunity.api.security.CorrelationIdFilter;
import com.manacommunity.api.security.JwtAuthenticationFilter;
import com.manacommunity.api.security.JwtTokenProvider;
import com.manacommunity.api.security.SecurityErrorWriter;
import com.manacommunity.api.security.TokenBlacklistService;
import com.manacommunity.api.service.RolePermissionService;
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
import org.slf4j.MDC;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("End-to-End Community, Tower, Flat, and User ID Propagation")
class EndToEndIdPropagationTest {

    @Mock private AppUserRepository userRepository;
    @Mock private FlatMembershipRepository flatMembershipRepository;
    @Mock private CommunityRepository communityRepository;
    @Mock private SecurityErrorWriter securityErrorWriter;
    @Mock private RolePermissionRepository rolePermissionRepository;
    @Mock private CommunityModuleRepository communityModuleRepository;
    @Mock private Environment environment;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private RolePermissionService rolePermissionService;
    @Mock private CookieAuthHelper cookieAuthHelper;

    private CommunityMembershipService membershipService;
    private JwtTokenProvider jwtTokenProvider;
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    private TenantFilter tenantFilter;
    private TenantSecurityFilter tenantSecurityFilter;
    private MultiCommunityAuthController authController;

    private Community commA;
    private Community commB;
    private AppUser resident;
    private CommunityFlat flatA;
    private CommunityFlat flatB;
    private Long expectedTowerIdA;
    private Long expectedTowerIdB;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
        MDC.clear();

        membershipService = new CommunityMembershipService(userRepository, flatMembershipRepository, communityRepository);

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
                userRepository, communityRepository, membershipService, jwtTokenProvider,
                rolePermissionService, communityModuleRepository, cookieAuthHelper);

        commA = Community.builder().id(101L).name("Palm Meadows").organizationId(10L).build();
        commB = Community.builder().id(202L).name("Cyber Heights").organizationId(20L).build();

        resident = AppUser.builder()
                .id(42L)
                .fullName("Ananya Sharma")
                .email("ananya@example.com")
                .passwordHash("encoded")
                .role("USER")
                .community(commA)
                .tower("Tower A")
                .flatNo("A-804")
                .isActive(true)
                .organizationId(10L)
                .build();

        flatA = CommunityFlat.builder().id(1001L).community(commA).towerBlock("Tower A").flatNumber("A-804").build();
        flatB = CommunityFlat.builder().id(2002L).community(commB).towerBlock("Tower C").flatNumber("C-302").build();
        expectedTowerIdA = CommunityMembershipService.computeTowerId(101L, "Tower A");
        expectedTowerIdB = CommunityMembershipService.computeTowerId(202L, "Tower C");

        FlatMembership fmA = FlatMembership.builder()
                .id(1L).user(resident).flat(flatA).residentType("OWNER")
                .isPrimaryResident(true).appAccessStatus("FULL_ACCESS").build();
        FlatMembership fmB = FlatMembership.builder()
                .id(2L).user(resident).flat(flatB).residentType("TENANT")
                .isPrimaryResident(false).appAccessStatus("FULL_ACCESS").build();

        lenient().when(userRepository.findById(42L)).thenReturn(Optional.of(resident));
        lenient().when(flatMembershipRepository.findByUserId(42L)).thenReturn(List.of(fmA, fmB));
        lenient().when(flatMembershipRepository.findByUserIdAndFlatId(42L, 1001L)).thenReturn(Optional.of(fmA));
        lenient().when(flatMembershipRepository.findByUserIdAndFlatId(42L, 2002L)).thenReturn(Optional.of(fmB));
        lenient().when(communityRepository.findById(101L)).thenReturn(Optional.of(commA));
        lenient().when(communityRepository.findById(202L)).thenReturn(Optional.of(commB));
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
        MDC.clear();
    }

    @Test
    @DisplayName("computeTowerId is canonical and isolated per community")
    void computeTowerIdIsCanonicalPerCommunity() {
        assertThat(expectedTowerIdA).isNotNull().isPositive();
        assertThat(CommunityMembershipService.computeTowerId(101L, "A")).isEqualTo(expectedTowerIdA);
        assertThat(CommunityMembershipService.computeTowerId(101L, "tower a")).isEqualTo(expectedTowerIdA);
        assertThat(CommunityMembershipService.computeTowerId(202L, "A")).isNotEqualTo(expectedTowerIdA);
    }

    @Test
    @DisplayName("JWT switch -> JwtAuthenticationFilter -> TenantFilter -> TenantSecurityFilter -> Async propagates all 4 IDs and cleans up")
    void propagatesCommunityTowerFlatAndUserIdsEndToEnd() throws Exception {
        UserPrincipal initialPrincipal = new UserPrincipal(
                42L, "ananya@example.com", "encoded",
                List.of(new SimpleGrantedAuthority("ROLE_OWNER")), resident, 101L, expectedTowerIdA, 1001L);

        AuthResponse switched = authController.switchCommunity(
                initialPrincipal,
                new MultiCommunityAuthController.SwitchCommunityRequest(202L, 2002L),
                new MockHttpServletResponse()
        ).getBody();

        assertThat(switched).isNotNull();
        assertThat(switched.getCommunityId()).isEqualTo(202L);
        assertThat(switched.getTowerId()).isEqualTo(expectedTowerIdB);
        assertThat(switched.getFlatId()).isEqualTo(2002L);
        assertThat(switched.getTower()).isEqualTo("Tower C");
        assertThat(switched.getFlatNo()).isEqualTo("C-302");
        assertThat(jwtTokenProvider.getUserId(switched.getToken())).isEqualTo(42L);
        assertThat(jwtTokenProvider.getCommunityId(switched.getToken())).isEqualTo(202L);
        assertThat(jwtTokenProvider.getTowerId(switched.getToken())).isEqualTo(expectedTowerIdB);
        assertThat(jwtTokenProvider.getFlatId(switched.getToken())).isEqualTo(2002L);

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/clubhouse/pos/account");
        req.addHeader("Authorization", "Bearer " + switched.getToken());
        req.addHeader("X-Community-ID", "202");
        req.addHeader("X-Tower-Id", String.valueOf(expectedTowerIdB));
        req.addHeader("X-Flat-Id", "2002");
        MockHttpServletResponse res = new MockHttpServletResponse();

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setTaskDecorator(new TenantAwareTaskDecorator());
        executor.initialize();

        AtomicReference<UserPrincipal> capturedPrincipal = new AtomicReference<>();
        AtomicReference<TenantContext> capturedContext = new AtomicReference<>();
        AtomicReference<Long> capturedHolderCid = new AtomicReference<>();
        AtomicReference<Long> capturedHolderTid = new AtomicReference<>();
        AtomicReference<Long> capturedHolderFid = new AtomicReference<>();
        AtomicReference<Long> capturedHolderUid = new AtomicReference<>();
        AtomicReference<String> capturedMdcTower = new AtomicReference<>();
        AtomicReference<String> capturedMdcFlat = new AtomicReference<>();
        AtomicReference<TenantContext> asyncContext = new AtomicReference<>();

        try {
            FilterChain downstreamChain = (request, response) -> {
                System.out.println("DEBUG before tenantFilter: auth=" + SecurityContextHolder.getContext().getAuthentication()
                        + " ctx=" + TenantContext.getRawContext()
                        + " tenantFilterAttr=" + request.getAttribute("com.manacommunity.api.config.tenant.TenantFilter.FILTERED"));
                tenantFilter.doFilter(request, response, (req2, res2) -> {
                    System.out.println("DEBUG inside req2 (after tenantFilter): ctx=" + TenantContext.getRawContext());
                    tenantSecurityFilter.doFilter(req2, res2, (req3, res3) -> {
                        System.out.println("DEBUG inside req3 (after tenantSecurityFilter): ctx=" + TenantContext.getRawContext());
                        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                        capturedPrincipal.set((UserPrincipal) auth.getPrincipal());
                        TenantContext rawNow = TenantContext.getRawContext();
                        TenantContext copiedNow = rawNow.copy();
                        System.out.println("DEBUG line229 id=" + System.identityHashCode(copiedNow) + " val=" + copiedNow);
                        capturedContext.set(copiedNow);
                        capturedHolderCid.set(TenantContextHolder.get());
                        capturedHolderTid.set(TenantContextHolder.getTowerId());
                        capturedHolderFid.set(TenantContextHolder.getFlatId());
                        capturedHolderUid.set(TenantContextHolder.getUserId());
                        capturedMdcTower.set(MDC.get(CorrelationIdFilter.MDC_TOWER_ID));
                        capturedMdcFlat.set(MDC.get(CorrelationIdFilter.MDC_FLAT_ID));

                        CountDownLatch latch = new CountDownLatch(1);
                        executor.execute(() -> {
                            asyncContext.set(TenantContext.getRawContext().copy());
                            latch.countDown();
                        });
                        try {
                            latch.await(5, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                });
            };

            jwtAuthenticationFilter.doFilter(req, res, downstreamChain);
            System.out.println("DEBUG line256 id=" + System.identityHashCode(capturedContext.get()) + " val=" + capturedContext.get() + " async=" + asyncContext.get());

            assertThat(res.getStatus()).isEqualTo(200);
            assertThat(capturedPrincipal.get()).isNotNull();
            assertThat(capturedPrincipal.get().getCommunityId()).isEqualTo(202L);
            assertThat(capturedPrincipal.get().getTowerId()).isEqualTo(expectedTowerIdB);
            assertThat(capturedPrincipal.get().getFlatId()).isEqualTo(2002L);

            assertThat(capturedContext.get()).as("capturedContext was null").isNotNull();
            assertThat(capturedContext.get().getCommunityId())
                    .as("capturedContext=%s", capturedContext.get())
                    .isEqualTo(202L);
            assertThat(capturedContext.get().getTowerId()).isEqualTo(expectedTowerIdB);
            assertThat(capturedContext.get().getTowerBlock()).isEqualTo("Tower C");
            assertThat(capturedContext.get().getFlatId()).isEqualTo(2002L);
            assertThat(capturedContext.get().getPropertyId()).isEqualTo(2002L);
            assertThat(capturedContext.get().getUserId()).isEqualTo(42L);

            assertThat(capturedHolderCid.get()).isEqualTo(202L);
            assertThat(capturedHolderTid.get()).isEqualTo(expectedTowerIdB);
            assertThat(capturedHolderFid.get()).isEqualTo(2002L);
            assertThat(capturedHolderUid.get()).isEqualTo(42L);

            assertThat(capturedMdcTower.get()).isEqualTo(String.valueOf(expectedTowerIdB));
            assertThat(capturedMdcFlat.get()).isEqualTo("2002");

            assertThat(asyncContext.get().getCommunityId()).isEqualTo(202L);
            assertThat(asyncContext.get().getTowerId()).isEqualTo(expectedTowerIdB);
            assertThat(asyncContext.get().getFlatId()).isEqualTo(2002L);
            assertThat(asyncContext.get().getUserId()).isEqualTo(42L);

            // Guaranteed cleanup on request exit
            assertThat(TenantContext.getRawContext()).isNull();
            assertThat(TenantContextHolder.get()).isNull();
            assertThat(TenantContextHolder.getTowerId()).isNull();
            assertThat(TenantContextHolder.getFlatId()).isNull();
            assertThat(MDC.get(CorrelationIdFilter.MDC_COMMUNITY_ID)).isNull();
            assertThat(MDC.get(CorrelationIdFilter.MDC_TOWER_ID)).isNull();
            assertThat(MDC.get(CorrelationIdFilter.MDC_FLAT_ID)).isNull();
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("TenantFilter rejects spoofed X-Tower-Id header with 403 TOWER_ACCESS_DENIED")
    void rejectsUnauthorizedTowerHeader() throws Exception {
        UserPrincipal principal = new UserPrincipal(
                42L, "ananya@example.com", "encoded",
                List.of(new SimpleGrantedAuthority("ROLE_USER")), resident, 101L, expectedTowerIdA, 1001L);
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities()));

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/visitor/passes");
        req.addHeader(TenantFilter.COMMUNITY_HEADER, "101");
        req.addHeader(TenantFilter.TOWER_HEADER, "999999");
        MockHttpServletResponse res = new MockHttpServletResponse();

        tenantFilter.doFilter(req, res, (r, s) -> {});

        verify(securityErrorWriter).write(
                org.mockito.ArgumentMatchers.eq(req),
                org.mockito.ArgumentMatchers.eq(res),
                org.mockito.ArgumentMatchers.eq(org.springframework.http.HttpStatus.FORBIDDEN),
                org.mockito.ArgumentMatchers.eq("TOWER_ACCESS_DENIED"),
                org.mockito.ArgumentMatchers.contains("999999"));
        assertThat(TenantContext.getRawContext()).isNull();
    }
}
