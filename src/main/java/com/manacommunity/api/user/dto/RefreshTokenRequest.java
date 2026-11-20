package com.manacommunity.api.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Body for {@code POST /api/auth/refresh} — exchanges a valid refresh token (from body or HttpOnly cookie) for a new token pair. */
@Data
public class RefreshTokenRequest {
    private String refreshToken;
}
