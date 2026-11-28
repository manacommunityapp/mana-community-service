package com.manacommunity.api.messaging.workload.model;

/**
 * Enumeration of system-wide workloads across Mana Community / Mygate modules.
 */
public enum WorkloadType {
    // Ingress & Physical Security (Kafka: High volume, ordered, replayable audit)
    GATE_ANPR_STREAM,
    GATE_RFID_BADGE_TAP,
    BOOM_BARRIER_TELEMETRY,

    // Safety & Emergency (RabbitMQ: Sub-millisecond fanout, priority 10)
    EMERGENCY_SOS_FANOUT,
    CHILD_EXIT_ALERT,

    // Real-Time Telephony & Notifications (RabbitMQ: Ephemeral, TTL, DLX)
    TELEPHONY_VOIP_SIGNALING,
    RESIDENT_PUSH_NOTIFICATION,

    // Billing & Financial Ledger (Kafka: Partitioned batch, double-entry audit, compaction)
    MONTHLY_INVOICE_BATCH,
    BANK_SETTLEMENT_RECONCILIATION,
    GENERAL_LEDGER_STREAM,

    // Payment Processing (RabbitMQ: Webhook retry with exponential backoff & DLQ)
    PAYMENT_GATEWAY_WEBHOOK,

    // Operations & Staffing (RabbitMQ: Competing consumers, fair dispatch, manual ACKs)
    GUARD_PATROL_DISPATCH,
    HELPDESK_TICKET_ASSIGNMENT,

    // Domestic Staff & Visitor Ingress (Dual-Bus: Kafka for audit log, RabbitMQ for push alerts)
    DAILY_HELP_ATTENDANCE,
    VISITOR_PASS_APPROVAL,

    // IoT & Utility Metering (Kafka: Time-series sliding window, leak anomaly detection)
    WATER_METER_TELEMETRY,
    PREPAID_ELECTRICITY_TICK,

    // Conversational & Asynchronous Integrations (RabbitMQ: Delayed exchange)
    WHATSAPP_BOT_TASK,

    // Social Graph & Cache Invalidation (RabbitMQ: Fanout exchange to all app pods)
    CACHE_INVALIDATION_PRIVACY,

    // Compliance & Scheduled Maintenance (Kafka: Large-scale batch jobs, partitioned)
    DPDP_DATA_PURGE_JOB
}
