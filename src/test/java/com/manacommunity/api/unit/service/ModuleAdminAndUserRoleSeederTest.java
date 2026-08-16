package com.manacommunity.api.unit.service;

import com.manacommunity.api.constants.PermissionConstants;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.Role;
import com.manacommunity.api.repository.RoleRepository;
import com.manacommunity.api.service.sample.data.CommunitySeeder;
import com.manacommunity.api.service.sample.data.UserSeeder;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ModuleAdminAndUserRoleSeederTest")
class ModuleAdminAndUserRoleSeederTest {

    @Mock AppUserRepository userRepo;
    @Mock RoleRepository roleRepo;
    @Mock PasswordEncoder passwordEncoder;
    @Mock CommunitySeeder communitySeeder;

    @InjectMocks UserSeeder userSeeder;

    @Test
    @DisplayName("Verify all 15 module-specific admin roles exist in PermissionConstants")
    void testAllModuleAdminRoleConstantsExist() {
        assertThat(PermissionConstants.ROLE_COMMUNITY_FEED_ADMIN).isEqualTo("COMMUNITY_FEED_ADMIN");
        assertThat(PermissionConstants.ROLE_SPORTS_ADMIN).isEqualTo("SPORTS_ADMIN");
        assertThat(PermissionConstants.ROLE_MARKETPLACE_ADMIN).isEqualTo("MARKETPLACE_ADMIN");
        assertThat(PermissionConstants.ROLE_VISITORS_ADMIN).isEqualTo("VISITORS_ADMIN");
        assertThat(PermissionConstants.ROLE_NOTICES_ADMIN).isEqualTo("NOTICES_ADMIN");
        assertThat(PermissionConstants.ROLE_BOOKINGS_ADMIN).isEqualTo("BOOKINGS_ADMIN");
        assertThat(PermissionConstants.ROLE_HELPDESK_ADMIN).isEqualTo("HELPDESK_ADMIN");
        assertThat(PermissionConstants.ROLE_POLLS_ADMIN).isEqualTo("POLLS_ADMIN");
        assertThat(PermissionConstants.ROLE_JOBS_ADMIN).isEqualTo("JOBS_ADMIN");
        assertThat(PermissionConstants.ROLE_EVENTS_ADMIN).isEqualTo("EVENTS_ADMIN");
        assertThat(PermissionConstants.ROLE_COMMUNITY_MGMT_ADMIN).isEqualTo("COMMUNITY_MGMT_ADMIN");
        assertThat(PermissionConstants.ROLE_FINANCE_MGMT_ADMIN).isEqualTo("FINANCE_MGMT_ADMIN");
        assertThat(PermissionConstants.ROLE_ADMIN_HUB_ADMIN).isEqualTo("ADMIN_HUB_ADMIN");
        assertThat(PermissionConstants.ROLE_FOOD_OS_ADMIN).isEqualTo("FOOD_OS_ADMIN");
        assertThat(PermissionConstants.ROLE_VENDOR_MANAGEMENT_ADMIN).isEqualTo("VENDOR_MANAGEMENT_ADMIN");
    }

    @Test
    @DisplayName("UserSeeder automatically assigns USER role entity into userRoles set")
    void testUserSeederAutomaticallyIncludesUserRole() {
        Community mockCommunity = Community.builder().id(10L).name("Test Community").build();

        Role userRoleEntity = Role.builder().id(1L).name("USER").communityId(10L).build();
        Role sportsAdminRoleEntity = Role.builder().id(2L).name("SPORTS_ADMIN").communityId(10L).build();

        when(userRepo.findByEmail("chethan@gmail.com")).thenReturn(Optional.empty());
        when(userRepo.existsByPhone(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed123");
        when(roleRepo.findByNameIgnoreCaseAndCommunityId("USER", 10L)).thenReturn(Optional.of(userRoleEntity));
        when(roleRepo.findByNameIgnoreCaseAndCommunityId("SPORTS_ADMIN", 10L)).thenReturn(Optional.of(sportsAdminRoleEntity));
        when(userRepo.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        AppUser createdUser = userSeeder.getOrCreateUser("chethan@gmail.com", "Chethan", "SPORTS_ADMIN", mockCommunity);

        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getUserRoles()).extracting(Role::getName).containsExactlyInAnyOrder("USER", "SPORTS_ADMIN");
        assertThat(createdUser.getRole()).contains("USER");
        assertThat(createdUser.getRole()).contains("SPORTS_ADMIN");

        verify(userRepo).save(argThat(u ->
                u.getUserRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("USER"))
        ));
    }
}
