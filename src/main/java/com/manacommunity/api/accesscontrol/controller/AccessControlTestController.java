package com.manacommunity.api.accesscontrol.controller;

import com.manacommunity.api.accesscontrol.entity.*;
import com.manacommunity.api.accesscontrol.entity.SmartLockTest.*;
import com.manacommunity.api.accesscontrol.entity.BiometricTest.*;
import com.manacommunity.api.accesscontrol.entity.VehicleAccessTest.*;
import com.manacommunity.api.accesscontrol.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/access-control-test")
@RequiredArgsConstructor
public class AccessControlTestController {

    private final SmartLockTestRepository lockRepo;
    private final BiometricTestRepository bioRepo;
    private final VehicleAccessTestRepository vehicleRepo;

    public record AccessTestDashboard(
            long totalTests, long passed, long failed, long pending, double passRate,
            List<SmartLockTest> smartLockTests,
            List<BiometricTest> biometricTests,
            List<VehicleAccessTest> vehicleTests
    ) {}

    @GetMapping("/dashboard")
    public ResponseEntity<AccessTestDashboard> getDashboard() {
        var locks = lockRepo.findAll();
        var bios = bioRepo.findAll();
        var vehicles = vehicleRepo.findAll();

        List<TestResult> all = new ArrayList<>();
        locks.forEach(l -> all.add(l.getTestResult()));
        bios.forEach(b -> all.add(b.getTestResult()));
        vehicles.forEach(v -> all.add(v.getTestResult()));

        long passed = all.stream().filter(r -> r == TestResult.PASS).count();
        long failed = all.stream().filter(r -> r == TestResult.FAIL).count();
        long pending = all.stream().filter(r -> r == TestResult.PENDING).count();
        double passRate = all.isEmpty() ? 0 : (double) passed / all.size() * 100;

        return ResponseEntity.ok(new AccessTestDashboard(all.size(), passed, failed, pending, passRate, locks, bios, vehicles));
    }

    @PostMapping("/seed-demo")
    public ResponseEntity<Map<String, Object>> seedDemo() {
        lockRepo.deleteAll(); bioRepo.deleteAll(); vehicleRepo.deleteAll();

        lockRepo.saveAll(List.of(
                SmartLockTest.builder().communityId("COMM-001").testCaseCode("SL-001").testCaseName("TOTP pin on offline lock — exact start boundary")
                        .scenario(LockScenario.EXACT_START_BOUNDARY).lockId("LOCK-A305").lockModel("Yale Assure 2 BLE")
                        .flatNumber("A-305").pinType(PinType.TOTP).generatedPin("847291")
                        .validFrom("2026-11-28T14:00:00").validUntil("2026-11-28T16:00:00")
                        .clockDriftToleranceSec(30).actualClockDriftSec(0).internetConnected(false)
                        .pinAccepted(true).boundaryMinuteTest(true).earlyAccessBlocked(true).expiredAccessBlocked(true)
                        .testResult(TestResult.PASS).build(),

                SmartLockTest.builder().communityId("COMM-001").testCaseCode("SL-002").testCaseName("TOTP pin — 5-minute clock drift exceeds tolerance")
                        .scenario(LockScenario.CLOCK_DRIFT_5MIN).lockId("LOCK-B102").lockModel("Samsung SHP-DP609")
                        .flatNumber("B-102").pinType(PinType.TOTP).generatedPin("293847")
                        .validFrom("2026-11-28T10:00:00").validUntil("2026-11-28T12:00:00")
                        .clockDriftToleranceSec(30).actualClockDriftSec(300).internetConnected(false)
                        .pinAccepted(false).boundaryMinuteTest(false).earlyAccessBlocked(false).expiredAccessBlocked(false)
                        .testResult(TestResult.FAIL).failureReason("Lock accepted TOTP despite 5-min drift exceeding 30-sec tolerance. Lock firmware 2.1.3 ignores drift validation.").build(),

                SmartLockTest.builder().communityId("COMM-001").testCaseCode("SL-003").testCaseName("Bluetooth PIN offline — exact expiry boundary")
                        .scenario(LockScenario.EXACT_EXPIRY_BOUNDARY).lockId("LOCK-C401").lockModel("Godrej Advantis BLE")
                        .flatNumber("C-401").pinType(PinType.BLUETOOTH_PIN).generatedPin("5829")
                        .validFrom("2026-11-28T09:00:00").validUntil("2026-11-28T11:00:00")
                        .clockDriftToleranceSec(30).actualClockDriftSec(15).internetConnected(false)
                        .pinAccepted(false).boundaryMinuteTest(true).earlyAccessBlocked(true).expiredAccessBlocked(true)
                        .testResult(TestResult.PASS).build(),

                SmartLockTest.builder().communityId("COMM-001").testCaseCode("SL-004").testCaseName("Expired PIN reuse attempt — 2 hours after expiry")
                        .scenario(LockScenario.EXPIRED_PIN_REUSE).lockId("LOCK-D201").lockModel("Yale Assure 2 BLE")
                        .flatNumber("D-201").pinType(PinType.TOTP).generatedPin("192837")
                        .validFrom("2026-11-28T08:00:00").validUntil("2026-11-28T10:00:00")
                        .clockDriftToleranceSec(30).actualClockDriftSec(0).internetConnected(false)
                        .pinAccepted(false).boundaryMinuteTest(false).earlyAccessBlocked(false).expiredAccessBlocked(true)
                        .testResult(TestResult.PASS).build()
        ));

        bioRepo.saveAll(List.of(
                BiometricTest.builder().communityId("COMM-001").testCaseCode("BIO-001").testCaseName("Face recognition — normal lighting, domestic staff")
                        .scenario(BiometricScenario.FACE_RECOGNITION_NORMAL).gateId("GATE-MAIN").gateName("Main Gate")
                        .subjectName("Kamala Devi").subjectRole("Domestic Help").recognitionType(RecognitionType.FACIAL)
                        .turnstileSyncLatencyMs(180).multiGateSync(false).gatesPropagated(1).totalGates(3).propagationDelayMs(0)
                        .spoofAttempt(false).spoofRejected(false).lightingCondition(LightingCondition.NORMAL)
                        .confidenceScore(0.94).thresholdScore(0.85).accessGranted(true).blacklisted(false)
                        .testResult(TestResult.PASS).build(),

                BiometricTest.builder().communityId("COMM-001").testCaseCode("BIO-002").testCaseName("Anti-spoof — printed photo rejection")
                        .scenario(BiometricScenario.ANTI_SPOOF_PRINTED_PHOTO).gateId("GATE-MAIN").gateName("Main Gate")
                        .subjectName("Unknown").subjectRole("Intruder").recognitionType(RecognitionType.FACIAL)
                        .turnstileSyncLatencyMs(0).multiGateSync(false).gatesPropagated(0).totalGates(3).propagationDelayMs(0)
                        .spoofAttempt(true).spoofMethod("A4 color printout held at camera").spoofRejected(true)
                        .lightingCondition(LightingCondition.NORMAL).confidenceScore(0.12).thresholdScore(0.85)
                        .accessGranted(false).blacklisted(false).testResult(TestResult.PASS).build(),

                BiometricTest.builder().communityId("COMM-001").testCaseCode("BIO-003").testCaseName("Anti-spoof — phone screen replay attack")
                        .scenario(BiometricScenario.ANTI_SPOOF_PHONE_SCREEN).gateId("GATE-SIDE").gateName("Side Gate")
                        .subjectName("Unknown").subjectRole("Intruder").recognitionType(RecognitionType.FACIAL)
                        .turnstileSyncLatencyMs(0).multiGateSync(false).gatesPropagated(0).totalGates(3).propagationDelayMs(0)
                        .spoofAttempt(true).spoofMethod("Samsung Galaxy screen showing WhatsApp video of authorized person").spoofRejected(false)
                        .lightingCondition(LightingCondition.LOW_LIGHT).confidenceScore(0.78).thresholdScore(0.85)
                        .accessGranted(false).blacklisted(false)
                        .testResult(TestResult.FAIL).failureReason("Video not rejected by anti-spoof but scored below threshold. Liveness detection flagged 'uncertain' — should have been 'spoof_detected'. Night IR mode needed.").build(),

                BiometricTest.builder().communityId("COMM-001").testCaseCode("BIO-004").testCaseName("Multi-gate blacklist propagation — domestic staff terminated")
                        .scenario(BiometricScenario.MULTI_GATE_BLACKLIST_SYNC).gateId("GATE-MAIN").gateName("Main Gate")
                        .subjectName("Ramu Prasad").subjectRole("Domestic Help — Terminated").recognitionType(RecognitionType.FACIAL)
                        .turnstileSyncLatencyMs(220).multiGateSync(true).gatesPropagated(2).totalGates(3).propagationDelayMs(4500)
                        .spoofAttempt(false).spoofRejected(false).lightingCondition(LightingCondition.NORMAL)
                        .confidenceScore(0.96).thresholdScore(0.85).accessGranted(false).blacklisted(true)
                        .testResult(TestResult.PASS).build()
        ));

        vehicleRepo.saveAll(List.of(
                VehicleAccessTest.builder().communityId("COMM-001").testCaseCode("VH-001").testCaseName("Anti-passback — double ENTRY without EXIT")
                        .scenario(VehicleScenario.ANTI_PASSBACK_DOUBLE_ENTRY).vehicleNumber("KA-01-AB-1234").rfidTagId("RFID-001")
                        .gateId("GATE-MAIN").gateName("Main Gate Boom").basementLevel("B1")
                        .lastEvent(EventType.ENTRY).attemptedEvent(EventType.ENTRY)
                        .antiPassbackTriggered(true).entryAllowed(false)
                        .visitorParkingSpotsTotal(0).visitorParkingSpotsUsed(0).capacityLimitEnforced(false)
                        .overflowRedirected(false).rfidReadDistanceCm(150).rfidReadTimeMs(80).boomBarrierOpened(false).boomBarrierResponseMs(0)
                        .testResult(TestResult.PASS).build(),

                VehicleAccessTest.builder().communityId("COMM-001").testCaseCode("VH-002").testCaseName("Visitor parking at capacity — B2 full, redirect to B3")
                        .scenario(VehicleScenario.VISITOR_PARKING_OVERFLOW_REDIRECT).vehicleNumber("MH-02-CD-5678").rfidTagId("RFID-VISITOR-TMP")
                        .gateId("GATE-MAIN").gateName("Main Gate Boom").basementLevel("B2")
                        .lastEvent(EventType.NONE).attemptedEvent(EventType.ENTRY)
                        .antiPassbackTriggered(false).entryAllowed(true)
                        .visitorParkingSpotsTotal(20).visitorParkingSpotsUsed(20).capacityLimitEnforced(true)
                        .overflowRedirected(true).redirectedTo("B3 — 8 spots available")
                        .rfidReadDistanceCm(120).rfidReadTimeMs(95).boomBarrierOpened(true).boomBarrierResponseMs(1200)
                        .testResult(TestResult.PASS).build(),

                VehicleAccessTest.builder().communityId("COMM-001").testCaseCode("VH-003").testCaseName("Unregistered vehicle — no RFID tag, manual override")
                        .scenario(VehicleScenario.UNREGISTERED_VEHICLE_ENTRY).vehicleNumber("TN-09-EF-9012").rfidTagId("")
                        .gateId("GATE-MAIN").gateName("Main Gate Boom").basementLevel("")
                        .lastEvent(EventType.NONE).attemptedEvent(EventType.ENTRY)
                        .antiPassbackTriggered(false).entryAllowed(false)
                        .visitorParkingSpotsTotal(20).visitorParkingSpotsUsed(15).capacityLimitEnforced(false)
                        .overflowRedirected(false).rfidReadDistanceCm(0).rfidReadTimeMs(0).boomBarrierOpened(false).boomBarrierResponseMs(0)
                        .testResult(TestResult.PASS).build(),

                VehicleAccessTest.builder().communityId("COMM-001").testCaseCode("VH-004").testCaseName("RFID boom barrier response — >2 sec SLA breach")
                        .scenario(VehicleScenario.BOOM_BARRIER_RESPONSE_TIME).vehicleNumber("KA-01-GH-3456").rfidTagId("RFID-045")
                        .gateId("GATE-SIDE").gateName("Side Gate Boom").basementLevel("B1")
                        .lastEvent(EventType.EXIT).attemptedEvent(EventType.ENTRY)
                        .antiPassbackTriggered(false).entryAllowed(true)
                        .visitorParkingSpotsTotal(0).visitorParkingSpotsUsed(0).capacityLimitEnforced(false)
                        .overflowRedirected(false).rfidReadDistanceCm(180).rfidReadTimeMs(120).boomBarrierOpened(true).boomBarrierResponseMs(3400)
                        .testResult(TestResult.FAIL).failureReason("Boom barrier took 3.4 sec to open (SLA: <2 sec). Motor relay delay on Side Gate — maintenance ticket raised.").build()
        ));

        return ResponseEntity.ok(Map.of("status", "seeded", "smartLockTests", 4, "biometricTests", 4, "vehicleTests", 4));
    }
}
