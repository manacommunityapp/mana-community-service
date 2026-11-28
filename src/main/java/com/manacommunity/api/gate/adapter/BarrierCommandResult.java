package com.manacommunity.api.gate.adapter;

import lombok.Builder;

@Builder
public record BarrierCommandResult(
        boolean success,
        String gateId,
        String action,
        String message,
        /** Round-trip time in ms, if measured. */
        Long latencyMs
) {}
