package com.manacommunity.api.messaging.workload.service;

import com.manacommunity.api.messaging.workload.model.*;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;

/**
 * Evaluates message workload profiles to determine target broker, routing keys,
 * partition strategies, and reliability policies.
 */
@Service
public class WorkloadClassifierService {

    private final Map<WorkloadType, WorkloadProfile> profileRegistry = new EnumMap<>(WorkloadType.class);

    public WorkloadClassifierService() {
        registerDefaultProfiles();
    }

    private void registerDefaultProfiles() {
        // 1. Ingress & Physical Security (Kafka: High volume, ordered, replayable)
        register(WorkloadProfile.builder()
                .workloadType(WorkloadType.GATE_ANPR_STREAM)
                .recommendedBroker(TargetBroker.KAFKA)
                .defaultPriority(WorkloadPriority.NORMAL)
                .requiresStrictOrdering(true)
                .requiresReplay(true)
                .requiresDeduplication(false)
                .retentionDays(30)
                .defaultDestination("gate.ingress.events")
                .defaultRoutingKeyPattern("gate.{societyId}.anpr")
                .rationale("High throughput camera burst (8.2k QPS), append-only time series, replayable audit")
                .build());

        register(WorkloadProfile.builder()
                .workloadType(WorkloadType.GATE_RFID_BADGE_TAP)
                .recommendedBroker(TargetBroker.KAFKA)
                .defaultPriority(WorkloadPriority.NORMAL)
                .requiresStrictOrdering(true)
                .requiresReplay(true)
                .requiresDeduplication(true)
                .retentionDays(30)
                .defaultDestination("gate.rfid.events")
                .defaultRoutingKeyPattern("gate.{societyId}.rfid")
                .rationale("Continuous badge scans across turnstiles, strict partition ordering per reader")
                .build());

        register(WorkloadProfile.builder()
                .workloadType(WorkloadType.BOOM_BARRIER_TELEMETRY)
                .recommendedBroker(TargetBroker.KAFKA)
                .defaultPriority(WorkloadPriority.LOW)
                .requiresStrictOrdering(false)
                .requiresReplay(true)
                .requiresDeduplication(false)
                .retentionDays(14)
                .defaultDestination("gate.barrier.telemetry")
                .defaultRoutingKeyPattern("gate.{societyId}.barrier")
                .rationale("Periodic IoT hardware status ticks, health analytics, anomaly detection")
                .build());

        // 2. Safety & Emergency (RabbitMQ: Sub-millisecond fanout, max priority)
        register(WorkloadProfile.builder()
                .workloadType(WorkloadType.EMERGENCY_SOS_FANOUT)
                .recommendedBroker(TargetBroker.RABBITMQ)
                .defaultPriority(WorkloadPriority.CRITICAL_EMERGENCY)
                .requiresStrictOrdering(false)
                .requiresReplay(false)
                .requiresDeduplication(true)
                .retentionDays(0)
                .defaultDestination("emergency.sos.fanout")
                .defaultRoutingKeyPattern("emergency.sos.{societyId}.#")
                .rationale("Zero-latency broadcast to security desks, clubhouse sirens, and guard handsets")
                .build());

        register(WorkloadProfile.builder()
                .workloadType(WorkloadType.CHILD_EXIT_ALERT)
                .recommendedBroker(TargetBroker.RABBITMQ)
                .defaultPriority(WorkloadPriority.CRITICAL_EMERGENCY)
                .requiresStrictOrdering(true)
                .requiresReplay(false)
                .requiresDeduplication(true)
                .retentionDays(0)
                .defaultDestination("emergency.child.exit")
                .defaultRoutingKeyPattern("child.exit.{societyId}")
                .rationale("Immediate guard desk lockout alert when unaccompanied minor attempts gate transit")
                .build());

        // 3. Real-Time Telephony & Notifications (RabbitMQ: Ephemeral, TTL, DLX)
        register(WorkloadProfile.builder()
                .workloadType(WorkloadType.TELEPHONY_VOIP_SIGNALING)
                .recommendedBroker(TargetBroker.RABBITMQ)
                .defaultPriority(WorkloadPriority.HIGH)
                .requiresStrictOrdering(true)
                .requiresReplay(false)
                .requiresDeduplication(false)
                .retentionDays(0)
                .defaultDestination("intercom.voip.signaling")
                .defaultRoutingKeyPattern("voip.signaling.{societyId}.{flatId}")
                .rationale("Sub-second SIP/WebRTC call setup messages with strict 30s TTL expiry")
                .build());

        register(WorkloadProfile.builder()
                .workloadType(WorkloadType.RESIDENT_PUSH_NOTIFICATION)
                .recommendedBroker(TargetBroker.RABBITMQ)
                .defaultPriority(WorkloadPriority.NORMAL)
                .requiresStrictOrdering(false)
                .requiresReplay(false)
                .requiresDeduplication(false)
                .retentionDays(0)
                .defaultDestination("notification.resident.push")
                .defaultRoutingKeyPattern("push.dispatch.{societyId}")
                .rationale("Transient worker queue for Firebase FCM / Apple APNs dispatch with invalid token DLX")
                .build());

        // 4. Billing & Financial Ledger (Kafka: Partitioned batch, double-entry audit, compaction)
        register(WorkloadProfile.builder()
                .workloadType(WorkloadType.MONTHLY_INVOICE_BATCH)
                .recommendedBroker(TargetBroker.KAFKA)
                .defaultPriority(WorkloadPriority.NORMAL)
                .requiresStrictOrdering(true)
                .requiresReplay(true)
                .requiresDeduplication(true)
                .retentionDays(90)
                .defaultDestination("billing.invoice.generation")
                .defaultRoutingKeyPattern("billing.{societyId}")
                .rationale("10M PDF invoice generation burst across 128 partitions with worker crash recovery")
                .build());

        register(WorkloadProfile.builder()
                .workloadType(WorkloadType.BANK_SETTLEMENT_RECONCILIATION)
                .recommendedBroker(TargetBroker.KAFKA)
                .defaultPriority(WorkloadPriority.HIGH)
                .requiresStrictOrdering(true)
                .requiresReplay(true)
                .requiresDeduplication(true)
                .retentionDays(365)
                .defaultDestination("finance.reconciliation.ledger")
                .defaultRoutingKeyPattern("reconciliation.{societyId}")
                .rationale("T+1 MT940 50,000 UTR batch reconciliation; immutable audit log with log compaction")
                .build());

        register(WorkloadProfile.builder()
                .workloadType(WorkloadType.GENERAL_LEDGER_STREAM)
                .recommendedBroker(TargetBroker.KAFKA)
                .defaultPriority(WorkloadPriority.HIGH)
                .requiresStrictOrdering(true)
                .requiresReplay(true)
                .requiresDeduplication(true)
                .retentionDays(730)
                .defaultDestination("finance.general-ledger.events")
                .defaultRoutingKeyPattern("ledger.{societyId}")
                .rationale("Double-entry bookkeeping journal entries; regulatory compliance event sourcing")
                .build());

        // 5. Payment Processing (RabbitMQ: Webhook retry with exponential backoff & DLQ)
        register(WorkloadProfile.builder()
                .workloadType(WorkloadType.PAYMENT_GATEWAY_WEBHOOK)
                .recommendedBroker(TargetBroker.RABBITMQ)
                .defaultPriority(WorkloadPriority.HIGH)
                .requiresStrictOrdering(false)
                .requiresReplay(false)
                .requiresDeduplication(true)
                .retentionDays(0)
                .defaultDestination("payment.gateway.webhooks")
                .defaultRoutingKeyPattern("webhook.payment.{gateway}")
                .rationale("Competing consumers with DLX exponential retry queues (1m, 5m, 30m) for delayed callbacks")
                .build());

        // 6. Operations & Staffing (RabbitMQ: Competing consumers, fair dispatch, manual ACKs)
        register(WorkloadProfile.builder()
                .workloadType(WorkloadType.GUARD_PATROL_DISPATCH)
                .recommendedBroker(TargetBroker.RABBITMQ)
                .defaultPriority(WorkloadPriority.NORMAL)
                .requiresStrictOrdering(false)
                .requiresReplay(false)
                .requiresDeduplication(false)
                .retentionDays(0)
                .defaultDestination("guard.patrol.dispatch")
                .defaultRoutingKeyPattern("guard.dispatch.{societyId}")
                .rationale("Point-to-point round robin dispatch to on-duty guards with prefetch_count=1")
                .build());

        register(WorkloadProfile.builder()
                .workloadType(WorkloadType.HELPDESK_TICKET_ASSIGNMENT)
                .recommendedBroker(TargetBroker.RABBITMQ)
                .defaultPriority(WorkloadPriority.NORMAL)
                .requiresStrictOrdering(false)
                .requiresReplay(false)
                .requiresDeduplication(false)
                .retentionDays(0)
                .defaultDestination("helpdesk.ticket.assignment")
                .defaultRoutingKeyPattern("helpdesk.assign.{societyId}")
                .rationale("Technician work queue dispatch with priority support for electrical/plumbing emergencies")
                .build());

        // 7. Domestic Staff & Visitor Ingress (Dual-Bus: Kafka for audit log, RabbitMQ for push alerts)
        register(WorkloadProfile.builder()
                .workloadType(WorkloadType.DAILY_HELP_ATTENDANCE)
                .recommendedBroker(TargetBroker.DUAL_BUS)
                .defaultPriority(WorkloadPriority.NORMAL)
                .requiresStrictOrdering(true)
                .requiresReplay(true)
                .requiresDeduplication(true)
                .retentionDays(60)
                .defaultDestination("staff.attendance.events")
                .defaultRoutingKeyPattern("staff.attendance.{societyId}")
                .rationale("Immutable entry log to Kafka; immediate concurrent push fanout to employing flats via RabbitMQ")
                .build());

        register(WorkloadProfile.builder()
                .workloadType(WorkloadType.VISITOR_PASS_APPROVAL)
                .recommendedBroker(TargetBroker.DUAL_BUS)
                .defaultPriority(WorkloadPriority.HIGH)
                .requiresStrictOrdering(true)
                .requiresReplay(true)
                .requiresDeduplication(true)
                .retentionDays(90)
                .defaultDestination("visitor.pass.lifecycle")
                .defaultRoutingKeyPattern("visitor.approval.{societyId}")
                .rationale("Gate pass creation logged to Kafka; real-time interactive approval request dispatched via RabbitMQ")
                .build());

        // 8. IoT & Utility Metering (Kafka: Time-series sliding window, leak anomaly detection)
        register(WorkloadProfile.builder()
                .workloadType(WorkloadType.WATER_METER_TELEMETRY)
                .recommendedBroker(TargetBroker.KAFKA)
                .defaultPriority(WorkloadPriority.LOW)
                .requiresStrictOrdering(true)
                .requiresReplay(true)
                .requiresDeduplication(false)
                .retentionDays(45)
                .defaultDestination("iot.telemetry.water-meters")
                .defaultRoutingKeyPattern("iot.water.{societyId}")
                .rationale("Continuous LoRaWAN 5-second ticks; windowed analytics for burst/leak anomaly detection")
                .build());

        register(WorkloadProfile.builder()
                .workloadType(WorkloadType.PREPAID_ELECTRICITY_TICK)
                .recommendedBroker(TargetBroker.KAFKA)
                .defaultPriority(WorkloadPriority.NORMAL)
                .requiresStrictOrdering(true)
                .requiresReplay(true)
                .requiresDeduplication(true)
                .retentionDays(60)
                .defaultDestination("iot.telemetry.prepaid-meters")
                .defaultRoutingKeyPattern("iot.prepaid.{societyId}")
                .rationale("Energy kWh pulses with balance debit ledger tracking per flat")
                .build());

        // 9. Conversational & Asynchronous Integrations (RabbitMQ: Delayed exchange)
        register(WorkloadProfile.builder()
                .workloadType(WorkloadType.WHATSAPP_BOT_TASK)
                .recommendedBroker(TargetBroker.RABBITMQ)
                .defaultPriority(WorkloadPriority.NORMAL)
                .requiresStrictOrdering(false)
                .requiresReplay(false)
                .requiresDeduplication(true)
                .retentionDays(0)
                .defaultDestination("whatsapp.bot.tasks")
                .defaultRoutingKeyPattern("whatsapp.task.{societyId}")
                .rationale("Delayed message exchange for 24-hr Meta session window and asynchronous PDF delivery")
                .build());

        // 10. Social Graph & Cache Invalidation (RabbitMQ: Fanout exchange to all app pods)
        register(WorkloadProfile.builder()
                .workloadType(WorkloadType.CACHE_INVALIDATION_PRIVACY)
                .recommendedBroker(TargetBroker.RABBITMQ)
                .defaultPriority(WorkloadPriority.HIGH)
                .requiresStrictOrdering(false)
                .requiresReplay(false)
                .requiresDeduplication(false)
                .retentionDays(0)
                .defaultDestination("cache.invalidation.privacy")
                .defaultRoutingKeyPattern("cache.invalidate.#")
                .rationale("Cluster-wide fanout broadcast to evict local and Redis directory caches instantly")
                .build());

        // 11. Compliance & Scheduled Maintenance (Kafka: Large-scale batch jobs, partitioned)
        register(WorkloadProfile.builder()
                .workloadType(WorkloadType.DPDP_DATA_PURGE_JOB)
                .recommendedBroker(TargetBroker.KAFKA)
                .defaultPriority(WorkloadPriority.LOW)
                .requiresStrictOrdering(false)
                .requiresReplay(true)
                .requiresDeduplication(true)
                .retentionDays(180)
                .defaultDestination("compliance.dpdp.purge-jobs")
                .defaultRoutingKeyPattern("compliance.purge.{societyId}")
                .rationale("Nightly massive deletion of >90-day visitor photos and anonymization with audit proofs")
                .build());
    }

    public void register(WorkloadProfile profile) {
        profileRegistry.put(profile.getWorkloadType(), profile);
    }

    public WorkloadProfile getProfile(WorkloadType workloadType) {
        WorkloadProfile profile = profileRegistry.get(workloadType);
        if (profile == null) {
            throw new IllegalArgumentException("No workload profile registered for: " + workloadType);
        }
        return profile;
    }

    /**
     * Resolves the complete routing decision for an incoming message.
     */
    public RoutingDecision classify(WorkloadMessage message) {
        WorkloadProfile profile = getProfile(message.getWorkloadType());

        // Priority resolution: message priority overrides profile default if explicitly provided
        WorkloadPriority priority = (message.getPriority() != null)
                ? message.getPriority()
                : profile.getDefaultPriority();

        // Broker determination
        TargetBroker broker = profile.getRecommendedBroker();

        // Key resolution
        String societyId = message.getSocietyId() != null ? message.getSocietyId() : "global";
        String partitionKey = (message.getPartitionOrRoutingKey() != null)
                ? message.getPartitionOrRoutingKey()
                : societyId;

        String routingKey = profile.getDefaultRoutingKeyPattern()
                .replace("{societyId}", societyId)
                .replace("{gateway}", "default")
                .replace("{flatId}", "all");

        // Dead letter destination calculation
        String destination = profile.getDefaultDestination();
        String deadLetterDestination = (broker == TargetBroker.KAFKA)
                ? destination + ".DLT"
                : destination + ".dlx";

        int timeoutMs = (priority == WorkloadPriority.CRITICAL_EMERGENCY) ? 1000 : 5000;

        return RoutingDecision.builder()
                .targetBroker(broker)
                .destination(destination)
                .routingKey(routingKey)
                .partitionKey(partitionKey)
                .priority(priority)
                .dlqEnabled(true)
                .deadLetterDestination(deadLetterDestination)
                .deliveryTimeoutMs(timeoutMs)
                .rationale(profile.getRationale())
                .build();
    }
}
