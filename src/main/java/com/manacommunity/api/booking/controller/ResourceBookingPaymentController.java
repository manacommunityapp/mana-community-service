package com.manacommunity.api.booking.controller;

import com.manacommunity.api.booking.entity.ResourceBooking;
import com.manacommunity.api.booking.service.ResourceBookingService;
import com.manacommunity.api.payments.dto.UnifiedPaymentOrderResponse;
import com.manacommunity.api.payments.entity.PaymentOrder;
import com.manacommunity.api.payments.model.PaymentGateway;
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
 * Initiates payment orders for amenity/resource bookings.
 *
 * <p>TRUST: payment confirmation happens via gateway webhook, never via frontend callback.
 */
@Slf4j
@Tag(name = "Resource Booking Payments")
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ResourceBookingPaymentController {

    private final ResourceBookingService bookingService;
    private final com.manacommunity.api.booking.repository.ResourceBookingRepository bookingRepo;

    @Operation(summary = "Create a payment order for a resource booking")
    @PostMapping("/{bookingId}/payment-order")
    public ResponseEntity<UnifiedPaymentOrderResponse> createPaymentOrder(
            @PathVariable Long bookingId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {

        ResourceBooking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Booking not found: " + bookingId));

        if (!principal.getCommunityId().equals(booking.getCommunity().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        if (booking.getTotalAmount() == null || booking.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Booking has no payable amount");
        }

        PaymentGateway gateway = PaymentGateway.valueOf(
                body.getOrDefault("gateway", "CASHFREE").toUpperCase());

        PaymentOrder order = bookingService.initiateBookingPayment(
                bookingId,
                booking.getTotalAmount(),
                principal.getCommunityId(),
                principal.getUserId(),
                gateway);

        log.info("[BookingPayment] Created payment order {} for booking {}", order.getId(), bookingId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UnifiedPaymentOrderResponse.from(order));
    }
}
