package com.manacommunity.api.sports.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.sports.model.SportsExternalResponse;
import com.manacommunity.api.sports.service.SportsExternalFormService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/integrations/google/forms")
@RequiredArgsConstructor
public class GoogleFormsWebhookController {

    private final SportsExternalFormService externalFormService;
    private final ObjectMapper objectMapper;

    /**
     * Webhook endpoint called by Google Apps Script onFormSubmit trigger.
     */
    @PostMapping("/webhook/{token}")
    public ResponseEntity<Map<String, Object>> handleWebhook(
            @PathVariable String token,
            @RequestBody Map<String, Object> payload
    ) {
        log.info("Received Google Forms webhook for token: {}", token);

        String externalResponseId = (String) payload.getOrDefault(
                "externalResponseId",
                "gf_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12)
        );

        String respondentEmail = (String) payload.get("respondentEmail");

        LocalDateTime submittedAt = LocalDateTime.now();
        Object subTimeObj = payload.get("submittedAt");
        if (subTimeObj instanceof String s && !s.isBlank()) {
            try {
                submittedAt = OffsetDateTime.parse(s, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime();
            } catch (Exception ignored) {
                try {
                    submittedAt = LocalDateTime.parse(s);
                } catch (Exception ignored2) {}
            }
        }

        Map<String, Object> answers = new HashMap<>();
        if (payload.containsKey("answers") && payload.get("answers") instanceof Map<?, ?> m) {
            for (Map.Entry<?, ?> entry : m.entrySet()) {
                answers.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        } else {
            // Flatten payload excluding metadata keys
            for (Map.Entry<String, Object> entry : payload.entrySet()) {
                if (!entry.getKey().equals("externalResponseId")
                        && !entry.getKey().equals("respondentEmail")
                        && !entry.getKey().equals("submittedAt")) {
                    answers.put(entry.getKey(), entry.getValue());
                }
            }
        }

        SportsExternalResponse processed = externalFormService.handleIncomingWebhook(
                token,
                externalResponseId,
                respondentEmail,
                submittedAt,
                answers
        );

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("responseId", processed.getId());
        response.put("importStatus", processed.getStatus().name());
        if (processed.getLinkedRegistration() != null) {
            response.put("registrationId", processed.getLinkedRegistration().getId());
        }
        if (processed.getReviewReason() != null) {
            response.put("reviewReason", processed.getReviewReason());
        }

        return ResponseEntity.ok(response);
    }
}
