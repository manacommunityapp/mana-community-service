package com.manacommunity.api.chaosresilience.controller;

import com.manacommunity.api.chaosresilience.entity.*;
import com.manacommunity.api.chaosresilience.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chaos-resilience-test")
public class ChaosResilienceTestController {

    private final ThirdPartyOutageTestRepository outageRepo;
    private final BackwardCompatibilityTestRepository compatRepo;
    private final SoakStabilityTestRepository soakRepo;

    public ChaosResilienceTestController(ThirdPartyOutageTestRepository outageRepo,
                                          BackwardCompatibilityTestRepository compatRepo,
                                          SoakStabilityTestRepository soakRepo) {
        this.outageRepo = outageRepo;
        this.compatRepo = compatRepo;
        this.soakRepo = soakRepo;
    }

    public record ChaosDashboard(int totalTests, int passed, int failed, int pending, double passRate,
                                   List<ThirdPartyOutageTest> outageTests, List<BackwardCompatibilityTest> compatTests,
                                   List<SoakStabilityTest> soakTests) {}

    @GetMapping("/dashboard")
    public ResponseEntity<ChaosDashboard> getDashboard() {
        var o = outageRepo.findAll(); var c = compatRepo.findAll(); var s = soakRepo.findAll();
        int total = o.size() + c.size() + s.size();
        if (total == 0) return seedDemo();
        long pass = o.stream().filter(t -> t.getTestResult() == ThirdPartyOutageTest.TestResult.PASS).count()
                + c.stream().filter(t -> t.getTestResult() == BackwardCompatibilityTest.TestResult.PASS).count()
                + s.stream().filter(t -> t.getTestResult() == SoakStabilityTest.TestResult.PASS).count();
        long fail = total - pass;
        return ResponseEntity.ok(new ChaosDashboard(total, (int) pass, (int) fail, 0, Math.round(pass * 1000.0 / total) / 10.0, o, c, s));
    }

    @PostMapping("/seed-demo")
    public ResponseEntity<ChaosDashboard> seedDemo() {
        var o1 = ThirdPartyOutageTest.builder().communityId("COMM-001").testCaseCode("OUT-001").testCaseName("FCM push 30s delay — SMS fallback")
                .scenario(ThirdPartyOutageTest.OutageScenario.FCM_PUSH_DELAY_30S).serviceName("Firebase Cloud Messaging")
                .injectedHttpStatus(200).injectedDelayMs(30000).injectedErrorType("TIMEOUT")
                .fallbackChannel(ThirdPartyOutageTest.FallbackChannel.SMS).fallbackTriggered(true).fallbackLatencyMs(4500)
                .userErrorMessageGraceful(true).userErrorMessage("Notification sent via SMS").dataLossPrevented(true)
                .retryAttempted(true).retryCount(3)
                .testResult(ThirdPartyOutageTest.TestResult.PASS).failureReason("").build();

        var o2 = ThirdPartyOutageTest.builder().communityId("COMM-001").testCaseCode("OUT-002").testCaseName("Razorpay 502 — payment stuck in PENDING")
                .scenario(ThirdPartyOutageTest.OutageScenario.RAZORPAY_502_ERROR).serviceName("Razorpay")
                .injectedHttpStatus(502).injectedDelayMs(0).injectedErrorType("BAD_GATEWAY")
                .fallbackChannel(ThirdPartyOutageTest.FallbackChannel.NONE).fallbackTriggered(false).fallbackLatencyMs(0)
                .userErrorMessageGraceful(false).userErrorMessage("java.net.SocketException: Connection reset").dataLossPrevented(true)
                .retryAttempted(true).retryCount(3)
                .testResult(ThirdPartyOutageTest.TestResult.FAIL)
                .failureReason("Raw exception shown to user instead of friendly error — payment stuck PENDING with no retry UI").build();

        var o3 = ThirdPartyOutageTest.builder().communityId("COMM-001").testCaseCode("OUT-003").testCaseName("SMS gateway timeout — IVR fallback")
                .scenario(ThirdPartyOutageTest.OutageScenario.SMS_GATEWAY_TIMEOUT).serviceName("Msg91")
                .injectedHttpStatus(504).injectedDelayMs(15000).injectedErrorType("GATEWAY_TIMEOUT")
                .fallbackChannel(ThirdPartyOutageTest.FallbackChannel.IVR_CALL).fallbackTriggered(true).fallbackLatencyMs(8200)
                .userErrorMessageGraceful(true).userErrorMessage("OTP sent via voice call").dataLossPrevented(true)
                .retryAttempted(true).retryCount(2)
                .testResult(ThirdPartyOutageTest.TestResult.PASS).failureReason("").build();

        var c1 = BackwardCompatibilityTest.builder().communityId("COMM-001").testCaseCode("BWC-001").testCaseName("6-month old app v4.8 — new API v5.2")
                .scenario(BackwardCompatibilityTest.CompatScenario.SIX_MONTH_OLD_APP_NEW_API)
                .oldAppVersion("4.8.0").currentApiVersion("5.2.0").monthsOld(6)
                .jsonParsingSuccessful(true).unknownFieldsIgnored(12).missingFieldsHandled(3).parsingError("")
                .forceUpdatePromptShown(true).forceUpdateBlocking(false).appFunctionalAfterDismiss(true)
                .testResult(BackwardCompatibilityTest.TestResult.PASS).failureReason("").build();

        var c2 = BackwardCompatibilityTest.builder().communityId("COMM-001").testCaseCode("BWC-002").testCaseName("v4.5 crashes on unknown enum value")
                .scenario(BackwardCompatibilityTest.CompatScenario.ENUM_VALUE_UNKNOWN_HANDLED)
                .oldAppVersion("4.5.0").currentApiVersion("5.2.0").monthsOld(9)
                .jsonParsingSuccessful(false).unknownFieldsIgnored(8).missingFieldsHandled(0)
                .parsingError("com.google.gson.JsonSyntaxException: Unknown enum RFID_BLE for AccessType")
                .forceUpdatePromptShown(true).forceUpdateBlocking(true).appFunctionalAfterDismiss(false)
                .testResult(BackwardCompatibilityTest.TestResult.FAIL)
                .failureReason("App crashes on unknown enum RFID_BLE — Gson strict mode, no @SerializedName fallback").build();

        var c3 = BackwardCompatibilityTest.builder().communityId("COMM-001").testCaseCode("BWC-003").testCaseName("Critical force-update blocks app launch")
                .scenario(BackwardCompatibilityTest.CompatScenario.CRITICAL_FORCE_UPDATE_BLOCKS)
                .oldAppVersion("4.2.0").currentApiVersion("5.2.0").monthsOld(12)
                .jsonParsingSuccessful(false).unknownFieldsIgnored(0).missingFieldsHandled(0).parsingError("")
                .forceUpdatePromptShown(true).forceUpdateBlocking(true).appFunctionalAfterDismiss(false)
                .testResult(BackwardCompatibilityTest.TestResult.PASS).failureReason("").build();

        var s1 = SoakStabilityTest.builder().communityId("COMM-001").testCaseCode("SOK-001").testCaseName("Guard tablet 12hr soak — continuous visitor entries")
                .scenario(SoakStabilityTest.SoakScenario.GUARD_TABLET_12HR_SOAK)
                .deviceModel("Samsung Galaxy Tab A8").testDurationHours(12).simulatedVisitorEntries(1800).qrScansProcessed(1800)
                .memoryStartMb(180).memoryEndMb(340).memoryPeakMb(380).memoryLeakDetected(true).memoryGrowthRatePerHour(13.3)
                .batteryTempStartCelsius(28.5).batteryTempPeakCelsius(41.2).batteryOverheatDetected(false)
                .sqliteCorrupted(false).sqliteIntegrityErrors(0).appCrashes(0).anrEvents(2)
                .testResult(SoakStabilityTest.TestResult.FAIL)
                .failureReason("Memory grew 180→340MB over 12hrs (13.3MB/hr) — Bitmap cache not evicting processed QR frames").build();

        var s2 = SoakStabilityTest.builder().communityId("COMM-001").testCaseCode("SOK-002").testCaseName("Resident app 8hr background — zero leaks")
                .scenario(SoakStabilityTest.SoakScenario.RESIDENT_APP_8HR_BACKGROUND)
                .deviceModel("Xiaomi Redmi Note 12").testDurationHours(8).simulatedVisitorEntries(0).qrScansProcessed(0)
                .memoryStartMb(95).memoryEndMb(102).memoryPeakMb(115).memoryLeakDetected(false).memoryGrowthRatePerHour(0.9)
                .batteryTempStartCelsius(26.0).batteryTempPeakCelsius(29.5).batteryOverheatDetected(false)
                .sqliteCorrupted(false).sqliteIntegrityErrors(0).appCrashes(0).anrEvents(0)
                .testResult(SoakStabilityTest.TestResult.PASS).failureReason("").build();

        outageRepo.saveAll(List.of(o1, o2, o3));
        compatRepo.saveAll(List.of(c1, c2, c3));
        soakRepo.saveAll(List.of(s1, s2));

        return ResponseEntity.ok(new ChaosDashboard(8, 5, 3, 0, 62.5,
                List.of(o1, o2, o3), List.of(c1, c2, c3), List.of(s1, s2)));
    }
}
