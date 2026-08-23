package com.manacommunity.api.user.service.impl;

import com.manacommunity.api.security.AuditAction;

import com.manacommunity.api.security.AuditModule;

import com.manacommunity.api.security.AuditService;

import com.manacommunity.api.user.security.SessionService;

import static com.manacommunity.api.constants.PermissionConstants.*;
import com.manacommunity.api.user.dto.AuthResponse;
import com.manacommunity.api.user.dto.KycRequest;
import com.manacommunity.api.user.dto.LoginRequest;
import com.manacommunity.api.user.dto.RegisterRequest;
import com.manacommunity.api.user.dto.ResetPasswordRequest;
import com.manacommunity.api.exception.*;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.Role;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.repository.RoleRepository;
import com.manacommunity.api.security.AuditLogService;
import com.manacommunity.api.security.JwtTokenProvider;
import com.manacommunity.api.security.PasswordPolicy;
import com.manacommunity.api.security.TokenBlacklistService;
import com.manacommunity.api.user.service.AuthService;
import com.manacommunity.api.service.CommunityModuleService;
import com.manacommunity.api.service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    // ── Brute-force lockout policy ────────────────────────────────────────
    private static final int MAX_ATTEMPTS_USER = 5;
    private static final int MAX_ATTEMPTS_ADMIN = 3;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private static final Set<String> ADMIN_ROLES = Set.of(ROLE_ADMIN, ROLE_SUPER_ADMIN, ROLE_COMMUNITY_ADMIN);

    @Autowired
    private AppUserRepository userRepository;
    @Autowired
    private CommunityRepository communityRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private AuditLogService auditLog;
    @Autowired
    private com.manacommunity.api.security.AuditService auditService;
    @Autowired
    private com.manacommunity.api.user.security.SessionService sessionService;
    @Autowired
    private com.manacommunity.api.service.FieldEncryptionService fieldEncryptionService;
    @Autowired
    private CommunityModuleService communityModuleService;
    @Autowired
    private TokenBlacklistService tokenBlacklistService;
    @Autowired
    private OtpService otpService;

    @Override
    @Transactional
    public AuthResponse registerUser(RegisterRequest request) {

        // 1. Verify Community Invite Code
        Community community = communityRepository.findByInviteCode(request.getInviteCode())
                .orElseThrow(() -> new InvalidInviteCodeException(request.getInviteCode()));

        // 2. Duplicate email / phone check (both are UNIQUE in DB)
        String email = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email))
            throw new DuplicateResourceException("User", "email", email);

        if (userRepository.existsByPhone(request.getPhone()))
            throw new DuplicateResourceException("User", "phone", request.getPhone());

        // 2c. Check if block & flat number combination is already registered in this community
        if (community != null && request.getBlock() != null && !request.getBlock().trim().isEmpty()
                && request.getFlatNo() != null && !request.getFlatNo().trim().isEmpty()) {
            boolean unitExists = userRepository.existsByCommunityIdAndBlockIgnoreCaseAndFlatNoIgnoreCase(
                    community.getId(), request.getBlock().trim(), request.getFlatNo().trim()
            );
            if (unitExists) {
                throw new ManaCommunityException(
                        "Unit Block " + request.getBlock().trim().toUpperCase() + " Flat " + request.getFlatNo().trim() + " is already registered in this community.",
                        HttpStatus.CONFLICT, "DUPLICATE_UNIT");
            }
        }

        // 2b. Enforce password strength before hashing — also reject passwords
        // derived from the user's own data (email, name, phone, community).
        // Arrays.asList permits nulls; the evaluator skips null/blank tokens.
        PasswordPolicy.validate(request.getPassword(), java.util.Arrays.asList(
                email,
                request.getFullName(),
                request.getPhone(),
                community.getName()
        ));

        // 3. Mock Aadhaar masking
        String maskedAadhar = maskAadharNumber(request.getAadharNumber());

        // 4. Build and save AppUser
        AppUser user = new AppUser();
        user.setFullName(request.getFullName());
        user.setEmail(email);
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setDateOfBirth(request.getDateOfBirth());
        user.setGender(request.getGender());
        user.setGovtIdNumber(maskedAadhar);
        user.setGovtIdType("AADHAAR");
        user.setKycStatus("VERIFIED");
        user.setRole(ROLE_USER); // All new self-registrations start as USER
        Role userRole;
        if (community != null) {
            userRole = roleRepository.findByNameIgnoreCaseAndCommunityId(ROLE_USER, community.getId())
                    .orElseGet(() -> roleRepository.save(Role.builder()
                            .name(ROLE_USER)
                            .communityId(community.getId())
                            .build()));
        } else {
            userRole = roleRepository.findByNameIgnoreCaseAndCommunityIdIsNull(ROLE_USER)
                    .orElseGet(() -> roleRepository.save(Role.builder()
                            .name(ROLE_USER)
                            .build()));
        }
        user.setRoleEntity(userRole);
        if (user.getUserRoles() == null) {
            user.setUserRoles(new java.util.LinkedHashSet<>());
        }
        user.getUserRoles().add(userRole);
        user.syncRoleString();
        user.setIsActive(true);
        user.setCommunity(community);
        user.setFlatNo(request.getFlatNo());
        user.setBlock(request.getBlock());

        AppUser saved = userRepository.save(user);
        auditLog.record(AuditLogService.Action.REGISTER, saved.getId(), saved.getEmail());
        auditService.record(
                com.manacommunity.api.security.AuditAction.USER_CREATED,
                com.manacommunity.api.security.AuditModule.USER_MANAGEMENT,
                "AppUser", String.valueOf(saved.getId()),
                null,
                "role=MEMBER, community=" + (community != null ? community.getId() : "none"));
        return buildAuthResponse(saved, "Registration & KYC successful!");
    }

    @Override
    @Transactional
    public AuthResponse loginUser(LoginRequest request) {
        String identifier = request.getIdentifier() == null ? "" : request.getIdentifier().trim();
        boolean isMobile = identifier.matches("\\d{10}");

        AppUser user;
        if (isMobile) {
            user = userRepository.findByPhone(identifier).orElseThrow(() -> {
                auditLog.record(AuditLogService.Action.LOGIN_FAILED, identifier);
                return new ManaCommunityException(
                        "Mobile number not found. Please check and try again.",
                        HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
            });
        } else {
            user = userRepository.findByEmailIgnoreCase(identifier).orElseThrow(() -> {
                auditLog.record(AuditLogService.Action.LOGIN_FAILED, identifier);
                return new ManaCommunityException(
                        "Email address not found. Please check and try again.",
                        HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
            });
        }

        // 1. Refuse if the account is currently locked.
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            long minutes = Math.max(1, Duration.between(LocalDateTime.now(), user.getLockedUntil()).toMinutes() + 1);
            auditLog.record(AuditLogService.Action.LOGIN_LOCKED, user.getId(), user.getEmail());
            throw new ManaCommunityException(
                    "Account locked due to too many failed attempts. Try again in " + minutes + " minute(s).",
                    HttpStatus.LOCKED, "ACCOUNT_LOCKED");
        }

        // 2. Wrong password → count the failure and possibly lock.
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            registerFailedAttempt(user);
            String wrongPwMsg = isMobile
                    ? "Incorrect password for this mobile number."
                    : "Incorrect password for this email address.";
            throw new ManaCommunityException(wrongPwMsg, HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
        }

        // 3. Success → clear any prior failures and issue tokens.
        if (user.getFailedLoginAttempts() != 0 || user.getLockedUntil() != null) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }
        auditLog.record(AuditLogService.Action.LOGIN_SUCCESS, user.getId(), user.getEmail());
        sessionService.startSession(user.getId(), user.getEmail());
        return buildAuthResponse(user, "Login successful!");
    }

    /** Increments the failure counter and locks the account once the role's threshold is hit. */
    private void registerFailedAttempt(AppUser user) {
        int attempts = (user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts()) + 1;
        user.setFailedLoginAttempts(attempts);
        int max = isAdmin(user) ? MAX_ATTEMPTS_ADMIN : MAX_ATTEMPTS_USER;
        if (attempts >= max) {
            user.setLockedUntil(LocalDateTime.now().plus(LOCK_DURATION));
            user.setFailedLoginAttempts(0); // reset the counter for the next window
            userRepository.save(user);
            auditLog.record(AuditLogService.Action.ACCOUNT_LOCKED, user.getId(), user.getEmail());
        } else {
            userRepository.save(user);
            auditLog.record(AuditLogService.Action.LOGIN_FAILED, user.getId(), user.getEmail());
        }
    }

    private boolean isAdmin(AppUser user) {
        return user.getRole() != null && ADMIN_ROLES.contains(user.getRole().toUpperCase());
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()
                || !jwtTokenProvider.validateToken(refreshToken)
                || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            auditLog.record(AuditLogService.Action.TOKEN_REFRESH_FAILED, (String) null);
            throw new ManaCommunityException("Session expired. Please log in again.",
                    HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN");
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        AppUser user = (userId == null) ? null : userRepository.findById(userId).orElse(null);
        if (user == null || Boolean.FALSE.equals(user.getIsActive())) {
            auditLog.record(AuditLogService.Action.TOKEN_REFRESH_FAILED, userId, user == null ? null : user.getEmail());
            throw new ManaCommunityException("Session is no longer valid. Please log in again.",
                    HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN");
        }

        auditLog.record(AuditLogService.Action.TOKEN_REFRESH, user.getId(), user.getEmail());
        // Rotation: issue a brand-new access + refresh pair on every refresh.
        return buildAuthResponse(user, "Token refreshed.");
    }

    @Override
    public void logout(Long userId, String email, String accessToken) {
        // Blacklist the access token so it cannot be reused after logout.
        // The jti uniquely identifies this specific token; entries auto-expire
        // when the token's own expiry is reached.
        if (accessToken != null && !accessToken.isBlank()) {
            try {
                String jti = jwtTokenProvider.getJti(accessToken);
                long expiryMs = jwtTokenProvider.getExpiryMs(accessToken);
                tokenBlacklistService.blacklist(jti, expiryMs);
            } catch (Exception ex) {
                // Do not fail logout if the token is already expired or malformed.
                log.warn("Could not blacklist token on logout for userId={}: {}", userId, ex.getMessage());
            }
        }
        auditLog.record(AuditLogService.Action.LOGOUT, userId, email);
        sessionService.endSession(userId);
    }

    /** Builds the standard auth payload with a fresh access + refresh token pair. */
    private AuthResponse buildAuthResponse(AppUser user, String message) {
        AuthResponse response = new AuthResponse(
                String.valueOf(user.getId()),
                message,
                jwtTokenProvider.generateToken(user),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getCommunity() != null ? user.getCommunity().getId() : null,
                user.getDateOfBirth()
        );
        response.setRefreshToken(jwtTokenProvider.generateRefreshToken(user));
        
        java.util.List<String> enabledModules;
        if (user.hasRole(ROLE_SUPER_ADMIN)) {
            enabledModules = com.manacommunity.api.constants.ModuleConstants.ALL_MODULES.stream()
                    .map(com.manacommunity.api.constants.ModuleConstants.ModuleDef::key)
                    .toList();
        } else {
            enabledModules = user.getCommunity() != null
                    ? communityModuleService.getEnabledModuleKeys(user.getCommunity().getId())
                    : java.util.Collections.emptyList();
        }
        response.setEnabledModules(enabledModules);
        
        return response;
    }

    @Override
    public boolean submitKyc(Long userId, KycRequest req) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (req.getConsentGiven() == null || !req.getConsentGiven())
            throw new ManaCommunityException("KYC consent is required to proceed.",
                    org.springframework.http.HttpStatus.BAD_REQUEST, "KYC_CONSENT_MISSING");

        user.setGovtIdType(req.getGovtIdType().name());
        user.setGovtIdNumber(fieldEncryptionService.encrypt(req.getGovtIdNumber()));
        user.setKycStatus("PENDING");
        userRepository.save(user);
        return true;
    }

    @Override
    @Transactional
    public void sendPasswordResetOtp(String rawEmail) {
        if (rawEmail == null || rawEmail.trim().isEmpty()) {
            throw new ManaCommunityException("Email address is required", HttpStatus.BAD_REQUEST, "INVALID_EMAIL");
        }
        String email = rawEmail.trim().toLowerCase();
        AppUser user = userRepository.findByEmailIgnoreCase(email).orElseThrow(() ->
                new ManaCommunityException("No account found with email address: " + email,
                        HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new ManaCommunityException("This account has been deactivated. Please contact your community administrator.",
                    HttpStatus.FORBIDDEN, "ACCOUNT_INACTIVE");
        }

        com.manacommunity.api.dto.otp.OtpResponse otpResp = otpService.send(user.getEmail(), user.getFullName());
        if (!otpResp.success()) {
            throw new ManaCommunityException(otpResp.message() != null ? otpResp.message() : "Failed to send verification code.",
                    HttpStatus.BAD_REQUEST, "OTP_SEND_FAILED");
        }
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        if (req.getEmail() == null || req.getEmail().trim().isEmpty()) {
            throw new ManaCommunityException("Email address is required", HttpStatus.BAD_REQUEST, "INVALID_EMAIL");
        }
        String email = req.getEmail().trim().toLowerCase();
        AppUser user = userRepository.findByEmailIgnoreCase(email).orElseThrow(() ->
                new ManaCommunityException("No account found with email address: " + email,
                        HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new ManaCommunityException("This account has been deactivated. Please contact support.",
                    HttpStatus.FORBIDDEN, "ACCOUNT_INACTIVE");
        }

        // Verify the submitted OTP code
        com.manacommunity.api.dto.otp.OtpResponse verifyResp = otpService.verify(email, req.getOtpCode());
        if (!verifyResp.success() || !verifyResp.verified()) {
            throw new ManaCommunityException(
                    verifyResp.message() != null ? verifyResp.message() : "Invalid or expired verification code.",
                    HttpStatus.BAD_REQUEST, "INVALID_OTP");
        }

        // Validate password strength against policy and user personal tokens
        PasswordPolicy.validate(req.getNewPassword(), java.util.Arrays.asList(
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getCommunity() != null ? user.getCommunity().getName() : null
        ));

        // Update password and clear lockout / failure attempts
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        auditLog.record(AuditLogService.Action.PASSWORD_CHANGED, user.getId(), user.getEmail());
        auditService.record(
                com.manacommunity.api.security.AuditAction.USER_UPDATED,
                com.manacommunity.api.security.AuditModule.USER_MANAGEMENT,
                "AppUser", String.valueOf(user.getId()),
                null,
                "Password reset via OTP verification");
    }

    @Override
    @Transactional
    public void changePassword(Long userId, com.manacommunity.api.user.dto.ChangePasswordRequest req) {
        if (userId == null) {
            throw new ManaCommunityException("User ID is required", HttpStatus.BAD_REQUEST, "INVALID_USER_ID");
        }
        AppUser user = userRepository.findById(userId).orElseThrow(() ->
                new ResourceNotFoundException("User", userId));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new ManaCommunityException("This account has been deactivated. Please contact support.",
                    HttpStatus.FORBIDDEN, "ACCOUNT_INACTIVE");
        }

        if (req.getCurrentPassword() == null || req.getCurrentPassword().isBlank()) {
            throw new InvalidInputException("Current password is required.");
        }

        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
            throw new InvalidInputException("Current password does not match.");
        }

        if (req.getConfirmPassword() != null && !req.getConfirmPassword().isBlank()
                && !req.getConfirmPassword().equals(req.getNewPassword())) {
            throw new InvalidInputException("New password and confirm password do not match.");
        }

        PasswordPolicy.validate(req.getNewPassword(), java.util.Arrays.asList(
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getCommunity() != null ? user.getCommunity().getName() : null
        ));

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        auditLog.record(AuditLogService.Action.PASSWORD_CHANGED, user.getId(), user.getEmail());
        auditService.record(
                com.manacommunity.api.security.AuditAction.USER_UPDATED,
                com.manacommunity.api.security.AuditModule.USER_MANAGEMENT,
                "AppUser", String.valueOf(user.getId()),
                null,
                "Password changed by user");
    }

    private String maskAadharNumber(String aadhar) {
        if (aadhar == null || aadhar.length() < 4)
            return "INVALID_ID";
        return "XXXX-XXXX-" + aadhar.substring(aadhar.length() - 4);
    }
}
