package com.cpn.web;

import com.cpn.application.communication.ChatService;
import com.cpn.domain.communication.model.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cpn/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/conversation")
    public ResponseEntity<List<ChatMessage>> getConversation(@RequestParam UUID u1, @RequestParam UUID u2) {
        return ResponseEntity.ok(chatService.getConversation(u1, u2));
    }

    @PostMapping("/send")
    public ResponseEntity<ChatMessage> sendMessage(
            @RequestParam UUID senderId,
            @RequestParam UUID recipientId,
            @RequestBody String content) {
        return ResponseEntity.ok(chatService.sendMessage(senderId, recipientId, content));
    }
}
