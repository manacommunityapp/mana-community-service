package com.manacommunity.api.security.dpdp;

import com.manacommunity.api.privacy.PiiMaskingService;
import com.manacommunity.api.security.dpdp.IdorAccessGuard.*;
import com.manacommunity.api.security.dpdp.QrPasscodeReplayGuard.*;
import com.manacommunity.api.security.dpdp.VisitorDataPurgeScheduler.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DpdpSecurityAndComplianceTest {

    private IdorAccessGuard idorGuard;
    private QrPasscodeReplayGuard replayGuard;
    private VisitorDataPurgeScheduler purgeScheduler;
    private PiiMaskingService maskingService;

    @BeforeEach
    void setUp() {
        idorGuard = new IdorAccessGuard();
        replayGuard = new QrPasscodeReplayGuard();
        purgeScheduler = new VisitorDataPurgeScheduler();
        maskingService = new PiiMaskingService();
    }

    // ==========================================
    // 1. IDOR DEFENSIVE CONTROLS
    // ==========================================

    @Test
    void testIdorCrossSocietyAccessRejectedWithSecurityAlert() {
        AuthenticatedCaller caller = AuthenticatedCaller.builder()
                .userId("USR-101")
                .societyId("SOC-MUMBAI-01")
                .flatNumber("A-402")
                .role(UserRole.RESIDENT)
                .build();

        // Resident in SOC-MUMBAI-01 attempts to view flat/invoice in SOC-DELHI-02
        assertThatThrownBy(() ->
                idorGuard.validateResourceAccess(caller, "SOC-DELHI-02", "B-101")
        ).isInstanceOf(SecurityException.class)
         .hasMessageContaining("cannot access resources in foreign society");

        // Verify security audit trail recorded
        assertThat(idorGuard.getAuditLogs()).hasSize(1);
        SecurityAuditEvent event = idorGuard.getAuditLogs().get(0);
        assertThat(event.getEventType()).isEqualTo("IDOR_CROSS_SOCIETY_ATTEMPT");
        assertThat(event.getCallerUserId()).isEqualTo("USR-101");
        assertThat(event.getAttemptedSocietyId()).isEqualTo("SOC-DELHI-02");
    }

    @Test
    void testIdorCrossFlatAccessRejectedForResidentsWithinSameSociety() {
        AuthenticatedCaller caller = AuthenticatedCaller.builder()
                .userId("USR-202")
                .societyId("SOC-BLR-05")
                .flatNumber("A-101")
                .role(UserRole.RESIDENT)
                .build();

        // Resident in A-101 attempts to read neighbor's dues in A-102
        assertThatThrownBy(() ->
                idorGuard.validateResourceAccess(caller, "SOC-BLR-05", "A-102")
        ).isInstanceOf(SecurityException.class)
         .hasMessageContaining("cannot access data for flat [A-102]");

        assertThat(idorGuard.getAuditLogs()).hasSize(1);
        assertThat(idorGuard.getAuditLogs().get(0).getEventType()).isEqualTo("IDOR_CROSS_FLAT_ATTEMPT");
    }

    @Test
    void testSuperAdminBypassesTenantGuardForLegitimateSystemOps() {
        AuthenticatedCaller admin = AuthenticatedCaller.builder()
                .userId("ADMIN-001")
                .societyId("SYSTEM")
                .role(UserRole.SUPER_ADMIN)
                .build();

        // Super-admin can access any society and flat without throwing exception
        idorGuard.validateResourceAccess(admin, "SOC-HYD-99", "PH-01");
        assertThat(idorGuard.getAuditLogs()).isEmpty();
    }

    // ==========================================
    // 2. QR / PASSCODE REPLAY ATTACK PREVENTION
    // ==========================================

    @Test
    void testQrPasscodeFirstUseSucceedsAndSubsequentReplayFails() {
        Instant now = Instant.now();
        String passId = "PASS-QR-9912";
        String nonce = "crypto-nonce-uuid-77124";

        VisitorPassToken pass = VisitorPassToken.builder()
                .passId(passId)
                .nonce(nonce)
                .societyId("SOC-001")
                .flatNumber("C-302")
                .visitorName("Amazon Delivery Partner")
                .issuedAt(now.minusSeconds(600))
                .expiresAt(now.plusSeconds(1800)) // 30 min validity
                .status(PassStatus.ACTIVE)
                .build();

        replayGuard.registerPass(pass);

        // 1. First scan at Main Gate -> Legitimate entry authorized
        PassValidationResult firstScan = replayGuard.validateAndConsumePass(
                passId, nonce, "GATE-MAIN-01", now
        );

        assertThat(firstScan.getOutcome()).isEqualTo(ValidationOutcome.VALID_ENTRY_GRANTED);
        assertThat(firstScan.isBarrierOpenAuthorized()).isTrue();
        assertThat(firstScan.getSecurityMessage()).contains("Welcome Amazon Delivery Partner");

        VisitorPassToken updatedPass = replayGuard.getPass(passId);
        assertThat(updatedPass.getStatus()).isEqualTo(PassStatus.USED);
        assertThat(updatedPass.getUsedAtGateId()).isEqualTo("GATE-MAIN-01");

        // 2. Adversary replays identical QR code 15 minutes later at Back Gate
        Instant replayTime = now.plusSeconds(900);
        PassValidationResult replayScan = replayGuard.validateAndConsumePass(
                passId, nonce, "GATE-BACK-02", replayTime
        );

        // Replay attack must be blocked!
        assertThat(replayScan.getOutcome()).isEqualTo(ValidationOutcome.REPLAY_ATTACK_DETECTED);
        assertThat(replayScan.isBarrierOpenAuthorized()).isFalse();
        assertThat(replayScan.getSecurityMessage())
                .contains("CRITICAL ALERT: Replay attack detected!")
                .contains("GATE-MAIN-01");
    }

    @Test
    void testExpiredQrPasscodeRejectedEvenOnFirstUse() {
        Instant now = Instant.now();
        String passId = "PASS-EXPIRED-01";
        String nonce = "nonce-exp-99";

        VisitorPassToken pass = VisitorPassToken.builder()
                .passId(passId)
                .nonce(nonce)
                .societyId("SOC-001")
                .flatNumber("B-201")
                .visitorName("Guest - Vikram")
                .issuedAt(now.minusSeconds(3600))
                .expiresAt(now.minusSeconds(60)) // Expired 1 minute ago
                .status(PassStatus.ACTIVE)
                .build();

        replayGuard.registerPass(pass);

        PassValidationResult result = replayGuard.validateAndConsumePass(passId, nonce, "GATE-01", now);
        assertThat(result.getOutcome()).isEqualTo(ValidationOutcome.PASS_EXPIRED);
        assertThat(result.isBarrierOpenAuthorized()).isFalse();
        assertThat(result.getSecurityMessage()).contains("expired");
    }

    // ==========================================
    // 3. PII MASKING (DPDP ACT SECTION 8)
    // ==========================================

    @Test
    void testPiiMaskingRedactsSensitiveFieldsAcrossUntrustedViews() {
        // Mobile Phone Number Masking (keeps first 2, masks middle, keeps last 2)
        assertThat(maskingService.maskPhone("9876543210")).isEqualTo("98******10");

        // Vehicle Plate Masking
        assertThat(maskingService.maskVehicleNumber("TS09AB1234")).isEqualTo("TS09****34");

        // Resident Name Masking
        assertThat(maskingService.maskName("Rajesh Kumar Sharma")).isEqualTo("Rajesh S.");

        // Flat Number Masking
        assertThat(maskingService.maskFlat("B-402")).isEqualTo("B-***");
    }

    // ==========================================
    // 4. AUTOMATED 30/90-DAY DPDP DATA PURGING
    // ==========================================

    @Test
    void testAutomatedThirtyAndNinetyDayVisitorPurge() {
        Instant asOfDate = Instant.parse("2026-11-28T00:00:00Z");

        // Record 1: 10 Days old (Active window -> neither photo nor log purged)
        VisitorLogRecord recentRecord = VisitorLogRecord.builder()
                .entryId("ENT-10D")
                .visitorName("Rohan Mehta")
                .visitorPhone("+919876543210")
                .vehicleNumber("DL01AB9988")
                .photoS3Key("s3://society/photos/ENT-10D.webp")
                .entryTimestamp(asOfDate.minus(10, ChronoUnit.DAYS))
                .photoPurged(false)
                .piiAnonymized(false)
                .build();

        // Record 2: 45 Days old (Photo > 30 days -> photo purged from S3, audit log retained)
        VisitorLogRecord photoPurgeCandidate = VisitorLogRecord.builder()
                .entryId("ENT-45D")
                .visitorName("Courier Agent")
                .visitorPhone("+919812345678")
                .vehicleNumber("KA03CD1122")
                .photoS3Key("s3://society/photos/ENT-45D.webp")
                .entryTimestamp(asOfDate.minus(45, ChronoUnit.DAYS))
                .photoPurged(false)
                .piiAnonymized(false)
                .build();

        // Record 3: 120 Days old (Log > 90 days -> photo purged AND PII anonymized)
        VisitorLogRecord fullPurgeCandidate = VisitorLogRecord.builder()
                .entryId("ENT-120D")
                .visitorName("Maid - Shanti")
                .visitorPhone("+919899887766")
                .vehicleNumber("DL04EF5566")
                .photoS3Key("s3://society/photos/ENT-120D.webp")
                .entryTimestamp(asOfDate.minus(120, ChronoUnit.DAYS))
                .photoPurged(false)
                .piiAnonymized(false)
                .build();

        purgeScheduler.registerVisitorRecord(recentRecord);
        purgeScheduler.registerVisitorRecord(photoPurgeCandidate);
        purgeScheduler.registerVisitorRecord(fullPurgeCandidate);

        // Execute DPDP purge cycle
        PurgeExecutionReport report = purgeScheduler.executeDataPurge(asOfDate);

        assertThat(report.getTotalRecordsEvaluated()).isEqualTo(3);
        assertThat(report.getPhotosPurgedCount()).isEqualTo(2); // Records 2 and 3
        assertThat(report.getLogsAnonymizedCount()).isEqualTo(1); // Record 3 only

        // Verify Record 1: 10 Days old (Intact)
        VisitorLogRecord r1 = purgeScheduler.getRecord("ENT-10D");
        assertThat(r1.isPhotoPurged()).isFalse();
        assertThat(r1.getPhotoS3Key()).isEqualTo("s3://society/photos/ENT-10D.webp");
        assertThat(r1.isPiiAnonymized()).isFalse();
        assertThat(r1.getVisitorName()).isEqualTo("Rohan Mehta");

        // Verify Record 2: 45 Days old (Photo deleted, PII retained for audit)
        VisitorLogRecord r2 = purgeScheduler.getRecord("ENT-45D");
        assertThat(r2.isPhotoPurged()).isTrue();
        assertThat(r2.getPhotoS3Key()).isNull();
        assertThat(r2.isPiiAnonymized()).isFalse();
        assertThat(r2.getVisitorName()).isEqualTo("Courier Agent");

        // Verify Record 3: 120 Days old (Photo deleted AND PII permanently redacted)
        VisitorLogRecord r3 = purgeScheduler.getRecord("ENT-120D");
        assertThat(r3.isPhotoPurged()).isTrue();
        assertThat(r3.getPhotoS3Key()).isNull();
        assertThat(r3.isPiiAnonymized()).isTrue();
        assertThat(r3.getVisitorName()).isEqualTo(VisitorDataPurgeScheduler.PURGED_REDACTED_LABEL);
        assertThat(r3.getVisitorPhone()).isEqualTo(VisitorDataPurgeScheduler.PURGED_REDACTED_LABEL);
        assertThat(r3.getVehicleNumber()).isEqualTo(VisitorDataPurgeScheduler.PURGED_REDACTED_LABEL);
    }
}
