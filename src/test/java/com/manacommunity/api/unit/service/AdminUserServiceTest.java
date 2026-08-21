package com.manacommunity.api.unit.service;

import com.manacommunity.api.exception.DuplicateResourceException;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.InvalidInviteCodeException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.Role;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.repository.RolePermissionRepository;
import com.manacommunity.api.security.AuditLogService;
import com.manacommunity.api.service.RoleService;
import com.manacommunity.api.user.dto.AdminCreateUserRequest;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.user.service.impl.AdminUserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUserServiceImpl - Bulk & Admin User Creation Test Suite")
class AdminUserServiceTest {

    @Mock private AppUserRepository userRepository;
    @Mock private CommunityRepository communityRepository;
    @Mock private RoleService roleService;
    @Mock private RolePermissionRepository rolePermissionRepo;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditLogService auditLog;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    private Community testCommunity;

    @BeforeEach
    void setUp() {
        testCommunity = Community.builder()
                .id(1L)
                .name("Green Valley Society")
                .inviteCode("APT-TOWER-A-2024")
                .build();
    }

    @Test
    @DisplayName("createUser via inviteCode: sets default DOB, gender, default password 'Pass1234', active status and pre-verified KYC")
    void createUser_withInviteCode_success() {
        AdminCreateUserRequest req = new AdminCreateUserRequest();
        req.setFirstName("Priya");
        req.setLastName("Sharma");
        req.setEmail("priya.sharma@example.com");
        req.setPhone("+91 98765 43210");
        req.setRole("member");
        req.setInviteCode("APT-TOWER-A-2024");
        req.setFlatNo("Apt 402");
        req.setGovtIdType("Aadhaar Card");
        req.setGovtIdNumber("XXXX-XXXX-1234");
        // dateOfBirth & gender omitted (bulk upload scenario)

        when(communityRepository.findByInviteCode("APT-TOWER-A-2024")).thenReturn(Optional.of(testCommunity));
        when(userRepository.existsByEmail("priya.sharma@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("+91 98765 43210")).thenReturn(false);
        when(passwordEncoder.encode("Pass1234")).thenReturn("$2a$10$hashedPass1234");
        when(roleService.findOrCreateRole(anyString(), anyLong())).thenAnswer(inv ->
                Role.builder().id(10L).name(inv.getArgument(0)).communityId(inv.getArgument(1)).permissions(new HashSet<>()).build());
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> {
            AppUser u = inv.getArgument(0);
            u.setId(101L);
            return u;
        });

        AppUser saved = adminUserService.createUser(req);

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(101L);
        assertThat(saved.getFullName()).isEqualTo("Priya Sharma");
        assertThat(saved.getEmail()).isEqualTo("priya.sharma@example.com");
        assertThat(saved.getPhone()).isEqualTo("+91 98765 43210");
        assertThat(saved.getPasswordHash()).isEqualTo("$2a$10$hashedPass1234");
        assertThat(saved.getDateOfBirth()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(saved.getGender()).isEqualTo("OTHER");
        assertThat(saved.getGovtIdType()).isEqualTo("Aadhaar Card");
        assertThat(saved.getGovtIdNumber()).isEqualTo("XXXX-XXXX-1234");
        assertThat(saved.getFlatNo()).isEqualTo("Apt 402");
        assertThat(saved.getKycStatus()).isEqualTo("VERIFIED");
        assertThat(saved.getIsActive()).isTrue();
        assertThat(saved.getCommunity()).isEqualTo(testCommunity);

        verify(auditLog).record(AuditLogService.Action.REGISTER, 101L, "priya.sharma@example.com");
    }

    @Test
    @DisplayName("createUser via communityId: persists explicit DOB, gender, custom role and permissions")
    void createUser_withCommunityId_success() {
        AdminCreateUserRequest req = new AdminCreateUserRequest();
        req.setFirstName("Rahul");
        req.setLastName("Verma");
        req.setEmail("rahul.verma@example.com");
        req.setPhone("+91 98765 12345");
        req.setRole("vendor");
        req.setCommunityId(1L);
        req.setDateOfBirth(LocalDate.of(1992, 5, 15));
        req.setGender("Male");

        when(communityRepository.findById(1L)).thenReturn(Optional.of(testCommunity));
        when(userRepository.existsByEmail("rahul.verma@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("+91 98765 12345")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedDefault");
        when(roleService.findOrCreateRole(anyString(), anyLong())).thenAnswer(inv ->
                Role.builder().id(11L).name(inv.getArgument(0)).communityId(inv.getArgument(1)).permissions(new HashSet<>()).build());
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> {
            AppUser u = inv.getArgument(0);
            u.setId(102L);
            return u;
        });

        AppUser saved = adminUserService.createUser(req);

        assertThat(saved).isNotNull();
        assertThat(saved.getFullName()).isEqualTo("Rahul Verma");
        assertThat(saved.getDateOfBirth()).isEqualTo(LocalDate.of(1992, 5, 15));
        assertThat(saved.getGender()).isEqualTo("MALE");
        assertThat(saved.getRole()).contains("VENDOR");
    }

    @Test
    @DisplayName("createUser throws DuplicateResourceException when email already exists")
    void createUser_duplicateEmail_throwsException() {
        AdminCreateUserRequest req = new AdminCreateUserRequest();
        req.setFirstName("Priya");
        req.setLastName("Sharma");
        req.setEmail("priya.sharma@example.com");
        req.setPhone("+91 98765 43210");
        req.setInviteCode("APT-TOWER-A-2024");

        when(communityRepository.findByInviteCode("APT-TOWER-A-2024")).thenReturn(Optional.of(testCommunity));
        when(userRepository.existsByEmail("priya.sharma@example.com")).thenReturn(true);

        assertThatThrownBy(() -> adminUserService.createUser(req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("priya.sharma@example.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("createUser throws DuplicateResourceException when phone number already exists")
    void createUser_duplicatePhone_throwsException() {
        AdminCreateUserRequest req = new AdminCreateUserRequest();
        req.setFirstName("Priya");
        req.setLastName("Sharma");
        req.setEmail("priya.sharma@example.com");
        req.setPhone("+91 98765 43210");
        req.setInviteCode("APT-TOWER-A-2024");

        when(communityRepository.findByInviteCode("APT-TOWER-A-2024")).thenReturn(Optional.of(testCommunity));
        when(userRepository.existsByEmail("priya.sharma@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("+91 98765 43210")).thenReturn(true);

        assertThatThrownBy(() -> adminUserService.createUser(req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("+91 98765 43210");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("createUser throws InvalidInviteCodeException when invite code is not found")
    void createUser_invalidInviteCode_throwsException() {
        AdminCreateUserRequest req = new AdminCreateUserRequest();
        req.setFirstName("Priya");
        req.setLastName("Sharma");
        req.setEmail("priya.sharma@example.com");
        req.setPhone("+91 98765 43210");
        req.setInviteCode("UNKNOWN-CODE");

        when(communityRepository.findByInviteCode("UNKNOWN-CODE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.createUser(req))
                .isInstanceOf(InvalidInviteCodeException.class)
                .hasMessageContaining("UNKNOWN-CODE");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("createUser throws InvalidInputException when neither communityId nor inviteCode is provided")
    void createUser_missingCommunityIdentifiers_throwsException() {
        AdminCreateUserRequest req = new AdminCreateUserRequest();
        req.setFirstName("Priya");
        req.setLastName("Sharma");
        req.setEmail("priya.sharma@example.com");
        req.setPhone("+91 98765 43210");

        assertThatThrownBy(() -> adminUserService.createUser(req))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("A community is required");

        verify(userRepository, never()).save(any());
    }
}
