package com.manacommunity.api.observability.controller;

import com.manacommunity.api.observability.entity.*;
import com.manacommunity.api.observability.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/observability-test")
public class ObservabilityTestController {

    private final ApiLatencyTestRepository latencyRepo;
    private final ReleaseTelemetryTestRepository releaseRepo;
    private final AlertEscalationTestRepository alertRepo;

    public ObservabilityTestController(ApiLatencyTestRepository latencyRepo,
                                        ReleaseTelemetryTestRepository releaseRepo,
                                        AlertEscalationTestRepository alertRepo) {
        this.latencyRepo = latencyRepo;
        this.releaseRepo = releaseRepo;
        this.alertRepo = alertRepo;
    }

    public record ObservabilityDashboard(int totalTests, int passed, int failed, int pending, double passRate,
                                          List<ApiLatencyTest> latencyTests, List<ReleaseTelemetryTest> releaseTests,
                                          List<AlertEscalationTest> alertTests) {}

    @GetMapping("/dashboard")
    public ResponseEntity<ObservabilityDashboard> getDashboard() {
        var l = latencyRepo.findAll(); var r = releaseRepo.findAll(); var a = alertRepo.findAll();
        int total = l.size() + r.size() + a.size();
        if (total == 0) return ResponseEntity.ok(seedDemo());
        long pass = l.stream().filter(t -> t.getTestResult() == ApiLatencyTest.TestResult.PASS).count()
                + r.stream().filter(t -> t.getTestResult() == ReleaseTelemetryTest.TestResult.PASS).count()
                + a.stream().filter(t -> t.getTestResult() == AlertEscalationTest.TestResult.PASS).count();
        return ResponseEntity.ok(new ObservabilityDashboard(total, (int) pass, (int)(total - pass), 0, Math.round(pass * 1000.0 / total) / 10.0, l, r, a));
    }

    @PostMapping("/seed-demo")
    public ResponseEntity<ObservabilityDashboard> seedDemo() {
        var l1 = ApiLatencyTest.builder().communityId("COMM-001").testCaseCode("LAT-001").testCaseName("Dashboard load P95 within 2s SLA")
                .scenario(ApiLatencyTest.LatencyScenario.DASHBOARD_LOAD_P95)
                .endpoint("/api/dashboard/summary").httpMethod("GET")
                .p50Ms(320).p95Ms(1850).p99Ms(2400).slaMs(2000).slaMet(true)
                .requestCount(5000).errorCount(12).errorRate(0.24)
                .tracingEnabled(true).traceId("trace-abc-123").slowQueryDetected(false).slowQueryMs(0)
                .testResult(ApiLatencyTest.TestResult.PASS).failureReason("").build();

        var l2 = ApiLatencyTest.builder().communityId("COMM-001").testCaseCode("LAT-002").testCaseName("Visitor log API — N+1 slow query detected")
                .scenario(ApiLatencyTest.LatencyScenario.VISITOR_LOG_API_SLOW_QUERY)
                .endpoint("/api/visitors/log").httpMethod("GET")
                .p50Ms(890).p95Ms(4200).p99Ms(6800).slaMs(1000).slaMet(false)
                .requestCount(1200).errorCount(3).errorRate(0.25)
                .tracingEnabled(true).traceId("trace-def-456").slowQueryDetected(true).slowQueryMs(3400)
                .testResult(ApiLatencyTest.TestResult.FAIL)
                .failureReason("N+1 query on visitor_photos join — 3400ms slow query, P95 at 4200ms (SLA: 1000ms)").build();

        var l3 = ApiLatencyTest.builder().communityId("COMM-001").testCaseCode("LAT-003").testCaseName("Real-time gate response under 500ms")
                .scenario(ApiLatencyTest.LatencyScenario.REAL_TIME_GATE_RESPONSE)
                .endpoint("/api/gate/verify").httpMethod("POST")
                .p50Ms(85).p95Ms(220).p99Ms(380).slaMs(500).slaMet(true)
                .requestCount(8000).errorCount(2).errorRate(0.025)
                .tracingEnabled(true).traceId("trace-ghi-789").slowQueryDetected(false).slowQueryMs(0)
                .testResult(ApiLatencyTest.TestResult.PASS).failureReason("").build();

        var r1 = ReleaseTelemetryTest.builder().communityId("COMM-001").testCaseCode("REL-001").testCaseName("Staged rollout 5% — crash rate stable")
                .scenario(ReleaseTelemetryTest.TelemetryScenario.STAGED_ROLLOUT_5_PERCENT)
                .releaseVersion("4.2.1").previousVersion("4.2.0").rolloutPercent(5)
                .crashCount(3).crashRate(0.12).previousCrashRate(0.11).crashRateRegression(false)
                .anrCount(1).anrRate(0.04).featureFlagEnabled(true).featureFlagName("new_gate_ui")
                .adoptionPercent(5).autoRollbackTriggered(false)
                .testResult(ReleaseTelemetryTest.TestResult.PASS).failureReason("").build();

        var r2 = ReleaseTelemetryTest.builder().communityId("COMM-001").testCaseCode("REL-002").testCaseName("Crash rate regression — 0.11% → 0.89% at 25% rollout")
                .scenario(ReleaseTelemetryTest.TelemetryScenario.CRASH_RATE_REGRESSION_DETECTED)
                .releaseVersion("4.2.1").previousVersion("4.2.0").rolloutPercent(25)
                .crashCount(42).crashRate(0.89).previousCrashRate(0.11).crashRateRegression(true)
                .anrCount(8).anrRate(0.17).featureFlagEnabled(true).featureFlagName("new_gate_ui")
                .adoptionPercent(25).autoRollbackTriggered(false)
                .testResult(ReleaseTelemetryTest.TestResult.FAIL)
                .failureReason("Crash rate jumped 8x at 25% rollout (0.89% vs 0.11% baseline) — auto-rollback threshold 1.0% not triggered yet").build();

        var r3 = ReleaseTelemetryTest.builder().communityId("COMM-001").testCaseCode("REL-003").testCaseName("Auto-rollback triggered at 1.2% crash rate")
                .scenario(ReleaseTelemetryTest.TelemetryScenario.AUTO_ROLLBACK_THRESHOLD)
                .releaseVersion("4.2.1").previousVersion("4.2.0").rolloutPercent(50)
                .crashCount(120).crashRate(1.2).previousCrashRate(0.11).crashRateRegression(true)
                .anrCount(15).anrRate(0.3).featureFlagEnabled(false).featureFlagName("new_gate_ui")
                .adoptionPercent(0).autoRollbackTriggered(true)
                .testResult(ReleaseTelemetryTest.TestResult.PASS).failureReason("").build();

        var a1 = AlertEscalationTest.builder().communityId("COMM-001").testCaseCode("ALR-001").testCaseName("CPU 92% — PagerDuty alert fired in 30s")
                .scenario(AlertEscalationTest.AlertScenario.CPU_THRESHOLD_ALERT_FIRED)
                .alertName("High CPU Usage").severity("Warning").channel("PagerDuty")
                .triggerThreshold(80).actualValue(92).alertFired(true).alertDelaySeconds(30)
                .escalated(true).escalatedTo("DevOps Lead").escalationDelayMinutes(5)
                .acknowledged(true).resolved(true).mttrMinutes(12)
                .testResult(AlertEscalationTest.TestResult.PASS).failureReason("").build();

        var a2 = AlertEscalationTest.builder().communityId("COMM-001").testCaseCode("ALR-002").testCaseName("Disk 95% full — alert not escalated")
                .scenario(AlertEscalationTest.AlertScenario.DISK_FULL_NOT_ESCALATED)
                .alertName("Disk Space Critical").severity("Critical").channel("Slack")
                .triggerThreshold(90).actualValue(95).alertFired(true).alertDelaySeconds(45)
                .escalated(false).escalatedTo("").escalationDelayMinutes(0)
                .acknowledged(false).resolved(false).mttrMinutes(0)
                .testResult(AlertEscalationTest.TestResult.FAIL)
                .failureReason("Critical disk alert fired but not escalated to PagerDuty — Slack-only channel, no oncall acknowledgement after 2 hours").build();

        var a3 = AlertEscalationTest.builder().communityId("COMM-001").testCaseCode("ALR-003").testCaseName("Payment failure spike — silent, no alert configured")
                .scenario(AlertEscalationTest.AlertScenario.SILENT_FAILURE_NO_ALERT)
                .alertName("Payment Gateway Errors").severity("Critical").channel("")
                .triggerThreshold(5).actualValue(47).alertFired(false).alertDelaySeconds(0)
                .escalated(false).escalatedTo("").escalationDelayMinutes(0)
                .acknowledged(false).resolved(false).mttrMinutes(0)
                .testResult(AlertEscalationTest.TestResult.FAIL)
                .failureReason("47 payment failures in 15 min — no alert configured for payment gateway error rate, discovered manually after 3 hours").build();

        latencyRepo.saveAll(List.of(l1, l2, l3));
        releaseRepo.saveAll(List.of(r1, r2, r3));
        alertRepo.saveAll(List.of(a1, a2, a3));

        return ResponseEntity.ok(new ObservabilityDashboard(9, 5, 4, 0, 55.6,
                List.of(l1, l2, l3), List.of(r1, r2, r3), List.of(a1, a2, a3)));
    }
}
