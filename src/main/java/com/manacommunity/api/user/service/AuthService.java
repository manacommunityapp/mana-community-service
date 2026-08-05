package com.manacommunity.api.user.service;

import com.manacommunity.api.user.dto.AuthResponse;
import com.manacommunity.api.user.dto.KycRequest;
import com.manacommunity.api.user.dto.LoginRequest;
import com.manacommunity.api.user.dto.RegisterRequest;

public interface AuthService {
    AuthResponse registerUser(RegisterRequest request) throws Exception;
    AuthResponse loginUser(LoginRequest request) throws Exception;

    /** Exchanges a valid refresh token for a fresh access + refresh token pair (rotation). */
    AuthResponse refreshToken(String refreshToken);

    /**
     * Blacklists the given access token and records a logout audit entry.
     * The token remains cryptographically valid until expiry but will be rejected
     * by {@code JwtAuthenticationFilter} once added to the blacklist.
     */
    void logout(Long userId, String email, String accessToken);

    boolean submitKyc(Long userId, KycRequest req);
}
