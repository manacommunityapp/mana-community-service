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

        public String getTurnstileIdentifier() { return turnstileIdentifier; }
        public void setTurnstileIdentifier(String turnstileIdentifier) { this.turnstileIdentifier = turnstileIdentifier; }
        public String getFaceEmbeddingHash() { return faceEmbeddingHash; }
        public void setFaceEmbeddingHash(String faceEmbeddingHash) { this.faceEmbeddingHash = faceEmbeddingHash; }
        public BigDecimal getConfidenceScore() { return confidenceScore; }
        public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }
        public String getSnapshotUrl() { return snapshotUrl; }
        public void setSnapshotUrl(String snapshotUrl) { this.snapshotUrl = snapshotUrl; }
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

        public static VerifyFaceResultBuilder builder() { return new VerifyFaceResultBuilder(); }
        public static class VerifyFaceResultBuilder {
            private String turnstileIdentifier;
            private AccessDecision decision;
            private boolean relayUnlock;
            private int relayUnlockDurationMs;
            private String personName;
            private BiometricPersonType personType;
            private String unitNumber;
            private BigDecimal confidenceScore;
            private String message;

            public VerifyFaceResultBuilder turnstileIdentifier(String t) { this.turnstileIdentifier = t; return this; }
            public VerifyFaceResultBuilder decision(AccessDecision d) { this.decision = d; return this; }
            public VerifyFaceResultBuilder relayUnlock(boolean r) { this.relayUnlock = r; return this; }
            public VerifyFaceResultBuilder relayUnlockDurationMs(int m) { this.relayUnlockDurationMs = m; return this; }
            public VerifyFaceResultBuilder personName(String p) { this.personName = p; return this; }
            public VerifyFaceResultBuilder personType(BiometricPersonType pt) { this.personType = pt; return this; }
            public VerifyFaceResultBuilder unitNumber(String u) { this.unitNumber = u; return this; }
            public VerifyFaceResultBuilder confidenceScore(BigDecimal c) { this.confidenceScore = c; return this; }
            public VerifyFaceResultBuilder message(String msg) { this.message = msg; return this; }
            public VerifyFaceResult build() {
                return new VerifyFaceResult(turnstileIdentifier, decision, relayUnlock, relayUnlockDurationMs, personName, personType, unitNumber, confidenceScore, message);
            }
        }
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

        @NotBlank(message = "Face embedding hash is required")
        private String faceEmbeddingHash;

        private String faceFeatureVersion;

        private String timeWindowStart;
        private String timeWindowEnd;
        private String allowedDays;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Long getCommunityId() { return communityId; }
        public void setCommunityId(Long communityId) { this.communityId = communityId; }
        public BiometricPersonType getPersonType() { return personType; }
        public void setPersonType(BiometricPersonType personType) { this.personType = personType; }
        public String getPersonName() { return personName; }
        public void setPersonName(String personName) { this.personName = personName; }
        public String getUnitNumber() { return unitNumber; }
        public void setUnitNumber(String unitNumber) { this.unitNumber = unitNumber; }
        public String getFaceEmbeddingHash() { return faceEmbeddingHash; }
        public void setFaceEmbeddingHash(String faceEmbeddingHash) { this.faceEmbeddingHash = faceEmbeddingHash; }
        public String getFaceFeatureVersion() { return faceFeatureVersion; }
        public void setFaceFeatureVersion(String faceFeatureVersion) { this.faceFeatureVersion = faceFeatureVersion; }
        public String getTimeWindowStart() { return timeWindowStart; }
        public void setTimeWindowStart(String timeWindowStart) { this.timeWindowStart = timeWindowStart; }
        public String getTimeWindowEnd() { return timeWindowEnd; }
        public void setTimeWindowEnd(String timeWindowEnd) { this.timeWindowEnd = timeWindowEnd; }
        public String getAllowedDays() { return allowedDays; }
        public void setAllowedDays(String allowedDays) { this.allowedDays = allowedDays; }
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

        public static BiometricTurnstileDtoBuilder builder() { return new BiometricTurnstileDtoBuilder(); }
        public static class BiometricTurnstileDtoBuilder {
            private Long id;
            private String turnstileIdentifier;
            private String turnstileName;
            private Long communityId;
            private String gateLocation;
            private TurnstileDirection direction;
            private TurnstileStatus status;
            private Integer relayUnlockMs;
            private LocalDateTime lastHeartbeat;

            public BiometricTurnstileDtoBuilder id(Long id) { this.id = id; return this; }
            public BiometricTurnstileDtoBuilder turnstileIdentifier(String ti) { this.turnstileIdentifier = ti; return this; }
            public BiometricTurnstileDtoBuilder turnstileName(String tn) { this.turnstileName = tn; return this; }
            public BiometricTurnstileDtoBuilder communityId(Long cid) { this.communityId = cid; return this; }
            public BiometricTurnstileDtoBuilder gateLocation(String gl) { this.gateLocation = gl; return this; }
            public BiometricTurnstileDtoBuilder direction(TurnstileDirection d) { this.direction = d; return this; }
            public BiometricTurnstileDtoBuilder status(TurnstileStatus s) { this.status = s; return this; }
            public BiometricTurnstileDtoBuilder relayUnlockMs(Integer r) { this.relayUnlockMs = r; return this; }
            public BiometricTurnstileDtoBuilder lastHeartbeat(LocalDateTime lh) { this.lastHeartbeat = lh; return this; }
            public BiometricTurnstileDto build() {
                return new BiometricTurnstileDto(id, turnstileIdentifier, turnstileName, communityId, gateLocation, direction, status, relayUnlockMs, lastHeartbeat);
            }
        }
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

        public static BiometricAccessLogDtoBuilder builder() { return new BiometricAccessLogDtoBuilder(); }
        public static class BiometricAccessLogDtoBuilder {
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

            public BiometricAccessLogDtoBuilder id(Long id) { this.id = id; return this; }
            public BiometricAccessLogDtoBuilder turnstileId(Long tid) { this.turnstileId = tid; return this; }
            public BiometricAccessLogDtoBuilder turnstileName(String tn) { this.turnstileName = tn; return this; }
            public BiometricAccessLogDtoBuilder personName(String pn) { this.personName = pn; return this; }
            public BiometricAccessLogDtoBuilder personType(BiometricPersonType pt) { this.personType = pt; return this; }
            public BiometricAccessLogDtoBuilder unitNumber(String un) { this.unitNumber = un; return this; }
            public BiometricAccessLogDtoBuilder confidenceScore(BigDecimal cs) { this.confidenceScore = cs; return this; }
            public BiometricAccessLogDtoBuilder accessDecision(AccessDecision ad) { this.accessDecision = ad; return this; }
            public BiometricAccessLogDtoBuilder failureReason(String fr) { this.failureReason = fr; return this; }
            public BiometricAccessLogDtoBuilder snapshotUrl(String su) { this.snapshotUrl = su; return this; }
            public BiometricAccessLogDtoBuilder timestamp(LocalDateTime ts) { this.timestamp = ts; return this; }
            public BiometricAccessLogDto build() {
                return new BiometricAccessLogDto(id, turnstileId, turnstileName, personName, personType, unitNumber, confidenceScore, accessDecision, failureReason, snapshotUrl, timestamp);
            }
        }
    }
}
