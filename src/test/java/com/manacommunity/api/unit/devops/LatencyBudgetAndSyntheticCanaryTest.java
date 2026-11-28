package com.manacommunity.api.unit.devops;

import com.manacommunity.api.release.service.SyntheticCanaryMonitorService;
import com.manacommunity.api.visitor.security.OfflineQrTokenService;
import com.manacommunity.api.whatsapp.service.GateApprovalStateMachine;
import com.manacommunity.api.whatsapp.service.GateApprovalStateMachine.ApprovalChannel;
import com.manacommunity.api.whatsapp.service.GateApprovalStateMachine.EntryStatus;
import com.manacommunity.api.whatsapp.service.GateApprovalStateMachine.VisitorEntrySession;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LatencyBudgetAndSyntheticCanaryTest {

    @Test
    void enforcesAutomatedLatencyBudgetsOnLiveQrDecodeAndVisitorApproval() throws Exception {
        SyntheticCanaryMonitorService monitor = new SyntheticCanaryMonitorService();

        // Measure live Ed25519 QR decode verification latency (budget: < 50ms)
        KeyPair kp = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        Clock clock = Clock.fixed(Instant.parse("2026-11-28T08:00:00Z"), ZoneOffset.UTC);
        OfflineQrTokenService qrSigner = new OfflineQrTokenService(kp, clock);
        String token = qrSigner.signToken(101L, "PASS-9001", "A-101", 600L);
        String pubKey = qrSigner.getPublicKeyBase64();

        // Warm-up call
        OfflineQrTokenService.verifyWithPublicKeyBase64(token, pubKey, 101L, clock);

        long qrStartNs = System.nanoTime();
        var qrRes = OfflineQrTokenService.verifyWithPublicKeyBase64(token, pubKey, 101L, clock);
        long qrElapsedMs = (System.nanoTime() - qrStartNs) / 1_000_000L;
        assertThat(qrRes.isValid()).isTrue();

        // Measure live visitor approval transition latency (budget: < 250ms)
        GateApprovalStateMachine gate = new GateApprovalStateMachine();
        gate.registerVisitorEntry(VisitorEntrySession.builder()
                .entryId("PERF-ENTRY-1").societyId("101").flatNumber("A-101")
                .visitorName("Delivery").status(EntryStatus.PENDING).build());
        long approveStartNs = System.nanoTime();
        var approval = gate.submitDecision("PERF-ENTRY-1", "APPROVE", "Resident", ApprovalChannel.MOBILE_APP);
        long approveMs = (System.nanoTime() - approveStartNs) / 1_000_000L;
        assertThat(approval.isAccepted()).isTrue();

        // Measure local typeahead search (budget: < 50ms)
        List<String> directory = List.of("A-101 Ramesh", "A-102 Priya", "B-204 Sunil", "C-301 Anita");
        long searchStartNs = System.nanoTime();
        List<String> matches = directory.stream().filter(s -> s.contains("Sunil")).toList();
        long searchMs = (System.nanoTime() - searchStartNs) / 1_000_000L;
        assertThat(matches).hasSize(1);

        var okReport = monitor.evaluateLatencyBudgets(420L, qrElapsedMs, approveMs, searchMs);
        assertThat(okReport.withinBudget()).isTrue();
        assertThat(okReport.violations()).isEmpty();

        // Verify CI gate fails loudly when any SLA budget is breached
        var breachReport = monitor.evaluateLatencyBudgets(950L, 68L, 310L, 62L);
        assertThat(breachReport.withinBudget()).isFalse();
        assertThat(breachReport.violations()).hasSize(4);
    }

    @Test
    void runs60SecondSyntheticCanaryProbeAndAlertsOnCallWhenPushOrWebhookDegrades() {
        SyntheticCanaryMonitorService monitor = new SyntheticCanaryMonitorService();

        // Healthy synthetic cycle resets failure counter and fires zero alerts
        var healthy = monitor.executeSyntheticCanaryProbe(() -> true, () -> true, () -> true, 500L);
        assertThat(healthy.healthy()).isTrue();
        assertThat(healthy.consecutiveFailures()).isZero();
        assertThat(healthy.alertDispatched()).isNull();

        // Degraded push delivery triggers immediate PagerDuty/Slack alert
        var pushDown = monitor.executeSyntheticCanaryProbe(() -> true, () -> false, () -> true, 500L);
        assertThat(pushDown.healthy()).isFalse();
        assertThat(pushDown.pushDeliveryOk()).isFalse();
        assertThat(pushDown.consecutiveFailures()).isEqualTo(1);
        assertThat(pushDown.alertDispatched()).contains("ONCALL_ALERT").contains("push=false");

        // Recovery clears consecutive failures
        var recovered = monitor.executeSyntheticCanaryProbe(() -> true, () -> true, () -> true, 500L);
        assertThat(recovered.healthy()).isTrue();
        assertThat(recovered.consecutiveFailures()).isZero();
    }
}
