package com.manacommunity.api.ai.controller;

import com.manacommunity.api.ai.dto.AiChatRequest;
import com.manacommunity.api.ai.dto.AiChatResponse;
import com.manacommunity.api.ai.service.AiChatAgentService;
import com.manacommunity.api.model.AppUser;
import com.manacommunity.api.service.LoggedInUserService;
import com.manacommunity.api.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * REST API for the Mana Community AI assistant.
 *
 * <p>Two endpoints share the same security and validation pipeline:</p>
 * <ul>
 *   <li>{@code POST /api/ai/chat} — synchronous, returns the full reply at once</li>
 *   <li>{@code POST /api/ai/chat/stream} — SSE, streams tokens as they're generated</li>
 * </ul>
 *
 * <p>Authentication uses the existing JWT + {@link UserPrincipal} pipeline, resolved
 * to a full {@link AppUser} via {@link LoggedInUserService} — no separate auth layer.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/chat")
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatAgentService chatAgentService;
    private final LoggedInUserService loggedInUserService;

    /**
     * Synchronous chat — waits for the full AI response before returning.
     */
    @PostMapping
    public ResponseEntity<AiChatResponse> chat(
            @Valid @RequestBody AiChatRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        AppUser user = loggedInUserService.resolve(principal);
        validateCommunityMembership(user);

        AiChatResponse response = chatAgentService.chat(request, user);
        return ResponseEntity.ok(response);
    }

    /**
     * Streaming chat — returns an SSE stream of text tokens as they're generated.
     * The frontend can render tokens progressively for a ChatGPT-like experience.
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(
            @Valid @RequestBody AiChatRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        AppUser user = loggedInUserService.resolve(principal);
        validateCommunityMembership(user);

        return chatAgentService.chatStream(request, user);
    }

    private void validateCommunityMembership(AppUser user) {
        if (user.getCommunity() == null) {
            throw new IllegalArgumentException(
                    "AI chat requires community membership. Please join a community first.");
        }
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new SecurityException("Account is not active.");
        }
    }
}
