package com.manacommunity.api.dataprivacy.controller;

import com.manacommunity.api.dataprivacy.entity.*;
import com.manacommunity.api.dataprivacy.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/data-privacy-test")
public class DataPrivacyTestController {

    private final DpdpConsentTestRepository consentRepo;
    private final CctvForensicsTestRepository cctvRepo;
    private final SecurityAuditTestRepository securityRepo;

    public DataPrivacyTestController(DpdpConsentTestRepository consentRepo,
                                      CctvForensicsTestRepository cctvRepo,
                                      SecurityAuditTestRepository securityRepo) {
        this.consentRepo = consentRepo;
        this.cctvRepo = cctvRepo;
        this.securityRepo = securityRepo;
    }

    public record DataPrivacyDashboard(int totalTests, int passed, int failed, int pending, double passRate,
                                        List<DpdpConsentTest> consentTests, List<CctvForensicsTest> cctvTests,
                                        List<SecurityAuditTest> securityTests) {}

    @GetMapping("/dashboard")
    public ResponseEntity<DataPrivacyDashboard> getDashboard() {
        var c = consentRepo.findAll(); var v = cctvRepo.findAll(); var s = securityRepo.findAll();
        int total = c.size() + v.size() + s.size();
        if (total == 0) return seedDemo();
        long pass = c.stream().filter(t -> t.getTestResult() == DpdpConsentTest.TestResult.PASS).count()
                + v.stream().filter(t -> t.getTestResult() == CctvForensicsTest.TestResult.PASS).count()
                + s.stream().filter(t -> t.getTestResult() == SecurityAuditTest.TestResult.PASS).count();
        return ResponseEntity.ok(new DataPrivacyDashboard(total, (int) pass, (int)(total - pass), 0, Math.round(pass * 1000.0 / total) / 10.0, c, v, s));
    }

    @PostMapping("/seed-demo")
    public ResponseEntity<DataPrivacyDashboard> seedDemo() {
        var c1 = DpdpConsentTest.builder().communityId("COMM-001").testCaseCode("DPD-001").testCaseName("Resident consent collected at registration")
                .scenario(DpdpConsentTest.ConsentScenario.RESIDENT_CONSENT_COLLECTION)
                .dataCategory("Personal + Contact").consentPurpose("Society management, billing, communication")
                .consentCollected(true).consentWithdrawable(true).dataMinimized(true)
                .retentionPolicyEnforced(true).retentionDays(365).actualRetentionDays(340)
                .erasureCompleted(false).childDataProtected(false).childAge(0).parentalConsentObtained(false)
                .testResult(DpdpConsentTest.TestResult.PASS).failureReason("").build();

        var c2 = DpdpConsentTest.builder().communityId("COMM-001").testCaseCode("DPD-002").testCaseName("Data retained 540 days — exceeds 365 day policy")
                .scenario(DpdpConsentTest.ConsentScenario.RETENTION_PERIOD_EXCEEDED)
                .dataCategory("Visitor logs").consentPurpose("Security audit trail")
                .consentCollected(true).consentWithdrawable(true).dataMinimized(false)
                .retentionPolicyEnforced(false).retentionDays(365).actualRetentionDays(540)
                .erasureCompleted(false).childDataProtected(false).childAge(0).parentalConsentObtained(false)
                .testResult(DpdpConsentTest.TestResult.FAIL)
                .failureReason("Visitor logs retained 540 days (policy: 365) — auto-purge job not running, 175 days over limit").build();

        var c3 = DpdpConsentTest.builder().communityId("COMM-001").testCaseCode("DPD-003").testCaseName("Child (age 12) data — parental consent missing")
                .scenario(DpdpConsentTest.ConsentScenario.CHILD_DATA_PARENTAL_CONSENT)
                .dataCategory("Minor resident profile").consentPurpose("Child safety tracking")
                .consentCollected(true).consentWithdrawable(true).dataMinimized(true)
                .retentionPolicyEnforced(true).retentionDays(365).actualRetentionDays(90)
                .erasureCompleted(false).childDataProtected(false).childAge(12).parentalConsentObtained(false)
                .testResult(DpdpConsentTest.TestResult.FAIL)
                .failureReason("Child (12 yrs) profile created without verifiable parental consent — DPDP Act Section 9 violation").build();

        var v1 = CctvForensicsTest.builder().communityId("COMM-001").testCaseCode("CTV-001").testCaseName("CCTV footage encrypted at rest — AES-256")
                .scenario(CctvForensicsTest.ForensicScenario.FOOTAGE_ENCRYPTION_AT_REST)
                .cameraId("CAM-GATE-01").cameraLocation("Main Gate").retentionDays(30)
                .footageEncrypted(true).accessLogged(true).accessRequestedBy("").accessApprovedBy("")
                .watermarked(false).tamperDetected(false).chainOfCustodyMaintained(true)
                .exportFormat("").hashVerified(true)
                .testResult(CctvForensicsTest.TestResult.PASS).failureReason("").build();

        var v2 = CctvForensicsTest.builder().communityId("COMM-001").testCaseCode("CTV-002").testCaseName("Unauthorized download — committee member bypassed approval")
                .scenario(CctvForensicsTest.ForensicScenario.UNAUTHORIZED_DOWNLOAD_BLOCKED)
                .cameraId("CAM-PARK-03").cameraLocation("Parking Basement B1").retentionDays(30)
                .footageEncrypted(true).accessLogged(true).accessRequestedBy("Committee Member Sharma").accessApprovedBy("")
                .watermarked(false).tamperDetected(false).chainOfCustodyMaintained(false)
                .exportFormat("MP4").hashVerified(false)
                .testResult(CctvForensicsTest.TestResult.FAIL)
                .failureReason("Committee member downloaded 4GB footage without secretary approval — access control bypass via admin panel").build();

        var v3 = CctvForensicsTest.builder().communityId("COMM-001").testCaseCode("CTV-003").testCaseName("Police request — watermarked export with chain of custody")
                .scenario(CctvForensicsTest.ForensicScenario.POLICE_REQUEST_COMPLIANCE)
                .cameraId("CAM-LOBBY-02").cameraLocation("Tower A Lobby").retentionDays(30)
                .footageEncrypted(true).accessLogged(true).accessRequestedBy("Inspector Patil, Koramangala PS").accessApprovedBy("Secretary Reddy")
                .watermarked(true).tamperDetected(false).chainOfCustodyMaintained(true)
                .exportFormat("MP4").hashVerified(true)
                .testResult(CctvForensicsTest.TestResult.PASS).failureReason("").build();

        var s1 = SecurityAuditTest.builder().communityId("COMM-001").testCaseCode("SEC-001").testCaseName("SQL injection on visitor search endpoint")
                .scenario(SecurityAuditTest.AuditScenario.SQL_INJECTION_VISITOR_SEARCH)
                .endpoint("/api/visitors/search").httpMethod("GET").attackVector("' OR 1=1 --")
                .inputSanitized(true).sqlInjectionBlocked(true).xssBlocked(false)
                .rateLimitEnforced(true).requestsPerMinute(30).rateLimitThreshold(60)
                .jwtValidated(true).roleEscalationBlocked(false).vulnerabilitySeverity("Critical")
                .testResult(SecurityAuditTest.TestResult.PASS).failureReason("").build();

        var s2 = SecurityAuditTest.builder().communityId("COMM-001").testCaseCode("SEC-002").testCaseName("XSS in noticeboard post — script tag not sanitized")
                .scenario(SecurityAuditTest.AuditScenario.XSS_NOTICEBOARD_POST)
                .endpoint("/api/noticeboard").httpMethod("POST").attackVector("<script>alert('xss')</script>")
                .inputSanitized(false).sqlInjectionBlocked(false).xssBlocked(false)
                .rateLimitEnforced(true).requestsPerMinute(5).rateLimitThreshold(10)
                .jwtValidated(true).roleEscalationBlocked(false).vulnerabilitySeverity("High")
                .testResult(SecurityAuditTest.TestResult.FAIL)
                .failureReason("Script tag rendered in noticeboard HTML — stored XSS vulnerability, affects all residents viewing the post").build();

        var s3 = SecurityAuditTest.builder().communityId("COMM-001").testCaseCode("SEC-003").testCaseName("OTP rate limit — 200 requests in 1 min not blocked")
                .scenario(SecurityAuditTest.AuditScenario.RATE_LIMIT_OTP_ENDPOINT)
                .endpoint("/api/auth/send-otp").httpMethod("POST").attackVector("Automated 200 req/min")
                .inputSanitized(true).sqlInjectionBlocked(false).xssBlocked(false)
                .rateLimitEnforced(false).requestsPerMinute(200).rateLimitThreshold(5)
                .jwtValidated(false).roleEscalationBlocked(false).vulnerabilitySeverity("Critical")
                .testResult(SecurityAuditTest.TestResult.FAIL)
                .failureReason("OTP endpoint allowed 200 requests/min (limit: 5) — rate limiter not applied, SMS cost ₹400 in 1 min").build();

        consentRepo.saveAll(List.of(c1, c2, c3));
        cctvRepo.saveAll(List.of(v1, v2, v3));
        securityRepo.saveAll(List.of(s1, s2, s3));

        return ResponseEntity.ok(new DataPrivacyDashboard(9, 4, 5, 0, 44.4,
                List.of(c1, c2, c3), List.of(v1, v2, v3), List.of(s1, s2, s3)));
    }
}
