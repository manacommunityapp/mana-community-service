package com.manacommunity.api.noticeboard.controller;

import com.manacommunity.api.noticeboard.entity.*;
import com.manacommunity.api.noticeboard.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/noticeboard-test")
public class NoticeboardTestController {

    private final NoticeDeliveryTestRepository deliveryRepo;
    private final AudienceSegmentTestRepository segmentRepo;
    private final DigitalSignageTestRepository signageRepo;

    public NoticeboardTestController(NoticeDeliveryTestRepository deliveryRepo,
                                     AudienceSegmentTestRepository segmentRepo,
                                     DigitalSignageTestRepository signageRepo) {
        this.deliveryRepo = deliveryRepo;
        this.segmentRepo = segmentRepo;
        this.signageRepo = signageRepo;
    }

    public record NoticeboardDashboard(int totalTests, int passed, int failed, int pending, double passRate,
                                        List<NoticeDeliveryTest> deliveryTests, List<AudienceSegmentTest> segmentTests,
                                        List<DigitalSignageTest> signageTests) {}

    @GetMapping("/dashboard")
    public ResponseEntity<NoticeboardDashboard> getDashboard() {
        var d = deliveryRepo.findAll(); var s = segmentRepo.findAll(); var g = signageRepo.findAll();
        int total = d.size() + s.size() + g.size();
        if (total == 0) return seedDemo();
        long pass = d.stream().filter(t -> t.getTestResult() == NoticeDeliveryTest.TestResult.PASS).count()
                + s.stream().filter(t -> t.getTestResult() == AudienceSegmentTest.TestResult.PASS).count()
                + g.stream().filter(t -> t.getTestResult() == DigitalSignageTest.TestResult.PASS).count();
        return ResponseEntity.ok(new NoticeboardDashboard(total, (int) pass, (int)(total - pass), 0,
                Math.round(pass * 1000.0 / total) / 10.0, d, s, g));
    }

    @PostMapping("/seed-demo")
    public ResponseEntity<NoticeboardDashboard> seedDemo() {
        var d1 = NoticeDeliveryTest.builder().communityId("COMM-001").testCaseCode("NTD-001").testCaseName("AGM notice — 30-day legal delivery with read receipts")
                .scenario(NoticeDeliveryTest.DeliveryScenario.AGM_NOTICE_30_DAY_LEGAL)
                .noticeType("AGM Notice").noticeTitle("Annual General Meeting — 15 April 2024")
                .targetResidents(240).deliveredCount(238).readCount(195).acknowledgedCount(180)
                .deliveryRate(99.2).readRate(81.3).channelUsed("Push+Email+WhatsApp")
                .pushSent(true).smsSent(false).emailSent(true).whatsappSent(true)
                .retryAttempts(2).legalReadReceipt(true)
                .testResult(NoticeDeliveryTest.TestResult.PASS).failureReason("").build();

        var d2 = NoticeDeliveryTest.builder().communityId("COMM-001").testCaseCode("NTD-002").testCaseName("Maintenance hike notice — read receipt not enforced")
                .scenario(NoticeDeliveryTest.DeliveryScenario.MAINTENANCE_HIKE_READ_RECEIPT)
                .noticeType("Maintenance Hike").noticeTitle("Maintenance Revised — ₹4.5/sqft effective July 2024")
                .targetResidents(240).deliveredCount(240).readCount(89).acknowledgedCount(0)
                .deliveryRate(100.0).readRate(37.1).channelUsed("Push+Email")
                .pushSent(true).smsSent(false).emailSent(true).whatsappSent(false)
                .retryAttempts(0).legalReadReceipt(false)
                .testResult(NoticeDeliveryTest.TestResult.FAIL)
                .failureReason("Maintenance hike is a legal notice — read receipt and acknowledgement should be mandatory but was not enforced").build();

        var d3 = NoticeDeliveryTest.builder().communityId("COMM-001").testCaseCode("NTD-003").testCaseName("Emergency water shutdown — instant push to all")
                .scenario(NoticeDeliveryTest.DeliveryScenario.EMERGENCY_WATER_SHUTDOWN_PUSH)
                .noticeType("Emergency").noticeTitle("Water Supply Shutdown — 2 Hours (Pipe Burst)")
                .targetResidents(240).deliveredCount(237).readCount(201).acknowledgedCount(0)
                .deliveryRate(98.8).readRate(83.8).channelUsed("Push+SMS")
                .pushSent(true).smsSent(true).emailSent(false).whatsappSent(false)
                .retryAttempts(1).legalReadReceipt(false)
                .testResult(NoticeDeliveryTest.TestResult.PASS).failureReason("").build();

        var s1 = AudienceSegmentTest.builder().communityId("COMM-001").testCaseCode("SEG-001").testCaseName("Tower B plumbing notice — tower filter")
                .scenario(AudienceSegmentTest.SegmentScenario.TOWER_SPECIFIC_PLUMBING_NOTICE)
                .segmentName("Tower B Residents").segmentCriteria("tower = 'B'")
                .expectedRecipients(80).actualRecipients(80)
                .towerFilter(true).wingFilter(false).floorRangeFilter(false).ownerTenantFilter(false)
                .languageFilter(false).noticeLanguage("English").translationAccurate(true).excludeListApplied(false)
                .testResult(AudienceSegmentTest.TestResult.PASS).failureReason("").build();

        var s2 = AudienceSegmentTest.builder().communityId("COMM-001").testCaseCode("SEG-002").testCaseName("Owner-only AGM voting notice — tenants excluded")
                .scenario(AudienceSegmentTest.SegmentScenario.OWNER_ONLY_AGM_VOTING)
                .segmentName("Flat Owners Only").segmentCriteria("ownership = 'OWNER'")
                .expectedRecipients(180).actualRecipients(180)
                .towerFilter(false).wingFilter(false).floorRangeFilter(false).ownerTenantFilter(true)
                .languageFilter(false).noticeLanguage("English").translationAccurate(true).excludeListApplied(false)
                .testResult(AudienceSegmentTest.TestResult.PASS).failureReason("").build();

        var s3 = AudienceSegmentTest.builder().communityId("COMM-001").testCaseCode("SEG-003").testCaseName("Hindi notice — translation inaccurate for legal terms")
                .scenario(AudienceSegmentTest.SegmentScenario.MULTILINGUAL_HINDI_NOTICE)
                .segmentName("Hindi Preference").segmentCriteria("language_pref = 'hi'")
                .expectedRecipients(95).actualRecipients(95)
                .towerFilter(false).wingFilter(false).floorRangeFilter(false).ownerTenantFilter(false)
                .languageFilter(true).noticeLanguage("Hindi").translationAccurate(false).excludeListApplied(false)
                .testResult(AudienceSegmentTest.TestResult.FAIL)
                .failureReason("Hindi translation of 'corpus fund' rendered as generic 'fund' — legal meaning lost, RERA non-compliant phrasing").build();

        var g1 = DigitalSignageTest.builder().communityId("COMM-001").testCaseCode("DSG-001").testCaseName("Lobby screen auto-rotation — 15s slots")
                .scenario(DigitalSignageTest.SignageScenario.LOBBY_SCREEN_AUTO_ROTATE)
                .screenLocation("Main Lobby").contentType("Notice + Event + Ad")
                .scheduledSlots(8).displayedSlots(8).autoRotation(true).rotationIntervalSec(15)
                .emergencyOverride(false).contentExpired(false).offlineMode(false)
                .resolution("1920x1080").brightnessAutoAdjust(true)
                .testResult(DigitalSignageTest.TestResult.PASS).failureReason("").build();

        var g2 = DigitalSignageTest.builder().communityId("COMM-001").testCaseCode("DSG-002").testCaseName("Expired Diwali banner still on screen after 7 days")
                .scenario(DigitalSignageTest.SignageScenario.EXPIRED_CONTENT_NOT_REMOVED)
                .screenLocation("Clubhouse Entrance").contentType("Event Banner")
                .scheduledSlots(5).displayedSlots(5).autoRotation(true).rotationIntervalSec(10)
                .emergencyOverride(false).contentExpired(true).offlineMode(false)
                .resolution("1920x1080").brightnessAutoAdjust(false)
                .testResult(DigitalSignageTest.TestResult.FAIL)
                .failureReason("Diwali event banner expired 7 days ago — still in rotation, auto-removal cron not triggered").build();

        var g3 = DigitalSignageTest.builder().communityId("COMM-001").testCaseCode("DSG-003").testCaseName("Emergency fire drill override — all screens")
                .scenario(DigitalSignageTest.SignageScenario.EMERGENCY_OVERRIDE_ALL_SCREENS)
                .screenLocation("All Screens").contentType("Emergency Alert")
                .scheduledSlots(1).displayedSlots(1).autoRotation(false).rotationIntervalSec(0)
                .emergencyOverride(true).contentExpired(false).offlineMode(false)
                .resolution("1920x1080").brightnessAutoAdjust(true)
                .testResult(DigitalSignageTest.TestResult.PASS).failureReason("").build();

        deliveryRepo.saveAll(List.of(d1, d2, d3));
        segmentRepo.saveAll(List.of(s1, s2, s3));
        signageRepo.saveAll(List.of(g1, g2, g3));

        return ResponseEntity.ok(new NoticeboardDashboard(9, 6, 3, 0, 66.7,
                List.of(d1, d2, d3), List.of(s1, s2, s3), List.of(g1, g2, g3)));
    }
}
