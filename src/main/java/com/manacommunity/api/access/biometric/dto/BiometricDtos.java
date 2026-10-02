package com.manacommunity.api.access.biometric.dto;

import com.manacommunity.api.access.biometric.BiometricEnums.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BiometricDtos {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerifyFaceRequest {
        @NotBlank(message = "Turnstile identifier is required")
        private String turnstileIdentifier;

        @NotBlank(message = "Face embedding hash is required")
        private String faceEmbeddingHash;

        @NotNull(message = "Confidence score is required")
        private BigDecimal confidenceScore;

        private String snapshotUrl;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerifyFaceResult {
        private String turnstileIdentifier;
        private AccessDecision decision;
        private boolean relayUnlock;
        private int relayUnlockDurationMs;
        private String personName;
        private BiometricPersonType personType;
        private String unitNumber;
        private BigDecimal confidenceScore;
        private String message;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnrollBiometricRequest {
        @NotNull(message = "User ID is required")
        private Long userId;

        @NotNull(message = "Community ID is required")
        private Long communityId;

        @NotNull(message = "Person type is required")
        private BiometricPersonType personType;

        @NotBlank(message = "Person name is required")
        private String personName;

        private String unitNumber;

        @NotBlank(message = "Face embedding is required")
        private String faceEmbeddingHash;

        private String timeWindowStart;
        private String timeWindowEnd;
        private String allowedDays;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BiometricTurnstileDto {
        private Long id;
        private String turnstileIdentifier;
        private String turnstileName;
        private Long communityId;
        private String gateLocation;
        private TurnstileDirection direction;
        private TurnstileStatus status;
        private Integer relayUnlockMs;
        private LocalDateTime lastHeartbeat;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BiometricAccessLogDto {
        private Long id;
        private Long turnstileId;
        private String turnstileName;
        private String personName;
        private BiometricPersonType personType;
        private String unitNumber;
        private BigDecimal confidenceScore;
        private AccessDecision accessDecision;
        private String failureReason;
        private String snapshotUrl;
        private LocalDateTime timestamp;
    }
}
