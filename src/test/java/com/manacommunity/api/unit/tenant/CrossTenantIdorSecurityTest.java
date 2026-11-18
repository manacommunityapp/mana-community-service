package com.manacommunity.api.unit.tenant;

import com.manacommunity.api.config.tenant.TenantContext;
import com.manacommunity.api.config.tenant.TenantOwnershipValidator;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.resident.model.CommunityFlat;
import com.manacommunity.api.resident.model.FlatMembership;
import com.manacommunity.api.resident.repository.FlatMembershipRepository;
import com.manacommunity.api.security.CommunityMembershipService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Platform-Wide Cross-Tenant IDOR Security Test Suite")
class CrossTenantIdorSecurityTest {

    @Nested
    @DisplayName("1. TenantOwnershipValidator IDOR Guard")
    class OwnershipValidatorTests {

        private final TenantOwnershipValidator validator = new TenantOwnershipValidator();

        @BeforeEach
        @AfterEach
        void clean() {
            TenantContext.clear();
        }

        @Test
        @DisplayName("Resource in same community passes validation")
        void resourceInSameCommunityPasses() {
            TenantContext.setTenant(10L, 100L);

            // Should pass without throwing
            validator.validateCommunityOwnership(100L);
            assertThat(validator.matchesCurrentCommunity(100L)).isTrue();
        }

        @Test
        @DisplayName("Resource in different community throws AccessDeniedException (IDOR protection)")
        void resourceInDifferentCommunityThrowsAccessDenied() {
            TenantContext.setTenant(10L, 100L);

            assertThatThrownBy(() -> validator.validateCommunityOwnership(200L))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Cross-tenant access rejected")
                    .hasMessageContaining("resource community (200) does not match active tenant (100)");

            assertThat(validator.matchesCurrentCommunity(200L)).isFalse();
        }

        @Test
        @DisplayName("Resource in different organization throws AccessDeniedException")
        void resourceInDifferentOrganizationThrowsAccessDenied() {
            TenantContext.setTenant(10L, 100L);

            assertThatThrownBy(() -> validator.validateOrganizationOwnership(99L))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Cross-organization access rejected");
        }
    }

    @Nested
    @DisplayName("2. CommunityMembershipService Multi-Tenancy Authorization")
    class MembershipServiceTests {

        @Mock
        private AppUserRepository userRepository;

        @Mock
        private FlatMembershipRepository flatMembershipRepository;

        @Mock
        private CommunityRepository communityRepository;

        @InjectMocks
        private CommunityMembershipService membershipService;

        @Test
        @DisplayName("User with direct community and multiple flat memberships is authorized only for those communities")
        void multiCommunityResidentAuthorization() {
            Community c1 = Community.builder().name("Palm Grove").build();
            c1.setId(101L);
            Community c2 = Community.builder().name("Oak Crest").build();
            c2.setId(102L);

            AppUser user = AppUser.builder()
                    .id(50L)
                    .email("resident@example.com")
                    .role("USER")
                    .community(c1) // Primary community: 101
                    .build();

            CommunityFlat flat = CommunityFlat.builder().community(c2).flatNumber("A-402").build();
            flat.setId(301L);

            FlatMembership fm = FlatMembership.builder()
                    .user(user)
                    .flat(flat)
                    .appAccessStatus("FULL_ACCESS")
                    .build();

            when(flatMembershipRepository.findByUserId(50L)).thenReturn(List.of(fm));

            Set<Long> authorized = membershipService.getAuthorizedCommunityIds(user);

            assertThat(authorized).containsExactlyInAnyOrder(101L, 102L);
            assertThat(membershipService.isUserAuthorizedForCommunity(user, 101L)).isTrue();
            assertThat(membershipService.isUserAuthorizedForCommunity(user, 102L)).isTrue();
            // Unauthorized community 103
            assertThat(membershipService.isUserAuthorizedForCommunity(user, 103L)).isFalse();
        }

        @Test
        @DisplayName("Revoked flat membership does NOT grant community access")
        void revokedFlatMembershipDoesNotGrantAccess() {
            Community c2 = Community.builder().name("Oak Crest").build();
            c2.setId(102L);

            AppUser user = AppUser.builder()
                    .id(50L)
                    .email("former_resident@example.com")
                    .role("USER")
                    .community(null) // No direct community
                    .build();

            CommunityFlat flat = CommunityFlat.builder().community(c2).flatNumber("B-101").build();
            flat.setId(302L);

            FlatMembership fm = FlatMembership.builder()
                    .user(user)
                    .flat(flat)
                    .appAccessStatus("REVOKED")
                    .build();

            when(flatMembershipRepository.findByUserId(50L)).thenReturn(List.of(fm));

            assertThat(membershipService.isUserAuthorizedForCommunity(user, 102L)).isFalse();
        }

        @Test
        @DisplayName("Property authorization verifies user belongs to that flat")
        void propertyAuthorizationCheck() {
            AppUser user = AppUser.builder().id(50L).role("USER").build();
            FlatMembership fm = FlatMembership.builder().appAccessStatus("FULL_ACCESS").build();

            when(flatMembershipRepository.findByUserIdAndFlatId(50L, 201L)).thenReturn(Optional.of(fm));
            when(flatMembershipRepository.findByUserIdAndFlatId(50L, 999L)).thenReturn(Optional.empty());

            assertThat(membershipService.isUserAuthorizedForProperty(user, 201L)).isTrue();
            assertThat(membershipService.isUserAuthorizedForProperty(user, 999L)).isFalse();
        }

        @Test
        @DisplayName("Super admin is authorized for any community and property")
        void superAdminAuthorizedEverywhere() {
            AppUser admin = AppUser.builder().id(1L).role("SUPER_ADMIN").build();

            assertThat(membershipService.isUserAuthorizedForCommunity(admin, 999L)).isTrue();
            assertThat(membershipService.isUserAuthorizedForProperty(admin, 888L)).isTrue();
        }
    }
}
