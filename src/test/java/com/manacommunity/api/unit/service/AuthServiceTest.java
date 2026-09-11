package com.manacommunity.api.unit.service;

import com.manacommunity.api.user.dto.LoginRequest;
import com.manacommunity.api.user.dto.RegisterRequest;
import com.manacommunity.api.exception.DuplicateResourceException;
import com.manacommunity.api.exception.InvalidInviteCodeException;
import com.manacommunity.api.exception.ManaCommunityException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.Role;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.repository.RoleRepository;
import com.manacommunity.api.user.dto.AuthResponse;
import com.manacommunity.api.user.service.impl.AuthServiceImpl;
import com.manacommunity.api.support.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock AppUserRepository userRepository;
    @Mock CommunityRepository communityRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock com.manacommunity.api.security.JwtTokenProvider jwtTokenProvider;
    @Mock com.manacommunity.api.security.AuditLogService auditLog;
    @Mock com.manacommunity.api.security.AuditService auditService;
    @Mock com.manacommunity.api.user.security.SessionService sessionService;
    @Mock com.manacommunity.api.service.FieldEncryptionService fieldEncryptionService;
    @Mock com.manacommunity.api.service.CommunityModuleService communityModuleService;
    @Mock com.manacommunity.api.security.TokenBlacklistService tokenBlacklistService;
    @Mock com.manacommunity.api.service.OtpService otpService;
    @Mock com.manacommunity.api.service.CommunityBlockConfigService blockConfigService;

    @InjectMocks AuthServiceImpl authService;

    // ── register ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("registerUser")
    class Register {

        @Test
        @DisplayName("happy path returns AuthResponse with correct email")
        void happyPath() {
            RegisterRequest req = TestDataBuilder.registerRequest();
            Community community = TestDataBuilder.community(1L, "INVITE123");
            Role role = TestDataBuilder.memberRole();

            when(communityRepository.findByInviteCode("INVITE123")).thenReturn(Optional.of(community));
            when(userRepository.existsByEmailIgnoreCase(req.getEmail())).thenReturn(false);
            when(userRepository.existsByPhone(req.getPhone())).thenReturn(false);
            when(blockConfigService.validateBlockAndFlat(anyLong(), any(), any())).thenReturn(true);
            when(passwordEncoder.encode(req.getPassword())).thenReturn("hashed");
            when(roleRepository.findByNameIgnoreCaseAndCommunityId(anyString(), anyLong())).thenReturn(Optional.of(role));
            when(jwtTokenProvider.generateToken(any())).thenReturn("mock-token-123");
            when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("mock-refresh-123");
            when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> {
                AppUser u = inv.getArgument(0);
                u.setId(99L);
                return u;
            });

            AuthResponse response = authService.registerUser(req);

            assertThat(response).isNotNull();
            assertThat(response.getEmail()).isEqualTo(req.getEmail());
            assertThat(response.getToken()).startsWith("mock-token-");
            assertThat(response.getOccupancyStatus()).isEqualTo("Owner");
            verify(userRepository).save(any(AppUser.class));
        }

        @Test
        @DisplayName("tenant userType registration sets Tenant occupancyStatus")
        void tenantRegistration() {
            RegisterRequest req = TestDataBuilder.registerRequest();
            req.setUserType("Tenant");
            Community community = TestDataBuilder.community(1L, "INVITE123");
            Role role = TestDataBuilder.memberRole();

            when(communityRepository.findByInviteCode("INVITE123")).thenReturn(Optional.of(community));
            when(userRepository.existsByEmailIgnoreCase(req.getEmail())).thenReturn(false);
            when(userRepository.existsByPhone(req.getPhone())).thenReturn(false);
            when(blockConfigService.validateBlockAndFlat(anyLong(), any(), any())).thenReturn(true);
            when(passwordEncoder.encode(req.getPassword())).thenReturn("hashed");
            when(roleRepository.findByNameIgnoreCaseAndCommunityId(anyString(), anyLong())).thenReturn(Optional.of(role));
            when(jwtTokenProvider.generateToken(any())).thenReturn("mock-token-tenant");
            when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("mock-refresh-tenant");
            when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> {
                AppUser u = inv.getArgument(0);
                u.setId(100L);
                return u;
            });

            AuthResponse response = authService.registerUser(req);

            assertThat(response).isNotNull();
            assertThat(response.getOccupancyStatus()).isEqualTo("Tenant");
            assertThat(response.getUserType()).isEqualTo("Tenant");
            verify(userRepository).save(argThat(u -> "Tenant".equals(u.getOccupancyStatus())));
        }

        @Test
        @DisplayName("email not verified throws ManaCommunityException")
        void emailNotVerified() {
            RegisterRequest req = TestDataBuilder.registerRequest();
            doThrow(new ManaCommunityException("Please verify your email before registering.",
                    org.springframework.http.HttpStatus.BAD_REQUEST, "EMAIL_NOT_VERIFIED"))
                    .when(otpService).assertEmailVerified(req.getEmail().trim().toLowerCase());

            assertThatThrownBy(() -> authService.registerUser(req))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessageContaining("verify your email");
        }

        @Test
        @DisplayName("invalid invite code throws InvalidInviteCodeException")
        void invalidInviteCode() {
            RegisterRequest req = TestDataBuilder.registerRequest();
            when(communityRepository.findByInviteCode(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.registerUser(req))
                    .isInstanceOf(InvalidInviteCodeException.class);
        }

        @Test
        @DisplayName("duplicate email throws DuplicateResourceException")
        void duplicateEmail() {
            RegisterRequest req = TestDataBuilder.registerRequest();
            when(communityRepository.findByInviteCode(anyString()))
                    .thenReturn(Optional.of(TestDataBuilder.community()));
            when(userRepository.existsByEmailIgnoreCase(req.getEmail())).thenReturn(true);

            assertThatThrownBy(() -> authService.registerUser(req))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("duplicate phone throws DuplicateResourceException")
        void duplicatePhone() {
            RegisterRequest req = TestDataBuilder.registerRequest();
            when(communityRepository.findByInviteCode(anyString()))
                    .thenReturn(Optional.of(TestDataBuilder.community()));
            when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
            when(userRepository.existsByPhone(req.getPhone())).thenReturn(true);

            assertThatThrownBy(() -> authService.registerUser(req))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("Aadhaar number is masked in saved user")
        void aadhaarMasked() {
            RegisterRequest req = TestDataBuilder.registerRequest();
            req.setAadharNumber("987654321099");

            when(communityRepository.findByInviteCode(anyString()))
                    .thenReturn(Optional.of(TestDataBuilder.community()));
            when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
            when(userRepository.existsByPhone(anyString())).thenReturn(false);
            when(blockConfigService.validateBlockAndFlat(anyLong(), any(), any())).thenReturn(true);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(roleRepository.findByNameIgnoreCaseAndCommunityId(anyString(), anyLong()))
                    .thenReturn(Optional.of(TestDataBuilder.memberRole()));
            when(jwtTokenProvider.generateToken(any())).thenReturn("mock-token-123");
            when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("mock-refresh-123");
            when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> {
                AppUser u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });

            authService.registerUser(req);

            verify(userRepository).save(argThat(u ->
                    u.getGovtIdNumber() != null && u.getGovtIdNumber().endsWith("1099")
            ));
        }
    }

    // ── login ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("loginUser")
    class Login {

        @Test
        @DisplayName("correct credentials return AuthResponse")
        void correctCredentials() {
            AppUser user = TestDataBuilder.adminUser();
            user.setPasswordHash("hashed");

            LoginRequest req = TestDataBuilder.loginRequest(user.getEmail(), "password123");

            when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
            when(jwtTokenProvider.generateToken(any())).thenReturn("mock-token-123");
            when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("mock-refresh-123");

            AuthResponse response = authService.loginUser(req);

            assertThat(response.getEmail()).isEqualTo(user.getEmail());
            assertThat(response.getRole()).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("wrong password throws ManaCommunityException with 401")
        void wrongPassword() {
            AppUser user = TestDataBuilder.adminUser();
            user.setPasswordHash("hashed");

            LoginRequest req = TestDataBuilder.loginRequest(user.getEmail(), "wrong");

            when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

            assertThatThrownBy(() -> authService.loginUser(req))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessage("Incorrect password for this email address.");
        }

        @Test
        @DisplayName("unknown email throws ManaCommunityException with 401")
        void unknownEmail() {
            when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.loginUser(TestDataBuilder.loginRequest("x@x.com", "p")))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessage("Email address not found. Please check and try again.");
        }

        @Test
        @DisplayName("unknown mobile throws ManaCommunityException with 401")
        void unknownMobile() {
            when(userRepository.findByPhone("9876543210")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.loginUser(TestDataBuilder.loginRequest("9876543210", "p")))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessage("Mobile number not found. Please check and try again.");
        }
    }
}
