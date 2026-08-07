package com.manacommunity.api.user.dto;


import lombok.Data;

@Data
public class LoginRequest {
    /** Email address or mobile number (10-digit). */
    private String identifier;
    private String password;

}
