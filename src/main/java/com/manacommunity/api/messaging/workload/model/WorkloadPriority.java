package com.manacommunity.api.messaging.workload.model;

/**
 * Priority tier mapped to AMQP priority levels (0 to 10) or partition priority groupings.
 */
public enum WorkloadPriority {
    LOW(1),
    NORMAL(4),
    HIGH(7),
    CRITICAL_EMERGENCY(10);

    private final int amqpPriorityLevel;

    WorkloadPriority(int amqpPriorityLevel) {
        this.amqpPriorityLevel = amqpPriorityLevel;
    }

    public int getAmqpPriorityLevel() {
        return amqpPriorityLevel;
    }
}
