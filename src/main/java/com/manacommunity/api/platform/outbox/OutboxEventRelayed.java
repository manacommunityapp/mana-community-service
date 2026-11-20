package com.manacommunity.api.platform.outbox;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Spring application event fired by {@link OutboxRelayScheduler} once an outbox
 * entry is delivered. Listeners consume this in-process, same as before.
 */
@Getter
@RequiredArgsConstructor
public class OutboxEventRelayed {
    private final String eventType;
    private final String aggregateType;
    private final String aggregateId;
    private final Long communityId;
    private final Object payload;
}
