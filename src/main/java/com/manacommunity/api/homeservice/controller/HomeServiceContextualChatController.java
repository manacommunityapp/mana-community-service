package com.manacommunity.api.homeservice.controller;

import com.manacommunity.api.homeservice.dto.HomeServiceChatMessageRequest;
import com.manacommunity.api.homeservice.model.entity.ContextualChatMessageEntity;
import com.manacommunity.api.homeservice.service.HomeServiceContextualChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController("homeServiceContextualChatController")
@RequestMapping("/api/v1/home-services/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HomeServiceContextualChatController {
    private final HomeServiceContextualChatService chatService;

    @PostMapping
    public ResponseEntity<ContextualChatMessageEntity> sendMessage(@RequestBody HomeServiceChatMessageRequest req) {
        return ResponseEntity.ok(chatService.sendMessage(req));
    }

    @GetMapping
    public ResponseEntity<List<ContextualChatMessageEntity>> getMessages(
            @RequestParam String workerId,
            @RequestParam String residentUserId) {
        return ResponseEntity.ok(chatService.getChatHistory(workerId, residentUserId));
    }
}
