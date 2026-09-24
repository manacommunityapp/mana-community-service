package com.manacommunity.api.service;

import java.util.List;
import java.util.Map;

/**
 * Expo Push Notification service — the authoritative way to send mobile push
 * notifications from anywhere in the backend.
 *
 * All methods are fire-and-forget from the caller's perspective:
 * they run on the caller's thread but catch all delivery exceptions internally
 * so a push failure never breaks the primary business operation.
 *
 * Typed helpers (notifyXxx) are preferred over sendToUser/sendToUsers because
 * they enforce the correct channelId, priority, and data shape that the
 * mobile app's tap-navigation hook expects.
 */
public interface ExpoPushService {

    // ── Low-level ─────────────────────────────────────────────────────────

    /**
     * Send a push notification to all active devices registered for a user.
     *
     * @param userId    target user's DB id
     * @param title     notification title (shown in system tray)
     * @param body      notification body text
     * @param data      key-value pairs delivered to the app (must include "type")
     * @param channelId Android channel: "chat" | "events" | "default"
     */
    void sendToUser(Long userId,
                    String title,
                    String body,
                    Map<String, Object> data,
                    String channelId);

    /**
     * Send a push notification to multiple users in a single batched Expo API call.
     * Token de-duplication is performed automatically.
     */
    void sendToUsers(List<Long> userIds,
                     String title,
                     String body,
                     Map<String, Object> data,
                     String channelId);

    /**
     * Broadcast to all active device tokens registered for members of a community.
     * Used for announcements and new events. Limited to 500 users per call internally.
     */
    void sendToCommunity(Long communityId,
                         String title,
                         String body,
                         Map<String, Object> data,
                         String channelId);

    // ── Typed helpers ─────────────────────────────────────────────────────

    /** New chat message — notifies all conversation participants except the sender. */
    void notifyNewMessage(Long recipientId,
                          String senderName,
                          String messagePreview,
                          Long conversationId);

    /** Someone outbid the previous leader. */
    void notifyOutbid(Long outbidUserId,
                      String newBidderName,
                      long newAmount,
                      String itemTitle,
                      Long auctionId);

    /** Auction closed — notify the winner. */
    void notifyAuctionWon(Long winnerId,
                          String itemTitle,
                          long finalAmount,
                          Long auctionId);

    /** Auction closed — notify the seller. */
    void notifyAuctionSold(Long sellerId,
                           String itemTitle,
                           long finalAmount,
                           String winnerName,
                           Long auctionId);

    /** New community event created — broadcast to all community members. */
    void notifyNewCommunityEvent(Long communityId,
                                 String eventTitle,
                                 String venue,
                                 Long eventId);

    /** Community-wide announcement — broadcast to all members. */
    void notifyAnnouncement(Long communityId,
                            String title,
                            String body,
                            String priority,
                            Long announcementId);

    /** New post / poll / announcement in the feed (targeted). */
    void notifyNewPost(List<Long> recipientIds,
                       String authorName,
                       String contentPreview,
                       String postType,
                       Long postId);

    // ── Sports ────────────────────────────────────────────────────────────

    /** Notify registered players that a match starts in ~30 minutes. */
    void notifyMatchStartingSoon(List<Long> playerIds,
                                 String matchTitle,
                                 String sport,
                                 String venue,
                                 String scheduledTime,
                                 Long matchId);

    /** Prompt players to rate their teammates after a match ends. */
    void notifyRateYourPlayers(List<Long> participantIds,
                                String matchTitle,
                                Long matchId);

    /** Broadcast match result to the community. */
    void notifyMatchCompleted(Long communityId,
                              String matchTitle,
                              String result,
                              Long matchId);

    /** Congratulate a player on a new achievement badge. */
    void notifyBadgeEarned(Long userId,
                            String badgeName,
                            String badgeEmoji,
                            String rarity);
}
