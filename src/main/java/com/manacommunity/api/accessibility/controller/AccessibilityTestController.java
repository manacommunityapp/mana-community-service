package com.manacommunity.api.accessibility.controller;

import com.manacommunity.api.accessibility.entity.*;
import com.manacommunity.api.accessibility.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accessibility-test")
public class AccessibilityTestController {

    private final WcagComplianceTestRepository wcagRepo;
    private final DeviceFragmentationTestRepository deviceRepo;
    private final OsUpgradeTestRepository osRepo;

    public AccessibilityTestController(WcagComplianceTestRepository wcagRepo,
                                       DeviceFragmentationTestRepository deviceRepo,
                                       OsUpgradeTestRepository osRepo) {
        this.wcagRepo = wcagRepo;
        this.deviceRepo = deviceRepo;
        this.osRepo = osRepo;
    }

    public record AccessibilityDashboard(int totalTests, int passed, int failed, int pending, double passRate,
                                          List<WcagComplianceTest> wcagTests, List<DeviceFragmentationTest> deviceTests,
                                          List<OsUpgradeTest> osTests) {}

    @GetMapping("/dashboard")
    public ResponseEntity<AccessibilityDashboard> getDashboard() {
        var w = wcagRepo.findAll(); var d = deviceRepo.findAll(); var o = osRepo.findAll();
        int total = w.size() + d.size() + o.size();
        if (total == 0) return seedDemo();
        long pass = w.stream().filter(t -> t.getTestResult() == WcagComplianceTest.TestResult.PASS).count()
                + d.stream().filter(t -> t.getTestResult() == DeviceFragmentationTest.TestResult.PASS).count()
                + o.stream().filter(t -> t.getTestResult() == OsUpgradeTest.TestResult.PASS).count();
        return ResponseEntity.ok(new AccessibilityDashboard(total, (int) pass, (int)(total - pass), 0,
                Math.round(pass * 1000.0 / total) / 10.0, w, d, o));
    }

    @PostMapping("/seed-demo")
    public ResponseEntity<AccessibilityDashboard> seedDemo() {
        var w1 = WcagComplianceTest.builder().communityId("COMM-001").testCaseCode("WCG-001").testCaseName("Visitor log — screen reader announces form fields")
                .scenario(WcagComplianceTest.WcagScenario.VISITOR_LOG_SCREEN_READER)
                .pageName("Visitor Pre-Approval").wcagLevel("AA").criterionId("1.3.1").criterionName("Info and Relationships")
                .screenReaderCompatible(true).keyboardNavigable(true).contrastRatio(5.2).requiredContrast(4.5)
                .altTextPresent(true).ariaLabelsCorrect(true).focusIndicatorVisible(true).toolUsed("NVDA + Chrome")
                .testResult(WcagComplianceTest.TestResult.PASS).failureReason("").build();

        var w2 = WcagComplianceTest.builder().communityId("COMM-001").testCaseCode("WCG-002").testCaseName("Payment page — contrast ratio 2.8:1 on amount field")
                .scenario(WcagComplianceTest.WcagScenario.PAYMENT_KEYBOARD_ONLY)
                .pageName("Maintenance Payment").wcagLevel("AA").criterionId("1.4.3").criterionName("Contrast (Minimum)")
                .screenReaderCompatible(true).keyboardNavigable(true).contrastRatio(2.8).requiredContrast(4.5)
                .altTextPresent(true).ariaLabelsCorrect(false).focusIndicatorVisible(false).toolUsed("axe DevTools")
                .testResult(WcagComplianceTest.TestResult.FAIL)
                .failureReason("Amount input field contrast 2.8:1 — below WCAG AA minimum 4.5:1, focus indicator not visible on tab").build();

        var w3 = WcagComplianceTest.builder().communityId("COMM-001").testCaseCode("WCG-003").testCaseName("SOS button — voice control activation")
                .scenario(WcagComplianceTest.WcagScenario.EMERGENCY_SOS_VOICE_CONTROL)
                .pageName("Emergency SOS").wcagLevel("A").criterionId("2.1.1").criterionName("Keyboard")
                .screenReaderCompatible(true).keyboardNavigable(true).contrastRatio(8.1).requiredContrast(4.5)
                .altTextPresent(true).ariaLabelsCorrect(true).focusIndicatorVisible(true).toolUsed("VoiceOver iOS")
                .testResult(WcagComplianceTest.TestResult.PASS).failureReason("").build();

        var d1 = DeviceFragmentationTest.builder().communityId("COMM-001").testCaseCode("DEV-001").testCaseName("Samsung Galaxy M12 — 2GB RAM budget device")
                .scenario(DeviceFragmentationTest.DeviceScenario.BUDGET_ANDROID_2GB_RAM)
                .deviceModel("Samsung Galaxy M12").osName("Android").osVersion("12")
                .screenSize("6.5\" HD+").ramMb(2048).browserEngine("Chromium 109")
                .responsiveLayout(true).touchTargetSize(true).notchSafeArea(false)
                .lowEndPerformance(false).loadTimeSec(8.2).targetLoadTimeSec(3.0).crashFree(false)
                .testResult(DeviceFragmentationTest.TestResult.FAIL)
                .failureReason("Dashboard OOM crash on 2GB device — 8.2s load time, JS heap exceeded 150MB, app killed by Android LMK").build();

        var d2 = DeviceFragmentationTest.builder().communityId("COMM-001").testCaseCode("DEV-002").testCaseName("iPhone SE 3rd gen — small screen layout")
                .scenario(DeviceFragmentationTest.DeviceScenario.IPHONE_SE_SMALL_SCREEN)
                .deviceModel("iPhone SE (3rd gen)").osName("iOS").osVersion("17.2")
                .screenSize("4.7\" Retina").ramMb(4096).browserEngine("WebKit")
                .responsiveLayout(true).touchTargetSize(false).notchSafeArea(true)
                .lowEndPerformance(false).loadTimeSec(2.1).targetLoadTimeSec(3.0).crashFree(true)
                .testResult(DeviceFragmentationTest.TestResult.FAIL)
                .failureReason("Gate pass approve/deny buttons 32x32px — below 44px iOS Human Interface minimum, tap area overlaps").build();

        var d3 = DeviceFragmentationTest.builder().communityId("COMM-001").testCaseCode("DEV-003").testCaseName("Samsung Galaxy Tab S9 — landscape layout")
                .scenario(DeviceFragmentationTest.DeviceScenario.TABLET_LANDSCAPE_LAYOUT)
                .deviceModel("Galaxy Tab S9").osName("Android").osVersion("14")
                .screenSize("11\" WQXGA").ramMb(8192).browserEngine("Chromium 120")
                .responsiveLayout(true).touchTargetSize(true).notchSafeArea(false)
                .lowEndPerformance(false).loadTimeSec(1.4).targetLoadTimeSec(3.0).crashFree(true)
                .testResult(DeviceFragmentationTest.TestResult.PASS).failureReason("").build();

        var o1 = OsUpgradeTest.builder().communityId("COMM-001").testCaseCode("OSU-001").testCaseName("Android 13 → 14 — photo permission change")
                .scenario(OsUpgradeTest.UpgradeScenario.ANDROID_13_TO_14_PERMISSIONS)
                .platform("Android").previousVersion("13").upgradedVersion("14")
                .appLaunchSuccess(true).dataIntact(true).pushTokenRefreshed(true)
                .biometricWorking(true).deepLinksWorking(true).backgroundSyncWorking(true)
                .permissionsRetained(false).deprecatedApiUsed("READ_EXTERNAL_STORAGE")
                .testResult(OsUpgradeTest.TestResult.FAIL)
                .failureReason("Photo picker permission not migrated — visitor photo upload crashes with SecurityException on Android 14").build();

        var o2 = OsUpgradeTest.builder().communityId("COMM-001").testCaseCode("OSU-002").testCaseName("iOS 16 → 17 — push token refresh")
                .scenario(OsUpgradeTest.UpgradeScenario.IOS_16_TO_17_PUSH_CHANGE)
                .platform("iOS").previousVersion("16.6").upgradedVersion("17.0")
                .appLaunchSuccess(true).dataIntact(true).pushTokenRefreshed(true)
                .biometricWorking(true).deepLinksWorking(true).backgroundSyncWorking(true)
                .permissionsRetained(true).deprecatedApiUsed("")
                .testResult(OsUpgradeTest.TestResult.PASS).failureReason("").build();

        var o3 = OsUpgradeTest.builder().communityId("COMM-001").testCaseCode("OSU-003").testCaseName("Android 12 — exact alarm removed for reminders")
                .scenario(OsUpgradeTest.UpgradeScenario.ANDROID_12_EXACT_ALARM_REMOVED)
                .platform("Android").previousVersion("11").upgradedVersion("12")
                .appLaunchSuccess(true).dataIntact(true).pushTokenRefreshed(true)
                .biometricWorking(true).deepLinksWorking(true).backgroundSyncWorking(false)
                .permissionsRetained(true).deprecatedApiUsed("setExactAndAllowWhileIdle")
                .testResult(OsUpgradeTest.TestResult.FAIL)
                .failureReason("Maintenance reminder alarms silently fail — SCHEDULE_EXACT_ALARM permission not declared for Android 12+").build();

        wcagRepo.saveAll(List.of(w1, w2, w3));
        deviceRepo.saveAll(List.of(d1, d2, d3));
        osRepo.saveAll(List.of(o1, o2, o3));

        return ResponseEntity.ok(new AccessibilityDashboard(9, 3, 6, 0, 33.3,
                List.of(w1, w2, w3), List.of(d1, d2, d3), List.of(o1, o2, o3)));
    }
}
