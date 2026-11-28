package com.manacommunity.api.erpaudit.controller;

import com.manacommunity.api.erpaudit.entity.*;
import com.manacommunity.api.erpaudit.entity.InvoiceTestCase.*;
import com.manacommunity.api.erpaudit.entity.PaymentWebhookTest.*;
import com.manacommunity.api.erpaudit.entity.MakerCheckerTest.*;
import com.manacommunity.api.erpaudit.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/erp-audit")
@RequiredArgsConstructor
public class ErpAuditController {

    private final InvoiceTestCaseRepository invoiceRepo;
    private final PaymentWebhookTestRepository webhookRepo;
    private final MakerCheckerTestRepository makerCheckerRepo;

    public record AuditDashboard(
            long totalTests, long passed, long failed, long pending,
            double passRate,
            List<InvoiceTestCase> invoiceTests,
            List<PaymentWebhookTest> webhookTests,
            List<MakerCheckerTest> makerCheckerTests
    ) {}

    @GetMapping("/dashboard")
    public ResponseEntity<AuditDashboard> getDashboard() {
        var invoices = invoiceRepo.findAll();
        var webhooks = webhookRepo.findAll();
        var makers = makerCheckerRepo.findAll();

        List<TestResult> allResults = new ArrayList<>();
        invoices.forEach(i -> allResults.add(i.getTestResult()));
        webhooks.forEach(w -> allResults.add(w.getTestResult()));
        makers.forEach(m -> allResults.add(m.getTestResult()));

        long passed = allResults.stream().filter(r -> r == TestResult.PASS).count();
        long failed = allResults.stream().filter(r -> r == TestResult.FAIL).count();
        long pending = allResults.stream().filter(r -> r == TestResult.PENDING).count();
        double passRate = allResults.isEmpty() ? 0 : (double) passed / allResults.size() * 100;

        return ResponseEntity.ok(new AuditDashboard(allResults.size(), passed, failed, pending, passRate,
                invoices, webhooks, makers));
    }

    @PostMapping("/seed-demo")
    public ResponseEntity<Map<String, Object>> seedDemo() {
        invoiceRepo.deleteAll(); webhookRepo.deleteAll(); makerCheckerRepo.deleteAll();

        // Invoice test cases
        invoiceRepo.saveAll(List.of(
                InvoiceTestCase.builder().communityId("COMM-001").testCaseCode("INV-001").testCaseName("Mid-month move-in pro-rata (15th)")
                        .scenario(InvoiceScenario.MID_MONTH_MOVE_IN).flatNumber("A-305").moveInDate(LocalDate.of(2026, 11, 15))
                        .billingPeriodStart(LocalDate.of(2026, 11, 1)).billingPeriodEnd(LocalDate.of(2026, 11, 30))
                        .totalDaysInMonth(30).occupiedDays(16).fullMonthAmount(new BigDecimal("8500.00"))
                        .proRataAmount(new BigDecimal("4533.33")).gstRate(new BigDecimal("18")).gstAmount(new BigDecimal("816.00"))
                        .gstExempt(false).lateFeeRate(BigDecimal.ZERO).lateFeeAmount(BigDecimal.ZERO).compoundInterest(false).overdueDays(0)
                        .totalExpected(new BigDecimal("5349.33")).totalActual(new BigDecimal("5349.33")).variance(BigDecimal.ZERO)
                        .manualWaiverApplied(false).testResult(TestResult.PASS).build(),

                InvoiceTestCase.builder().communityId("COMM-001").testCaseCode("INV-002").testCaseName("GST exemption below ₹7,500 threshold")
                        .scenario(InvoiceScenario.GST_BELOW_THRESHOLD).flatNumber("B-102").moveInDate(LocalDate.of(2026, 1, 1))
                        .billingPeriodStart(LocalDate.of(2026, 11, 1)).billingPeriodEnd(LocalDate.of(2026, 11, 30))
                        .totalDaysInMonth(30).occupiedDays(30).fullMonthAmount(new BigDecimal("7000.00"))
                        .proRataAmount(new BigDecimal("7000.00")).gstRate(BigDecimal.ZERO).gstAmount(BigDecimal.ZERO)
                        .gstExempt(true).gstExemptionReason("Monthly charge ₹7,000 below ₹7,500 GST threshold per unit")
                        .lateFeeRate(BigDecimal.ZERO).lateFeeAmount(BigDecimal.ZERO).compoundInterest(false).overdueDays(0)
                        .totalExpected(new BigDecimal("7000.00")).totalActual(new BigDecimal("7000.00")).variance(BigDecimal.ZERO)
                        .manualWaiverApplied(false).testResult(TestResult.PASS).build(),

                InvoiceTestCase.builder().communityId("COMM-001").testCaseCode("INV-003").testCaseName("GST applied above ₹7,500 threshold")
                        .scenario(InvoiceScenario.GST_ABOVE_THRESHOLD).flatNumber("C-401").moveInDate(LocalDate.of(2025, 6, 1))
                        .billingPeriodStart(LocalDate.of(2026, 11, 1)).billingPeriodEnd(LocalDate.of(2026, 11, 30))
                        .totalDaysInMonth(30).occupiedDays(30).fullMonthAmount(new BigDecimal("12000.00"))
                        .proRataAmount(new BigDecimal("12000.00")).gstRate(new BigDecimal("18")).gstAmount(new BigDecimal("2160.00"))
                        .gstExempt(false).lateFeeRate(BigDecimal.ZERO).lateFeeAmount(BigDecimal.ZERO).compoundInterest(false).overdueDays(0)
                        .totalExpected(new BigDecimal("14160.00")).totalActual(new BigDecimal("14160.00")).variance(BigDecimal.ZERO)
                        .manualWaiverApplied(false).testResult(TestResult.PASS).build(),

                InvoiceTestCase.builder().communityId("COMM-001").testCaseCode("INV-004").testCaseName("Compound late-payment interest (90 days overdue)")
                        .scenario(InvoiceScenario.COMPOUND_LATE_INTEREST).flatNumber("D-201").moveInDate(LocalDate.of(2024, 3, 1))
                        .billingPeriodStart(LocalDate.of(2026, 8, 1)).billingPeriodEnd(LocalDate.of(2026, 8, 31))
                        .totalDaysInMonth(31).occupiedDays(31).fullMonthAmount(new BigDecimal("9500.00"))
                        .proRataAmount(new BigDecimal("9500.00")).gstRate(new BigDecimal("18")).gstAmount(new BigDecimal("1710.00"))
                        .gstExempt(false).lateFeeRate(new BigDecimal("1.5")).lateFeeAmount(new BigDecimal("513.42"))
                        .compoundInterest(true).overdueDays(90)
                        .totalExpected(new BigDecimal("11723.42")).totalActual(new BigDecimal("11580.00"))
                        .variance(new BigDecimal("-143.42")).manualWaiverApplied(false)
                        .testResult(TestResult.FAIL).failureReason("Simple interest applied instead of compound. Expected ₹513.42 late fee, got ₹427.50 (simple 1.5% × 3 months)").build(),

                InvoiceTestCase.builder().communityId("COMM-001").testCaseCode("INV-005").testCaseName("Manual waiver with Secretary approval")
                        .scenario(InvoiceScenario.MANUAL_WAIVER).flatNumber("E-502").moveInDate(LocalDate.of(2025, 1, 1))
                        .billingPeriodStart(LocalDate.of(2026, 11, 1)).billingPeriodEnd(LocalDate.of(2026, 11, 30))
                        .totalDaysInMonth(30).occupiedDays(30).fullMonthAmount(new BigDecimal("8500.00"))
                        .proRataAmount(new BigDecimal("8500.00")).gstRate(new BigDecimal("18")).gstAmount(new BigDecimal("1530.00"))
                        .gstExempt(false).lateFeeRate(new BigDecimal("1.5")).lateFeeAmount(BigDecimal.ZERO)
                        .compoundInterest(false).overdueDays(45)
                        .totalExpected(new BigDecimal("10030.00")).totalActual(new BigDecimal("10030.00")).variance(BigDecimal.ZERO)
                        .manualWaiverApplied(true).waiverApprovedBy("Dr. Meena Sharma (Secretary)").waiverReason("Medical emergency — resident hospitalized for 60 days")
                        .testResult(TestResult.PASS).build()
        ));

        // Payment webhook tests
        webhookRepo.saveAll(List.of(
                PaymentWebhookTest.builder().communityId("COMM-001").testCaseCode("WH-001").testCaseName("Delayed webhook — 30 min after timeout")
                        .scenario(WebhookScenario.DELAYED_WEBHOOK_30MIN).transactionId("TXN-20261128-001").gatewayName("Razorpay")
                        .amount(new BigDecimal("10030.00")).webhookStatus("PAYMENT_SUCCESS")
                        .paymentInitiatedAt(LocalDateTime.of(2026, 11, 28, 10, 0)).webhookReceivedAt(LocalDateTime.of(2026, 11, 28, 10, 32))
                        .webhookDelaySeconds(1920).duplicateCallbackCount(1).idempotencyHandled(true)
                        .walletAutoAdjusted(false).walletBalanceBefore(BigDecimal.ZERO).walletBalanceAfter(BigDecimal.ZERO)
                        .bounceType(BounceType.NONE).autoReversalTriggered(false)
                        .testResult(TestResult.PASS).build(),

                PaymentWebhookTest.builder().communityId("COMM-001").testCaseCode("WH-002").testCaseName("Duplicate PAYMENT_SUCCESS callbacks (3×)")
                        .scenario(WebhookScenario.DUPLICATE_SUCCESS_CALLBACK).transactionId("TXN-20261128-002").gatewayName("Razorpay")
                        .amount(new BigDecimal("8500.00")).webhookStatus("PAYMENT_SUCCESS")
                        .paymentInitiatedAt(LocalDateTime.of(2026, 11, 28, 11, 0)).webhookReceivedAt(LocalDateTime.of(2026, 11, 28, 11, 0, 5))
                        .webhookDelaySeconds(5).duplicateCallbackCount(3).idempotencyHandled(true)
                        .walletAutoAdjusted(false).walletBalanceBefore(BigDecimal.ZERO).walletBalanceAfter(BigDecimal.ZERO)
                        .bounceType(BounceType.NONE).autoReversalTriggered(false)
                        .testResult(TestResult.PASS).build(),

                PaymentWebhookTest.builder().communityId("COMM-001").testCaseCode("WH-003").testCaseName("Partial payment — ₹5,000 of ₹10,030")
                        .scenario(WebhookScenario.PARTIAL_PAYMENT).transactionId("TXN-20261128-003").gatewayName("PayU")
                        .amount(new BigDecimal("5000.00")).webhookStatus("PAYMENT_SUCCESS")
                        .paymentInitiatedAt(LocalDateTime.of(2026, 11, 28, 12, 0)).webhookReceivedAt(LocalDateTime.of(2026, 11, 28, 12, 0, 3))
                        .webhookDelaySeconds(3).duplicateCallbackCount(1).idempotencyHandled(true)
                        .walletAutoAdjusted(false).walletBalanceBefore(BigDecimal.ZERO).walletBalanceAfter(BigDecimal.ZERO)
                        .bounceType(BounceType.NONE).autoReversalTriggered(false)
                        .testResult(TestResult.PASS).build(),

                PaymentWebhookTest.builder().communityId("COMM-001").testCaseCode("WH-004").testCaseName("Bounced cheque — insufficient funds reversal")
                        .scenario(WebhookScenario.BOUNCED_CHEQUE).transactionId("CHQ-20261125-001").gatewayName("HDFC Bank")
                        .amount(new BigDecimal("14160.00")).webhookStatus("PAYMENT_BOUNCED")
                        .paymentInitiatedAt(LocalDateTime.of(2026, 11, 25, 9, 0)).webhookReceivedAt(LocalDateTime.of(2026, 11, 27, 14, 0))
                        .webhookDelaySeconds(190800).duplicateCallbackCount(1).idempotencyHandled(true)
                        .walletAutoAdjusted(true).walletBalanceBefore(new BigDecimal("14160.00")).walletBalanceAfter(BigDecimal.ZERO)
                        .bounceType(BounceType.INSUFFICIENT_FUNDS).bounceReason("Insufficient funds in savings account XXXX1234")
                        .autoReversalTriggered(true)
                        .testResult(TestResult.FAIL).failureReason("Bounce penalty ₹500 not auto-added to next invoice. Manual intervention required.").build(),

                PaymentWebhookTest.builder().communityId("COMM-001").testCaseCode("WH-005").testCaseName("Advance wallet auto-adjustment on overpayment")
                        .scenario(WebhookScenario.ADVANCE_WALLET_DEBIT).transactionId("TXN-20261128-005").gatewayName("Razorpay")
                        .amount(new BigDecimal("15000.00")).webhookStatus("PAYMENT_SUCCESS")
                        .paymentInitiatedAt(LocalDateTime.of(2026, 11, 28, 14, 0)).webhookReceivedAt(LocalDateTime.of(2026, 11, 28, 14, 0, 2))
                        .webhookDelaySeconds(2).duplicateCallbackCount(1).idempotencyHandled(true)
                        .walletAutoAdjusted(true).walletBalanceBefore(BigDecimal.ZERO).walletBalanceAfter(new BigDecimal("4970.00"))
                        .bounceType(BounceType.NONE).autoReversalTriggered(false)
                        .testResult(TestResult.PASS).build()
        ));

        // Maker-checker tests
        makerCheckerRepo.saveAll(List.of(
                MakerCheckerTest.builder().communityId("COMM-001").testCaseCode("MC-001").testCaseName("3-level expense approval: EM → Treasurer → Secretary")
                        .scenario(ReconciliationScenario.THREE_LEVEL_APPROVAL).expenseDescription("Annual lift maintenance contract — Schindler India")
                        .expenseAmount(new BigDecimal("450000.00")).makerRole("Estate Manager").makerName("Rajesh Nair")
                        .checkerRole("Treasurer").checkerName("Arvind Patel").finalApproverRole("Secretary").finalApproverName("Dr. Meena Sharma")
                        .approvalChain(ApprovalChain.ESTATE_MANAGER_TREASURER_SECRETARY)
                        .makerApproved(true).checkerApproved(true).finalApproved(true).autoEscalated(false).escalationHours(0)
                        .tallyLedgerName("Lift Maintenance").tallyVoucherType("Purchase").debitTotal(new BigDecimal("450000.00"))
                        .creditTotal(new BigDecimal("450000.00")).balanceDifference(BigDecimal.ZERO).doubleEntryValid(true).exportFormat("XML")
                        .testResult(TestResult.PASS).build(),

                MakerCheckerTest.builder().communityId("COMM-001").testCaseCode("MC-002").testCaseName("Bypass attempt — Estate Manager self-approves ₹2L expense")
                        .scenario(ReconciliationScenario.BYPASS_ATTEMPT).expenseDescription("Garden landscaping redesign")
                        .expenseAmount(new BigDecimal("200000.00")).makerRole("Estate Manager").makerName("Rajesh Nair")
                        .checkerRole("Treasurer").checkerName("Arvind Patel").finalApproverRole("Secretary").finalApproverName("Dr. Meena Sharma")
                        .approvalChain(ApprovalChain.ESTATE_MANAGER_TREASURER_SECRETARY)
                        .makerApproved(true).checkerApproved(false).finalApproved(false).autoEscalated(false).escalationHours(0)
                        .tallyLedgerName("").tallyVoucherType("").debitTotal(BigDecimal.ZERO).creditTotal(BigDecimal.ZERO)
                        .balanceDifference(BigDecimal.ZERO).doubleEntryValid(false).exportFormat("")
                        .testResult(TestResult.PASS).failureReason("Correctly blocked — maker cannot be checker on same transaction").build(),

                MakerCheckerTest.builder().communityId("COMM-001").testCaseCode("MC-003").testCaseName("Tally XML export — Debit/Credit mismatch detection")
                        .scenario(ReconciliationScenario.DEBIT_CREDIT_MISMATCH).expenseDescription("Swimming pool chlorination chemicals")
                        .expenseAmount(new BigDecimal("35000.00")).makerRole("Estate Manager").makerName("Rajesh Nair")
                        .checkerRole("Treasurer").checkerName("Arvind Patel").finalApproverRole("").finalApproverName("")
                        .approvalChain(ApprovalChain.TREASURER_SECRETARY)
                        .makerApproved(true).checkerApproved(true).finalApproved(false).autoEscalated(false).escalationHours(0)
                        .tallyLedgerName("Pool Maintenance").tallyVoucherType("Payment").debitTotal(new BigDecimal("35000.00"))
                        .creditTotal(new BigDecimal("34650.00")).balanceDifference(new BigDecimal("350.00")).doubleEntryValid(false).exportFormat("XML")
                        .testResult(TestResult.FAIL).failureReason("₹350 debit-credit mismatch: TDS deduction not reflected in credit side of Tally export").build(),

                MakerCheckerTest.builder().communityId("COMM-001").testCaseCode("MC-004").testCaseName("Auto-escalation after 48hr Treasurer timeout")
                        .scenario(ReconciliationScenario.AUTO_ESCALATION_TIMEOUT).expenseDescription("Emergency generator repair")
                        .expenseAmount(new BigDecimal("85000.00")).makerRole("Estate Manager").makerName("Rajesh Nair")
                        .checkerRole("Treasurer").checkerName("Arvind Patel").finalApproverRole("Secretary").finalApproverName("Dr. Meena Sharma")
                        .approvalChain(ApprovalChain.ESTATE_MANAGER_TREASURER_SECRETARY)
                        .makerApproved(true).checkerApproved(false).finalApproved(true).autoEscalated(true).escalationHours(48)
                        .tallyLedgerName("Generator Maintenance").tallyVoucherType("Payment").debitTotal(new BigDecimal("85000.00"))
                        .creditTotal(new BigDecimal("85000.00")).balanceDifference(BigDecimal.ZERO).doubleEntryValid(true).exportFormat("CSV")
                        .testResult(TestResult.PASS).build()
        ));

        return ResponseEntity.ok(Map.of("status", "seeded", "invoiceTests", 5, "webhookTests", 5, "makerCheckerTests", 4));
    }
}
