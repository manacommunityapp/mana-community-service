package com.manacommunity.api.serviceplatform.pricing.controller;

import com.manacommunity.api.serviceplatform.pricing.dto.PriceCalculationRequest;
import com.manacommunity.api.serviceplatform.pricing.dto.PriceCalculationResponse;
import com.manacommunity.api.serviceplatform.pricing.entity.ServiceCoupon;
import com.manacommunity.api.serviceplatform.pricing.entity.ServiceQuote;
import com.manacommunity.api.serviceplatform.pricing.service.PricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/service-platform/pricing")
@RequiredArgsConstructor
public class PricingController {

    private final PricingService pricingService;

    @PostMapping("/estimate")
    public ResponseEntity<PriceCalculationResponse> estimate(@RequestBody PriceCalculationRequest request) {
        return ResponseEntity.ok(pricingService.estimatePrice(request));
    }

    @PostMapping("/quotes")
    public ResponseEntity<ServiceQuote> submitQuote(
            @RequestParam Long serviceRequestId,
            @RequestParam Long providerId,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) Integer durationMinutes,
            @RequestParam(required = false) String notes) {
        return ResponseEntity.ok(pricingService.submitQuote(serviceRequestId, providerId, amount, durationMinutes, notes));
    }

    @GetMapping("/quotes/request/{requestId}")
    public ResponseEntity<List<ServiceQuote>> getQuotes(@PathVariable Long requestId) {
        return ResponseEntity.ok(pricingService.getQuotesForRequest(requestId));
    }

    @PostMapping("/coupons")
    public ResponseEntity<ServiceCoupon> createCoupon(@RequestBody ServiceCoupon coupon) {
        return ResponseEntity.ok(pricingService.saveCoupon(coupon));
    }
}
