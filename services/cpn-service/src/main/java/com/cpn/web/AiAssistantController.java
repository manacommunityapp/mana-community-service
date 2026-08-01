package com.cpn.web;

import com.cpn.application.ai.AiAssistantService;
import com.cpn.domain.ai.model.AiInsight;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cpn/ai")
@RequiredArgsConstructor
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    @PostMapping("/ask")
    public ResponseEntity<AiInsight> ask(
            @RequestParam UUID userId,
            @RequestParam String category,
            @RequestBody String prompt) {
        return ResponseEntity.ok(aiAssistantService.askAi(userId, category, prompt));
    }
}
