package com.manacommunity.platform.websocket;

/**
 * Redis Pub/Sub message DTO for group-order status events.
 *
 * @param communityId the community ID (determines the STOMP topic)
 * @param payload     the event payload
 */
public record GroupOrderRedisMessage(Long communityId, Object payload) {}
