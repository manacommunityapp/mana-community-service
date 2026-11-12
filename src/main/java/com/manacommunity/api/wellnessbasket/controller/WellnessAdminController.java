package com.manacommunity.api.wellnessbasket.controller;

import com.manacommunity.api.wellnessbasket.dto.WellnessClusterDto;
import com.manacommunity.api.wellnessbasket.dto.WellnessSwapDto;
import com.manacommunity.api.wellnessbasket.service.WellnessClusterService;
import com.manacommunity.api.wellnessbasket.service.WellnessMenuRotationService;
import com.manacommunity.api.wellnessbasket.service.WellnessSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/v1/wellness-basket/admin", "/wellness-basket/admin"})
@RequiredArgsConstructor
public class WellnessAdminController {

    private final WellnessMenuRotationService menuRotationService;
    private final WellnessClusterService clusterService;
    private final WellnessSubscriptionService subscriptionService;

    @GetMapping("/swaps/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DIETITIAN')")
    public ResponseEntity<List<WellnessSwapDto>> getPendingSwaps() {
        return ResponseEntity.ok(menuRotationService.getPendingSwaps());
    }

    @PatchMapping("/swaps/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DIETITIAN')")
    public ResponseEntity<WellnessSwapDto> approveSwap(@PathVariable Long id) {
        return ResponseEntity.ok(menuRotationService.approveSwap(id));
    }

    @PatchMapping("/swaps/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DIETITIAN')")
    public ResponseEntity<WellnessSwapDto> rejectSwap(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String reason = body != null ? body.getOrDefault("reason", "Item unavailable or nutritional conflict") : "Rejected by dietitian";
        return ResponseEntity.ok(menuRotationService.rejectSwap(id, reason));
    }

    @PatchMapping("/clusters/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<WellnessClusterDto> updateClusterStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        return ResponseEntity.ok(clusterService.updateClusterStatus(id, status));
    }

    @PostMapping("/trigger-daily-orders")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> triggerDailyOrders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
    ) {
        LocalDate date = targetDate != null ? targetDate : LocalDate.now().plusDays(1);
        int generated = subscriptionService.generateDailyWellnessBaskets(date);
        return ResponseEntity.ok(Map.of(
                "targetDate", date,
                "ordersGenerated", generated,
                "status", "SUCCESS"
        ));
    }
}
