package com.manacommunity.api.gate.adapter;

import lombok.Builder;

/** Command sent from the backend to a physical barrier or lock. */
@Builder
public record BarrierCommand(
        Long communityId,
        String gateId,
        /** OPEN | CLOSE | LOCK | UNLOCK */
        String action,
        String reason,
        Long triggeredByPassId,
        String triggeredBy
) {}
