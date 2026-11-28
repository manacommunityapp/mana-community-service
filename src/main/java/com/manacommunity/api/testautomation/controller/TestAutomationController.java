package com.manacommunity.api.testautomation.controller;

import com.manacommunity.api.testautomation.entity.*;
import com.manacommunity.api.testautomation.entity.ApiContractTest.*;
import com.manacommunity.api.testautomation.entity.MobileUiTest.*;
import com.manacommunity.api.testautomation.entity.HardwareSimulatorTest.*;
import com.manacommunity.api.testautomation.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/test-automation")
@RequiredArgsConstructor
public class TestAutomationController {

    private final ApiContractTestRepository apiRepo;
    private final MobileUiTestRepository mobileRepo;
    private final HardwareSimulatorTestRepository hwRepo;

    public record AutomationDashboard(
            long totalTests, long passed, long failed, long pending, double passRate,
            long totalExecutionTimeMs, int apiSuites, int mobileSuites, int hwSimulators,
            List<ApiContractTest> apiTests,
            List<MobileUiTest> mobileTests,
            List<HardwareSimulatorTest> hardwareTests
    ) {}

    @GetMapping("/dashboard")
    public ResponseEntity<AutomationDashboard> getDashboard() {
        var apis = apiRepo.findAll();
        var mobiles = mobileRepo.findAll();
        var hws = hwRepo.findAll();

        List<TestResult> all = new ArrayList<>();
        apis.forEach(a -> all.add(a.getTestResult()));
        mobiles.forEach(m -> all.add(m.getTestResult()));
        hws.forEach(h -> all.add(h.getTestResult()));

        long passed = all.stream().filter(r -> r == TestResult.PASS).count();
        long failed = all.stream().filter(r -> r == TestResult.FAIL).count();
        long pending = all.stream().filter(r -> r == TestResult.PENDING).count();
        double passRate = all.isEmpty() ? 0 : (double) passed / all.size() * 100;
        long totalTime = apis.stream().mapToLong(ApiContractTest::getExecutionTimeMs).sum()
                + mobiles.stream().mapToLong(MobileUiTest::getExecutionTimeMs).sum()
                + hws.stream().mapToLong(h -> h.getTestDurationSeconds() * 1000).sum();

        return ResponseEntity.ok(new AutomationDashboard(all.size(), passed, failed, pending, passRate,
                totalTime, apis.size(), mobiles.size(), hws.size(), apis, mobiles, hws));
    }

    @PostMapping("/seed-demo")
    public ResponseEntity<Map<String, Object>> seedDemo() {
        apiRepo.deleteAll(); mobileRepo.deleteAll(); hwRepo.deleteAll();

        apiRepo.saveAll(List.of(
                ApiContractTest.builder().communityId("COMM-001").testSuiteCode("API-001").testSuiteName("Billing API regression — 142 endpoints")
                        .framework(AutomationFramework.REST_ASSURED).layer(TestLayer.API_REGRESSION)
                        .serviceName("billing-service").endpointOrTopic("POST /api/invoices/generate").httpMethod("POST")
                        .totalAssertions(428).passedAssertions(425).failedAssertions(3).executionTimeMs(45200)
                        .contractTestEnabled(false).schemaBreakingChange(false)
                        .testResult(TestResult.FAIL).failureReason("3 assertions failed: pro-rata calculation off by ₹1 for leap year Feb (29 days vs 28)").build(),

                ApiContractTest.builder().communityId("COMM-001").testSuiteCode("API-002").testSuiteName("Pact contract — Billing → Notification service")
                        .framework(AutomationFramework.PACT).layer(TestLayer.CONTRACT_CONSUMER)
                        .serviceName("billing-service").endpointOrTopic("kafka:payment-events").httpMethod("")
                        .totalAssertions(24).passedAssertions(24).failedAssertions(0).executionTimeMs(8400)
                        .contractTestEnabled(true).consumerService("notification-service").providerService("billing-service")
                        .schemaBreakingChange(false).testResult(TestResult.PASS).build(),

                ApiContractTest.builder().communityId("COMM-001").testSuiteCode("API-003").testSuiteName("Pact contract — Guard App → Visitor API schema change")
                        .framework(AutomationFramework.PACT).layer(TestLayer.CONTRACT_PROVIDER)
                        .serviceName("visitor-service").endpointOrTopic("GET /api/visitors/expected").httpMethod("GET")
                        .totalAssertions(18).passedAssertions(15).failedAssertions(3).executionTimeMs(6200)
                        .contractTestEnabled(true).consumerService("guard-app").providerService("visitor-service")
                        .schemaBreakingChange(true).breakingChangeDescription("Field 'visitor_photo_url' renamed to 'photo_uri' in v2.3 — Guard App v2.2 still expects old field name")
                        .testResult(TestResult.FAIL).failureReason("Breaking schema change: visitor_photo_url → photo_uri not backward-compatible").build(),

                ApiContractTest.builder().communityId("COMM-001").testSuiteCode("API-004").testSuiteName("Supertest — Wallet API idempotency suite")
                        .framework(AutomationFramework.SUPERTEST).layer(TestLayer.API_REGRESSION)
                        .serviceName("wallet-service").endpointOrTopic("POST /api/wallet/credit").httpMethod("POST")
                        .totalAssertions(56).passedAssertions(56).failedAssertions(0).executionTimeMs(12800)
                        .contractTestEnabled(false).schemaBreakingChange(false)
                        .testResult(TestResult.PASS).build()
        ));

        mobileRepo.saveAll(List.of(
                MobileUiTest.builder().communityId("COMM-001").testSuiteCode("MOB-001").testSuiteName("E2E: Guard creates entry → Push → Resident approves")
                        .framework(MobileFramework.APPIUM).platform(Platform.ANDROID)
                        .flowDescription("Guard App scans visitor → API triggers FCM push → Resident App receives notification → Resident taps Approve → Guard App shows green checkmark")
                        .deviceModel("Samsung Galaxy A14").osVersion("Android 14").mockLocationInjected(true)
                        .mockLocationCoordinates("12.9716° N, 77.5946° E").mockCameraInjected(true).mockCameraImagePath("/fixtures/visitor_face_01.jpg")
                        .totalSteps(8).completedSteps(8).executionTimeMs(34500).screenshotsCapured(8).videoRecorded(true)
                        .pushNotificationVerified(true).crossAppFlowTested(true)
                        .guardAppAction("CREATE_VISITOR_ENTRY").residentAppAction("APPROVE_VISITOR")
                        .testResult(TestResult.PASS).build(),

                MobileUiTest.builder().communityId("COMM-001").testSuiteCode("MOB-002").testSuiteName("iOS: Resident raises complaint with photo attachment")
                        .framework(MobileFramework.XCUITEST).platform(Platform.IOS)
                        .flowDescription("Resident opens Complaints → Taps New Complaint → Selects category → Takes photo → Writes description → Submits → Sees confirmation")
                        .deviceModel("iPhone 15").osVersion("iOS 17.2").mockLocationInjected(false).mockCameraInjected(true)
                        .mockCameraImagePath("/fixtures/complaint_broken_pipe.jpg")
                        .totalSteps(7).completedSteps(7).executionTimeMs(28200).screenshotsCapured(7).videoRecorded(true)
                        .pushNotificationVerified(false).crossAppFlowTested(false)
                        .guardAppAction("").residentAppAction("CREATE_COMPLAINT")
                        .testResult(TestResult.PASS).build(),

                MobileUiTest.builder().communityId("COMM-001").testSuiteCode("MOB-003").testSuiteName("Guard App offline → sync on reconnect")
                        .framework(MobileFramework.ESPRESSO).platform(Platform.ANDROID)
                        .flowDescription("Guard App goes offline → Creates 3 visitor entries in SQLite queue → Reconnects → Entries sync to server → Server confirms all 3")
                        .deviceModel("Samsung Galaxy Tab A8").osVersion("Android 13").mockLocationInjected(true)
                        .mockLocationCoordinates("12.9716° N, 77.5946° E").mockCameraInjected(false)
                        .totalSteps(12).completedSteps(11).executionTimeMs(52000).screenshotsCapured(12).videoRecorded(true)
                        .pushNotificationVerified(false).crossAppFlowTested(false)
                        .guardAppAction("OFFLINE_SYNC").residentAppAction("")
                        .testResult(TestResult.FAIL).failureReason("3rd entry sync failed: SQLite WAL file locked during camera capture. Race condition between photo write and sync thread.").build()
        ));

        hwRepo.saveAll(List.of(
                HardwareSimulatorTest.builder().communityId("COMM-001").testSuiteCode("HW-001").testSuiteName("RFID reader simulator — 200 concurrent readers")
                        .simulatorType(SimulatorType.RFID_READER).simulatorLanguage("Python").concurrentDevices(200)
                        .payloadsPerSecond(400).testDurationSeconds(300).totalPayloadsSent(120000).totalPayloadsProcessed(119850)
                        .droppedPayloads(150).dropRatePercent(0.125)
                        .p50LatencyMs(45).p95LatencyMs(120).p99LatencyMs(380).maxLatencyMs(1200)
                        .stagingEnvironment(true).protocolUsed("MQTT").peakMemoryMb(2400).peakCpuPercent(72.5)
                        .testResult(TestResult.PASS).build(),

                HardwareSimulatorTest.builder().communityId("COMM-001").testSuiteCode("HW-002").testSuiteName("OCPP EV charger simulator — 50 chargers")
                        .simulatorType(SimulatorType.OCPP_EV_CHARGER).simulatorLanguage("Node.js").concurrentDevices(50)
                        .payloadsPerSecond(100).testDurationSeconds(600).totalPayloadsSent(60000).totalPayloadsProcessed(59200)
                        .droppedPayloads(800).dropRatePercent(1.33)
                        .p50LatencyMs(85).p95LatencyMs(250).p99LatencyMs(850).maxLatencyMs(4200)
                        .stagingEnvironment(true).protocolUsed("WebSocket (OCPP 1.6J)").peakMemoryMb(1800).peakCpuPercent(58.3)
                        .testResult(TestResult.FAIL).failureReason("1.33% drop rate exceeds 1% SLA. WebSocket connection pool exhausted at 48 concurrent connections — max pool size 50 too tight.").build(),

                HardwareSimulatorTest.builder().communityId("COMM-001").testSuiteCode("HW-003").testSuiteName("Smart water meter telemetry — 500 meters")
                        .simulatorType(SimulatorType.SMART_WATER_METER).simulatorLanguage("Python").concurrentDevices(500)
                        .payloadsPerSecond(50).testDurationSeconds(900).totalPayloadsSent(45000).totalPayloadsProcessed(45000)
                        .droppedPayloads(0).dropRatePercent(0)
                        .p50LatencyMs(32).p95LatencyMs(78).p99LatencyMs(145).maxLatencyMs(320)
                        .stagingEnvironment(true).protocolUsed("MQTT").peakMemoryMb(800).peakCpuPercent(28.1)
                        .testResult(TestResult.PASS).build()
        ));

        return ResponseEntity.ok(Map.of("status", "seeded", "apiTests", 4, "mobileTests", 3, "hardwareTests", 3));
    }
}
