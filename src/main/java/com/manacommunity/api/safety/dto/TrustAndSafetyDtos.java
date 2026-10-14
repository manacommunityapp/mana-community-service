package com.manacommunity.api.safety.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

public class TrustAndSafetyDtos {

    public enum TrustBadge {
        BRONZE, SILVER, GOLD, PLATINUM
    }

    public enum KycStatus {
        PENDING, VERIFIED, REJECTED, FLAGGED
    }

    public enum IncidentSeverity {
        CRITICAL_P1, HIGH_P2, MEDIUM_P3, LOW_P4
    }

    public enum IncidentStatus {
        REPORTED, INVESTIGATING, CONTAINED, RESOLVED, DISMISSED
    }

    public enum LocationConsentPurpose {
        EMERGENCY_SOS, SECURITY_PATROL, GEOFENCE_GATE, AMENITY_PROXIMITY
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerifiedMembershipDto {
        private Long userId;
        private String fullName;
        private String email;
        private String phone;
        private String flatNumber;
        private String tower;
        private String residentType; // OWNER, TENANT, FAMILY
        private KycStatus kycStatus;
        private String govtIdType;
        private int trustScore; // 0 - 100
        private TrustBadge trustBadge;
        private int tenureMonths;
        private int violationCount;
        private Instant verifiedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VisitorApprovalDto {
        private Long passId;
        private String passCode;
        private String visitorName;
        private String visitorPhone;
        private String vehicleNumber;
        private String flatNumber;
        private String residentName;
        private String purpose;
        private String status; // PENDING, APPROVED, CHECKED_IN, CHECKED_OUT, REJECTED
        private boolean residentDualConfirmed;
        private Instant expectedAt;
        private Instant checkedInAt;
        private Instant checkedOutAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IncidentLifecycleDto {
        private Long incidentId;
        private String incidentCode;
        private String title;
        private String category;
        private IncidentSeverity severity;
        private IncidentStatus status;
        private String location;
        private Long reportedById;
        private String reportedByName;
        private Long assignedToId;
        private String assignedToName;
        private String description;
        private List<String> evidenceUrls;
        private String resolutionNotes;
        private Instant reportedAt;
        private Instant slaDeadline;
        private boolean slaBreached;
        private Instant resolvedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationConsentDto {
        private Long id;
        private Long userId;
        private Long communityId;
        private LocationConsentPurpose purpose;
        private boolean isGranted;
        private String consentVersion;
        private Instant grantedAt;
        private Instant expiresAt;
        private Instant revokedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationAccessVerificationRequest {
        private Long userId;
        private Long communityId;
        private LocationConsentPurpose purpose;
        private String requesterRole; // GUARD, ADMIN, SYSTEM
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationAccessVerificationResponse {
        private boolean allowed;
        private String reason;
        private Instant consentGrantedAt;
        private String auditReference;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditableAdminActionDto {
        private Long id;
        private String action;
        private Long operatorId;
        private String operatorName;
        private String targetResource;
        private String justification;
        private String clientIp;
        private String correlationId;
        private String sha256Hash;
        private Instant timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrustSafetyOverviewDto {
        private int totalVerifiedMembers;
        private int pendingKycCount;
        private double averageTrustScore;
        private int activeVisitorsInside;
        private int openIncidentsCount;
        private int criticalSlaPendingCount;
        private double locationConsentAdherencePercent;
        private long totalAuditedActions;
        private List<VerifiedMembershipDto> recentMemberships;
        private List<VisitorApprovalDto> activeVisitorApprovals;
        private List<IncidentLifecycleDto> activeIncidents;
        private List<AuditableAdminActionDto> recentAuditTrail;
    }
}