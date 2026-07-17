package com.manacommunity.api.unit.ai;

import com.manacommunity.api.ai.controller.AiChatController;
import com.manacommunity.api.ai.dto.AiChatRequest;
import com.manacommunity.api.ai.dto.AiChatResponse;
import com.manacommunity.api.ai.service.AiChatAgentService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.support.BaseWebMvcTest;
import com.manacommunity.api.support.WithMockUserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AiChatController.class)
@DisplayName("AI Chat Controller")
class AiChatControllerTest extends BaseWebMvcTest {

    @MockitoBean
    private AiChatAgentService chatAgentService;

    @MockitoBean
    private LoggedInUserService loggedInUserService;

    @Test
    @DisplayName("POST /api/ai/chat — 401 without auth")
    void chatRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUserPrincipal
    @DisplayName("POST /api/ai/chat — 400 with empty message")
    void chatRejectsEmptyMessage() throws Exception {
        AppUser user = mockActiveUser();
        when(loggedInUserService.resolve(any())).thenReturn(user);

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUserPrincipal
    @DisplayName("POST /api/ai/chat — 200 with valid request")
    void chatReturnsResponse() throws Exception {
        AppUser user = mockActiveUser();
        when(loggedInUserService.resolve(any())).thenReturn(user);
        when(chatAgentService.chat(any(AiChatRequest.class), eq(user)))
                .thenReturn(new AiChatResponse(null, "Hello! How can I help?"));

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Hello! How can I help?"));
    }

    @Test
    @WithMockUserPrincipal
    @DisplayName("POST /api/ai/chat — rejects user without community")
    void chatRejectsUserWithoutCommunity() throws Exception {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setIsActive(true);
        user.setCommunity(null); // no community
        when(loggedInUserService.resolve(any())).thenReturn(user);

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello\"}"))
                .andExpect(status().isBadRequest());
    }

    private AppUser mockActiveUser() {
        Community community = new Community();
        community.setId(100L);

        AppUser user = new AppUser();
        user.setId(1L);
        user.setFullName("Test User");
        user.setIsActive(true);
        user.setRole("MEMBER");
        user.setCommunity(community);
        return user;
    }
}
