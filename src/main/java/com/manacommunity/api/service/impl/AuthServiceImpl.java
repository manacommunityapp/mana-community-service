package com.manacommunity.api.service.impl;

import com.manacommunity.api.response.AuthResponse;
import com.manacommunity.api.dto.KycRequest;
import com.manacommunity.api.dto.LoginRequest;
import com.manacommunity.api.dto.RegisterRequest;
import com.manacommunity.api.exception.*;
import com.manacommunity.api.model.AppUser;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.Role;
import com.manacommunity.api.repository.AppUserRepository;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.repository.RoleRepository;
import com.manacommunity.api.security.AuditLogService;
import com.manacommunity.api.security.JwtTokenProvider;
import com.manacommunity.api.security.PasswordPolicy;
import com.manacommunity.api.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

@Service
public class AuthServiceImpl implements AuthService {

    // ── Brute-force lockout policy ────────────────────────────────────────
    private static final int MAX_ATTEMPTS_USER = 5;
    private static final int MAX_ATTEMPTS_ADMIN = 3;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private static final Set<String> ADMIN_ROLES = Set.of("ADMIN", "SUPER_ADMIN", "COMMUNITY_ADMIN");

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

    @Override
    @Transactional
    public AuthResponse registerUser(RegisterRequest request) {

        // 1. Verify Community Invite Code
        Community community = communityRepository.findByInviteCode(request.getInviteCode())
                .orElseThrow(() -> new InvalidInviteCodeException(request.getInviteCode()));

        // 2. Duplicate email / phone check (both are UNIQUE in DB)
        if (userRepository.existsByEmail(request.getEmail()))
            throw new DuplicateResourceException("User", "email", request.getEmail());

        if (userRepository.existsByPhone(request.getPhone()))
            throw new DuplicateResourceException("User", "phone", request.getPhone());

        // 2b. Enforce password strength before hashing — also reject passwords
        // derived from the user's own data (email, name, phone, community).
        // Arrays.asList permits nulls; the evaluator skips null/blank tokens.
        PasswordPolicy.validate(request.getPassword(), java.util.Arrays.asList(
                request.getEmail(),
                request.getFullName(),
                request.getPhone(),
                community.getName()
        ));

        // 3. Mock Aadhaar masking
        String maskedAadhar = maskAadharNumber(request.getAadharNumber());

        // 4. Build and save AppUser
        AppUser user = new AppUser();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setDateOfBirth(request.getDateOfBirth());
        user.setGender(request.getGender());
        user.setGovtIdNumber(maskedAadhar);
        user.setGovtIdType("AADHAAR");
        user.setKycStatus("VERIFIED");
        user.setRole("MEMBER"); // Standard role for new users
        Role memberRole;
        if (community != null) {
            memberRole = roleRepository.findByNameIgnoreCaseAndCommunityId("MEMBER", community.getId())
                    .orElseGet(() -> roleRepository.save(Role.builder()
                            .name("MEMBER")
                            .communityId(community.getId())
                            .build()));
        } else {
            memberRole = roleRepository.findByNameIgnoreCaseAndCommunityIdIsNull("MEMBER")
                    .orElseGet(() -> roleRepository.save(Role.builder()
                            .name("MEMBER")
                            .build()));
        }
        user.setRoleEntity(memberRole);
        user.setIsActive(true);
        user.setCommunity(community);
        user.setFlatNo(request.getFlatNo());
        user.setBlock(request.getBlock());

        AppUser saved = userRepository.save(user);
        auditLog.record(AuditLogService.Action.REGISTER, saved.getId(), saved.getEmail());
        return buildAuthResponse(saved, "Registration & KYC successful!");
    }

    @Override
    @Transactional
    public AuthResponse loginUser(LoginRequest request) {
        AppUser user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    // Audit the attempt, but don't reveal whether the email exists.
                    auditLog.record(AuditLogService.Action.LOGIN_FAILED, request.getEmail());
                    return new ManaCommunityException("Invalid email or password.",
                            HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
                });

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
            throw new ManaCommunityException("Invalid email or password.",
                    HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
        }

        // 3. Success → clear any prior failures and issue tokens.
        if (user.getFailedLoginAttempts() != 0 || user.getLockedUntil() != null) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }
        auditLog.record(AuditLogService.Action.LOGIN_SUCCESS, user.getId(), user.getEmail());
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
    public void logout(Long userId, String email) {
        // Stateless tokens cannot be revoked server-side; the client discards them.
        // Short access-token lifetimes keep the post-logout exposure window small.
        auditLog.record(AuditLogService.Action.LOGOUT, userId, email);
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
        user.setGovtIdNumber(req.getGovtIdNumber()); // encrypt before storing in production
        user.setKycStatus("PENDING");
        userRepository.save(user);
        return true;
    }

    private String maskAadharNumber(String aadhar) {
        if (aadhar == null || aadhar.length() < 4)
            return "INVALID_ID";
        return "XXXX-XXXX-" + aadhar.substring(aadhar.length() - 4);
    }
}
