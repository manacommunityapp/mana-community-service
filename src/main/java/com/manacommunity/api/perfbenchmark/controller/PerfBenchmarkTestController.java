package com.manacommunity.api.perfbenchmark.controller;

import com.manacommunity.api.perfbenchmark.entity.*;
import com.manacommunity.api.perfbenchmark.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/perf-benchmark-test")
public class PerfBenchmarkTestController {

    private final LoadTestResultRepository loadRepo;
    private final SyntheticMonitorTestRepository monitorRepo;
    private final FrontendPerfTestRepository frontendRepo;

    public PerfBenchmarkTestController(LoadTestResultRepository loadRepo,
                                       SyntheticMonitorTestRepository monitorRepo,
                                       FrontendPerfTestRepository frontendRepo) {
        this.loadRepo = loadRepo;
        this.monitorRepo = monitorRepo;
        this.frontendRepo = frontendRepo;
    }

    public record PerfBenchmarkDashboard(int totalTests, int passed, int failed, int pending, double passRate,
                                          List<LoadTestResult> loadTests, List<SyntheticMonitorTest> monitorTests,
                                          List<FrontendPerfTest> frontendTests) {}

    @GetMapping("/dashboard")
    public ResponseEntity<PerfBenchmarkDashboard> getDashboard() {
        var l = loadRepo.findAll(); var m = monitorRepo.findAll(); var f = frontendRepo.findAll();
        int total = l.size() + m.size() + f.size();
        if (total == 0) return seedDemo();
        long pass = l.stream().filter(t -> t.getTestResult() == LoadTestResult.TestResult.PASS).count()
                + m.stream().filter(t -> t.getTestResult() == SyntheticMonitorTest.TestResult.PASS).count()
                + f.stream().filter(t -> t.getTestResult() == FrontendPerfTest.TestResult.PASS).count();
        return ResponseEntity.ok(new PerfBenchmarkDashboard(total, (int) pass, (int)(total - pass), 0,
                Math.round(pass * 1000.0 / total) / 10.0, l, m, f));
    }

    @PostMapping("/seed-demo")
    public ResponseEntity<PerfBenchmarkDashboard> seedDemo() {
        var l1 = LoadTestResult.builder().communityId("COMM-001").testCaseCode("LDT-001").testCaseName("Dashboard — 100 concurrent users")
                .scenario(LoadTestResult.LoadScenario.DASHBOARD_100_CONCURRENT)
                .endpointPath("/api/dashboard").concurrentUsers(100).requestsPerSecond(200)
                .p50LatencyMs(45).p95LatencyMs(180).p99LatencyMs(320).targetP95Ms(200)
                .errorRate(0.0).throughputRps(195).toolUsed("k6").durationMinutes(10).slaBreached(false)
                .testResult(LoadTestResult.TestResult.PASS).failureReason("").build();

        var l2 = LoadTestResult.builder().communityId("COMM-001").testCaseCode("LDT-002").testCaseName("Visitor log — peak 500 RPS, DB pool exhausted")
                .scenario(LoadTestResult.LoadScenario.VISITOR_LOG_PEAK_500_RPS)
                .endpointPath("/api/visitor-log").concurrentUsers(300).requestsPerSecond(500)
                .p50LatencyMs(120).p95LatencyMs(2800).p99LatencyMs(8500).targetP95Ms(500)
                .errorRate(12.4).throughputRps(380).toolUsed("k6").durationMinutes(5).slaBreached(true)
                .testResult(LoadTestResult.TestResult.FAIL)
                .failureReason("p95 latency 2800ms (target 500ms) — HikariCP pool (max 10) exhausted at 300 concurrent, 12.4% errors").build();

        var l3 = LoadTestResult.builder().communityId("COMM-001").testCaseCode("LDT-003").testCaseName("Gate open API — real-time SLA 100ms")
                .scenario(LoadTestResult.LoadScenario.GATE_OPEN_API_REALTIME)
                .endpointPath("/api/gate/open").concurrentUsers(50).requestsPerSecond(100)
                .p50LatencyMs(22).p95LatencyMs(68).p99LatencyMs(95).targetP95Ms(100)
                .errorRate(0.0).throughputRps(99).toolUsed("k6").durationMinutes(15).slaBreached(false)
                .testResult(LoadTestResult.TestResult.PASS).failureReason("").build();

        var m1 = SyntheticMonitorTest.builder().communityId("COMM-001").testCaseCode("SYN-001").testCaseName("API health check — 1 min interval, 99.95% uptime")
                .scenario(SyntheticMonitorTest.MonitorScenario.API_HEALTH_CHECK_1MIN)
                .monitorName("API Health").monitorType("HTTP").targetUrl("/api/health")
                .checkIntervalMin(1).uptimePercent(99.97).targetUptimePercent(99.95)
                .incidentCount(1).avgResponseMs(28).sslCertValid(true).sslExpiryDays(45)
                .alertConfigured(true).alertChannel("Slack #ops + PagerDuty")
                .testResult(SyntheticMonitorTest.TestResult.PASS).failureReason("").build();

        var m2 = SyntheticMonitorTest.builder().communityId("COMM-001").testCaseCode("SYN-002").testCaseName("SSL cert — 8 days to expiry, no alert")
                .scenario(SyntheticMonitorTest.MonitorScenario.SSL_CERT_EXPIRY_WARNING)
                .monitorName("SSL Cert Watch").monitorType("SSL").targetUrl("https://app.manacommunity.in")
                .checkIntervalMin(60).uptimePercent(100.0).targetUptimePercent(99.9)
                .incidentCount(0).avgResponseMs(0).sslCertValid(true).sslExpiryDays(8)
                .alertConfigured(false).alertChannel("")
                .testResult(SyntheticMonitorTest.TestResult.FAIL)
                .failureReason("SSL cert expires in 8 days — no alert configured, threshold should trigger at 30 days").build();

        var m3 = SyntheticMonitorTest.builder().communityId("COMM-001").testCaseCode("SYN-003").testCaseName("Payment flow synthetic — Razorpay test mode")
                .scenario(SyntheticMonitorTest.MonitorScenario.PAYMENT_END_TO_END_FLOW)
                .monitorName("Payment E2E").monitorType("Browser").targetUrl("/pay/maintenance")
                .checkIntervalMin(15).uptimePercent(98.2).targetUptimePercent(99.5)
                .incidentCount(4).avgResponseMs(3200).sslCertValid(true).sslExpiryDays(45)
                .alertConfigured(true).alertChannel("Slack #payments")
                .testResult(SyntheticMonitorTest.TestResult.FAIL)
                .failureReason("Payment synthetic 98.2% uptime (target 99.5%) — 4 incidents in 7 days, Razorpay webhook timeout not retried").build();

        var f1 = FrontendPerfTest.builder().communityId("COMM-001").testCaseCode("FPF-001").testCaseName("Dashboard LCP — 2.8s on 4G throttled")
                .scenario(FrontendPerfTest.FrontendScenario.DASHBOARD_LCP_BUDGET)
                .pageName("Resident Dashboard").lcpMs(2800).targetLcpMs(2500)
                .fidMs(45).targetFidMs(100).clsScore(0.05).targetCls(0.1)
                .ttfbMs(380).bundleSizeKb(420).targetBundleSizeKb(350)
                .lighthouseScore(72).lazyLoadEnabled(false).imageSrcsetUsed(false)
                .testResult(FrontendPerfTest.TestResult.FAIL)
                .failureReason("LCP 2.8s (budget 2.5s) — hero image 1.2MB not lazy loaded, no srcset, bundle 420KB (target 350KB)").build();

        var f2 = FrontendPerfTest.builder().communityId("COMM-001").testCaseCode("FPF-002").testCaseName("Gate pass page — CLS 0.03, clean layout")
                .scenario(FrontendPerfTest.FrontendScenario.GATE_PASS_TTFB)
                .pageName("Gate Pass Approval").lcpMs(1200).targetLcpMs(2500)
                .fidMs(28).targetFidMs(100).clsScore(0.03).targetCls(0.1)
                .ttfbMs(190).bundleSizeKb(180).targetBundleSizeKb(350)
                .lighthouseScore(94).lazyLoadEnabled(true).imageSrcsetUsed(true)
                .testResult(FrontendPerfTest.TestResult.PASS).failureReason("").build();

        var f3 = FrontendPerfTest.builder().communityId("COMM-001").testCaseCode("FPF-003").testCaseName("Payment page — CLS layout shift on Razorpay widget load")
                .scenario(FrontendPerfTest.FrontendScenario.PAYMENT_PAGE_CLS_SHIFT)
                .pageName("Maintenance Payment").lcpMs(1800).targetLcpMs(2500)
                .fidMs(55).targetFidMs(100).clsScore(0.28).targetCls(0.1)
                .ttfbMs(220).bundleSizeKb(290).targetBundleSizeKb(350)
                .lighthouseScore(68).lazyLoadEnabled(true).imageSrcsetUsed(false)
                .testResult(FrontendPerfTest.TestResult.FAIL)
                .failureReason("CLS 0.28 (target <0.1) — Razorpay iframe loads without reserved height, shifts payment button down 180px").build();

        loadRepo.saveAll(List.of(l1, l2, l3));
        monitorRepo.saveAll(List.of(m1, m2, m3));
        frontendRepo.saveAll(List.of(f1, f2, f3));

        return ResponseEntity.ok(new PerfBenchmarkDashboard(9, 3, 6, 0, 33.3,
                List.of(l1, l2, l3), List.of(m1, m2, m3), List.of(f1, f2, f3)));
    }
}
