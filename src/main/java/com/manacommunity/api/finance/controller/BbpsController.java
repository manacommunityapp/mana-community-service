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
import java.util.concurrent.ThreadLocalRandom;

/**
 * BBPS (Bharat Bill Payment System) stub controller.
 *
 * Exposes 3 endpoints that the mobile billerService.ts calls:
 *   GET  /api/v1/bbps/billers?category=ELECTRICITY
 *   POST /api/v1/bbps/bill/fetch
 *   POST /api/v1/bbps/bill/pay
 *
 * All responses are deterministic mocks. Wire to Setu or Razorpay
 * BillPay once BBPS aggregator credentials are provisioned.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/bbps")
@CrossOrigin(origins = "*")
public class BbpsController {

    // ── Inner DTOs ────────────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BillerDto {
        private String id;
        private String name;
        private String shortName;
        private String category;
        private String icon;
        private String state;
        private String consumerIdLabel;
        private String consumerIdHint;
        private Integer consumerIdLength;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class BillFetchRequest {
        private String billerId;
        private String consumerId;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class FetchedBillDto {
        private String billerId;
        private String billerName;
        private String consumerId;
        private String customerName;
        private String billNumber;
        private String billDate;
        private String dueDate;
        private Double amount;
        private Double overdueAmount;
        private Integer units;
        private String periodFrom;
        private String periodTo;
        private String status; // DUE | OVERDUE | PAID
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class BillPayRequest {
        private String billNumber;
        private String billerId;
        private Double amount;
        private String method; // UPI | WALLET
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BillPaymentResponse {
        private String upiIntentUrl;
        private String phonePeUrl;
        private String transactionRef;
        private Double amount;
    }

    // ── Static biller catalogue ───────────────────────────────────

    private static final List<BillerDto> ELECTRICITY_BILLERS = List.of(
        BillerDto.builder().id("BESCOM").name("BESCOM – Bangalore Electricity Supply Company")
            .shortName("BESCOM").category("ELECTRICITY").icon("⚡").state("Karnataka")
            .consumerIdLabel("RR Number (Consumer Account No.)").consumerIdHint("e.g. 9123456789")
            .consumerIdLength(10).build(),
        BillerDto.builder().id("TSSPDCL").name("TSSPDCL – Telangana Southern Power Distribution")
            .shortName("TSSPDCL").category("ELECTRICITY").icon("⚡").state("Telangana")
            .consumerIdLabel("Service Number (CA Number)").consumerIdHint("e.g. 0012345678")
            .consumerIdLength(10).build(),
        BillerDto.builder().id("TATA_POWER_MUMBAI").name("Tata Power – Mumbai Distribution")
            .shortName("Tata Power").category("ELECTRICITY").icon("⚡").state("Maharashtra")
            .consumerIdLabel("Consumer Number").consumerIdHint("e.g. 4001234567")
            .consumerIdLength(10).build(),
        BillerDto.builder().id("ADANI_ELECTRICITY").name("Adani Electricity – Mumbai")
            .shortName("Adani").category("ELECTRICITY").icon("⚡").state("Maharashtra")
            .consumerIdLabel("Consumer Account Number").consumerIdHint("e.g. 100023456").build(),
        BillerDto.builder().id("MSEDCL").name("MSEDCL – Maharashtra State Electricity")
            .shortName("MSEDCL").category("ELECTRICITY").icon("⚡").state("Maharashtra")
            .consumerIdLabel("Consumer Number").consumerIdHint("e.g. 020012345678").build(),
        BillerDto.builder().id("TNEB").name("TNEB – Tamil Nadu Electricity Board")
            .shortName("TNEB").category("ELECTRICITY").icon("⚡").state("Tamil Nadu")
            .consumerIdLabel("Consumer Number (EB No.)").consumerIdHint("e.g. 001234567890").build(),
        BillerDto.builder().id("BSES_RAJDHANI").name("BSES Rajdhani – South & West Delhi")
            .shortName("BSES Rajdhani").category("ELECTRICITY").icon("⚡").state("Delhi")
            .consumerIdLabel("Consumer Account Number (CA)").consumerIdHint("e.g. 50012345678").build(),
        BillerDto.builder().id("BSES_YAMUNA").name("BSES Yamuna – East & Central Delhi")
            .shortName("BSES Yamuna").category("ELECTRICITY").icon("⚡").state("Delhi")
            .consumerIdLabel("Consumer Account Number (CA)").consumerIdHint("e.g. 30012345678").build()
    );

    private static final List<BillerDto> GAS_BILLERS = List.of(
        BillerDto.builder().id("MGL").name("MGL – Mahanagar Gas Limited")
            .shortName("MGL").category("GAS").icon("🔥").state("Maharashtra")
            .consumerIdLabel("Business Partner (BP) Number").consumerIdHint("e.g. 200012345").build(),
        BillerDto.builder().id("IGL").name("IGL – Indraprastha Gas Limited")
            .shortName("IGL").category("GAS").icon("🔥").state("Delhi")
            .consumerIdLabel("BP Number").consumerIdHint("e.g. 100009876").build()
    );

    private static final Map<String, List<BillerDto>> BILLER_CATALOGUE = Map.of(
        "ELECTRICITY", ELECTRICITY_BILLERS,
        "GAS", GAS_BILLERS
    );

    // ── Endpoints ─────────────────────────────────────────────────

    @GetMapping("/billers")
    public ResponseEntity<List<BillerDto>> getBillers(@RequestParam(defaultValue = "ELECTRICITY") String category) {
        log.info("[BBPS] getBillers category={}", category);
        List<BillerDto> billers = BILLER_CATALOGUE.getOrDefault(category.toUpperCase(), List.of());
        return ResponseEntity.ok(billers);
    }

    @PostMapping("/bill/fetch")
    public ResponseEntity<FetchedBillDto> fetchBill(@RequestBody BillFetchRequest request) {
        log.info("[BBPS] fetchBill billerId={} consumerId={}", request.getBillerId(), request.getConsumerId());

        BillerDto biller = BILLER_CATALOGUE.values().stream()
            .flatMap(List::stream)
            .filter(b -> b.getId().equals(request.getBillerId()))
            .findFirst()
            .orElse(null);

        String billerName = biller != null ? biller.getName() : request.getBillerId();

        LocalDate now = LocalDate.now();
        LocalDate periodFrom = now.withDayOfMonth(1).minusMonths(1);
        LocalDate periodTo = now.withDayOfMonth(1).minusDays(1);
        LocalDate dueDate = now.withDayOfMonth(20);
        if (dueDate.isBefore(now)) dueDate = dueDate.plusMonths(1);

        int units = 150 + ThreadLocalRandom.current().nextInt(80);
        double amount = Math.round((units * 6.50 + 180.0) * 100.0) / 100.0;

        String billNumber = "BBPS-" + request.getBillerId().substring(0, Math.min(4, request.getBillerId().length()))
            + "-" + (System.currentTimeMillis() % 1_000_000L);

        FetchedBillDto bill = FetchedBillDto.builder()
            .billerId(request.getBillerId())
            .billerName(billerName)
            .consumerId(request.getConsumerId())
            .customerName("Resident Account")
            .billNumber(billNumber)
            .billDate(periodFrom.format(DateTimeFormatter.ISO_LOCAL_DATE))
            .dueDate(dueDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
            .amount(amount)
            .units(units)
            .periodFrom(periodFrom.format(DateTimeFormatter.ISO_LOCAL_DATE))
            .periodTo(periodTo.format(DateTimeFormatter.ISO_LOCAL_DATE))
            .status("DUE")
            .build();

        return ResponseEntity.ok(bill);
    }

    @PostMapping("/bill/pay")
    public ResponseEntity<BillPaymentResponse> payBill(@RequestBody BillPayRequest request) {
        log.info("[BBPS] payBill billNumber={} billerId={} amount={} method={}",
            request.getBillNumber(), request.getBillerId(), request.getAmount(), request.getMethod());

        String pa = "bbps@setu";
        String pn = "Mana+Pay+BBPS";
        String tr = "MANA" + System.currentTimeMillis();
        String tn = "Electricity+Bill";
        double amount = request.getAmount() != null ? request.getAmount() : 0.0;

        String upiUrl = String.format(
            "upi://pay?pa=%s&pn=%s&am=%.2f&cu=INR&tr=%s&tn=%s",
            pa, pn, amount, tr, tn);
        String phonePeUrl = String.format(
            "phonepe://pay?pa=%s&pn=%s&am=%.2f&cu=INR&tr=%s",
            pa, pn, amount, tr);

        BillPaymentResponse resp = BillPaymentResponse.builder()
            .upiIntentUrl(upiUrl)
            .phonePeUrl(phonePeUrl)
            .transactionRef(tr)
            .amount(amount)
            .build();

        return ResponseEntity.ok(resp);
    }
}
