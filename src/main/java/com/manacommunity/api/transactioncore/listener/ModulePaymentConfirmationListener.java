package com.manacommunity.api.transactioncore.listener;

import com.manacommunity.api.booking.entity.enums.PaymentStatus;
import com.manacommunity.api.booking.repository.ResourceBookingRepository;
import com.manacommunity.api.events.repository.EventBookingRegistrationRepository;
import com.manacommunity.api.health.repository.HealthAppointmentRepository;
import com.manacommunity.api.payments.entity.PaymentOrder;
import com.manacommunity.api.payments.repository.PaymentOrderRepository;
import com.manacommunity.api.transactioncore.event.GatewayPaymentCapturedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Updates the originating module entity's paymentStatus when a gateway payment is confirmed.
 *
 * <p>Runs AFTER_COMMIT alongside {@link PaymentLedgerPostingListener}. A failure here
 * is logged but must never roll back the payment confirmation — the payment_order record
 * is already committed as SUCCESS before this fires.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModulePaymentConfirmationListener {

    private final PaymentOrderRepository              paymentOrderRepository;
    private final EventBookingRegistrationRepository  eventRegistrationRepo;
    private final ResourceBookingRepository           resourceBookingRepo;
    private final HealthAppointmentRepository         healthAppointmentRepo;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCaptured(GatewayPaymentCapturedEvent event) {
        try {
            PaymentOrder order = paymentOrderRepository.findById(event.getInternalOrderId())
                    .orElse(null);
            if (order == null) {
                log.warn("[ModuleConfirm] PaymentOrder {} not found — cannot update module entity",
                        event.getInternalOrderId());
                return;
            }

            switch (order.getReferenceType()) {
                case EVENT_REGISTRATION, POOJA_BOOKING ->
                    eventRegistrationRepo.findById(order.getReferenceId()).ifPresent(reg -> {
                        reg.setPaymentStatus("PAID");
                        eventRegistrationRepo.save(reg);
                        log.info("[ModuleConfirm] Event registration {} marked PAID", order.getReferenceId());
                    });

                case AMENITY_BOOKING ->
                    resourceBookingRepo.findById(order.getReferenceId()).ifPresent(booking -> {
                        booking.setPaymentStatus(PaymentStatus.PAID);
                        resourceBookingRepo.save(booking);
                        log.info("[ModuleConfirm] Resource booking {} marked PAID", order.getReferenceId());
                    });

                case HEALTH_BOOKING ->
                    healthAppointmentRepo.findById(order.getReferenceId()).ifPresent(appt -> {
                        appt.setPaymentReferenceId(event.getGatewayPaymentId());
                        healthAppointmentRepo.save(appt);
                        log.info("[ModuleConfirm] Health appointment {} payment ref updated", order.getReferenceId());
                    });

                default ->
                    log.debug("[ModuleConfirm] No module update needed for referenceType={}", order.getReferenceType());
            }
        } catch (Exception e) {
            // Module status update failure must NOT roll back the payment record.
            log.error("[ModuleConfirm] Failed to update module entity for payment order {}: {}",
                    event.getInternalOrderId(), e.getMessage(), e);
        }
    }
}
