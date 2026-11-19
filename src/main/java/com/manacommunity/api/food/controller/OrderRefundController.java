package com.manacommunity.api.food.controller;

import com.manacommunity.api.food.entity.OrderRefund;
import com.manacommunity.api.food.entity.OrderRefund.RefundTrigger;
import com.manacommunity.api.food.service.OrderRefundService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/food/refunds")
@RequiredArgsConstructor
public class OrderRefundController {

    private final OrderRefundService refundService;

    @PostMapping
    public ResponseEntity<OrderRefund> initiate(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(201).body(refundService.initiateRefund(
            principal.getCommunityId(),
            Long.parseLong(body.get("orderId").toString()),
            body.get("orderType").toString(),
            principal.getUserId(),
            RefundTrigger.valueOf(body.get("triggerType").toString()),
            new BigDecimal(body.get("amount").toString()),
            body.containsKey("itemDetails") ? body.get("itemDetails").toString() : null
        ));
    }

    @GetMapping
    public ResponseEntity<List<OrderRefund>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String status) {
        if (status != null && status.equals("INITIATED")) {
            return ResponseEntity.ok(refundService.getPendingRefunds(principal.getCommunityId()));
        }
        return ResponseEntity.ok(refundService.getAllRefunds(principal.getCommunityId()));
    }

    @PutMapping("/{id}/review")
    public ResponseEntity<OrderRefund> review(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean approve = Boolean.parseBoolean(body.get("approve").toString());
        String notes = body.containsKey("notes") ? body.get("notes").toString() : null;
        return ResponseEntity.ok(refundService.processRefund(id, principal.getUserId(), approve, notes));
    }
}
