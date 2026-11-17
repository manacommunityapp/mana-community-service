package com.manacommunity.api.trip.controller;

import com.manacommunity.api.payments.dto.CreatePaymentOrderRequest;
import com.manacommunity.api.payments.dto.UnifiedPaymentOrderResponse;
import com.manacommunity.api.payments.entity.PaymentOrder;
import com.manacommunity.api.payments.model.PaymentGateway;
import com.manacommunity.api.payments.model.PaymentReferenceType;
import com.manacommunity.api.payments.service.UnifiedPaymentOrderService;
import com.manacommunity.api.trip.entity.Trip;
import com.manacommunity.api.trip.entity.TripBooking;
import com.manacommunity.api.trip.repository.TripBookingRepository;
import com.manacommunity.api.trip.repository.TripRepository;
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
 * Initiates payment orders for community trip bookings.
 *
 * <p>TRUST: amount is taken from the stored TripBooking entity — never from the client request.
 * Community isolation is enforced by verifying the booking's parent Trip belongs to the
 * principal's community. Payment confirmation happens via gateway webhook.
 */
@Slf4j
@Tag(name = "Trip Booking Payments")
@RestController
@RequestMapping("/api/trips/bookings")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class TripBookingPaymentController {

    private final TripBookingRepository tripBookingRepo;
    private final TripRepository tripRepo;
    private final UnifiedPaymentOrderService paymentOrderService;

    @Operation(summary = "Create a payment order for a community trip booking")
    @PostMapping("/{bookingId}/payment-order")
    public ResponseEntity<UnifiedPaymentOrderResponse> createPaymentOrder(
            @PathVariable String bookingId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {

        TripBooking booking = tripBookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Trip booking not found: " + bookingId));

        // Verify community isolation via parent Trip
        Trip trip = tripRepo.findById(booking.getTripId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Trip not found for booking: " + bookingId));

        if (!principal.getCommunityId().equals(trip.getCommunityId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        // Only the booking owner may initiate payment
        if (!principal.getUserId().equals(booking.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the booking owner may pay");
        }

        // Amount from server entity — never trust client
        BigDecimal amount = booking.getTotalAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking has no payable amount");
        }

        PaymentGateway gateway = PaymentGateway.valueOf(
                body.getOrDefault("gateway", "CASHFREE").toUpperCase());

        // TripBooking uses String PK — store as-is in notes; referenceId is a numeric handle
        // We hash the string id to a stable long for referenceId so the FK constraint holds
        long numericRef = (long) bookingId.hashCode() & 0xFFFFFFFFL;

        PaymentOrder order = paymentOrderService.createOrder(
                CreatePaymentOrderRequest.builder()
                        .communityId(principal.getCommunityId())
                        .idempotencyKey("TRIP-BOOKING-" + bookingId + "-" + principal.getUserId())
                        .gateway(gateway)
                        .referenceType(PaymentReferenceType.TRIP_SETTLEMENT)
                        .referenceId(numericRef)
                        .payerUserId(principal.getUserId())
                        .amount(amount)
                        .build());

        booking.setPaymentOrderId(order.getId());
        tripBookingRepo.save(booking);

        log.info("[TripPayment] Created payment order {} for booking {}", order.getId(), bookingId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UnifiedPaymentOrderResponse.from(order));
    }
}
