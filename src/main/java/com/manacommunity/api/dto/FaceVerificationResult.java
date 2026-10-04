package com.manacommunity.api.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaceVerificationResult {
    private String decision;
    private Long userId;
    private String userFullName;
    private String userType;
    private String roleName;
    private Double confidenceScore;
    private String reason;
    private Integer relayPulseDurationMs;
    private String turnstileCode;
}
