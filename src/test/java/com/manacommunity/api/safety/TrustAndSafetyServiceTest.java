package com.manacommunity.api.safety;

import com.manacommunity.api.audit.TamperResistantAuditService;
import com.manacommunity.api.safety.dto.TrustAndSafetyDtos.*;
import com.manacommunity.api.safety.model.LocationConsent;
import com.manacommunity.api.safety.repository.LocationConsentRepository;
import com.manacommunity.api.safety.repository.SecurityIncidentRepository;
import com.manacommunity.api.safety.service.TrustAndSafetyService;
import com.manacommunity.api.visitor.repository.VisitorPassRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("Community Trust and Safety Service Tests")
class TrustAndSafetyServiceTest {

    private LocationConsentRepository consentRepository;
    private SecurityIncidentRepository incidentRepository;
    private VisitorPassRepository visitorPassRepository;
    private TamperResistantAuditService auditService;
    private TrustAndSafetyService trustService;

    @BeforeEach
    void setUp() {
        consentRepository = Mockito.mock(LocationConsentRepository.class);
        incidentRepository = Mockito.mock(SecurityIncidentRepository.class);
        visitorPassRepository = Mockito.mock(VisitorPassRepository.class);
        auditService = Mockito.mock(TamperResistantAuditService.class);

        trustService = new TrustAndSafetyService(
                consentRepository,
                incidentRepository,
                visitorPassRepository,
                auditService
        );
    }

    @Test
    @DisplayName("Should compute dynamic trust scores correctly across membership tiers")
    void testCalculateTrustScore() {
        // 1. Verified owner with 24 months tenure and 0 violations
        int verifiedScore = trustService.calculateTrustScore(KycStatus.VERIFIED, 24, 0, true);
        assertEquals(87, verifiedScore); // 40 baseline + 35 kyc + 12 tenure - 0
        assertEquals(TrustBadge.GOLD, trustService.deriveBadge(verifiedScore));

        // 2. Verified resident with 36 months tenure (max tenure bonus)
        int platinumScore = trustService.calculateTrustScore(KycStatus.VERIFIED, 40, 0, true);
        assertEquals(95, platinumScore); // 40 baseline + 35 kyc + 20 tenure max - 0
        assertEquals(TrustBadge.PLATINUM, trustService.deriveBadge(platinumScore));

        // 3. Flagged resident with violations
        int flaggedScore = trustService.calculateTrustScore(KycStatus.FLAGGED, 6, 2, false);
        assertEquals(0, flaggedScore); // 40 - 25 kyc + 3 tenure - 30 violations = -12 capped to 0
        assertEquals(TrustBadge.BRONZE, trustService.deriveBadge(flaggedScore));
    }

    @Test
    @DisplayName("Should strictly enforce DPDP location consent access controls")
    void testVerifyLocationAccessConsent() {
        Long userId = 401L;

        // Case A: Resident has active granted consent
        when(consentRepository.findByUserIdAndPurpose(userId, LocationConsentPurpose.EMERGENCY_SOS))
                .thenReturn(Optional.of(LocationConsent.builder()
                        .userId(userId)
                        .purpose(LocationConsentPurpose.EMERGENCY_SOS)
                        .isGranted(true)
                        .grantedAt(Instant.now().minusSeconds(100))
                        .expiresAt(Instant.now().plusSeconds(3600 * 24))
                        .build()));

        LocationAccessVerificationResponse respA = trustService.verifyLocationAccess(
                LocationAccessVerificationRequest.builder()
                        .userId(userId)
                        .purpose(LocationConsentPurpose.EMERGENCY_SOS)
                        .requesterRole("GUARD")
                        .build()
        );
        assertTrue(respA.isAllowed());
        assertNotNull(respA.getAuditReference());

        // Case B: Resident has explicitly revoked consent
        when(consentRepository.findByUserIdAndPurpose(userId, LocationConsentPurpose.GEOFENCE_GATE))
                .thenReturn(Optional.of(LocationConsent.builder()
                        .userId(userId)
                        .purpose(LocationConsentPurpose.GEOFENCE_GATE)
                        .isGranted(false)
                        .build()));

        LocationAccessVerificationResponse respB = trustService.verifyLocationAccess(
                LocationAccessVerificationRequest.builder()
                        .userId(userId)
                        .purpose(LocationConsentPurpose.GEOFENCE_GATE)
                        .requesterRole("SYSTEM")
                        .build()
        );
        assertFalse(respB.isAllowed());
        assertTrue(respB.getReason().contains("opted-out / revoked"));

        // Case C: No consent record found (default deny)
        when(consentRepository.findByUserIdAndPurpose(userId, LocationConsentPurpose.SECURITY_PATROL))
                .thenReturn(Optional.empty());

        LocationAccessVerificationResponse respC = trustService.verifyLocationAccess(
                LocationAccessVerificationRequest.builder()
                        .userId(userId)
                        .purpose(LocationConsentPurpose.SECURITY_PATROL)
                        .requesterRole("GUARD")
                        .build()
        );
        assertFalse(respC.isAllowed());
        assertTrue(respC.getReason().contains("DPDP Act 2023"));
    }

    @Test
    @DisplayName("Should aggregate trust and safety command overview with verified metrics")
    void testGetOverview() {
        TrustSafetyOverviewDto overview = trustService.getOverview(1L);

        assertNotNull(overview);
        assertTrue(overview.getTotalVerifiedMembers() > 0);
        assertTrue(overview.getAverageTrustScore() > 50.0);
        assertEquals(100.0, overview.getLocationConsentAdherencePercent());
        assertFalse(overview.getActiveVisitorApprovals().isEmpty());
        assertFalse(overview.getActiveIncidents().isEmpty());
        assertFalse(overview.getRecentAuditTrail().isEmpty());

        // Verify P1 critical incident present
        assertTrue(overview.getActiveIncidents().stream()
                .anyMatch(i -> i.getSeverity() == IncidentSeverity.CRITICAL_P1));
    }
}