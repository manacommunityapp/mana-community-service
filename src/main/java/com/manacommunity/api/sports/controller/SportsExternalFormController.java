package com.manacommunity.api.sports.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.sports.dto.*;
import com.manacommunity.api.sports.model.SportsExternalResponse;
import com.manacommunity.api.sports.service.SportsExternalFormService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/sports")
@RequiredArgsConstructor
public class SportsExternalFormController {

    private final SportsExternalFormService externalFormService;
    private final ObjectMapper objectMapper;

    /**
     * Create an external registration form (Google Forms or Mana Public Form) for a tournament event.
     */
    @PostMapping("/events/{eventId}/external-forms")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SPORTS_ADMIN')")
    public ResponseEntity<SportsExternalFormResponse> createExternalForm(
            @PathVariable Long eventId,
            @Valid @RequestBody CreateExternalFormRequest request
    ) {
        request.setEventId(eventId);
        SportsExternalFormResponse response = externalFormService.createExternalForm(eventId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * List all external registration forms associated with an event.
     */
    @GetMapping("/events/{eventId}/external-forms")
    public ResponseEntity<List<SportsExternalFormResponse>> getFormsForEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(externalFormService.getFormsForEvent(eventId));
    }

    /**
     * Get a single external form by its ID.
     */
    @GetMapping("/external-forms/{formId}")
    public ResponseEntity<SportsExternalFormResponse> getForm(@PathVariable Long formId) {
        return ResponseEntity.ok(externalFormService.getForm(formId));
    }

    /**
     * Update question-to-field mappings for an external form.
     */
    @PutMapping("/external-forms/{formId}/mappings")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SPORTS_ADMIN')")
    public ResponseEntity<SportsExternalFormResponse> updateFieldMappings(
            @PathVariable Long formId,
            @RequestBody List<FieldMappingDto> mappings
    ) {
        return ResponseEntity.ok(externalFormService.updateFieldMappings(formId, mappings));
    }

    /**
     * Trigger synchronization with external form responses.
     */
    @PostMapping("/external-forms/{formId}/sync")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SPORTS_ADMIN')")
    public ResponseEntity<SyncResultDto> syncResponses(@PathVariable Long formId) {
        return ResponseEntity.ok(externalFormService.syncFormResponses(formId));
    }

    /**
     * Get responses for a specific external form.
     */
    @GetMapping("/external-forms/{formId}/responses")
    public ResponseEntity<List<ExternalResponseDto>> getResponses(
            @PathVariable Long formId,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(externalFormService.getResponses(formId, status));
    }

    /**
     * Get review queue for an event (entries with status NEEDS_REVIEW or REJECTED).
     */
    @GetMapping("/events/{eventId}/external-forms/review")
    public ResponseEntity<List<ExternalResponseDto>> getReviewQueue(@PathVariable Long eventId) {
        return ResponseEntity.ok(externalFormService.getReviewQueue(eventId));
    }

    /**
     * Resolve a quarantined response (APPROVE, REJECT, or LINK_USER).
     */
    @PostMapping("/external-forms/responses/{responseId}/resolve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SPORTS_ADMIN')")
    public ResponseEntity<ExternalResponseDto> resolveResponse(
            @PathVariable Long responseId,
            @Valid @RequestBody ResolveReviewRequest request
    ) {
        return ResponseEntity.ok(externalFormService.resolveReviewResponse(responseId, request));
    }

    /**
     * Get generated Google Apps Script webhook integration code.
     */
    @GetMapping("/external-forms/{formId}/apps-script")
    public ResponseEntity<Map<String, String>> getAppsScript(@PathVariable Long formId) {
        String script = externalFormService.getAppsScriptSnippet(formId);
        return ResponseEntity.ok(Map.of("script", script));
    }

    /**
     * Public endpoint to get public form metadata by webhookToken.
     */
    @GetMapping("/external-forms/public/{token}")
    public ResponseEntity<SportsExternalFormResponse> getPublicForm(@PathVariable String token) {
        return ResponseEntity.ok(externalFormService.getFormByWebhookToken(token));
    }

    /**
     * Public endpoint to submit registration via Mana Public External Form.
     */
    @PostMapping("/external-forms/public/{token}/submit")
    public ResponseEntity<Map<String, Object>> submitPublicForm(
            @PathVariable String token,
            @RequestBody Map<String, Object> body
    ) {
        String externalResponseId = (String) body.getOrDefault("externalResponseId", "pub_" + UUID.randomUUID().toString());
        String email = (String) body.get("email");
        LocalDateTime submittedAt = LocalDateTime.now();

        Map<String, Object> answers = body.containsKey("answers") && body.get("answers") instanceof Map
                ? (Map<String, Object>) body.get("answers")
                : body;

        SportsExternalResponse resp = externalFormService.handleIncomingWebhook(token, externalResponseId, email, submittedAt, answers);

        return ResponseEntity.ok(Map.of(
                "status", resp.getStatus().name(),
                "responseId", resp.getId(),
                "registrationId", resp.getLinkedRegistration() != null ? resp.getLinkedRegistration().getId() : 0,
                "reviewReason", resp.getReviewReason() != null ? resp.getReviewReason() : ""
        ));
    }
}
