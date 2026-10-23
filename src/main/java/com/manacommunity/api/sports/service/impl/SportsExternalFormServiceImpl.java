package com.manacommunity.api.sports.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.sports.dto.*;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.service.GoogleFormsApiClient;
import com.manacommunity.api.sports.service.SportsExternalFormService;
import com.manacommunity.api.sports.service.SportsRegistrationImportProcessor;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SportsExternalFormServiceImpl implements SportsExternalFormService {

    private final SportsExternalFormRepository formRepo;
    private final SportsExternalFormFieldMappingRepository mappingRepo;
    private final SportsExternalResponseRepository responseRepo;
    private final SportsExternalSyncStateRepository syncStateRepo;
    private final SportsEventRepository eventRepo;
    private final SportsPlayerCategoryRepository categoryRepo;
    private final SportsEventRegistrationRepository regRepo;
    private final AppUserRepository userRepo;
    private final SportsRegistrationImportProcessor importProcessor;
    private final GoogleFormsApiClient googleFormsApiClient;
    private final ObjectMapper objectMapper;

    @Value("${app.base-url:https://hub.manacommunity.com}")
    private String appBaseUrl;

    @Override
    @Transactional
    public SportsExternalFormResponse createExternalForm(Long eventId, CreateExternalFormRequest request) {
        SportsEvent event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("SportsEvent not found: " + eventId));

        List<SportsPlayerCategory> categories = categoryRepo.findDefaultAndCommunityCategories(event.getCommunity() != null ? event.getCommunity().getId() : null);

        List<String> fieldKeys = request.getFieldKeys();
        if (fieldKeys == null || fieldKeys.isEmpty()) {
            fieldKeys = List.of(
                    "playerName", "email", "phone", "flatNumber",
                    "age", "gender", "categoryId", "matchFormat",
                    "partnerName", "partnerPhone", "cricheroesUrl"
            );
        }

        SportsExternalForm.FormProvider provider = "MANA_FORM".equalsIgnoreCase(request.getProvider())
                ? SportsExternalForm.FormProvider.MANA_FORM
                : SportsExternalForm.FormProvider.GOOGLE_FORMS;

        SportsExternalForm.ApprovalMode approvalMode = SportsExternalForm.ApprovalMode.TOURNAMENT_DEFAULT;
        if (request.getApprovalMode() != null) {
            try {
                approvalMode = SportsExternalForm.ApprovalMode.valueOf(request.getApprovalMode().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        String webhookToken = UUID.randomUUID().toString();
        String externalFormId;
        String responderUrl;
        String editUrl;
        Map<String, String> questionNameToIdMap = new HashMap<>();

        if (provider == SportsExternalForm.FormProvider.GOOGLE_FORMS) {
            GoogleFormsApiClient.CreatedFormDetails details = googleFormsApiClient.createFormForEvent(event, categories, fieldKeys);
            externalFormId = details.getFormId();
            responderUrl = details.getResponderUri();
            editUrl = details.getEditUri();
            questionNameToIdMap = details.getQuestionNameToIdMap();
        } else {
            externalFormId = "mana_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
            responderUrl = appBaseUrl.replaceAll("/+$", "") + "/sports/register/external/" + webhookToken;
            editUrl = null;
            for (String f : fieldKeys) {
                questionNameToIdMap.put(f, "q_" + f);
            }
        }

        SportsExternalForm form = SportsExternalForm.builder()
                .event(event)
                .community(event.getCommunity())
                .provider(provider)
                .externalFormId(externalFormId)
                .responderUrl(responderUrl)
                .editUrl(editUrl)
                .title(request.getTitle() != null && !request.getTitle().isBlank()
                        ? request.getTitle()
                        : event.getName() + " Registration Form")
                .description(request.getDescription() != null && !request.getDescription().isBlank()
                        ? request.getDescription()
                        : "Official registration form for " + event.getName())
                .webhookToken(webhookToken)
                .status(SportsExternalForm.FormStatus.ACTIVE)
                .approvalMode(approvalMode)
                .build();

        form = formRepo.save(form);

        // Build default field mappings
        List<SportsExternalFormFieldMapping> mappings = new ArrayList<>();
        for (String fieldKey : fieldKeys) {
            String questionId = questionNameToIdMap.getOrDefault(fieldKey, "q_" + fieldKey);
            String questionTitle = fieldToQuestionTitle(fieldKey);
            SportsExternalFormFieldMapping.MappingType mappingType = SportsExternalFormFieldMapping.MappingType.DIRECT;
            String mappingRulesJson = null;

            if ("categoryId".equalsIgnoreCase(fieldKey)) {
                mappingType = SportsExternalFormFieldMapping.MappingType.CATEGORY_LOOKUP;
                Map<String, String> catRules = new HashMap<>();
                for (SportsPlayerCategory cat : categories) {
                    catRules.put(cat.getName(), String.valueOf(cat.getId()));
                }
                try {
                    mappingRulesJson = objectMapper.writeValueAsString(catRules);
                } catch (Exception ignored) {}
            } else if ("matchFormat".equalsIgnoreCase(fieldKey)) {
                mappingType = SportsExternalFormFieldMapping.MappingType.FORMAT_LOOKUP;
                Map<String, String> fmtRules = Map.of(
                        "Singles", "SINGLES",
                        "Doubles", "DOUBLES",
                        "Mixed Doubles", "MIXED_DOUBLES"
                );
                try {
                    mappingRulesJson = objectMapper.writeValueAsString(fmtRules);
                } catch (Exception ignored) {}
            }

            SportsExternalFormFieldMapping mapping = SportsExternalFormFieldMapping.builder()
                    .form(form)
                    .manaFieldKey(fieldKey)
                    .externalQuestionId(questionId)
                    .questionTitle(questionTitle)
                    .mappingType(mappingType)
                    .mappingRulesJson(mappingRulesJson)
                    .required("playerName".equalsIgnoreCase(fieldKey) || "phone".equalsIgnoreCase(fieldKey))
                    .build();
            mappings.add(mapping);
        }
        mappingRepo.saveAll(mappings);
        form.setFieldMappings(mappings);

        // Initialize sync state
        SportsExternalSyncState syncState = SportsExternalSyncState.builder()
                .form(form)
                .syncMode(provider == SportsExternalForm.FormProvider.GOOGLE_FORMS
                        ? SportsExternalSyncState.SyncMode.POLLING
                        : SportsExternalSyncState.SyncMode.WEBHOOK)
                .syncStatus(SportsExternalSyncState.SyncStatus.IDLE)
                .totalResponsesPolled(0)
                .totalImported(0)
                .totalQuarantined(0)
                .totalRejected(0)
                .build();
        syncState = syncStateRepo.save(syncState);
        form.setSyncState(syncState);

        return mapToResponse(form);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SportsExternalFormResponse> getFormsForEvent(Long eventId) {
        List<SportsExternalForm> forms = formRepo.findByEventId(eventId);
        return forms.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SportsExternalFormResponse getForm(Long formId) {
        SportsExternalForm form = formRepo.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("External form not found: " + formId));
        return mapToResponse(form);
    }

    @Override
    @Transactional(readOnly = true)
    public SportsExternalFormResponse getFormByWebhookToken(String token) {
        SportsExternalForm form = formRepo.findByWebhookToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("External form not found for token: " + token));
        return mapToResponse(form);
    }

    @Override
    @Transactional
    public SportsExternalFormResponse updateFieldMappings(Long formId, List<FieldMappingDto> dtos) {
        SportsExternalForm form = formRepo.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("External form not found: " + formId));

        if (dtos != null && !dtos.isEmpty()) {
            mappingRepo.deleteByFormId(formId);
            List<SportsExternalFormFieldMapping> newMappings = new ArrayList<>();
            for (FieldMappingDto dto : dtos) {
                SportsExternalFormFieldMapping.MappingType mappingType = SportsExternalFormFieldMapping.MappingType.DIRECT;
                if (dto.getMappingType() != null) {
                    try {
                        mappingType = SportsExternalFormFieldMapping.MappingType.valueOf(dto.getMappingType().toUpperCase());
                    } catch (IllegalArgumentException ignored) {}
                }
                newMappings.add(SportsExternalFormFieldMapping.builder()
                        .form(form)
                        .manaFieldKey(dto.getManaFieldKey())
                        .externalQuestionId(dto.getExternalQuestionId())
                        .questionTitle(dto.getQuestionTitle())
                        .mappingType(mappingType)
                        .mappingRulesJson(dto.getMappingRulesJson())
                        .required(Boolean.TRUE.equals(dto.getRequired()))
                        .build());
            }
            mappingRepo.saveAll(newMappings);
            form.setFieldMappings(newMappings);
        }

        return mapToResponse(form);
    }

    @Override
    @Transactional
    public SyncResultDto syncFormResponses(Long formId) {
        SportsExternalForm form = formRepo.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("External form not found: " + formId));

        SportsExternalSyncState syncState = syncStateRepo.findByFormId(formId)
                .orElseGet(() -> SportsExternalSyncState.builder()
                        .form(form)
                        .syncMode(SportsExternalSyncState.SyncMode.POLLING)
                        .build());

        syncState.setSyncStatus(SportsExternalSyncState.SyncStatus.SYNCING);
        syncStateRepo.save(syncState);

        int polled = 0;
        int imported = 0;
        int quarantined = 0;
        int rejected = 0;

        try {
            if (form.getProvider() == SportsExternalForm.FormProvider.GOOGLE_FORMS) {
                List<GoogleFormsApiClient.ExternalFormSubmission> submissions =
                        googleFormsApiClient.fetchResponses(form.getExternalFormId());
                polled = submissions.size();

                for (GoogleFormsApiClient.ExternalFormSubmission sub : submissions) {
                    SportsExternalResponse processed = importProcessor.processResponse(
                            form,
                            sub.getExternalResponseId(),
                            sub.getRespondentEmail(),
                            sub.getSubmittedAt(),
                            sub.getAnswers()
                    );
                    if (processed.getStatus() == SportsExternalResponse.ResponseStatus.IMPORTED) {
                        imported++;
                    } else if (processed.getStatus() == SportsExternalResponse.ResponseStatus.NEEDS_REVIEW) {
                        quarantined++;
                    } else if (processed.getStatus() == SportsExternalResponse.ResponseStatus.REJECTED) {
                        rejected++;
                    }
                }
            }

            syncState.setLastSyncTimestamp(LocalDateTime.now());
            syncState.setTotalResponsesPolled(syncState.getTotalResponsesPolled() + polled);
            syncState.setTotalImported(syncState.getTotalImported() + imported);
            syncState.setTotalQuarantined(syncState.getTotalQuarantined() + quarantined);
            syncState.setTotalRejected(syncState.getTotalRejected() + rejected);
            syncState.setSyncStatus(SportsExternalSyncState.SyncStatus.IDLE);
            syncState.setSyncError(null);
            syncStateRepo.save(syncState);

            return SyncResultDto.builder()
                    .formId(formId)
                    .polledCount(polled)
                    .importedCount(imported)
                    .quarantinedCount(quarantined)
                    .rejectedCount(rejected)
                    .message("Synchronized " + polled + " responses (" + imported + " imported, " + quarantined + " in review).")
                    .build();
        } catch (Exception e) {
            log.error("Failed to sync form responses for form {}: {}", formId, e.getMessage(), e);
            syncState.setSyncStatus(SportsExternalSyncState.SyncStatus.ERROR);
            syncState.setSyncError(e.getMessage());
            syncStateRepo.save(syncState);

            return SyncResultDto.builder()
                    .formId(formId)
                    .polledCount(polled)
                    .importedCount(imported)
                    .quarantinedCount(quarantined)
                    .rejectedCount(rejected)
                    .message("Sync completed with error: " + e.getMessage())
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExternalResponseDto> getResponses(Long formId, String status) {
        List<SportsExternalResponse> responses;
        if (status != null && !status.isBlank()) {
            try {
                SportsExternalResponse.ResponseStatus s = SportsExternalResponse.ResponseStatus.valueOf(status.toUpperCase());
                responses = responseRepo.findByFormIdAndStatusOrderBySubmittedAtDesc(formId, s);
            } catch (IllegalArgumentException e) {
                responses = responseRepo.findByFormIdOrderBySubmittedAtDesc(formId);
            }
        } else {
            responses = responseRepo.findByFormIdOrderBySubmittedAtDesc(formId);
        }

        return responses.stream().map(this::mapToResponseDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExternalResponseDto> getReviewQueue(Long eventId) {
        List<SportsExternalResponse> responses = responseRepo.findByEventIdAndStatusIn(
                eventId,
                List.of(SportsExternalResponse.ResponseStatus.NEEDS_REVIEW, SportsExternalResponse.ResponseStatus.REJECTED)
        );
        return responses.stream().map(this::mapToResponseDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ExternalResponseDto resolveReviewResponse(Long responseId, ResolveReviewRequest request) {
        SportsExternalResponse resp = responseRepo.findById(responseId)
                .orElseThrow(() -> new ResourceNotFoundException("External response not found: " + responseId));

        String action = request.getAction() != null ? request.getAction().toUpperCase() : "APPROVE";

        if ("REJECT".equals(action)) {
            resp.setStatus(SportsExternalResponse.ResponseStatus.REJECTED);
            resp.setReviewReason(request.getReviewNotes() != null ? request.getReviewNotes() : "Rejected by organizer");
            resp.setReviewNotes(request.getReviewNotes());
            resp.setReviewedAt(LocalDateTime.now());
            responseRepo.save(resp);
            return mapToResponseDto(resp);
        }

        // Action is APPROVE, OVERRIDE, or LINK_USER
        SportsExternalForm form = resp.getForm();
        SportsEvent event = form.getEvent();

        Map<String, Object> rawMap = new HashMap<>();
        try {
            rawMap = objectMapper.readValue(resp.getRawPayloadJson(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception ignored) {}

        SportsPlayerCategory category = null;
        if (request.getOverrideCategoryId() != null) {
            category = categoryRepo.findById(request.getOverrideCategoryId()).orElse(null);
        }

        SportsEvent.MatchFormat matchFormat = SportsEvent.MatchFormat.SINGLES;
        if (request.getOverrideMatchType() != null && !request.getOverrideMatchType().isBlank()) {
            try {
                matchFormat = SportsEvent.MatchFormat.valueOf(request.getOverrideMatchType().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        AppUser resolvedUser = null;
        if (request.getResolvedUserId() != null) {
            resolvedUser = userRepo.findById(request.getResolvedUserId()).orElse(null);
        }

        String playerName = String.valueOf(rawMap.getOrDefault("playerName", "Guest Player"));
        String email = resp.getRespondentEmail() != null ? resp.getRespondentEmail() : String.valueOf(rawMap.getOrDefault("email", ""));
        String phone = String.valueOf(rawMap.getOrDefault("phone", ""));
        String flatNumber = String.valueOf(rawMap.getOrDefault("flatNumber", ""));
        String partnerName = String.valueOf(rawMap.getOrDefault("partnerName", ""));
        String partnerPhone = String.valueOf(rawMap.getOrDefault("partnerPhone", ""));

        SportsEventRegistration reg = SportsEventRegistration.builder()
                .event(event)
                .user(resolvedUser)
                .category(category)
                .playerName(playerName)
                .flatNumber(flatNumber)
                .phone(phone)
                .matchType(matchFormat)
                .status(SportsEventRegistration.RegistrationStatus.CONFIRMED)
                .registrationSource(SportsEventRegistration.RegistrationSource.GOOGLE_FORMS)
                .externalFormId(form.getId())
                .externalResponseId(resp.getExternalResponseId())
                .isExternalGuest(resolvedUser == null)
                .build();

        reg = regRepo.save(reg);

        resp.setStatus(SportsExternalResponse.ResponseStatus.IMPORTED);
        resp.setLinkedRegistration(reg);
        resp.setResolvedUser(resolvedUser);
        resp.setReviewReason("Approved with manual override");
        resp.setReviewNotes(request.getReviewNotes());
        resp.setReviewedAt(LocalDateTime.now());
        responseRepo.save(resp);

        // Update sync state
        syncStateRepo.findByFormId(form.getId()).ifPresent(state -> {
            state.setTotalImported(state.getTotalImported() + 1);
            if (state.getTotalQuarantined() > 0) {
                state.setTotalQuarantined(state.getTotalQuarantined() - 1);
            }
            syncStateRepo.save(state);
        });

        return mapToResponseDto(resp);
    }

    @Override
    @Transactional(readOnly = true)
    public String getAppsScriptSnippet(Long formId) {
        SportsExternalForm form = formRepo.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("External form not found: " + formId));
        return googleFormsApiClient.generateAppsScriptSnippet(form.getExternalFormId(), form.getWebhookToken());
    }

    @Override
    @Transactional
    public SportsExternalResponse handleIncomingWebhook(
            String webhookToken,
            String externalResponseId,
            String email,
            LocalDateTime submittedAt,
            Map<String, Object> answers
    ) {
        SportsExternalForm form = formRepo.findByWebhookToken(webhookToken)
                .orElseThrow(() -> new ResourceNotFoundException("No external form matching token: " + webhookToken));

        SportsExternalResponse processed = importProcessor.processResponse(
                form,
                externalResponseId,
                email,
                submittedAt,
                answers
        );

        // Update sync state
        syncStateRepo.findByFormId(form.getId()).ifPresent(state -> {
            state.setTotalResponsesPolled(state.getTotalResponsesPolled() + 1);
            if (processed.getStatus() == SportsExternalResponse.ResponseStatus.IMPORTED) {
                state.setTotalImported(state.getTotalImported() + 1);
            } else if (processed.getStatus() == SportsExternalResponse.ResponseStatus.NEEDS_REVIEW) {
                state.setTotalQuarantined(state.getTotalQuarantined() + 1);
            } else if (processed.getStatus() == SportsExternalResponse.ResponseStatus.REJECTED) {
                state.setTotalRejected(state.getTotalRejected() + 1);
            }
            state.setLastSyncTimestamp(LocalDateTime.now());
            syncStateRepo.save(state);
        });

        return processed;
    }

    private SportsExternalFormResponse mapToResponse(SportsExternalForm form) {
        String webhookUrl = appBaseUrl.replaceAll("/+$", "") + "/api/integrations/google/forms/webhook/" + form.getWebhookToken();

        List<FieldMappingDto> mappings = form.getFieldMappings() != null
                ? form.getFieldMappings().stream().map(m -> FieldMappingDto.builder()
                        .id(m.getId())
                        .manaFieldKey(m.getManaFieldKey())
                        .externalQuestionId(m.getExternalQuestionId())
                        .questionTitle(m.getQuestionTitle())
                        .mappingType(m.getMappingType() != null ? m.getMappingType().name() : "DIRECT")
                        .mappingRulesJson(m.getMappingRulesJson())
                        .required(m.getRequired())
                        .build()).collect(Collectors.toList())
                : Collections.emptyList();

        SportsExternalSyncState syncState = form.getSyncState();
        if (syncState == null && form.getId() != null) {
            syncState = syncStateRepo.findByFormId(form.getId()).orElse(null);
        }

        SportsExternalFormResponse.SyncStateDto syncDto = null;
        if (syncState != null) {
            syncDto = SportsExternalFormResponse.SyncStateDto.builder()
                    .syncMode(syncState.getSyncMode() != null ? syncState.getSyncMode().name() : null)
                    .lastSyncTimestamp(syncState.getLastSyncTimestamp())
                    .totalResponsesPolled(syncState.getTotalResponsesPolled())
                    .totalImported(syncState.getTotalImported())
                    .totalQuarantined(syncState.getTotalQuarantined())
                    .totalRejected(syncState.getTotalRejected())
                    .syncStatus(syncState.getSyncStatus() != null ? syncState.getSyncStatus().name() : "IDLE")
                    .syncError(syncState.getSyncError())
                    .build();
        }

        return SportsExternalFormResponse.builder()
                .id(form.getId())
                .eventId(form.getEvent().getId())
                .eventName(form.getEvent().getName())
                .communityId(form.getCommunity() != null ? form.getCommunity().getId() : null)
                .provider(form.getProvider() != null ? form.getProvider().name() : "GOOGLE_FORMS")
                .externalFormId(form.getExternalFormId())
                .responderUrl(form.getResponderUrl())
                .editUrl(form.getEditUrl())
                .title(form.getTitle())
                .description(form.getDescription())
                .webhookToken(form.getWebhookToken())
                .webhookUrl(webhookUrl)
                .status(form.getStatus() != null ? form.getStatus().name() : "ACTIVE")
                .approvalMode(form.getApprovalMode() != null ? form.getApprovalMode().name() : "TOURNAMENT_DEFAULT")
                .fieldMappings(mappings)
                .syncState(syncDto)
                .createdAt(form.getCreatedAt())
                .updatedAt(form.getUpdatedAt())
                .build();
    }

    private ExternalResponseDto mapToResponseDto(SportsExternalResponse r) {
        return ExternalResponseDto.builder()
                .id(r.getId())
                .formId(r.getForm() != null ? r.getForm().getId() : null)
                .externalResponseId(r.getExternalResponseId())
                .respondentEmail(r.getRespondentEmail())
                .submittedAt(r.getSubmittedAt())
                .rawPayloadJson(r.getRawPayloadJson())
                .status(r.getStatus() != null ? r.getStatus().name() : null)
                .reviewReason(r.getReviewReason())
                .reviewNotes(r.getReviewNotes())
                .validationErrors(r.getValidationErrors())
                .linkedRegistrationId(r.getLinkedRegistration() != null ? r.getLinkedRegistration().getId() : null)
                .linkedPlayerName(r.getLinkedRegistration() != null ? r.getLinkedRegistration().getPlayerName() : null)
                .resolvedUserId(r.getResolvedUser() != null ? r.getResolvedUser().getId() : null)
                .resolvedUserName(r.getResolvedUser() != null ? r.getResolvedUser().getFullName() : null)
                .reviewedByUserName(r.getReviewedBy() != null ? r.getReviewedBy().getFullName() : null)
                .reviewedAt(r.getReviewedAt())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private String fieldToQuestionTitle(String field) {
        return switch (field) {
            case "playerName" -> "Full Name";
            case "email" -> "Email Address";
            case "phone" -> "Phone Number / WhatsApp";
            case "flatNumber" -> "Flat / Apartment Number";
            case "age" -> "Age";
            case "dob" -> "Date of Birth (YYYY-MM-DD)";
            case "gender" -> "Gender";
            case "categoryId" -> "Tournament Category";
            case "matchFormat" -> "Match Format";
            case "partnerName" -> "Doubles Partner Full Name (if applicable)";
            case "partnerPhone" -> "Doubles Partner Phone Number";
            case "cricheroesUrl" -> "CricHeroes Profile Link";
            default -> field;
        };
    }
}
