package com.manacommunity.api.user.controller;

import com.manacommunity.api.user.dto.AuthResponse;
import com.manacommunity.api.user.dto.ChangePasswordRequest;
import com.manacommunity.api.user.dto.ForgotPasswordRequest;
import com.manacommunity.api.user.dto.LoginRequest;
import com.manacommunity.api.user.dto.RefreshTokenRequest;
import com.manacommunity.api.user.dto.RegisterRequest;
import com.manacommunity.api.user.dto.ResetPasswordRequest;
import com.manacommunity.api.user.dto.SendSignupOtpRequest;
import com.manacommunity.api.user.dto.VerifySignupOtpRequest;
import com.manacommunity.api.exception.ManaCommunityException;
import com.manacommunity.api.security.CookieAuthHelper;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Auth endpoints — exceptions bubble up to GlobalExceptionHandler
 * automatically.
 * No try/catch needed here; the handler returns the correct HTTP status +
 * ErrorResponse JSON.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private CookieAuthHelper cookieAuthHelper;

    @Autowired
    private com.manacommunity.api.security.RsaKeyService rsaKeyService;

    /**
     * Exposes the active RSA public key for client-side password encryption.
     */
    @GetMapping("/public-key")
    public ResponseEntity<Map<String, Object>> getPublicKey() {
        return ResponseEntity.ok(Map.of(
                "publicKey", rsaKeyService.getPublicKeyPem(),
                "keyId", rsaKeyService.getKeyId(),
                "algorithm", "RSA-OAEP-256"
        ));
    }

    /**
     * Sends a 6-digit OTP to the provided email to verify it before signup.
     * Call this first, then include the received code in the /register request.
     */
    @PostMapping("/send-signup-otp")
    public ResponseEntity<Map<String, Object>> sendSignupOtp(@Valid @RequestBody SendSignupOtpRequest request) {
        authService.sendSignupOtp(request.getEmail(), request.getPhone());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Verification code has been sent to your email address."
        ));
    }

    /**
     * Validates that email and phone do not exist, and verifies the 6-digit code.
     */
    @PostMapping("/verify-signup-otp")
    public ResponseEntity<Map<String, Object>> verifySignupOtp(@Valid @RequestBody VerifySignupOtpRequest request) {
        authService.verifySignupOtp(request.getEmail(), request.getPhone(), request.getCode());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "verified", true,
                "message", "Email and phone verified successfully."
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse httpResponse) throws Exception {
        AuthResponse response = authService.registerUser(request);
        if (response != null && response.getToken() != null) {
            cookieAuthHelper.setAuthCookies(httpResponse, response.getToken(), response.getRefreshToken());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/login")
    public ResponseEntity<Map<String, String>> loginHealth() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "auth"));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse httpResponse) throws Exception {
        AuthResponse response = authService.loginUser(request);
        if (response != null && response.getToken() != null) {
            cookieAuthHelper.setAuthCookies(httpResponse, response.getToken(), response.getRefreshToken());
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Exchanges a valid refresh token for a fresh access + refresh token pair.
     * Supports both JSON request body and HttpOnly refresh_token cookie.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String refreshToken = (request != null && request.getRefreshToken() != null && !request.getRefreshToken().isBlank())
                ? request.getRefreshToken().trim()
                : cookieAuthHelper.extractRefreshToken(httpRequest);

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ManaCommunityException("Refresh token is required.", HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN");
        }

        AuthResponse response = authService.refreshToken(refreshToken);
        if (response != null && response.getToken() != null) {
            cookieAuthHelper.setAuthCookies(httpResponse, response.getToken(), response.getRefreshToken());
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Sends a 6-digit OTP verification code to the registered email address for password reset.
     * Prevents user enumeration by returning a generic response whether or not the account exists.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.sendPasswordResetOtp(request.getEmail());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "If an account exists with that email address, a verification code has been sent."
        ));
    }

    /**
     * Validates the email OTP and updates the user's password.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Password has been successfully updated. You can now log in with your new password."
        ));
    }

    /**
     * Changes the password for the authenticated user.
     */
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        if (principal == null || principal.getId() == null) {
            throw new com.manacommunity.api.exception.UnauthorizedActionException("Authentication is required to change password.");
        }
        authService.changePassword(principal.getId(), request);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Password changed successfully."
        ));
    }

    @PutMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePasswordPut(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        return changePassword(principal, request);
    }

    /**
     * Logout. Extracts the access token from the Authorization header or HttpOnly cookie,
     * records it in the token blacklist, and clears the authentication cookies.
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String accessToken = null;
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7).trim();
        } else {
            accessToken = cookieAuthHelper.getCookieValue(httpRequest, CookieAuthHelper.ACCESS_TOKEN_COOKIE);
        }
        authService.logout(
                principal != null ? principal.getId() : null,
                principal != null ? principal.getUsername() : null,
                accessToken);

        cookieAuthHelper.clearAuthCookies(httpResponse);
        return ResponseEntity.ok("Logged out.");
    }
}
