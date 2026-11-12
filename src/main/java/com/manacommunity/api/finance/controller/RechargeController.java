package com.manacommunity.api.finance.controller;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

import java.util.List;
import java.util.Map;

/**
 * Mobile Recharge stub controller.
 *
 * Endpoints used by mobile rechargeService.ts and web rechargeService.ts:
 *   GET  /api/v1/recharge/plans?operatorId=JIO
 *   POST /api/v1/recharge/initiate
 *
 * Wire to Juspay / Razorpay BillPay / BBPS once recharge aggregator is provisioned.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/recharge")
@CrossOrigin(origins = "*")
public class RechargeController {

    // ── DTOs ─────────────────────────────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RechargePlanDto {
        private String id;
        private String operatorId;
        private String category;   // POPULAR | DATA | UNLIMITED | VALIDITY | TOPUP
        private Integer price;
        private String validity;
        private String data;
        private String calls;
        private String sms;
        private String description;
        private Boolean isBestSeller;
        private Boolean isRecommended;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class RechargeInitiateRequest {
        @NotBlank
        @Pattern(regexp = "\\d{10}", message = "mobileNumber must be a 10 digit number")
        private String mobileNumber;
        @NotBlank
        private String operatorId;
        @NotBlank
        private String circle;
        @NotBlank
        private String planId;
        @NotNull @Positive
        private Double amount;
        @NotBlank
        @Pattern(regexp = "PREPAID", message = "Only prepaid recharge is currently supported")
        private String rechargeType; // PREPAID | POSTPAID
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RechargeResponse {
        private String transactionId;
        private String status;
        private String upiIntentUrl;
        private String phonePeUrl;
        private Double amount;
        private String mobileNumber;
        private String operatorId;
        private String planDescription;
    }

    // ── Static plan catalogue ─────────────────────────────────────────────────

    private static final List<RechargePlanDto> JIO_PLANS = List.of(
        RechargePlanDto.builder().id("JIO-239").operatorId("JIO").category("POPULAR").price(239).validity("28 days").data("1.5 GB/day").calls("Unlimited").sms("100/day").description("Standard daily data pack").isBestSeller(true).build(),
        RechargePlanDto.builder().id("JIO-299").operatorId("JIO").category("UNLIMITED").price(299).validity("28 days").data("2 GB/day").calls("Unlimited").sms("100/day").description("Extra data for heavy users").isRecommended(true).build(),
        RechargePlanDto.builder().id("JIO-479").operatorId("JIO").category("DATA").price(479).validity("56 days").data("1.5 GB/day").calls("Unlimited").sms("100/day").description("2-month pack").build(),
        RechargePlanDto.builder().id("JIO-719").operatorId("JIO").category("VALIDITY").price(719).validity("84 days").data("1.5 GB/day").calls("Unlimited").sms("100/day").description("3-month plan").build(),
        RechargePlanDto.builder().id("JIO-2879").operatorId("JIO").category("VALIDITY").price(2879).validity("365 days").data("2 GB/day").calls("Unlimited").sms("100/day").description("Annual plan — best per-day value").build()
    );

    private static final List<RechargePlanDto> AIRTEL_PLANS = List.of(
        RechargePlanDto.builder().id("AIRTEL-249").operatorId("AIRTEL").category("POPULAR").price(249).validity("28 days").data("2 GB/day").calls("Unlimited").sms("100/day").description("Airtel Wynk + HelloTunes included").isBestSeller(true).build(),
        RechargePlanDto.builder().id("AIRTEL-299").operatorId("AIRTEL").category("UNLIMITED").price(299).validity("28 days").data("3 GB/day").calls("Unlimited").sms("100/day").description("Max data with Airtel XStream").isRecommended(true).build(),
        RechargePlanDto.builder().id("AIRTEL-479").operatorId("AIRTEL").category("DATA").price(479).validity("56 days").data("2 GB/day").calls("Unlimited").sms("100/day").description("2-month pack with OTT benefits").build(),
        RechargePlanDto.builder().id("AIRTEL-3599").operatorId("AIRTEL").category("VALIDITY").price(3599).validity("365 days").data("2.5 GB/day").calls("Unlimited").sms("100/day").description("Annual plan + Amazon Prime").build()
    );

    private static final List<RechargePlanDto> VI_PLANS = List.of(
        RechargePlanDto.builder().id("VI-239").operatorId("VI").category("POPULAR").price(239).validity("28 days").data("1.5 GB/day").calls("Unlimited").sms("100/day").description("Vi Movies & TV included").isBestSeller(true).build(),
        RechargePlanDto.builder().id("VI-299").operatorId("VI").category("UNLIMITED").price(299).validity("28 days").data("2.5 GB/day").calls("Unlimited").sms("100/day").description("Weekend Data Rollover benefit").isRecommended(true).build(),
        RechargePlanDto.builder().id("VI-719").operatorId("VI").category("VALIDITY").price(719).validity("84 days").data("1.5 GB/day").calls("Unlimited").sms("100/day").description("Quarterly plan").build()
    );

    private static final List<RechargePlanDto> BSNL_PLANS = List.of(
        RechargePlanDto.builder().id("BSNL-197").operatorId("BSNL").category("POPULAR").price(197).validity("26 days").data("2 GB/day").calls("Unlimited").sms("100/day").description("BSNL STV 197 daily data pack").isBestSeller(true).build(),
        RechargePlanDto.builder().id("BSNL-247").operatorId("BSNL").category("UNLIMITED").price(247).validity("30 days").data("2 GB/day").calls("Unlimited").sms("100/day").description("Monthly all-in-one pack").isRecommended(true).build()
    );

    private static final List<RechargePlanDto> MTNL_PLANS = List.of(
        RechargePlanDto.builder().id("MTNL-199").operatorId("MTNL").category("POPULAR").price(199).validity("28 days").data("1 GB/day").calls("Unlimited local + STD").sms("100/day").description("Mumbai/Delhi pack").isBestSeller(true).build()
    );

    private static final Map<String, List<RechargePlanDto>> PLAN_CATALOGUE = Map.of(
        "JIO", JIO_PLANS,
        "AIRTEL", AIRTEL_PLANS,
        "VI", VI_PLANS,
        "BSNL", BSNL_PLANS,
        "MTNL", MTNL_PLANS
    );

    // ── Endpoints ─────────────────────────────────────────────────────────────

    @GetMapping("/plans")
    public ResponseEntity<List<RechargePlanDto>> getPlans(
            @RequestParam(defaultValue = "JIO") String operatorId) {
        log.info("[Recharge] getPlans operatorId={}", operatorId);
        return ResponseEntity.ok(PLAN_CATALOGUE.getOrDefault(operatorId.toUpperCase(), List.of()));
    }

    @PostMapping("/initiate")
    public ResponseEntity<RechargeResponse> initiateRecharge(@Valid @RequestBody RechargeInitiateRequest request) {
        String operatorId = request.getOperatorId().trim().toUpperCase();
        List<RechargePlanDto> plans = PLAN_CATALOGUE.get(operatorId);
        if (plans == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported mobile operator");
        }
        RechargePlanDto plan = plans.stream()
            .filter(candidate -> candidate.getId().equalsIgnoreCase(request.getPlanId().trim()))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plan is not available for this operator"));
        if (Double.compare(request.getAmount(), plan.getPrice().doubleValue()) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount does not match the selected plan");
        }
        log.info("[Recharge] initiate mobile={} operator={} plan={} amount={}",
            request.getMobileNumber(), operatorId, plan.getId(), request.getAmount());

        String tr = "MANA-RCH-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        String pa = "recharge@manapay";
        String pn = "Mana+Pay+Recharge";
        double amount = request.getAmount() != null ? request.getAmount() : 0.0;

        String upiUrl = String.format(
            "upi://pay?pa=%s&pn=%s&am=%.2f&cu=INR&tr=%s&tn=Mobile+Recharge",
            pa, pn, amount, tr);
        String phonePeUrl = String.format(
            "phonepe://pay?pa=%s&pn=%s&am=%.2f&cu=INR&tr=%s",
            pa, pn, amount, tr);

        return ResponseEntity.ok(RechargeResponse.builder()
            .transactionId(tr)
            .status("PENDING")
            .upiIntentUrl(upiUrl)
            .phonePeUrl(phonePeUrl)
            .amount(amount)
            .mobileNumber(request.getMobileNumber())
            .operatorId(operatorId)
            .planDescription(plan.getDescription())
            .build());
    }
}
