package com.manacommunity.api.user.controller;

import com.manacommunity.api.user.dto.AuthResponse;
import com.manacommunity.api.user.dto.ForgotPasswordRequest;
import com.manacommunity.api.user.dto.LoginRequest;
import com.manacommunity.api.user.dto.RefreshTokenRequest;
import com.manacommunity.api.user.dto.RegisterRequest;
import com.manacommunity.api.user.dto.ResetPasswordRequest;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
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

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) throws Exception {
        AuthResponse response = authService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) throws Exception {
        return ResponseEntity.ok(authService.loginUser(request));
    }

    /**
     * Exchanges a valid refresh token for a fresh access + refresh token pair.
     * Call this when the access token has (or is about to) expire.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken()));
    }

    /**
     * Sends a 6-digit OTP verification code to the registered email address for password reset.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.sendPasswordResetOtp(request.getEmail());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Verification code has been sent to your email address."
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
     * Logout. Extracts the access token from the Authorization header and records it
     * in the token blacklist so it cannot be reused within its remaining lifetime.
     * The client should also discard both access and refresh tokens locally.
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {
        String accessToken = null;
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7).trim();
        }
        authService.logout(
                principal != null ? principal.getId() : null,
                principal != null ? principal.getUsername() : null,
                accessToken);
        return ResponseEntity.ok("Logged out.");
    }
}
