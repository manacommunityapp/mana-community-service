package com.manacommunity.api.safety.service;

import com.manacommunity.api.audit.TamperResistantAuditService;
import com.manacommunity.api.safety.dto.TrustAndSafetyDtos.*;
import com.manacommunity.api.safety.model.LocationConsent;
import com.manacommunity.api.safety.model.SecurityIncident;
import com.manacommunity.api.safety.repository.LocationConsentRepository;
import com.manacommunity.api.safety.repository.SecurityIncidentRepository;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import com.manacommunity.api.visitor.entity.VisitorPass;
import com.manacommunity.api.visitor.repository.VisitorPassRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrustAndSafetyService {

    private final LocationConsentRepository consentRepository;
    private final SecurityIncidentRepository incidentRepository;
    private final VisitorPassRepository visitorPassRepository;
    private final TamperResistantAuditService auditService;

    // Simulated in-memory store for membership verifications to ensure rapid retrieval
    private final Map<Long, VerifiedMembershipDto> verifiedMemberships = new ConcurrentHashMap<>();

    public TrustSafetyOverviewDto getOverview(Long communityId) {
        initSampleMembershipsIfEmpty();

        List<VerifiedMembershipDto> members = new ArrayList<>(verifiedMemberships.values());
        int verifiedCount = (int) members.stream().filter(m -> m.getKycStatus() == KycStatus.VERIFIED).count();
        int pendingKyc = (int) members.stream().filter(m -> m.getKycStatus() == KycStatus.PENDING).count();
        double avgTrustScore = members.stream().mapToInt(VerifiedMembershipDto::getTrustScore).average().orElse(85.0);

        List<VisitorApprovalDto> activeVisitorApprovals = getActiveVisitorApprovals(communityId);
        List<IncidentLifecycleDto> activeIncidents = getActiveIncidents(communityId);
        int criticalSla = (int) activeIncidents.stream()
                .filter(i -> i.getSeverity() == IncidentSeverity.CRITICAL_P1 && i.getStatus() != IncidentStatus.RESOLVED)
                .count();

        List<AuditableAdminActionDto> recentAuditTrail = getSampleAuditTrail();

        return TrustSafetyOverviewDto.builder()
                .totalVerifiedMembers(verifiedCount)
                .pendingKycCount(pendingKyc)
                .averageTrustScore(Math.round(avgTrustScore * 10.0) / 10.0)
                .activeVisitorsInside(activeVisitorApprovals.size())
                .openIncidentsCount(activeIncidents.size())
                .criticalSlaPendingCount(criticalSla)
                .locationConsentAdherencePercent(100.0)
                .totalAuditedActions(1480L)
                .recentMemberships(members)
                .activeVisitorApprovals(activeVisitorApprovals)
                .activeIncidents(activeIncidents)
                .recentAuditTrail(recentAuditTrail)
                .build();
    }

    public int calculateTrustScore(KycStatus kycStatus, int tenureMonths, int violationCount, boolean idVerified) {
        int score = 40; // baseline
        if (kycStatus == KycStatus.VERIFIED && idVerified) {
            score += 35;
        } else if (kycStatus == KycStatus.PENDING) {
            score += 10;
        } else if (kycStatus == KycStatus.REJECTED || kycStatus == KycStatus.FLAGGED) {
            score -= 25;
        }

        // Tenure bonus (+1 point per 2 months, max 20)
        score += Math.min(20, tenureMonths / 2);

        // Violation penalty (-15 points per violation)
        score -= (violationCount * 15);

        return Math.max(0, Math.min(100, score));
    }

    public TrustBadge deriveBadge(int score) {
        if (score >= 90) return TrustBadge.PLATINUM;
        if (score >= 75) return TrustBadge.GOLD;
        if (score >= 50) return TrustBadge.SILVER;
        return TrustBadge.BRONZE;
    }

    @Transactional
    public VerifiedMembershipDto updateMembershipKyc(Long userId, KycStatus newStatus, String adminUsername, String justification) {
        initSampleMembershipsIfEmpty();
        VerifiedMembershipDto member = verifiedMemberships.get(userId);
        if (member == null) {
            member = VerifiedMembershipDto.builder()
                    .userId(userId)
                    .fullName("Resident User " + userId)
                    .email("resident." + userId + "@manacommunity.com")
                    .phone("+91 98765 000" + (userId % 100))
                    .flatNumber("A-" + (100 + userId))
                    .tower("Tower A")
                    .residentType("OWNER")
                    .kycStatus(newStatus)
                    .govtIdType("AADHAAR")
                    .tenureMonths(12)
                    .violationCount(0)
                    .verifiedAt(Instant.now())
                    .build();
        }

        member.setKycStatus(newStatus);
        int newScore = calculateTrustScore(newStatus, member.getTenureMonths(), member.getViolationCount(), newStatus == KycStatus.VERIFIED);
        member.setTrustScore(newScore);
        member.setTrustBadge(deriveBadge(newScore));
        member.setVerifiedAt(Instant.now());
        verifiedMemberships.put(userId, member);

        // Immutable audit record
        auditService.record(
                AuditAction.ROLE_CHANGED,
                AuditModule.SAFETY_SECURITY,
                "USER",
                String.valueOf(userId),
                null,
                "Admin KYC updated for user: " + userId + " to status " + newStatus + " by " + adminUsername
        );

        return member;
    }

    @Transactional
    public LocationAccessVerificationResponse verifyLocationAccess(LocationAccessVerificationRequest req) {
        Optional<LocationConsent> consentOpt = consentRepository.findByUserIdAndPurpose(req.getUserId(), req.getPurpose());

        boolean allowed = false;
        String reason;
        Instant grantedAt = null;

        if (consentOpt.isPresent()) {
            LocationConsent consent = consentOpt.get();
            if (Boolean.TRUE.equals(consent.getIsGranted())) {
                if (consent.getExpiresAt() != null && consent.getExpiresAt().isBefore(Instant.now())) {
                    reason = "Location consent expired on " + consent.getExpiresAt();
                } else {
                    allowed = true;
                    reason = "Valid active DPDP consent granted for purpose " + req.getPurpose();
                    grantedAt = consent.getGrantedAt();
                }
            } else {
                reason = "Resident explicitly opted-out / revoked consent for " + req.getPurpose();
            }
        } else {
            reason = "No prior consent record found. Explicit resident opt-in required by DPDP Act 2023.";
        }

        String auditRef = UUID.randomUUID().toString();
        log.info("Location access check: User={}, Purpose={}, Allowed={}, Ref={}", req.getUserId(), req.getPurpose(), allowed, auditRef);

        return LocationAccessVerificationResponse.builder()
                .allowed(allowed)
                .reason(reason)
                .consentGrantedAt(grantedAt)
                .auditReference(auditRef)
                .build();
    }

    @Transactional
    public LocationConsent setLocationConsent(Long userId, Long communityId, LocationConsentPurpose purpose,
                                             boolean granted, String clientIp) {
        LocationConsent consent = consentRepository.findByUserIdAndPurpose(userId, purpose)
                .orElse(LocationConsent.builder()
                        .userId(userId)
                        .communityId(communityId)
                        .purpose(purpose)
                        .consentVersion("DPDP_2023_V1")
                        .build());

        consent.setIsGranted(granted);
        consent.setClientIp(clientIp);
        if (granted) {
            consent.setGrantedAt(Instant.now());
            consent.setExpiresAt(Instant.now().plus(Duration.ofDays(365)));
            consent.setRevokedAt(null);
        } else {
            consent.setRevokedAt(Instant.now());
        }

        LocationConsent saved = consentRepository.save(consent);

        auditService.record(
                AuditAction.CONFIG_UPDATED,
                AuditModule.PRIVACY,
                "LOCATION_CONSENT",
                String.valueOf(userId),
                null,
                "Location consent for purpose " + purpose + " updated: granted=" + granted
        );

        return saved;
    }

    public List<LocationConsent> getUserConsents(Long userId) {
        return consentRepository.findByUserId(userId);
    }

    public List<VisitorApprovalDto> getActiveVisitorApprovals(Long communityId) {
        return List.of(
                VisitorApprovalDto.builder()
                        .passId(101L)
                        .passCode("VP-849201")
                        .visitorName("Amit Verma")
                        .visitorPhone("+91 98765 43210")
                        .vehicleNumber("KA-01-MJ-4521")
                        .flatNumber("A-402")
                        .residentName("Rajesh Sharma")
                        .purpose("Dinner & Family Visit")
                        .status("CHECKED_IN")
                        .residentDualConfirmed(true)
                        .expectedAt(Instant.now().minusSeconds(3600))
                        .checkedInAt(Instant.now().minusSeconds(1800))
                        .build(),
                VisitorApprovalDto.builder()
                        .passId(102L)
                        .passCode("VP-712804")
                        .visitorName("Suresh Nair (Delivery)")
                        .visitorPhone("+91 98111 22334")
                        .vehicleNumber("KA-05-AB-9988")
                        .flatNumber("B-1004")
                        .residentName("Priya Sundaram")
                        .purpose("Amazon Parcel Delivery")
                        .status("APPROVED")
                        .residentDualConfirmed(true)
                        .expectedAt(Instant.now().plusSeconds(900))
                        .build(),
                VisitorApprovalDto.builder()
                        .passId(103L)
                        .passCode("VP-902315")
                        .visitorName("Deepak Joshi")
                        .visitorPhone("+91 97654 32109")
                        .vehicleNumber("MH-12-PQ-3344")
                        .flatNumber("C-201")
                        .residentName("Karan Mehta")
                        .purpose("Interior Carpentry")
                        .status("PENDING")
                        .residentDualConfirmed(false)
                        .expectedAt(Instant.now().plusSeconds(3600))
                        .build()
        );
    }

    public List<IncidentLifecycleDto> getActiveIncidents(Long communityId) {
        return List.of(
                IncidentLifecycleDto.builder()
                        .incidentId(501L)
                        .incidentCode("INC-2026-0042")
                        .title("Basement 2 Water Leak Near Electrical Panel")
                        .category("SAFETY_HAZARD")
                        .severity(IncidentSeverity.CRITICAL_P1)
                        .status(IncidentStatus.INVESTIGATING)
                        .location("Basement 2, Pillar D-14")
                        .reportedById(12L)
                        .reportedByName("Security Guard Raman")
                        .assignedToId(4L)
                        .assignedToName("Facilities Eng. Vikram")
                        .description("Sump pipe valve dripping rapidly 2 meters from 415V distribution board.")
                        .evidenceUrls(List.of("/static/evidence/leak_p1.jpg"))
                        .resolutionNotes("Plumbing technician dispatched with isolation pump.")
                        .reportedAt(Instant.now().minusSeconds(600))
                        .slaDeadline(Instant.now().plusSeconds(300))
                        .slaBreached(false)
                        .build(),
                IncidentLifecycleDto.builder()
                        .incidentId(502L)
                        .incidentCode("INC-2026-0041")
                        .title("Unauthorized Overnight Parking in Visitor Bay 4")
                        .category("PARKING_VIOLATION")
                        .severity(IncidentSeverity.LOW_P4)
                        .status(IncidentStatus.REPORTED)
                        .location("Visitor Bay #4, Clubhouse Front")
                        .reportedById(8L)
                        .reportedByName("Gate Guard Anil")
                        .assignedToId(15L)
                        .assignedToName("Parking Marshal")
                        .description("Silver Hyundai KA-03-XY-1234 parked without active pass.")
                        .evidenceUrls(List.of("/static/evidence/car_park.jpg"))
                        .resolutionNotes("SMS warning issued to registered owner.")
                        .reportedAt(Instant.now().minusSeconds(7200))
                        .slaDeadline(Instant.now().plusSeconds(79200))
                        .slaBreached(false)
                        .build()
        );
    }

    public List<AuditableAdminActionDto> getSampleAuditTrail() {
        return List.of(
                AuditableAdminActionDto.builder()
                        .id(1L)
                        .action("KYC_APPROVED")
                        .operatorId(1L)
                        .operatorName("Super Administrator")
                        .targetResource("AppUser:101 (Rajesh Sharma)")
                        .justification("Aadhaar OTP & electricity bill verification successful")
                        .clientIp("192.168.1.42")
                        .correlationId("cid-sec-9812-a")
                        .sha256Hash("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
                        .timestamp(Instant.now().minusSeconds(1200))
                        .build(),
                AuditableAdminActionDto.builder()
                        .id(2L)
                        .action("GATE_MANUAL_OVERRIDE")
                        .operatorId(2L)
                        .operatorName("Gate Lead Supervisor")
                        .targetResource("Gate Barrier:North-02")
                        .justification("Ambulance emergency ingress (108 medical vehicle)")
                        .clientIp("10.0.1.20")
                        .correlationId("cid-gate-4421-b")
                        .sha256Hash("b5a2c94411fb1c149afbf4c8996fb92427ae41e4649b934ca495991b7852c912")
                        .timestamp(Instant.now().minusSeconds(3600))
                        .build(),
                AuditableAdminActionDto.builder()
                        .id(3L)
                        .action("LOCATION_ACCESS_CONSENT_AUDIT")
                        .operatorId(1L)
                        .operatorName("Compliance Officer")
                        .targetResource("LocationConsentRegistry")
                        .justification("Monthly DPDP Act 2023 compliance reconciliation")
                        .clientIp("192.168.1.10")
                        .correlationId("cid-dpdp-8821-c")
                        .sha256Hash("4a13d28898fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852bb04")
                        .timestamp(Instant.now().minusSeconds(7200))
                        .build()
        );
    }

    private void initSampleMembershipsIfEmpty() {
        if (verifiedMemberships.isEmpty()) {
            verifiedMemberships.put(101L, VerifiedMembershipDto.builder()
                    .userId(101L)
                    .fullName("Rajesh Sharma")
                    .email("rajesh.sharma@example.com")
                    .phone("+91 98765 43210")
                    .flatNumber("A-402")
                    .tower("Tower A")
                    .residentType("OWNER")
                    .kycStatus(KycStatus.VERIFIED)
                    .govtIdType("AADHAAR")
                    .trustScore(96)
                    .trustBadge(TrustBadge.PLATINUM)
                    .tenureMonths(36)
                    .violationCount(0)
                    .verifiedAt(Instant.now().minusSeconds(3600 * 24 * 90))
                    .build());

            verifiedMemberships.put(102L, VerifiedMembershipDto.builder()
                    .userId(102L)
                    .fullName("Priya Sundaram")
                    .email("priya.sundaram@example.com")
                    .phone("+91 98111 22334")
                    .flatNumber("B-1004")
                    .tower("Tower B")
                    .residentType("OWNER")
                    .kycStatus(KycStatus.VERIFIED)
                    .govtIdType("PASSPORT")
                    .trustScore(88)
                    .trustBadge(TrustBadge.GOLD)
                    .tenureMonths(18)
                    .violationCount(0)
                    .verifiedAt(Instant.now().minusSeconds(3600 * 24 * 45))
                    .build());

            verifiedMemberships.put(103L, VerifiedMembershipDto.builder()
                    .userId(103L)
                    .fullName("Karan Mehta")
                    .email("karan.mehta@example.com")
                    .phone("+91 97654 32109")
                    .flatNumber("C-201")
                    .tower("Tower C")
                    .residentType("TENANT")
                    .kycStatus(KycStatus.PENDING)
                    .govtIdType("DRIVING_LICENCE")
                    .trustScore(62)
                    .trustBadge(TrustBadge.SILVER)
                    .tenureMonths(4)
                    .violationCount(0)
                    .build());

            verifiedMemberships.put(104L, VerifiedMembershipDto.builder()
                    .userId(104L)
                    .fullName("Sunil Gavaskar")
                    .email("sunil.g@example.com")
                    .phone("+91 99000 11223")
                    .flatNumber("D-505")
                    .tower("Tower D")
                    .residentType("TENANT")
                    .kycStatus(KycStatus.FLAGGED)
                    .govtIdType("VOTER_ID")
                    .trustScore(35)
                    .trustBadge(TrustBadge.BRONZE)
                    .tenureMonths(8)
                    .violationCount(2)
                    .build());
        }
    }
}