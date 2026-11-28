package com.manacommunity.api.whatsappbot.controller;

import com.manacommunity.api.whatsappbot.entity.*;
import com.manacommunity.api.whatsappbot.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/whatsapp-bot-test")
public class WhatsappBotTestController {

    private final InteractiveButtonTestRepository buttonRepo;
    private final BotSessionTestRepository sessionRepo;
    private final DltTemplateTestRepository dltRepo;

    public WhatsappBotTestController(InteractiveButtonTestRepository buttonRepo,
                                      BotSessionTestRepository sessionRepo,
                                      DltTemplateTestRepository dltRepo) {
        this.buttonRepo = buttonRepo;
        this.sessionRepo = sessionRepo;
        this.dltRepo = dltRepo;
    }

    public record WhatsappDashboard(int totalTests, int passed, int failed, int pending, double passRate,
                                     List<InteractiveButtonTest> buttonTests, List<BotSessionTest> sessionTests,
                                     List<DltTemplateTest> dltTests) {}

    @GetMapping("/dashboard")
    public ResponseEntity<WhatsappDashboard> getDashboard() {
        var b = buttonRepo.findAll(); var s = sessionRepo.findAll(); var d = dltRepo.findAll();
        int total = b.size() + s.size() + d.size();
        if (total == 0) return seedDemo();
        long pass = b.stream().filter(t -> t.getTestResult() == InteractiveButtonTest.TestResult.PASS).count()
                + s.stream().filter(t -> t.getTestResult() == BotSessionTest.TestResult.PASS).count()
                + d.stream().filter(t -> t.getTestResult() == DltTemplateTest.TestResult.PASS).count();
        return ResponseEntity.ok(new WhatsappDashboard(total, (int) pass, (int)(total - pass), 0, Math.round(pass * 1000.0 / total) / 10.0, b, s, d));
    }

    @PostMapping("/seed-demo")
    public ResponseEntity<WhatsappDashboard> seedDemo() {
        var b1 = InteractiveButtonTest.builder().communityId("COMM-001").testCaseCode("WHA-001").testCaseName("Approve WA + Deny App simultaneously")
                .scenario(InteractiveButtonTest.ButtonScenario.APPROVE_WA_DENY_APP_CONCURRENT)
                .visitorName("Delivery Agent Swiggy").whatsappAction("APPROVE").mobileAppAction("DENY").actionDelayMs(120).concurrentActions(true)
                .singleWinnerLocked(true).winningAction("APPROVE").losingAction("DENY").duplicateWebhookReceived(false).webhookCount(1).webhookDeduped(true)
                .testResult(InteractiveButtonTest.TestResult.PASS).failureReason("").build();

        var b2 = InteractiveButtonTest.builder().communityId("COMM-001").testCaseCode("WHA-002").testCaseName("Duplicate Meta webhook — both processed")
                .scenario(InteractiveButtonTest.ButtonScenario.DUPLICATE_META_WEBHOOK)
                .visitorName("Amazon Delivery").whatsappAction("APPROVE").mobileAppAction("").actionDelayMs(0).concurrentActions(false)
                .singleWinnerLocked(false).winningAction("APPROVE").losingAction("").duplicateWebhookReceived(true).webhookCount(3).webhookDeduped(false)
                .testResult(InteractiveButtonTest.TestResult.FAIL)
                .failureReason("3 duplicate Meta webhooks processed — idempotency key not checked, visitor approved 3 times in audit log").build();

        var b3 = InteractiveButtonTest.builder().communityId("COMM-001").testCaseCode("WHA-003").testCaseName("Stale button tapped after visitor already left")
                .scenario(InteractiveButtonTest.ButtonScenario.STALE_BUTTON_AFTER_RESOLUTION)
                .visitorName("Plumber Raju").whatsappAction("APPROVE").mobileAppAction("").actionDelayMs(0).concurrentActions(false)
                .singleWinnerLocked(true).winningAction("ALREADY_RESOLVED").losingAction("").duplicateWebhookReceived(false).webhookCount(1).webhookDeduped(true)
                .testResult(InteractiveButtonTest.TestResult.PASS).failureReason("").build();

        var s1 = BotSessionTest.builder().communityId("COMM-001").testCaseCode("BOT-001").testCaseName("Pay dues — 45 min gap, session expired")
                .scenario(BotSessionTest.SessionScenario.PAY_DUES_SESSION_EXPIRED_45MIN)
                .conversationFlow("Pay dues → Select flat → (45 min gap) → Confirm").sessionTimeoutMinutes(30).actualGapMinutes(45)
                .sessionExpired(true).stateRecovered(false).userInput("Confirm").invalidInputHandled(true)
                .botResponse("Your session expired. Type 'Pay dues' to start again.").pdfReceiptGenerated(false)
                .paymentMethod("").upiCallbackReceived(false).receiptGenerationDelayMs(0)
                .testResult(BotSessionTest.TestResult.PASS).failureReason("").build();

        var s2 = BotSessionTest.builder().communityId("COMM-001").testCaseCode("BOT-002").testCaseName("PDF receipt after UPI intent callback")
                .scenario(BotSessionTest.SessionScenario.PDF_RECEIPT_AFTER_UPI_CALLBACK)
                .conversationFlow("Pay dues → UPI intent → Payment success → PDF receipt").sessionTimeoutMinutes(30).actualGapMinutes(2)
                .sessionExpired(false).stateRecovered(true).userInput("").invalidInputHandled(false)
                .botResponse("Payment of ₹4,500 received. Here is your receipt:").pdfReceiptGenerated(true)
                .paymentMethod("UPI_INTENT").upiCallbackReceived(true).receiptGenerationDelayMs(3200)
                .testResult(BotSessionTest.TestResult.PASS).failureReason("").build();

        var s3 = BotSessionTest.builder().communityId("COMM-001").testCaseCode("BOT-003").testCaseName("Invalid input 'asdf' mid-flow — no crash")
                .scenario(BotSessionTest.SessionScenario.INVALID_TEXT_INPUT_HANDLED)
                .conversationFlow("Pay dues → Select flat → 'asdf'").sessionTimeoutMinutes(30).actualGapMinutes(0)
                .sessionExpired(false).stateRecovered(false).userInput("asdf").invalidInputHandled(false)
                .botResponse("").pdfReceiptGenerated(false).paymentMethod("").upiCallbackReceived(false).receiptGenerationDelayMs(0)
                .testResult(BotSessionTest.TestResult.FAIL)
                .failureReason("Bot returned HTTP 500 on invalid input — NullPointerException in flow parser").build();

        var d1 = DltTemplateTest.builder().communityId("COMM-001").testCaseCode("DLT-001").testCaseName("SMS OTP template — DLT registered")
                .scenario(DltTemplateTest.DltScenario.SMS_TEMPLATE_MATCH_CHECK).channel(DltTemplateTest.ChannelType.SMS)
                .templateName("otp_login").dltEntityId("1101456789012345").dltTemplateId("1107167890123456").dltRegistered(true)
                .language("English").variableCount(1).maxVariableChars(6).actualVariableChars(6).charLimitExceeded(false)
                .deliverySuccessful(true).deliveryRatePercent(99.2).deliveryFailureReason("")
                .testResult(DltTemplateTest.TestResult.PASS).failureReason("").build();

        var d2 = DltTemplateTest.builder().communityId("COMM-001").testCaseCode("DLT-002").testCaseName("WhatsApp utility template — variable overflow")
                .scenario(DltTemplateTest.DltScenario.VARIABLE_CHAR_LIMIT_EXCEEDED).channel(DltTemplateTest.ChannelType.WHATSAPP)
                .templateName("maintenance_reminder").dltEntityId("1101456789012345").dltTemplateId("1107167890123457").dltRegistered(true)
                .language("Hindi").variableCount(3).maxVariableChars(30).actualVariableChars(48).charLimitExceeded(true)
                .deliverySuccessful(false).deliveryRatePercent(0).deliveryFailureReason("Variable {{flat_address}} exceeded 30 char limit — 48 chars sent")
                .testResult(DltTemplateTest.TestResult.FAIL)
                .failureReason("Hindi template variable overflow — flat address truncated, delivery blocked by Meta").build();

        var d3 = DltTemplateTest.builder().communityId("COMM-001").testCaseCode("DLT-003").testCaseName("Telugu regional language SMS delivery")
                .scenario(DltTemplateTest.DltScenario.REGIONAL_LANGUAGE_DELIVERY).channel(DltTemplateTest.ChannelType.SMS)
                .templateName("visitor_alert_te").dltEntityId("1101456789012345").dltTemplateId("1107167890123458").dltRegistered(true)
                .language("Telugu").variableCount(2).maxVariableChars(20).actualVariableChars(16).charLimitExceeded(false)
                .deliverySuccessful(true).deliveryRatePercent(94.8).deliveryFailureReason("")
                .testResult(DltTemplateTest.TestResult.PASS).failureReason("").build();

        buttonRepo.saveAll(List.of(b1, b2, b3));
        sessionRepo.saveAll(List.of(s1, s2, s3));
        dltRepo.saveAll(List.of(d1, d2, d3));

        return ResponseEntity.ok(new WhatsappDashboard(9, 6, 3, 0, 66.7,
                List.of(b1, b2, b3), List.of(s1, s2, s3), List.of(d1, d2, d3)));
    }
}
