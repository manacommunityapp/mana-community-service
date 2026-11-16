package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.entity.EventBookingRegistration;
import com.manacommunity.api.events.repository.EventBookingRegistrationRepository;
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
 * Initiates and tracks payment orders for event/pooja registrations.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST /api/events/registrations/{id}/payment-order  — create INITIATED payment order</li>
 *   <li>POST /api/events/registrations/{id}/payment-confirm — admin: mark offline payment collected</li>
 * </ul>
 *
 * <p>TRUST: payment is confirmed only via gateway webhook, never via frontend callback.
 */
@Slf4j
@Tag(name = "Event Registration Payments")
@RestController
@RequestMapping("/api/events/registrations")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class EventRegistrationPaymentController {

    private final EventBookingRegistrationRepository registrationRepo;
    private final UnifiedPaymentOrderService paymentOrderService;

    @Operation(summary = "Create a payment order for an event registration")
    @PostMapping("/{registrationId}/payment-order")
    public ResponseEntity<UnifiedPaymentOrderResponse> createPaymentOrder(
            @PathVariable Long registrationId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {

        EventBookingRegistration reg = registrationRepo.findById(registrationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Registration not found: " + registrationId));

        if (!principal.getCommunityId().equals(reg.getCommunity().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        if (reg.getBookingFee() == null || reg.getBookingFee() <= 0.0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Registration has no booking fee — no payment order required");
        }

        PaymentGateway gateway = PaymentGateway.valueOf(
                body.getOrDefault("gateway", "CASHFREE").toUpperCase());

        PaymentOrder order = paymentOrderService.createOrder(
                CreatePaymentOrderRequest.builder()
                        .communityId(principal.getCommunityId())
                        .idempotencyKey("EVENT-REG-" + registrationId)
                        .gateway(gateway)
                        .referenceType(PaymentReferenceType.EVENT_REGISTRATION)
                        .referenceId(registrationId)
                        .payerUserId(principal.getUserId())
                        .amount(BigDecimal.valueOf(reg.getBookingFee()))
                        .build());

        reg.setPaymentOrderId(order.getId());
        reg.setPaymentStatus("INITIATED");
        registrationRepo.save(reg);

        log.info("[EventPayment] Created payment order {} for registration {}", order.getId(), registrationId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UnifiedPaymentOrderResponse.from(order));
    }

    /**
     * Admin-only: record an offline/manual payment for a registration.
     * Sets paymentStatus to PAID directly — use only for cash/manual collections.
     */
    @Operation(summary = "Admin: confirm manual/offline payment for a registration")
    @PostMapping("/{registrationId}/payment-confirm")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> confirmOfflinePayment(
            @PathVariable Long registrationId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {

        EventBookingRegistration reg = registrationRepo.findById(registrationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Registration not found: " + registrationId));

        if (!principal.getCommunityId().equals(reg.getCommunity().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        reg.setPaymentStatus("PAID");
        reg.setTransactionId(body.getOrDefault("transactionId", "MANUAL"));
        reg.setPaymentMethod(body.getOrDefault("paymentMethod", "CASH"));
        registrationRepo.save(reg);

        log.info("[EventPayment] Admin confirmed offline payment for registration {}", registrationId);
        return ResponseEntity.noContent().build();
    }
}
