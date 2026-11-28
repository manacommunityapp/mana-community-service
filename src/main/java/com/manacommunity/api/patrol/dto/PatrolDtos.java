package com.manacommunity.api.patrol.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PatrolDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatrolScanSubmission {
        private Long checkpointId;
        private Long shiftId;
        private Long scannedAtDeviceBootMs;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private BigDecimal gpsAccuracyMeters;
        private boolean isMockDetected;
        private Long nfcTapCounter;
        private String nfcCmacSignature;
        private String detectedBleBeaconUuid;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatrolScanResult {
        private Long scanAuditId;
        private Long checkpointId;
        private String checkpointName;
        private boolean accepted;
        private String validityStatus; // VALID, SPOOF_REJECTED, REPLAY_REJECTED, OUT_OF_GEOFENCE, KINEMATIC_TELEPORTATION_SPOOF
        private BigDecimal distanceMeters;
        private BigDecimal calculatedSpeedKmh;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatrolBreachReport {
        private Long breachId;
        private Long checkpointId;
        private String checkpointName;
        private String routeName;
        private String breachType;
        private LocalDateTime deadline;
        private String escalationStatus;
    }
}
