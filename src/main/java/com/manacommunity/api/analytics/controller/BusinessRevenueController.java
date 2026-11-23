package com.manacommunity.api.analytics.controller;

import com.manacommunity.api.analytics.dto.BusinessRevenueSummary;
import com.manacommunity.api.analytics.dto.CommunitySubscriptionRequest;
import com.manacommunity.api.analytics.dto.CommunitySubscriptionResponse;
import com.manacommunity.api.analytics.dto.BusinessModuleFinanceRequest;
import com.manacommunity.api.analytics.entity.BusinessModuleFinanceDaily;
import com.manacommunity.api.analytics.service.CommunitySubscriptionAdminService;
import com.manacommunity.api.analytics.service.BusinessModuleFinanceAdminService;
import com.manacommunity.api.analytics.service.BusinessRevenueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/business-revenue")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class BusinessRevenueController {
    private final BusinessRevenueService businessRevenueService;
    private final CommunitySubscriptionAdminService subscriptionAdminService;
    private final BusinessModuleFinanceAdminService moduleFinanceAdminService;

    @GetMapping("/summary")
    public ResponseEntity<BusinessRevenueSummary> getSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Double gatewayMdrRate,
            @RequestParam(required = false) Double perTxnFixedCost,
            @RequestParam(required = false) Double moduleHostingAllocation) {
        if (from.isAfter(to)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from must be on or before to");
        return ResponseEntity.ok(businessRevenueService.getSummary(from, to, gatewayMdrRate, perTxnFixedCost, moduleHostingAllocation));
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<List<CommunitySubscriptionResponse>> listSubscriptions() {
        return ResponseEntity.ok(subscriptionAdminService.list());
    }

    @PostMapping("/subscriptions")
    public ResponseEntity<CommunitySubscriptionResponse> createSubscription(
            @Valid @RequestBody CommunitySubscriptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subscriptionAdminService.create(request));
    }

    @PutMapping("/subscriptions/{id}")
    public ResponseEntity<CommunitySubscriptionResponse> updateSubscription(
            @PathVariable Long id, @Valid @RequestBody CommunitySubscriptionRequest request) {
        return ResponseEntity.ok(subscriptionAdminService.update(id, request));
    }

    @PostMapping("/module-finance")
    public ResponseEntity<BusinessModuleFinanceDaily> upsertModuleFinance(
            @Valid @RequestBody BusinessModuleFinanceRequest request) {
        return ResponseEntity.ok(moduleFinanceAdminService.upsert(request));
    }
}
