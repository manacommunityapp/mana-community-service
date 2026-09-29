package com.manacommunity.api.user.controller;

import com.manacommunity.api.security.CookieAuthHelper;
import com.manacommunity.api.user.dto.AuthResponse;
import com.manacommunity.api.user.dto.ChangePasswordRequest;
import com.manacommunity.api.user.dto.ForgotPasswordRequest;
import com.manacommunity.api.user.dto.LoginRequest;
import com.manacommunity.api.user.dto.RefreshTokenRequest;
import com.manacommunity.api.user.dto.RegisterRequest;
import com.manacommunity.api.user.dto.ResetPasswordRequest;
import com.manacommunity.api.user.dto.SendSignupOtpRequest;
import com.manacommunity.api.user.dto.VerifySignupOtpRequest;
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

    @PostMapping("/send-signup-otp")
    public ResponseEntity<Map<String, Object>> sendSignupOtp(@Valid @RequestBody SendSignupOtpRequest request) {
        authService.sendSignupOtp(request.getEmail(), request.getPhone());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Verification code has been sent to your email address."
        ));
    }

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
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) throws Exception {
        AuthResponse response = authService.registerUser(request);
        applyWebCookies(httpRequest, httpResponse, response);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) throws Exception {
        AuthResponse response = authService.loginUser(request);
        applyWebCookies(httpRequest, httpResponse, response);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String refreshToken;
        if (cookieAuthHelper.isWebPlatform(httpRequest)) {
            refreshToken = cookieAuthHelper.extractRefreshToken(httpRequest);
            if (refreshToken == null || refreshToken.isBlank()) {
                throw new com.manacommunity.api.exception.UnauthorizedActionException(
                        "Refresh token cookie is missing or expired. Please log in again.");
            }
        } else {
            if (request == null || request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
                throw new com.manacommunity.api.exception.InvalidInputException("refreshToken is required.");
            }
            refreshToken = request.getRefreshToken();
        }
        AuthResponse response = authService.refreshToken(refreshToken);
        applyWebCookies(httpRequest, httpResponse, response);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.sendPasswordResetOtp(request.getEmail());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Verification code has been sent to your email address."
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Password has been successfully updated. You can now log in with your new password."
        ));
    }

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

    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String accessToken = null;
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7).trim();
        }
        if (accessToken == null) {
            accessToken = cookieAuthHelper.extractAccessToken(httpRequest);
        }
        String refreshToken = cookieAuthHelper.extractRefreshToken(httpRequest);
        authService.logout(
                principal != null ? principal.getId() : null,
                principal != null ? principal.getUsername() : null,
                accessToken,
                refreshToken);
        if (cookieAuthHelper.isWebPlatform(httpRequest)) {
            cookieAuthHelper.clearAuthCookies(httpResponse);
        }
        return ResponseEntity.ok("Logged out.");
    }

    private void applyWebCookies(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
                                 AuthResponse response) {
        if (!cookieAuthHelper.isWebPlatform(httpRequest)) return;
        cookieAuthHelper.setAuthCookies(httpResponse, response.getToken(), response.getRefreshToken());
        response.setToken(null);
        response.setRefreshToken(null);
    }
}
