package com.manacommunity.api.payments.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.groupbuying.service.GroupBuyPaymentConfirmationService;
import com.manacommunity.api.market.bazaar.listener.MarketBazaarPaymentListener.CashfreePaymentSucceededEvent;
import com.manacommunity.api.notification.orchestrator.NotificationDtos.UnifiedNotificationRequest;
import com.manacommunity.api.notification.orchestrator.NotificationEnums.DeliveryStrategy;
import com.manacommunity.api.notification.orchestrator.NotificationEnums.NotificationCategory;
import com.manacommunity.api.notification.orchestrator.NotificationEnums.NotificationPriority;
import com.manacommunity.api.notification.orchestrator.UnifiedNotificationService;
import com.manacommunity.api.sports.model.SportsEventRegistration;
import com.manacommunity.api.sports.repository.SportsEventRegistrationRepository;
import com.manacommunity.platform.sqs.SqsEventEnvelope;
import com.manacommunity.platform.sqs.SqsMessageHandler;
import com.manacommunity.platform.sqs.SqsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * SQS consumer for {@code mana-payment-events}.
 *
 * <p>Decouples downstream payment notification fan-out (SMS/Email/Push via
 * {@link UnifiedNotificationService}) and module business confirmation
 * workflows from Cashfree and Razorpay webhook transactions so webhooks return
 * HTTP 200 within milliseconds.</p>
 *
 * <p>Supported {@code eventType} values:</p>
 * <ul>
 *   <li>{@code PAYMENT_NOTIFICATION}           — notifies payer of payment success or failure</li>
 *   <li>{@code CASHFREE_BUSINESS_CONFIRMATION} — confirms originating order/registration</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventsSqsHandler implements SqsMessageHandler {

    private final ObjectProvider<UnifiedNotificationService> notificationServiceProvider;
    private final SportsEventRegistrationRepository regRepository;
    private final ObjectProvider<GroupBuyPaymentConfirmationService> groupBuyPaymentConfirmationService;
    private final ObjectProvider<com.manacommunity.api.mobility.service.MobilityPaymentCallbackService> mobilityPaymentCallbackService;
    private final ApplicationEventPublisher eventPublisher;
    private final SqsProperties sqsProperties;
    private final ObjectMapper objectMapper;

    public record PaymentNotificationPayload(
            Long userId,
            String orderRef,
            String gateway,
            BigDecimal amount,
            boolean success,
            String failureReason
    ) {}

    public record CashfreeBusinessConfirmationPayload(
            String manaOrderId,
            String cfPaymentId,
            String paymentMethod,
            BigDecimal amount,
            Long userId,
            String referenceType,
            Long referenceId
    ) {}

    @Override
    public String queueName() {
        return sqsProperties.getQueues().getPaymentEvents();
    }

    @Override
    @Transactional
    public void handle(SqsEventEnvelope envelope) throws Exception {
        switch (envelope.eventType()) {
            case "PAYMENT_NOTIFICATION" -> deliverPaymentNotification(
                    objectMapper.readValue(envelope.payloadJson(), PaymentNotificationPayload.class));
            case "CASHFREE_BUSINESS_CONFIRMATION" -> deliverBusinessConfirmation(
                    objectMapper.readValue(envelope.payloadJson(), CashfreeBusinessConfirmationPayload.class));
            default -> log.debug("Unhandled payment SQS event type={} eventId={}",
                    envelope.eventType(), envelope.eventId());
        }
    }

    public void deliverPaymentNotification(PaymentNotificationPayload payload) {
        if (payload == null || payload.userId() == null) return;
        UnifiedNotificationService notificationService = notificationServiceProvider.getIfAvailable();
        if (notificationService == null) {
            log.warn("UnifiedNotificationService not available for order {}", payload.orderRef());
            return;
        }
        try {
            double amountVal = payload.amount() != null ? payload.amount().doubleValue() : 0.0;
            String amountStr = payload.amount() != null ? payload.amount().toPlainString() : "0.00";
            String title = payload.success() ? "Payment Successful" : "Payment Failed";
            String body = payload.success()
                    ? String.format("Your payment of ₹%.2f was successful. Order ID: %s",
                            amountVal, payload.orderRef())
                    : String.format("Your payment of ₹%.2f could not be completed. %s",
                            amountVal,
                            payload.failureReason() != null ? payload.failureReason() : "Please try again.");

            notificationService.orchestrate(
                    UnifiedNotificationRequest.builder()
                            .recipientUserId(payload.userId())
                            .category(NotificationCategory.FINANCIAL_BILLING)
                            .priority(NotificationPriority.HIGH)
                            .strategy(DeliveryStrategy.PREFERENCE_BASED)
                            .title(title)
                            .body(body)
                            .data(Map.of(
                                    "type", "PAYMENT_" + (payload.success() ? "SUCCESS" : "FAILED"),
                                    "manaOrderId", payload.orderRef() != null ? payload.orderRef() : "",
                                    "gateway", payload.gateway() != null ? payload.gateway() : "",
                                    "amount", amountStr
                            ))
                            .build()
            );
        } catch (Exception e) {
            log.warn("Payment notification dispatch failed for order {}: {}", payload.orderRef(), e.getMessage());
        }
    }

    @Transactional
    public void deliverBusinessConfirmation(CashfreeBusinessConfirmationPayload payload) {
        if (payload == null || payload.referenceType() == null) return;
        switch (payload.referenceType()) {
            case "SPORTS_REGISTRATION" -> confirmSportsRegistration(payload);
            case "GROUP_BUY_ORDER" -> confirmGroupBuyOrder(payload);
            case "MOBILITY_TRIP", "MOBILITY_CARPOOL" -> {
                var mobilityCb = mobilityPaymentCallbackService.getIfAvailable();
                if (mobilityCb != null) {
                    mobilityCb.confirmPayment(
                            payload.referenceType(), payload.referenceId(),
                            payload.manaOrderId(), payload.cfPaymentId(), "CASHFREE");
                }
            }
            case "MARKET_STALL_BOOKING" -> {
                if (payload.userId() != null) {
                    eventPublisher.publishEvent(new CashfreePaymentSucceededEvent(
                            payload.referenceType(), payload.referenceId(), payload.userId(), payload.amount()));
                }
            }
            default -> log.debug("No business confirmation handler for referenceType={}", payload.referenceType());
        }
    }

    private void confirmSportsRegistration(CashfreeBusinessConfirmationPayload payload) {
        if (payload.userId() == null || payload.referenceId() == null) {
            log.warn("Cannot confirm sports registration: missing user or referenceId on order {}", payload.manaOrderId());
            return;
        }
        Long eventId = payload.referenceId();
        Long userId = payload.userId();

        regRepository.findFirstByEventIdAndUserIdAndStatus(
                eventId, userId, SportsEventRegistration.RegistrationStatus.PENDING)
            .ifPresentOrElse(reg -> {
                reg.setStatus(SportsEventRegistration.RegistrationStatus.CONFIRMED);
                reg.setReviewedAt(LocalDateTime.now());
                regRepository.save(reg);
                log.info("Sports registration {} CONFIRMED after payment {}", reg.getId(), payload.manaOrderId());
            }, () -> log.warn("No PENDING sports registration found for eventId={} userId={} after payment {}",
                    eventId, userId, payload.manaOrderId()));
    }

    private void confirmGroupBuyOrder(CashfreeBusinessConfirmationPayload payload) {
        if (payload.referenceId() == null) {
            log.warn("Cannot confirm group buy order: missing referenceId on order {}", payload.manaOrderId());
            return;
        }
        try {
            var confirmationService = groupBuyPaymentConfirmationService.getIfAvailable();
            if (confirmationService != null) {
                confirmationService.confirmPayment(
                        payload.referenceId(),
                        payload.manaOrderId(),
                        payload.cfPaymentId(),
                        payload.paymentMethod(),
                        payload.amount()
                );
                log.info("Group buy payment confirmed for commerceOrderId={} via payment {}",
                        payload.referenceId(), payload.manaOrderId());
            } else {
                log.warn("GroupBuyPaymentConfirmationService not available to confirm group buy order {}",
                        payload.manaOrderId());
            }
        } catch (Exception e) {
            log.error("Failed to confirm group buy order {}: {}", payload.manaOrderId(), e.getMessage(), e);
        }
    }
}
