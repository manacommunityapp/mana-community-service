package com.manacommunity.api.food.controller;

import com.manacommunity.api.food.entity.FoodGroceryOrder;
import com.manacommunity.api.food.repository.FoodGroceryOrderRepository;
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
 * Initiates payment orders for food grocery orders.
 *
 * <p>TRUST: amount is taken from the stored FoodGroceryOrder entity — never from the client request.
 * Payment confirmation happens via gateway webhook, never via frontend callback.
 */
@Slf4j
@Tag(name = "Food Grocery Order Payments")
@RestController
@RequestMapping("/api/food/grocery-orders")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class FoodGroceryOrderPaymentController {

    private final FoodGroceryOrderRepository groceryOrderRepo;
    private final UnifiedPaymentOrderService paymentOrderService;

    @Operation(summary = "Create a payment order for a food grocery order")
    @PostMapping("/{orderId}/payment-order")
    public ResponseEntity<UnifiedPaymentOrderResponse> createPaymentOrder(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {

        FoodGroceryOrder groceryOrder = groceryOrderRepo.findByIdAndCommunityId(orderId, principal.getCommunityId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Grocery order not found or access denied: " + orderId));

        // Amount from server entity — never trust client
        BigDecimal amount = groceryOrder.getTotalAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order has no payable amount");
        }

        PaymentGateway gateway = PaymentGateway.valueOf(
                body.getOrDefault("gateway", "CASHFREE").toUpperCase());

        PaymentOrder order = paymentOrderService.createOrder(
                CreatePaymentOrderRequest.builder()
                        .communityId(principal.getCommunityId())
                        .idempotencyKey("FOOD-GROCERY-" + orderId + "-" + principal.getUserId())
                        .gateway(gateway)
                        .referenceType(PaymentReferenceType.FOOD_GROCERY_ORDER)
                        .referenceId(orderId)
                        .payerUserId(principal.getUserId())
                        .amount(amount)
                        .build());

        groceryOrder.setPaymentOrderId(order.getId());
        groceryOrderRepo.save(groceryOrder);

        log.info("[GroceryPayment] Created payment order {} for grocery order {}", order.getId(), orderId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UnifiedPaymentOrderResponse.from(order));
    }
}
