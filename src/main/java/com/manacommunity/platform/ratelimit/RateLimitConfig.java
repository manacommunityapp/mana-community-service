package com.manacommunity.platform.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitConfig {

    private EndpointLimit login         = new EndpointLimit(5,   60);
    private EndpointLimit otp           = new EndpointLimit(3,   300);
    private EndpointLimit passwordReset = new EndpointLimit(3,   3600);
    private EndpointLimit registration  = new EndpointLimit(10,  3600);
    private EndpointLimit aiCalls       = new EndpointLimit(20,  60);
    private EndpointLimit defaultLimit  = new EndpointLimit(100, 60);

    public EndpointLimit forType(RateLimitType type) {
        return switch (type) {
            case LOGIN          -> login;
            case OTP            -> otp;
            case PASSWORD_RESET -> passwordReset;
            case REGISTRATION   -> registration;
            case AI_CALL        -> aiCalls;
            case DEFAULT        -> defaultLimit;
        };
    }

    public EndpointLimit getLogin()         { return login; }
    public void setLogin(EndpointLimit v)   { this.login = v; }
    public EndpointLimit getOtp()           { return otp; }
    public void setOtp(EndpointLimit v)     { this.otp = v; }
    public EndpointLimit getPasswordReset() { return passwordReset; }
    public void setPasswordReset(EndpointLimit v) { this.passwordReset = v; }
    public EndpointLimit getRegistration()  { return registration; }
    public void setRegistration(EndpointLimit v)  { this.registration = v; }
    public EndpointLimit getAiCalls()       { return aiCalls; }
    public void setAiCalls(EndpointLimit v) { this.aiCalls = v; }
    public EndpointLimit getDefaultLimit()  { return defaultLimit; }
    public void setDefaultLimit(EndpointLimit v)  { this.defaultLimit = v; }

    public record EndpointLimit(int maxRequests, int windowSeconds) {
        public EndpointLimit() { this(100, 60); }
    }
}
