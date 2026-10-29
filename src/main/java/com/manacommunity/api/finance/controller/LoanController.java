package com.manacommunity.api.finance.controller;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Personal Loan stub controller.
 * GET  /api/v1/loans/offers      — browse loan offers
 * GET  /api/v1/loans/my-loans    — active loans for the user
 * POST /api/v1/loans/apply       — submit a loan application
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/loans")
@CrossOrigin(origins = "*")
public class LoanController {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LoanOfferDto {
        private String id;
        private String lender;
        private String lenderIcon;
        private String type; // PERSONAL | HOME | VEHICLE | EDUCATION | GOLD
        private int minAmount;
        private int maxAmount;
        private double interestRateMin;
        private double interestRateMax;
        private int maxTenureMonths;
        private double processingFeePct;
        private String disbursalTime;
        private List<String> features;
        private boolean preApproved;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class LoanApplyRequest {
        private String offerId;
        private int amount;
        private int tenureMonths;
        private double rate;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LoanApplyResponse {
        private String applicationId;
        private String status;
        private String message;
        private int emi;
        private int totalPayable;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ActiveLoanDto {
        private String id;
        private String lender;
        private String loanAccount;
        private int amount;
        private int emi;
        private int outstanding;
        private double interestRate;
        private int emisPaid;
        private int emisTotal;
        private String nextEmiDate;
        private String status;
    }

    // ── Loan offers ────────────────────────────────────────────────

    private static final List<LoanOfferDto> LOAN_OFFERS = List.of(
        LoanOfferDto.builder().id("PHONEPE_CREDIT").lender("PhonePe Credit Line").lenderIcon("💜").type("PERSONAL")
            .minAmount(1000).maxAmount(500000).interestRateMin(10.99).interestRateMax(23.99)
            .maxTenureMonths(36).processingFeePct(0.0).disbursalTime("Instant")
            .features(List.of("Zero processing fee", "Instant disbursal", "Flexible repayment")).preApproved(true).build(),
        LoanOfferDto.builder().id("BAJAJ_PERSONAL").lender("Bajaj Finserv Personal Loan").lenderIcon("💼").type("PERSONAL")
            .minAmount(200000).maxAmount(5000000).interestRateMin(11.00).interestRateMax(26.00)
            .maxTenureMonths(60).processingFeePct(2.0).disbursalTime("24 Hours")
            .features(List.of("No collateral", "Up to ₹50L", "60 month tenure")).preApproved(false).build(),
        LoanOfferDto.builder().id("HDFC_PERSONAL").lender("HDFC Bank Personal Loan").lenderIcon("🏦").type("PERSONAL")
            .minAmount(50000).maxAmount(4000000).interestRateMin(10.50).interestRateMax(24.00)
            .maxTenureMonths(60).processingFeePct(2.5).disbursalTime("4 Hours")
            .features(List.of("Pre-approved offers", "10.50% onwards", "Quick disbursement")).preApproved(false).build(),
        LoanOfferDto.builder().id("ICICI_PERSONAL").lender("ICICI Bank Instant Loan").lenderIcon("🔷").type("PERSONAL")
            .minAmount(50000).maxAmount(5000000).interestRateMin(10.85).interestRateMax(19.00)
            .maxTenureMonths(72).processingFeePct(2.25).disbursalTime("3 Seconds")
            .features(List.of("3-second disbursal", "Minimal docs", "72 month tenure")).preApproved(false).build(),
        LoanOfferDto.builder().id("KREDITBEE").lender("KreditBee Personal Loan").lenderIcon("🐝").type("PERSONAL")
            .minAmount(1000).maxAmount(300000).interestRateMin(15.00).interestRateMax(29.95)
            .maxTenureMonths(24).processingFeePct(3.0).disbursalTime("10 Minutes")
            .features(List.of("₹1K to ₹3L", "Low eligibility bar", "Paperless process")).preApproved(false).build(),
        LoanOfferDto.builder().id("STASHFIN").lender("StashFin Credit Line").lenderIcon("💳").type("PERSONAL")
            .minAmount(5000).maxAmount(500000).interestRateMin(11.99).interestRateMax(59.99)
            .maxTenureMonths(36).processingFeePct(2.0).disbursalTime("Instant")
            .features(List.of("Credit line model", "Pay only on use", "Revolving credit")).preApproved(false).build()
    );

    private static int calcEMI(int principal, double annualRate, int months) {
        double r = annualRate / 100.0 / 12.0;
        if (r == 0) return principal / months;
        double emi = principal * r * Math.pow(1 + r, months) / (Math.pow(1 + r, months) - 1);
        return (int) Math.round(emi);
    }

    // ── Endpoints ─────────────────────────────────────────────────

    @GetMapping("/offers")
    public ResponseEntity<List<LoanOfferDto>> getOffers() {
        return ResponseEntity.ok(LOAN_OFFERS);
    }

    @GetMapping("/my-loans")
    public ResponseEntity<List<ActiveLoanDto>> getMyLoans() {
        int amount = 200000;
        double rate = 13.50;
        int months = 24;
        int emi = calcEMI(amount, rate, months);
        int emisPaid = 7;
        int outstanding = (months - emisPaid) * emi;
        LocalDate nextEmi = LocalDate.now().withDayOfMonth(5).plusMonths(1);
        ActiveLoanDto mockLoan = ActiveLoanDto.builder()
            .id("loan-mock-001")
            .lender("Bajaj Finserv Personal Loan")
            .loanAccount("BFL-PL-" + (System.currentTimeMillis() % 1_000_000L))
            .amount(amount).emi(emi).outstanding(outstanding).interestRate(rate)
            .emisPaid(emisPaid).emisTotal(months)
            .nextEmiDate(nextEmi.format(DateTimeFormatter.ISO_LOCAL_DATE))
            .status("ACTIVE")
            .build();
        return ResponseEntity.ok(List.of(mockLoan));
    }

    @PostMapping("/apply")
    public ResponseEntity<LoanApplyResponse> applyLoan(@RequestBody LoanApplyRequest req) {
        log.info("[LOAN] apply offerId={} amount={} tenure={}m rate={}", req.getOfferId(), req.getAmount(), req.getTenureMonths(), req.getRate());
        int emi = calcEMI(req.getAmount(), req.getRate(), req.getTenureMonths());
        int totalPayable = emi * req.getTenureMonths();
        String appId = "APP-" + req.getOfferId().substring(0, Math.min(4, req.getOfferId().length()))
            + "-" + (System.currentTimeMillis() % 1_000_000L);
        return ResponseEntity.ok(LoanApplyResponse.builder()
            .applicationId(appId)
            .status("UNDER_REVIEW")
            .message("Application submitted successfully. You will receive a decision within 24 hours.")
            .emi(emi)
            .totalPayable(totalPayable)
            .build());
    }
}
