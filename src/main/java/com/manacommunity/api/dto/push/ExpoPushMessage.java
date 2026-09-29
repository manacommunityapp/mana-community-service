package com.manacommunity.api.dto.push;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Serialises to a single entry in the Expo Push API request body.
 *
 * Expo endpoint: POST https://exp.host/--/api/v2/push/send
 * Accepts an array of these objects (max 100 per request).
 *
 * Full field reference:
 *   https://docs.expo.dev/push-notifications/sending-notifications/#message-request-format
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExpoPushMessage {

    /** Expo push token: "ExponentPushToken[...]" */
    private String to;

    private String title;
    private String body;

    /**
     * Extra key-value data delivered to the app.
     * The mobile hook reads data.type to navigate on tap:
     *   NEW_MESSAGE  → /chat/{conversationId}
     *   NEW_EVENT    → /tabs/events
     *   AUCTION_BID  → /auction/{auctionId}
     *   ANNOUNCEMENT → /tabs/feed
     *   NEW_POST     → /tabs/feed
     */
    private Map<String, Object> data;

    /**
     * Android notification channel (must match channels registered in the app):
     *   "chat"    → Messages channel (high importance)
     *   "events"  → Events & Announcements (default importance)
     *   "default" → General
     */
    private String channelId;

    /**
     * "default" plays the device's default notification sound.
     * Use null to send silently.
     */
    @Builder.Default
    private String sound = "default";

    /**
     * "high" wakes the device; "normal" is batched.
     * Always "high" for direct messages and outbid alerts.
     */
    @Builder.Default
    private String priority = "high";

    /**
     * iOS badge count. null = no change.
     */
    private Integer badge;

    /**
     * TTL in seconds. 0 = deliver now or drop.
     * Default 2 days (172800) is fine for most notifications.
     */
    @Builder.Default
    private int ttl = 172_800;
}
