package com.manacommunity.api.sosemergency.controller;

import com.manacommunity.api.sosemergency.entity.*;
import com.manacommunity.api.sosemergency.entity.ChildExitGuardrailTest.*;
import com.manacommunity.api.sosemergency.entity.PanicSosTest.*;
import com.manacommunity.api.sosemergency.entity.EmergencyBroadcastTest.*;
import com.manacommunity.api.sosemergency.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/sos-emergency-test")
@RequiredArgsConstructor
public class SosEmergencyTestController {

    private final ChildExitGuardrailTestRepository childRepo;
    private final PanicSosTestRepository panicRepo;
    private final EmergencyBroadcastTestRepository broadcastRepo;

    public record SosDashboard(
            long totalTests, long passed, long failed, long pending, double passRate,
            List<ChildExitGuardrailTest> childExitTests,
            List<PanicSosTest> panicTests,
            List<EmergencyBroadcastTest> broadcastTests
    ) {}

    @GetMapping("/dashboard")
    public ResponseEntity<SosDashboard> getDashboard() {
        var children = childRepo.findAll();
        var panics = panicRepo.findAll();
        var broadcasts = broadcastRepo.findAll();

        List<TestResult> all = new ArrayList<>();
        children.forEach(c -> all.add(c.getTestResult()));
        panics.forEach(p -> all.add(p.getTestResult()));
        broadcasts.forEach(b -> all.add(b.getTestResult()));

        long passed = all.stream().filter(r -> r == TestResult.PASS).count();
        long failed = all.stream().filter(r -> r == TestResult.FAIL).count();
        long pending = all.stream().filter(r -> r == TestResult.PENDING).count();
        double passRate = all.isEmpty() ? 0 : (double) passed / all.size() * 100;

        return ResponseEntity.ok(new SosDashboard(all.size(), passed, failed, pending, passRate, children, panics, broadcasts));
    }

    @PostMapping("/seed-demo")
    public ResponseEntity<Map<String, Object>> seedDemo() {
        childRepo.deleteAll(); panicRepo.deleteAll(); broadcastRepo.deleteAll();

        childRepo.saveAll(List.of(
                ChildExitGuardrailTest.builder().communityId("COMM-001").testCaseCode("CE-001").testCaseName("Unaccompanied child exit — alert triggered")
                        .scenario(ChildExitScenario.UNACCOMPANIED_ALERT).childName("Aarav Sharma").childAge(6)
                        .flatNumber("A-305").requestedBy("Guard Rajan").requestedByRole("GUARD")
                        .primaryParentOnlyLock(false).overrideAttempted(false).overrideBlocked(false)
                        .ivrTriggered(true).ivrTimeoutSeconds(0).ivrAnswered(true).ivrCalledNumber("+91-98765-43210")
                        .backupContactTriggered(false).autoAlertToGuard(true)
                        .testResult(TestResult.PASS).build(),

                ChildExitGuardrailTest.builder().communityId("COMM-001").testCaseCode("CE-002").testCaseName("Primary Parent Only lock — grandparent override blocked")
                        .scenario(ChildExitScenario.PRIMARY_PARENT_LOCK_OVERRIDE_ATTEMPT).childName("Ananya Patel").childAge(4)
                        .flatNumber("B-102").requestedBy("Meera Patel (Grandmother)").requestedByRole("FAMILY_MEMBER")
                        .primaryParentOnlyLock(true).overrideAttempted(true).overrideBlocked(true).overrideAttemptedBy("Meera Patel (Grandmother)")
                        .ivrTriggered(true).ivrTimeoutSeconds(0).ivrAnswered(false).ivrCalledNumber("+91-98765-11111")
                        .backupContactTriggered(true).backupContactName("Rahul Patel (Father)").autoAlertToGuard(true)
                        .testResult(TestResult.PASS).build(),

                ChildExitGuardrailTest.builder().communityId("COMM-001").testCaseCode("CE-003").testCaseName("IVR timeout 60s — backup call triggered")
                        .scenario(ChildExitScenario.IVR_TIMEOUT_BACKUP_CALL).childName("Vihaan Singh").childAge(8)
                        .flatNumber("C-401").requestedBy("Guard Sunil").requestedByRole("GUARD")
                        .primaryParentOnlyLock(false).overrideAttempted(false).overrideBlocked(false)
                        .ivrTriggered(true).ivrTimeoutSeconds(60).ivrAnswered(false).ivrCalledNumber("+91-98765-22222")
                        .backupContactTriggered(true).backupContactName("Priya Singh (Aunt)").autoAlertToGuard(true)
                        .testResult(TestResult.PASS).build(),

                ChildExitGuardrailTest.builder().communityId("COMM-001").testCaseCode("CE-004").testCaseName("Unauthorized person pickup attempt")
                        .scenario(ChildExitScenario.UNAUTHORIZED_PERSON_ATTEMPT).childName("Isha Reddy").childAge(5)
                        .flatNumber("D-201").requestedBy("Unknown Male").requestedByRole("UNKNOWN")
                        .primaryParentOnlyLock(true).overrideAttempted(true).overrideBlocked(true).overrideAttemptedBy("Unknown Male — no authorization record")
                        .ivrTriggered(true).ivrTimeoutSeconds(0).ivrAnswered(true).ivrCalledNumber("+91-98765-33333")
                        .backupContactTriggered(false).autoAlertToGuard(true)
                        .testResult(TestResult.FAIL).failureReason("Guard tablet did not show 'HIGH ALERT' banner for unauthorized person. Standard visitor notification shown instead. Priority escalation missing.").build()
        ));

        panicRepo.saveAll(List.of(
                PanicSosTest.builder().communityId("COMM-001").testCaseCode("SOS-001").testCaseName("Elevator SOS — 1-bar 2G, SMS fallback")
                        .scenario(PanicScenario.LOW_SIGNAL_ELEVATOR_SOS).triggeredBy("Mrs. Kamala Devi, A-305")
                        .location("Tower A, Lift #2, Floor 7").signalStrength("2G").signalBars(1)
                        .primaryChannel(FallbackChannel.HTTP_PUSH).fallbackChannel(FallbackChannel.SMS)
                        .httpDelivered(false).httpLatencyMs(15000).smsFallbackTriggered(true).smsDelivered(true).smsLatencyMs(3200)
                        .guardTabletSilentMode(true).sirenOverrideTriggered(true).sirenAudible(true)
                        .totalRecipientsTargeted(3).totalRecipientsReached(3).deliverySuccessPercent(100)
                        .testResult(TestResult.PASS).build(),

                PanicSosTest.builder().communityId("COMM-001").testCaseCode("SOS-002").testCaseName("Guard tablet silent mode — siren override test")
                        .scenario(PanicScenario.GUARD_TABLET_SILENT_SIREN_OVERRIDE).triggeredBy("Mr. Arvind Patel, B-102")
                        .location("Parking B2").signalStrength("4G").signalBars(4)
                        .primaryChannel(FallbackChannel.HTTP_PUSH).fallbackChannel(FallbackChannel.SIREN)
                        .httpDelivered(true).httpLatencyMs(180).smsFallbackTriggered(false).smsDelivered(false).smsLatencyMs(0)
                        .guardTabletSilentMode(true).sirenOverrideTriggered(true).sirenAudible(true)
                        .totalRecipientsTargeted(2).totalRecipientsReached(2).deliverySuccessPercent(100)
                        .testResult(TestResult.PASS).build(),

                PanicSosTest.builder().communityId("COMM-001").testCaseCode("SOS-003").testCaseName("Mass SOS load test — 50 concurrent panic triggers")
                        .scenario(PanicScenario.MASS_SOS_LOAD_TEST).triggeredBy("Load Test Harness")
                        .location("All towers").signalStrength("4G").signalBars(4)
                        .primaryChannel(FallbackChannel.HTTP_PUSH).fallbackChannel(FallbackChannel.SMS)
                        .httpDelivered(true).httpLatencyMs(2400).smsFallbackTriggered(false).smsDelivered(false).smsLatencyMs(0)
                        .guardTabletSilentMode(false).sirenOverrideTriggered(false).sirenAudible(false)
                        .totalRecipientsTargeted(150).totalRecipientsReached(142).deliverySuccessPercent(94)
                        .testResult(TestResult.FAIL).failureReason("8 of 150 push notifications dropped under concurrent load. FCM batch limit hit — need to shard into 500-recipient batches.").build()
        ));

        broadcastRepo.saveAll(List.of(
                EmergencyBroadcastTest.builder().communityId("COMM-001").testCaseCode("EB-001").testCaseName("Tower-specific fire evacuation — Tower B only")
                        .scenario(BroadcastScenario.TOWER_SPECIFIC_EVACUATION).emergencyType(EmergencyType.FIRE)
                        .affectedTower("Tower B").residentsInTower(240).staffInTower(8).activeVisitorsInTower(12).totalTargeted(260).totalReached(255)
                        .falsePositives(0).geoFencingAccurate(true).nonAffectedTowerExcluded(true).nonAffectedResidentsWronglyAlerted(0)
                        .broadcastLatencyMs(1800).channelUsed("HTTP Push + SMS + PA System")
                        .testResult(TestResult.PASS).build(),

                EmergencyBroadcastTest.builder().communityId("COMM-001").testCaseCode("EB-002").testCaseName("Gas leak alert — visitors included in Tower A alert")
                        .scenario(BroadcastScenario.VISITOR_INCLUDED_IN_ALERT).emergencyType(EmergencyType.GAS_LEAK)
                        .affectedTower("Tower A").residentsInTower(320).staffInTower(12).activeVisitorsInTower(25).totalTargeted(357).totalReached(350)
                        .falsePositives(3).geoFencingAccurate(true).nonAffectedTowerExcluded(true).nonAffectedResidentsWronglyAlerted(3)
                        .broadcastLatencyMs(2100).channelUsed("HTTP Push + SMS")
                        .testResult(TestResult.FAIL).failureReason("3 residents from Tower C (adjacent block) received alert due to Bluetooth beacon overlap at tower boundary. Geo-fence radius needs 5m tightening.").build(),

                EmergencyBroadcastTest.builder().communityId("COMM-001").testCaseCode("EB-003").testCaseName("Monthly fire drill — full society, marked as DRILL")
                        .scenario(BroadcastScenario.FALSE_POSITIVE_DRILL).emergencyType(EmergencyType.DRILL)
                        .affectedTower("All Towers").residentsInTower(1200).staffInTower(45).activeVisitorsInTower(60).totalTargeted(1305).totalReached(1280)
                        .falsePositives(0).geoFencingAccurate(true).nonAffectedTowerExcluded(false).nonAffectedResidentsWronglyAlerted(0)
                        .broadcastLatencyMs(4500).channelUsed("HTTP Push + PA System + SMS")
                        .testResult(TestResult.PASS).build()
        ));

        return ResponseEntity.ok(Map.of("status", "seeded", "childExitTests", 4, "panicTests", 3, "broadcastTests", 3));
    }
}
