package com.manacommunity.api.sports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalResponseDto {
    private Long id;
    private Long formId;
    private String externalResponseId;
    private String respondentEmail;
    private LocalDateTime submittedAt;
    private String rawPayloadJson;
    private String status;
    private String reviewReason;
    private String reviewNotes;
    private String validationErrors;
    private Long linkedRegistrationId;
    private String linkedPlayerName;
    private Long resolvedUserId;
    private String resolvedUserName;
    private String reviewedByUserName;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}
