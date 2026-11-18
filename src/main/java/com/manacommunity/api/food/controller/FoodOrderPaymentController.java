package com.manacommunity.api.food.controller;

import com.manacommunity.api.food.entity.FoodOrder;
import com.manacommunity.api.food.repository.FoodOrderRepository;
import com.manacommunity.api.payments.dto.CreatePaymentOrderRequest;
import com.manacommunity.api.payments.dto.UnifiedPaymentOrderResponse;
import com.manacommunity.api.payments.entity.PaymentOrder;
import com.manacommunity.api.payments.model.PaymentGateway;
import com.manacommunity.api.payments.model.PaymentReferenceType;
import com.manacommunity.api.payments.service.UnifiedPaymentOrderService;
import com.manacommunity.api.user.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Initiates payment orders for food (canteen/restaurant) orders.
 *
 * <p>TRUST: amount is taken from the stored FoodOrder entity — never from the client request.
 * Payment confirmation happens via gateway webhook, never via frontend callback.
 */
@Slf4j
@Tag(name = "Food Order Payments")
@RestController
@RequestMapping("/api/food/orders")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class FoodOrderPaymentController {

    private final FoodOrderRepository foodOrderRepo;
    private final UnifiedPaymentOrderService paymentOrderService;

    @Operation(summary = "Create a payment order for a food order")
    @PostMapping("/{orderId}/payment-order")
    public ResponseEntity<UnifiedPaymentOrderResponse> createPaymentOrder(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {

        FoodOrder foodOrder = foodOrderRepo.findByIdAndCommunityId(orderId, principal.getCommunityId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Food order not found or access denied: " + orderId));

        // Amount from server entity — never trust client
        BigDecimal amount = foodOrder.getTotalAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order has no payable amount");
        }

        PaymentGateway gateway = PaymentGateway.valueOf(
                body.getOrDefault("gateway", "CASHFREE").toUpperCase());

        PaymentOrder order = paymentOrderService.createOrder(
                CreatePaymentOrderRequest.builder()
                        .communityId(principal.getCommunityId())
                        .idempotencyKey("FOOD-ORDER-" + orderId + "-" + principal.getUserId())
                        .gateway(gateway)
                        .referenceType(PaymentReferenceType.FOOD_ORDER)
                        .referenceId(orderId)
                        .payerUserId(principal.getUserId())
                        .amount(amount)
                        .build());

        foodOrder.setPaymentOrderId(order.getId());
        foodOrderRepo.save(foodOrder);

        log.info("[FoodPayment] Created payment order {} for food order {}", order.getId(), orderId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UnifiedPaymentOrderResponse.from(order));
    }
}
