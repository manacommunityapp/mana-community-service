package com.manacommunity.api.messaging.workload.service;

import com.manacommunity.api.messaging.workload.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Core routing engine orchestrating dual-bus dispatch across Kafka and RabbitMQ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DualBusMessageRouter {

    private final WorkloadClassifierService classifierService;
    private final KafkaMessageBusAdapter kafkaAdapter;
    private final RabbitMQMessageBusAdapter rabbitMQAdapter;
    private final DeadLetterAndRetryEngine deadLetterEngine;

    /**
     * Routes a workload message to Kafka, RabbitMQ, or both according to architectural profile.
     */
    public RoutingResult route(WorkloadMessage message) {
        WorkloadMessage normalizedMessage = ensureMessageDefaults(message);
        RoutingDecision decision = classifierService.classify(normalizedMessage);

        try {
            return switch (decision.getTargetBroker()) {
                case KAFKA -> kafkaAdapter.publish(normalizedMessage, decision);
                case RABBITMQ -> rabbitMQAdapter.publish(normalizedMessage, decision);
                case DUAL_BUS -> routeDualBus(normalizedMessage, decision);
            };
        } catch (Exception ex) {
            log.error("[ROUTING-FAILURE] Failed to publish message {} (type: {}). Handing off to dead-letter engine.",
                    normalizedMessage.getMessageId(), normalizedMessage.getWorkloadType(), ex);
            return deadLetterEngine.deadLetter(normalizedMessage, decision, ex.getMessage());
        }
    }

    /**
     * Dispatches simultaneously to Kafka (immutable audit log) and RabbitMQ (instant edge push/worker queue).
     */
    private RoutingResult routeDualBus(WorkloadMessage message, RoutingDecision decision) {
        log.info("[DUAL-BUS] Executing concurrent fanout for MsgId={} Type={}",
                message.getMessageId(), message.getWorkloadType());

        RoutingResult kafkaResult = kafkaAdapter.publish(message, decision);
        RoutingResult rabbitResult = rabbitMQAdapter.publish(message, decision);

        List<String> combinedDestinations = new ArrayList<>();
        combinedDestinations.addAll(kafkaResult.getDeliveredDestinations());
        combinedDestinations.addAll(rabbitResult.getDeliveredDestinations());

        return RoutingResult.builder()
                .messageId(message.getMessageId())
                .workloadType(message.getWorkloadType())
                .dispatchedBroker(TargetBroker.DUAL_BUS)
                .success(kafkaResult.isSuccess() && rabbitResult.isSuccess())
                .deliveredDestinations(combinedDestinations)
                .partitionAssigned(kafkaResult.getPartitionAssigned() + " & " + rabbitResult.getPartitionAssigned())
                .priorityDelivered(decision.getPriority().getAmqpPriorityLevel())
                .dispatchedAt(Instant.now())
                .build();
    }

    // --- Domain-Specific Convenience Producers ---

    public RoutingResult routeGateAnpr(String societyId, String gateId, String plateNumber) {
        WorkloadMessage msg = WorkloadMessage.builder()
                .workloadType(WorkloadType.GATE_ANPR_STREAM)
                .societyId(societyId)
                .partitionOrRoutingKey(societyId + ":" + gateId)
                .payloadJson(String.format("{\"plate\":\"%s\",\"gate\":\"%s\",\"timestamp\":\"%s\"}", plateNumber, gateId, Instant.now()))
                .build();
        return route(msg);
    }

    public RoutingResult routeEmergencySos(String societyId, String flatId, String residentName) {
        WorkloadMessage msg = WorkloadMessage.builder()
                .workloadType(WorkloadType.EMERGENCY_SOS_FANOUT)
                .societyId(societyId)
                .priority(WorkloadPriority.CRITICAL_EMERGENCY)
                .partitionOrRoutingKey(societyId)
                .payloadJson(String.format("{\"flatId\":\"%s\",\"resident\":\"%s\",\"alert\":\"MEDICAL_SOS\"}", flatId, residentName))
                .build();
        return route(msg);
    }

    public RoutingResult routeMonthlyBillingInvoice(String societyId, String invoiceId, String flatId) {
        WorkloadMessage msg = WorkloadMessage.builder()
                .workloadType(WorkloadType.MONTHLY_INVOICE_BATCH)
                .societyId(societyId)
                .partitionOrRoutingKey(societyId + ":" + flatId)
                .payloadJson(String.format("{\"invoiceId\":\"%s\",\"flatId\":\"%s\",\"societyId\":\"%s\"}", invoiceId, flatId, societyId))
                .build();
        return route(msg);
    }

    public RoutingResult routePaymentWebhook(String gateway, String transactionId, String payloadJson) {
        WorkloadMessage msg = WorkloadMessage.builder()
                .workloadType(WorkloadType.PAYMENT_GATEWAY_WEBHOOK)
                .societyId("gateway-" + gateway)
                .partitionOrRoutingKey(gateway + "." + transactionId)
                .payloadJson(payloadJson)
                .build();
        return route(msg);
    }

    public RoutingResult routeDailyHelpAttendance(String societyId, String helperId, String helperName, List<String> flatIds) {
        WorkloadMessage msg = WorkloadMessage.builder()
                .workloadType(WorkloadType.DAILY_HELP_ATTENDANCE)
                .societyId(societyId)
                .partitionOrRoutingKey(societyId + ":" + helperId)
                .payloadJson(String.format("{\"helperId\":\"%s\",\"helperName\":\"%s\",\"flats\":%s}", helperId, helperName, flatIds))
                .build();
        return route(msg);
    }

    private WorkloadMessage ensureMessageDefaults(WorkloadMessage message) {
        if (message.getMessageId() == null || message.getCorrelationId() == null) {
            String id = UUID.randomUUID().toString();
            return message.toBuilder()
                    .messageId(message.getMessageId() != null ? message.getMessageId() : id)
                    .correlationId(message.getCorrelationId() != null ? message.getCorrelationId() : id)
                    .build();
        }
        return message;
    }
}
