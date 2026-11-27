package com.manacommunity.platform.websocket;

/**
 * Redis Pub/Sub message DTO for item-auction bid events.
 *
 * @param itemId  the auction item ID
 * @param payload the event payload (serialised as-is into the STOMP topic)
 */
public record EventAuctionRedisMessage(Long itemId, Object payload) {}
