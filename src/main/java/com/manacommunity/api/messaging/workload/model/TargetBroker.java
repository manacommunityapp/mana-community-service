package com.manacommunity.api.messaging.workload.model;

/**
 * Target broker classification based on workload architecture requirements.
 */
public enum TargetBroker {
    /**
     * Apache Kafka: High-throughput, distributed append-only log with partition ordering,
     * time-based retention, event sourcing, replayability, and multi-consumer groups.
     */
    KAFKA,

    /**
     * RabbitMQ: Low-latency smart broker supporting AMQP routing (Direct, Topic, Fanout),
     * message priority queues, per-message TTL, dead-letter exchanges (DLX), and transient task dispatch.
     */
    RABBITMQ,

    /**
     * Dual-Bus (Hybrid): Replicates to Kafka for immutable audit/analytics log while simultaneously
     * dispatching to RabbitMQ for real-time edge push notifications or worker queues.
     */
    DUAL_BUS
}
