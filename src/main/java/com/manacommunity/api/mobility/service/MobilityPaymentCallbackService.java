package com.manacommunity.api.mobility.service;

import com.manacommunity.api.mobility.model.MobilityCarpoolBooking;
import com.manacommunity.api.mobility.model.MobilityTrip;
import com.manacommunity.api.mobility.repository.MobilityCarpoolBookingRepository;
import com.manacommunity.api.mobility.repository.MobilityTripRepository;
import com.manacommunity.api.payments.entity.PaymentReceipt;
import com.manacommunity.api.payments.service.CashfreePaymentService;
import com.manacommunity.api.payments.service.PaymentReceiptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Locale;

/**
 * Handles payment capture confirmation, platform commission split, receipt generation,
 * and cancellation refunds for Mobility trips and carpool/journey bookings.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MobilityPaymentCallbackService {

    private static final BigDecimal PLATFORM_FEE_RATE = new BigDecimal("0.10");

    private final MobilityTripRepository tripRepository;
    private final MobilityCarpoolBookingRepository bookingRepository;
    private final PaymentReceiptService paymentReceiptService;
    private final ObjectProvider<CashfreePaymentService> cashfreePaymentServiceProvider;

    @Transactional
    public void confirmPayment(String referenceType, Long referenceId,
                               String orderId, String paymentRef, String gateway) {
        if (referenceType == null || referenceId == null) return;
        String normalized = referenceType.trim().toUpperCase(Locale.ROOT);
        if ("MOBILITY_TRIP".equals(normalized)) {
            confirmTripPayment(referenceId, orderId, paymentRef, gateway);
        } else if ("MOBILITY_CARPOOL".equals(normalized)) {
            confirmCarpoolPayment(referenceId, orderId, paymentRef);
        }
    }

    @Transactional
    public void confirmTripPayment(Long tripId, String orderId, String paymentRef, String gateway) {
        tripRepository.findById(tripId).ifPresent(trip -> {
            if ("PAID".equalsIgnoreCase(trip.getPaymentStatus())) return;

            BigDecimal amount = trip.getFinalFare() != null ? trip.getFinalFare() : trip.getQuotedTotal();
            if (amount == null) amount = BigDecimal.ZERO;

            BigDecimal fee = "CASH".equalsIgnoreCase(gateway)
                    ? BigDecimal.ZERO
                    : amount.multiply(PLATFORM_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
            BigDecimal payout = amount.subtract(fee).max(BigDecimal.ZERO);

            String receiptNo = trip.getReceiptNumber();
            if (receiptNo == null || receiptNo.isBlank()) {
                receiptNo = "MOB-RCP-" + trip.getTripRef();
            }

            trip.setPaymentStatus("PAID");
            trip.setPaymentOrderId(orderId);
            trip.setPaymentRef(paymentRef);
            trip.setGateway(gateway != null ? gateway.toUpperCase(Locale.ROOT) : "ONLINE");
            trip.setPlatformFee(fee);
            trip.setDriverPayout(payout);
            trip.setPaidAt(Instant.now());
            trip.setReceiptNumber(receiptNo);
            tripRepository.save(trip);

            if (trip.getCommunity() != null && trip.getPassenger() != null) {
                try {
                    PaymentReceipt receipt = new PaymentReceipt();
                    receipt.setCommunityId(trip.getCommunity().getId());
                    receipt.setUserId(trip.getPassenger().getId());
                    receipt.setSourceModule("MOBILITY_TRIP");
                    receipt.setReferenceId(trip.getId());
                    receipt.setReceiptNumber(receiptNo);
                    receipt.setAmount(amount);
                    receipt.setPaymentMethod(gateway != null ? gateway : trip.getPaymentMethod());
                    receipt.setGatewayRef(paymentRef != null ? paymentRef : orderId);
                    receipt.setPaidAt(trip.getPaidAt());
                    paymentReceiptService.issue(receipt);
                } catch (Exception e) {
                    log.warn("Could not store PaymentReceipt for trip {}: {}", tripId, e.getMessage());
                }
            }
            log.info("Mobility trip {} marked PAID orderId={} gateway={} amount={}",
                    tripId, orderId, gateway, amount);
        });
    }

    @Transactional
    public void confirmCarpoolPayment(Long bookingId, String orderId, String paymentRef) {
        bookingRepository.findById(bookingId).ifPresent(booking -> {
            if ("PAID".equalsIgnoreCase(booking.getPaymentStatus())) return;
            booking.setPaymentStatus("PAID");
            booking.setPaymentOrderId(orderId);
            booking.setPaymentRef(paymentRef);
            bookingRepository.save(booking);
            log.info("Mobility carpool booking {} marked PAID orderId={}", bookingId, orderId);
        });
    }

    @Transactional
    public void refundTripOnCancellation(MobilityTrip trip, String reason) {
        if (trip == null || !"PAID".equalsIgnoreCase(trip.getPaymentStatus())) {
            return;
        }
        if ("CASH".equalsIgnoreCase(trip.getPaymentMethod())) {
            return;
        }
        BigDecimal refundAmt = trip.getFinalFare() != null ? trip.getFinalFare() : trip.getQuotedTotal();
        if (refundAmt == null || refundAmt.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        CashfreePaymentService cfService = cashfreePaymentServiceProvider.getIfAvailable();
        if (cfService != null && trip.getPaymentOrderId() != null && "CASHFREE".equalsIgnoreCase(trip.getGateway())) {
            try {
                cfService.initiateRefund(trip.getPaymentOrderId(), refundAmt,
                        reason != null ? reason : "Mobility trip cancelled");
                trip.setRefundStatus("REFUND_PENDING");
            } catch (Exception e) {
                log.warn("Cashfree refund call failed for trip {} order {}: {}",
                        trip.getId(), trip.getPaymentOrderId(), e.getMessage());
                trip.setRefundStatus("REFUND_PENDING");
            }
        } else {
            trip.setRefundStatus("REFUNDED");
            trip.setPaymentStatus("REFUNDED");
        }
        trip.setRefundAmount(refundAmt);
        trip.setDriverPayout(BigDecimal.ZERO);
        trip.setPlatformFee(BigDecimal.ZERO);
        tripRepository.save(trip);
    }
}
