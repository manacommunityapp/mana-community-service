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

    /** Records a logout for audit. Stateless tokens can't be revoked server-side; the client clears them. */
    void logout(Long userId, String email);

    boolean submitKyc(Long userId, KycRequest req);
}
