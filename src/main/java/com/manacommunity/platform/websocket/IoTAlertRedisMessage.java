package com.manacommunity.platform.websocket;

/**
 * Redis Pub/Sub message DTO for IoT sensor anomaly alerts.
 *
 * @param communityId the community ID (determines the STOMP topic)
 * @param payload     the alert payload
 */
public record IoTAlertRedisMessage(Long communityId, Object payload) {}
