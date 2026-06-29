package com.manacommunity.api.controller;

import com.manacommunity.api.dto.chat.*;
import com.manacommunity.api.model.AppUser;
import com.manacommunity.api.security.UserPrincipal;
import com.manacommunity.api.service.ChatService;
import com.manacommunity.api.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST chat API. All endpoints require authentication (default security rule);
 * each one resolves the logged-in user and the service enforces membership.
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final LoggedInUserService loggedInUserService;
    private final ChatService chatService;

    /** GET /api/chat/conversations — the caller's threads, newest first. */
    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationResponse>> getConversations(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(chatService.getConversations(currentUser));
    }

    /** POST /api/chat/conversations/direct — find or create a 1:1 thread. */
    @PostMapping("/conversations/direct")
    public ResponseEntity<ConversationResponse> startDirect(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody StartDirectRequest request) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(chatService.startDirect(currentUser, request.userId()));
    }

    /** GET /api/chat/conversations/{id}/messages — full history. */
    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(chatService.getMessages(currentUser, id));
    }

    /** POST /api/chat/conversations/{id}/messages — send a message. */
    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody SendMessageRequest request) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(chatService.sendMessage(currentUser, id, request.content()));
    }

    /** POST /api/chat/conversations/{id}/read — clear the caller's unread count. */
    @PostMapping("/conversations/{id}/read")
    public ResponseEntity<Void> markRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        chatService.markRead(currentUser, id);
        return ResponseEntity.noContent().build();
    }

    /** GET /api/chat/contacts — community members the caller can message. */
    @GetMapping("/contacts")
    public ResponseEntity<List<ChatContactResponse>> getContacts(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(chatService.getContacts(currentUser));
    }
}
