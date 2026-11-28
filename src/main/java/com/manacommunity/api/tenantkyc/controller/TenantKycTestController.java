package com.manacommunity.api.tenantkyc.controller;

import com.manacommunity.api.tenantkyc.entity.*;
import com.manacommunity.api.tenantkyc.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenant-kyc-test")
public class TenantKycTestController {

    private final MoveOutRevocationTestRepository moveOutRepo;
    private final KycDocumentTestRepository kycRepo;
    private final ServiceLiftSlotTestRepository slotRepo;

    public TenantKycTestController(MoveOutRevocationTestRepository moveOutRepo,
                                    KycDocumentTestRepository kycRepo,
                                    ServiceLiftSlotTestRepository slotRepo) {
        this.moveOutRepo = moveOutRepo;
        this.kycRepo = kycRepo;
        this.slotRepo = slotRepo;
    }

    public record TenantKycDashboard(int totalTests, int passed, int failed, int pending, double passRate,
                                      List<MoveOutRevocationTest> moveOutTests, List<KycDocumentTest> kycTests,
                                      List<ServiceLiftSlotTest> slotTests) {}

    @GetMapping("/dashboard")
    public ResponseEntity<TenantKycDashboard> getDashboard() {
        var m = moveOutRepo.findAll(); var k = kycRepo.findAll(); var s = slotRepo.findAll();
        int total = m.size() + k.size() + s.size();
        if (total == 0) return seedDemo();
        long pass = m.stream().filter(t -> t.getTestResult() == MoveOutRevocationTest.TestResult.PASS).count()
                + k.stream().filter(t -> t.getTestResult() == KycDocumentTest.TestResult.PASS).count()
                + s.stream().filter(t -> t.getTestResult() == ServiceLiftSlotTest.TestResult.PASS).count();
        long fail = total - pass;
        return ResponseEntity.ok(new TenantKycDashboard(total, (int) pass, (int) fail, 0, Math.round(pass * 1000.0 / total) / 10.0, m, k, s));
    }

    @PostMapping("/seed-demo")
    public ResponseEntity<TenantKycDashboard> seedDemo() {
        var m1 = MoveOutRevocationTest.builder().communityId("COMM-001").testCaseCode("MVO-001").testCaseName("Clean move-out — all access revoked atomically")
                .scenario(MoveOutRevocationTest.RevocationScenario.CLEAN_MOVE_OUT_ALL_REVOKED)
                .flatNumber("A-401").tenantName("Priya Sharma").appAccessRevoked(true).guestPasscodesInvalidated(true)
                .activePasscodesBeforeMoveOut(3).activePasscodesAfterMoveOut(0).rfidStickersDeactivated(true)
                .dailyHelpUnlinked(true).gatePassBlocked(false).maintenanceDuesPending(false).pendingDuesAmount(0).revocationDelayMs(450)
                .testResult(MoveOutRevocationTest.TestResult.PASS).failureReason("").build();

        var m2 = MoveOutRevocationTest.builder().communityId("COMM-001").testCaseCode("MVO-002").testCaseName("Move-out blocked — ₹4,500 unpaid dues")
                .scenario(MoveOutRevocationTest.RevocationScenario.MOVE_OUT_WITH_PENDING_DUES)
                .flatNumber("B-203").tenantName("Rahul Verma").appAccessRevoked(false).guestPasscodesInvalidated(false)
                .activePasscodesBeforeMoveOut(1).activePasscodesAfterMoveOut(1).rfidStickersDeactivated(false)
                .dailyHelpUnlinked(false).gatePassBlocked(true).maintenanceDuesPending(true).pendingDuesAmount(4500).revocationDelayMs(0)
                .testResult(MoveOutRevocationTest.TestResult.PASS).failureReason("").build();

        var m3 = MoveOutRevocationTest.builder().communityId("COMM-001").testCaseCode("MVO-003").testCaseName("Guest passcode still works 2 hours post move-out")
                .scenario(MoveOutRevocationTest.RevocationScenario.GUEST_PASSCODE_LEAK_AFTER_MOVE_OUT)
                .flatNumber("C-102").tenantName("Ananya Reddy").appAccessRevoked(true).guestPasscodesInvalidated(false)
                .activePasscodesBeforeMoveOut(5).activePasscodesAfterMoveOut(2).rfidStickersDeactivated(true)
                .dailyHelpUnlinked(true).gatePassBlocked(false).maintenanceDuesPending(false).pendingDuesAmount(0).revocationDelayMs(7200000)
                .testResult(MoveOutRevocationTest.TestResult.FAIL).failureReason("2 of 5 guest passcodes still valid — cache TTL not flushed on move-out event").build();

        var k1 = KycDocumentTest.builder().communityId("COMM-001").testCaseCode("KYC-001").testCaseName("Aadhaar upload — OCR extraction")
                .scenario(KycDocumentTest.KycScenario.AADHAAR_UPLOAD_OCR).documentType(KycDocumentTest.DocumentType.AADHAAR)
                .fileName("aadhaar_priya.jpg").fileSizeBytes(1_200_000).maxAllowedSizeBytes(5_000_000).fileSizeValid(true)
                .ocrExtracted(true).ocrAccuracyPercent(97.5).ocrExtractedName("Priya Sharma").actualName("Priya Sharma")
                .watermarkApplied(true).watermarkText("MANA COMMUNITY - FOR VERIFICATION ONLY")
                .managementPreviewRestricted(true).leaseExpiryReminderSent(false).leaseExpiryDate("").reminderDaysBefore(0)
                .testResult(KycDocumentTest.TestResult.PASS).failureReason("").build();

        var k2 = KycDocumentTest.builder().communityId("COMM-001").testCaseCode("KYC-002").testCaseName("Oversize rental agreement — 12MB rejected")
                .scenario(KycDocumentTest.KycScenario.OVERSIZE_FILE_REJECTION).documentType(KycDocumentTest.DocumentType.RENTAL_AGREEMENT)
                .fileName("rental_agreement_scan.pdf").fileSizeBytes(12_500_000).maxAllowedSizeBytes(5_000_000).fileSizeValid(false)
                .ocrExtracted(false).ocrAccuracyPercent(0).ocrExtractedName("").actualName("Rahul Verma")
                .watermarkApplied(false).watermarkText("").managementPreviewRestricted(false)
                .leaseExpiryReminderSent(false).leaseExpiryDate("").reminderDaysBefore(0)
                .testResult(KycDocumentTest.TestResult.PASS).failureReason("").build();

        var k3 = KycDocumentTest.builder().communityId("COMM-001").testCaseCode("KYC-003").testCaseName("11-month lease expiry reminder — not sent")
                .scenario(KycDocumentTest.KycScenario.LEASE_EXPIRY_11_MONTH_REMINDER).documentType(KycDocumentTest.DocumentType.RENTAL_AGREEMENT)
                .fileName("lease_ananya.pdf").fileSizeBytes(2_300_000).maxAllowedSizeBytes(5_000_000).fileSizeValid(true)
                .ocrExtracted(true).ocrAccuracyPercent(92.0).ocrExtractedName("Ananya Reddy").actualName("Ananya Reddy")
                .watermarkApplied(true).watermarkText("MANA COMMUNITY - FOR VERIFICATION ONLY")
                .managementPreviewRestricted(true).leaseExpiryReminderSent(false).leaseExpiryDate("2027-01-15").reminderDaysBefore(30)
                .testResult(KycDocumentTest.TestResult.FAIL).failureReason("Lease expiry reminder not triggered — cron job skipped December batch").build();

        var s1 = ServiceLiftSlotTest.builder().communityId("COMM-001").testCaseCode("SLT-001").testCaseName("2nd move-in within quota — accepted")
                .scenario(ServiceLiftSlotTest.SlotScenario.WITHIN_QUOTA_ACCEPTED)
                .towerName("Tower A").maxMoveInsPerDay(2).currentBookings(1).requestedSlot("10:00–13:00")
                .freightElevatorWindow("10:00–17:00").bookingAccepted(true).overlapDetected(false).overlapWith("")
                .testResult(ServiceLiftSlotTest.TestResult.PASS).failureReason("").build();

        var s2 = ServiceLiftSlotTest.builder().communityId("COMM-001").testCaseCode("SLT-002").testCaseName("3rd move-in exceeds quota — not rejected")
                .scenario(ServiceLiftSlotTest.SlotScenario.QUOTA_EXCEEDED_REJECTED)
                .towerName("Tower B").maxMoveInsPerDay(2).currentBookings(2).requestedSlot("14:00–17:00")
                .freightElevatorWindow("10:00–17:00").bookingAccepted(true).overlapDetected(false).overlapWith("")
                .testResult(ServiceLiftSlotTest.TestResult.FAIL).failureReason("3rd booking accepted despite max 2/day quota — quota check bypassed").build();

        moveOutRepo.saveAll(List.of(m1, m2, m3));
        kycRepo.saveAll(List.of(k1, k2, k3));
        slotRepo.saveAll(List.of(s1, s2));

        return ResponseEntity.ok(new TenantKycDashboard(8, 5, 3, 0, 62.5,
                List.of(m1, m2, m3), List.of(k1, k2, k3), List.of(s1, s2)));
    }
}
