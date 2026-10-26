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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Fixed Deposit stub controller.
 * GET  /api/v1/fd/products      — browse available FD products
 * GET  /api/v1/fd/my-fds        — active FDs for the user
 * POST /api/v1/fd/book          — book a new FD (returns UPI intent)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/fd")
@CrossOrigin(origins = "*")
public class FDController {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TenureDto {
        private String label;
        private int months;
        private double rate;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class FDProductDto {
        private String id;
        private String bank;
        private String bankIcon;
        private String category; // BANK | NBFC | POST_OFFICE
        private double interestRate;
        private double seniorRate;
        private int minAmount;
        private int maxAmount;
        private List<TenureDto> tenures;
        private boolean isTaxSaver;
        private String rating;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class BookFDRequest {
        private String productId;
        private int principal;
        private int tenureMonths;
        private double rate;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BookFDResponse {
        private String fdNumber;
        private String upiIntentUrl;
        private String phonePeUrl;
        private String transactionRef;
        private int principal;
        private int maturityAmount;
        private String maturityDate;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ActiveFDDto {
        private String id;
        private String bank;
        private String fdNumber;
        private int principal;
        private double interestRate;
        private int maturityAmount;
        private int interestEarned;
        private String tenureLabel;
        private String openDate;
        private String maturityDate;
        private String status;
    }

    // ── Product catalogue ──────────────────────────────────────────

    private static final List<FDProductDto> FD_PRODUCTS = List.of(
        FDProductDto.builder().id("SBI_FD").bank("State Bank of India").bankIcon("🏛").category("BANK")
            .interestRate(6.80).seniorRate(7.30).minAmount(1000).maxAmount(10000000).isTaxSaver(true).rating("AAA")
            .tenures(List.of(
                new TenureDto("7 Days", 1, 4.50), new TenureDto("3 Months", 3, 6.00),
                new TenureDto("6 Months", 6, 6.50), new TenureDto("1 Year", 12, 6.80),
                new TenureDto("2 Years", 24, 7.00), new TenureDto("5 Years", 60, 6.50)
            )).build(),
        FDProductDto.builder().id("HDFC_FD").bank("HDFC Bank").bankIcon("🏦").category("BANK")
            .interestRate(7.10).seniorRate(7.60).minAmount(5000).maxAmount(10000000).isTaxSaver(true).rating("AAA")
            .tenures(List.of(
                new TenureDto("3 Months", 3, 6.25), new TenureDto("6 Months", 6, 6.60),
                new TenureDto("1 Year", 12, 7.10), new TenureDto("2 Years", 24, 7.15),
                new TenureDto("3 Years", 36, 7.00), new TenureDto("5 Years", 60, 7.00)
            )).build(),
        FDProductDto.builder().id("BAJAJ_FD").bank("Bajaj Finance").bankIcon("💼").category("NBFC")
            .interestRate(8.10).seniorRate(8.35).minAmount(15000).maxAmount(10000000).isTaxSaver(false).rating("AAA")
            .tenures(List.of(
                new TenureDto("12 Months", 12, 7.40), new TenureDto("24 Months", 24, 8.10),
                new TenureDto("36 Months", 36, 8.10), new TenureDto("44 Months", 44, 8.10),
                new TenureDto("60 Months", 60, 7.90)
            )).build(),
        FDProductDto.builder().id("ICICI_FD").bank("ICICI Bank").bankIcon("🔷").category("BANK")
            .interestRate(7.10).seniorRate(7.60).minAmount(10000).maxAmount(10000000).isTaxSaver(true).rating("AAA")
            .tenures(List.of(
                new TenureDto("3 Months", 3, 6.00), new TenureDto("6 Months", 6, 6.50),
                new TenureDto("1 Year", 12, 7.10), new TenureDto("15 Months", 15, 7.25),
                new TenureDto("2 Years", 24, 7.10), new TenureDto("5 Years", 60, 7.00)
            )).build(),
        FDProductDto.builder().id("POST_OFFICE_FD").bank("India Post (Post Office)").bankIcon("📮").category("POST_OFFICE")
            .interestRate(7.50).seniorRate(7.50).minAmount(1000).maxAmount(10000000).isTaxSaver(true).rating("Sovereign")
            .tenures(List.of(
                new TenureDto("1 Year", 12, 6.90), new TenureDto("2 Years", 24, 7.00),
                new TenureDto("3 Years", 36, 7.10), new TenureDto("5 Years", 60, 7.50)
            )).build(),
        FDProductDto.builder().id("SHRIRAM_FD").bank("Shriram Finance").bankIcon("🏢").category("NBFC")
            .interestRate(8.35).seniorRate(8.85).minAmount(5000).maxAmount(10000000).isTaxSaver(false).rating("AA+")
            .tenures(List.of(
                new TenureDto("12 Months", 12, 7.85), new TenureDto("24 Months", 24, 8.25),
                new TenureDto("36 Months", 36, 8.35), new TenureDto("60 Months", 60, 8.10)
            )).build()
    );

    private static int calcMaturity(int principal, double annualRatePct, int months) {
        double r = annualRatePct / 100.0;
        double quarters = months / 3.0;
        return (int) Math.round(principal * Math.pow(1.0 + r / 4.0, quarters));
    }

    // ── Endpoints ─────────────────────────────────────────────────

    @GetMapping("/products")
    public ResponseEntity<List<FDProductDto>> getProducts() {
        return ResponseEntity.ok(FD_PRODUCTS);
    }

    @GetMapping("/my-fds")
    public ResponseEntity<List<ActiveFDDto>> getMyFDs() {
        int principal = 50000;
        double rate = 8.10;
        int months = 24;
        int maturity = calcMaturity(principal, rate, months);
        LocalDate open = LocalDate.now().minusMonths(6);
        LocalDate matureDate = open.plusMonths(months);
        ActiveFDDto mockFD = ActiveFDDto.builder()
            .id("fd-mock-001")
            .bank("Bajaj Finance")
            .fdNumber("BFL-FD-" + (System.currentTimeMillis() % 1_000_000L))
            .principal(principal)
            .interestRate(rate)
            .maturityAmount(maturity)
            .interestEarned(maturity - principal)
            .tenureLabel("24 Months")
            .openDate(open.format(DateTimeFormatter.ISO_LOCAL_DATE))
            .maturityDate(matureDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
            .status("ACTIVE")
            .build();
        return ResponseEntity.ok(List.of(mockFD));
    }

    @PostMapping("/book")
    public ResponseEntity<BookFDResponse> bookFD(@RequestBody BookFDRequest req) {
        log.info("[FD] book productId={} principal={} tenure={}m rate={}", req.getProductId(), req.getPrincipal(), req.getTenureMonths(), req.getRate());
        int maturity = calcMaturity(req.getPrincipal(), req.getRate(), req.getTenureMonths());
        LocalDate matureDate = LocalDate.now().plusMonths(req.getTenureMonths());
        String fdNumber = "FD-" + req.getProductId().substring(0, Math.min(3, req.getProductId().length()))
            + "-" + (System.currentTimeMillis() % 1_000_000L);
        String tr = "MANAFD" + System.currentTimeMillis();
        String pa = "fd@setu";
        String pn = "Mana+FD+Booking";
        String upiUrl = String.format("upi://pay?pa=%s&pn=%s&am=%d&cu=INR&tr=%s&tn=FD+Booking", pa, pn, req.getPrincipal(), tr);
        String phonePeUrl = String.format("phonepe://pay?pa=%s&pn=%s&am=%d&cu=INR&tr=%s", pa, pn, req.getPrincipal(), tr);
        return ResponseEntity.ok(BookFDResponse.builder()
            .fdNumber(fdNumber)
            .upiIntentUrl(upiUrl)
            .phonePeUrl(phonePeUrl)
            .transactionRef(tr)
            .principal(req.getPrincipal())
            .maturityAmount(maturity)
            .maturityDate(matureDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
            .build());
    }
}
