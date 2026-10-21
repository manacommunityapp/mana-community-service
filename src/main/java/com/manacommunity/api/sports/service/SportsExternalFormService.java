package com.manacommunity.api.sports.service;

import com.manacommunity.api.sports.dto.*;
import com.manacommunity.api.sports.model.SportsExternalResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface SportsExternalFormService {

    SportsExternalFormResponse createExternalForm(Long eventId, CreateExternalFormRequest request);

    List<SportsExternalFormResponse> getFormsForEvent(Long eventId);

    SportsExternalFormResponse getForm(Long formId);

    SportsExternalFormResponse getFormByWebhookToken(String token);

    SportsExternalFormResponse updateFieldMappings(Long formId, List<FieldMappingDto> mappings);

    SyncResultDto syncFormResponses(Long formId);

    List<ExternalResponseDto> getResponses(Long formId, String status);

    List<ExternalResponseDto> getReviewQueue(Long eventId);

    ExternalResponseDto resolveReviewResponse(Long responseId, ResolveReviewRequest request);

    String getAppsScriptSnippet(Long formId);

    SportsExternalResponse handleIncomingWebhook(
            String webhookToken,
            String externalResponseId,
            String email,
            LocalDateTime submittedAt,
            Map<String, Object> answers
    );
}
