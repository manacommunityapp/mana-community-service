package com.manacommunity.api.user.dto;


import lombok.Data;

@Data
public class LoginRequest {
    /** Email address or mobile number (10-digit). */
    private String identifier;
    /** Plaintext password (supported for backward compatibility and automated tests). */
    private String password;
    /** Client-side encrypted RSA-OAEP SHA-256 base64 payload. */
    private String encryptedPassword;
}

