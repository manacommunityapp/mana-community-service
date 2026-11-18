package com.manacommunity.api.health.controller;

import com.manacommunity.api.health.entity.HealthAppointment;
import com.manacommunity.api.health.repository.HealthAppointmentRepository;
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
 * Initiates payment orders for health appointment consultations.
 *
 * <p>TRUST: payment confirmation happens via gateway webhook, never via frontend callback.
 */
@Slf4j
@Tag(name = "Health Appointment Payments")
@RestController
@RequestMapping("/api/health/appointments")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class HealthAppointmentPaymentController {

    private final HealthAppointmentRepository appointmentRepo;
    private final UnifiedPaymentOrderService paymentOrderService;

    @Operation(summary = "Create a payment order for a health appointment consultation fee")
    @PostMapping("/{appointmentId}/payment-order")
    public ResponseEntity<UnifiedPaymentOrderResponse> createPaymentOrder(
            @PathVariable Long appointmentId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {

        HealthAppointment appointment = appointmentRepo.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Appointment not found: " + appointmentId));

        if (!principal.getUserId().equals(appointment.getBookedByUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        BigDecimal fee = appointment.getConsultationFee();
        if (fee == null || fee.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Appointment has no payable consultation fee");
        }

        PaymentGateway gateway = PaymentGateway.valueOf(
                body.getOrDefault("gateway", "CASHFREE").toUpperCase());

        PaymentOrder order = paymentOrderService.createOrder(
                CreatePaymentOrderRequest.builder()
                        .communityId(principal.getCommunityId())
                        .idempotencyKey("HEALTH-APPT-" + appointmentId)
                        .gateway(gateway)
                        .referenceType(PaymentReferenceType.HEALTH_BOOKING)
                        .referenceId(appointmentId)
                        .payerUserId(principal.getUserId())
                        .amount(fee)
                        .build());

        appointment.setPaymentOrderId(order.getId());
        appointmentRepo.save(appointment);

        log.info("[HealthPayment] Created payment order {} for appointment {}", order.getId(), appointmentId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UnifiedPaymentOrderResponse.from(order));
    }
}
