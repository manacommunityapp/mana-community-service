package com.manacommunity.api.controller;

import com.manacommunity.api.dto.chat.ChatMessageResponse;
import com.manacommunity.api.dto.chat.SendMessageRequest;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * STOMP handler for real-time chat messages.
 * Client sends to: /app/chat/conversations/{conversationId}/send
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final AppUserRepository appUserRepository;

    @MessageMapping("/chat/conversations/{conversationId}/send")
    public void sendMessage(
            @DestinationVariable Long conversationId,
            @Payload SendMessageRequest request,
            Principal principal) {

        if (principal == null) {
            throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        }
        Long userId = Long.parseLong(principal.getName());
        AppUser sender = appUserRepository.findById(userId)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("User not found"));

        ChatMessageResponse response = chatService.sendMessage(sender, conversationId, request.content());
        log.debug("STOMP chat message sent conversationId={} senderId={}", conversationId, userId);
    }
}
