package com.manacommunity.api.slice.controller;

import com.manacommunity.api.user.controller.AuthController;
import com.manacommunity.api.user.dto.LoginRequest;
import com.manacommunity.api.user.dto.RegisterRequest;
import com.manacommunity.api.exception.DuplicateResourceException;
import com.manacommunity.api.exception.InvalidInviteCodeException;
import com.manacommunity.api.exception.ManaCommunityException;
import com.manacommunity.api.user.dto.AuthResponse;
import com.manacommunity.api.user.service.AuthService;
import com.manacommunity.api.support.BaseWebMvcTest;
import com.manacommunity.api.support.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController")
class AuthControllerTest extends BaseWebMvcTest {

    @MockitoBean AuthService authService;

    private AuthResponse dummyAuthResponse(String email) {
        return new AuthResponse("1", "OK", "mock-token-1",
                "Test User", email, "MEMBER", 1L, LocalDate.of(1990, 1, 1));
    }

    // ── POST /api/auth/register ───────────────────────────────────────

    @Nested
    @DisplayName("POST /api/auth/register")
    class Register {

        @Test
        @DisplayName("valid request returns 201 with auth response")
        void valid_returns201() throws Exception {
            RegisterRequest req = TestDataBuilder.registerRequest();
            when(authService.registerUser(any())).thenReturn(dummyAuthResponse(req.getEmail()));

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value(req.getEmail()))
                    .andExpect(jsonPath("$.token").exists());
        }

        @Test
        @DisplayName("missing required fields returns 400")
        void missingFields_returns400() throws Exception {
            String emptyBody = "{}";

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(emptyBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("invalid invite code returns 400")
        void invalidInvite_returns400() throws Exception {
            RegisterRequest req = TestDataBuilder.registerRequest();
            when(authService.registerUser(any()))
                    .thenThrow(new InvalidInviteCodeException("BAD_CODE"));

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("duplicate email returns 409")
        void duplicateEmail_returns409() throws Exception {
            RegisterRequest req = TestDataBuilder.registerRequest();
            when(authService.registerUser(any()))
                    .thenThrow(new DuplicateResourceException("User", "email", req.getEmail()));

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isConflict());
        }
    }

    // ── POST /api/auth/login ──────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("valid credentials return 200 with token")
        void validCredentials_returns200() throws Exception {
            LoginRequest req = TestDataBuilder.loginRequest("admin@test.com", "password123");
            when(authService.loginUser(any())).thenReturn(dummyAuthResponse("admin@test.com"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").exists())
                    .andExpect(jsonPath("$.email").value("admin@test.com"));
        }

        @Test
        @DisplayName("wrong password returns 401 with user-defined exception message")
        void wrongPassword_returns401() throws Exception {
            LoginRequest req = TestDataBuilder.loginRequest("x@x.com", "wrong");
            when(authService.loginUser(any()))
                    .thenThrow(new ManaCommunityException("Incorrect password for this email address.",
                            HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"))
                    .andExpect(jsonPath("$.message").value("Incorrect password for this email address."));
        }

        @Test
        @DisplayName("unknown identifier returns 401 with user-defined message")
        void unknownIdentifier_returns401() throws Exception {
            LoginRequest req = TestDataBuilder.loginRequest("unknown@test.com", "password");
            when(authService.loginUser(any()))
                    .thenThrow(new ManaCommunityException("Email address not found. Please check and try again.",
                            HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"))
                    .andExpect(jsonPath("$.message").value("Email address not found. Please check and try again."));
        }

        @Test
        @DisplayName("locked account returns 423 with lockout message")
        void lockedAccount_returns423() throws Exception {
            LoginRequest req = TestDataBuilder.loginRequest("admin@test.com", "password");
            when(authService.loginUser(any()))
                    .thenThrow(new ManaCommunityException("Account locked due to too many failed attempts. Try again in 15 minute(s).",
                            HttpStatus.LOCKED, "ACCOUNT_LOCKED"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isLocked())
                    .andExpect(jsonPath("$.status").value(423))
                    .andExpect(jsonPath("$.error").value("ACCOUNT_LOCKED"))
                    .andExpect(jsonPath("$.message").value("Account locked due to too many failed attempts. Try again in 15 minute(s)."));
        }
    }
}
