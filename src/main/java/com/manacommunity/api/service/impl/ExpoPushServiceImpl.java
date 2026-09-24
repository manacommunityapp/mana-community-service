package com.manacommunity.api.service.impl;

import com.manacommunity.api.dto.push.ExpoPushMessage;
import com.manacommunity.api.dto.push.ExpoPushResponse;
import com.manacommunity.api.model.PushToken;
import com.manacommunity.api.repository.PushTokenRepository;
import com.manacommunity.api.service.ExpoPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Expo Push Notification API v2 implementation.
 *
 * Key design decisions:
 *  - Batches up to BATCH_SIZE messages per HTTP call (Expo limit: 100)
 *  - Processes batches on the caller's thread (fast enough; use @Async wrappers
 *    from MobilePushEventListener for non-blocking fire-and-forget)
 *  - Inspects each ticket: DeviceNotRegistered → deactivates the token immediately
 *  - Catches ALL exceptions so push failures never bubble up to callers
 *  - Uses a dedicated NOTIFICATION logger for operational visibility
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpoPushServiceImpl implements ExpoPushService {

    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";
    private static final int    BATCH_SIZE    = 100;  // Expo hard limit per request

    // Module-specific logger → notification.log (configured in logback-spring.xml)
    private static final org.slf4j.Logger NOTIF_LOG =
            org.slf4j.LoggerFactory.getLogger("NOTIFICATION");

    private final PushTokenRepository tokenRepo;
    private final RestTemplate         restTemplate;

    @Value("${app.push.enabled:true}")
    private boolean pushEnabled;

    // ── Low-level dispatch ────────────────────────────────────────────────

    @Override
    public void sendToUser(Long userId, String title, String body,
                           Map<String, Object> data, String channelId) {
        if (userId == null) return;
        List<PushToken> tokens = tokenRepo.findByUserIdAndActiveTrue(userId);
        if (tokens.isEmpty()) return;
        List<ExpoPushMessage> messages = buildMessages(tokens, title, body, data, channelId);
        dispatch(messages);
    }

    @Override
    public void sendToUsers(List<Long> userIds, String title, String body,
                            Map<String, Object> data, String channelId) {
        if (userIds == null || userIds.isEmpty()) return;
        List<PushToken> tokens = tokenRepo.findActiveByUserIds(userIds);
        if (tokens.isEmpty()) return;
        List<ExpoPushMessage> messages = buildMessages(tokens, title, body, data, channelId);
        dispatch(messages);
    }

    @Override
    public void sendToCommunity(Long communityId, String title, String body,
                                Map<String, Object> data, String channelId) {
        if (communityId == null) return;
        List<PushToken> tokens = tokenRepo.findActiveByCommunityId(communityId);
        if (tokens.isEmpty()) return;
        List<ExpoPushMessage> messages = buildMessages(tokens, title, body, data, channelId);
        dispatch(messages);
    }

    // ── Typed helpers ─────────────────────────────────────────────────────

    @Override
    public void notifyNewMessage(Long recipientId, String senderName,
                                 String messagePreview, Long conversationId) {
        sendToUser(
            recipientId,
            senderName,                  // title = sender's name (like WhatsApp)
            messagePreview,
            Map.of(
                "type",           "NEW_MESSAGE",
                "conversationId", conversationId
            ),
            "chat"               // high-importance Android channel
        );
    }

    @Override
    public void notifyOutbid(Long outbidUserId, String newBidderName,
                             long newAmount, String itemTitle, Long auctionId) {
        sendToUser(
            outbidUserId,
            "⚠️ You've been outbid!",
            newBidderName + " bid ₹" + formatAmount(newAmount) + " on " + itemTitle,
            Map.of(
                "type",      "AUCTION_BID",
                "auctionId", auctionId
            ),
            "default"
        );
    }

    @Override
    public void notifyAuctionWon(Long winnerId, String itemTitle,
                                 long finalAmount, Long auctionId) {
        sendToUser(
            winnerId,
            "🏆 You won the auction!",
            itemTitle + " — ₹" + formatAmount(finalAmount),
            Map.of(
                "type",      "AUCTION_ENDED",
                "auctionId", auctionId
            ),
            "default"
        );
    }

    @Override
    public void notifyAuctionSold(Long sellerId, String itemTitle,
                                  long finalAmount, String winnerName, Long auctionId) {
        sendToUser(
            sellerId,
            "✅ Your item sold!",
            itemTitle + " sold to " + winnerName + " for ₹" + formatAmount(finalAmount),
            Map.of(
                "type",      "AUCTION_SOLD",
                "auctionId", auctionId
            ),
            "default"
        );
    }

    @Override
    public void notifyNewCommunityEvent(Long communityId, String eventTitle,
                                        String venue, Long eventId) {
        sendToCommunity(
            communityId,
            "📅 New Event: " + eventTitle,
            venue != null ? "📍 " + venue : eventTitle,
            Map.of(
                "type",    "NEW_EVENT",
                "eventId", eventId
            ),
            "events"
        );
    }

    @Override
    public void notifyAnnouncement(Long communityId, String title, String body,
                                   String priority, Long announcementId) {
        boolean urgent = "URGENT".equalsIgnoreCase(priority);
        sendToCommunity(
            communityId,
            urgent ? "🚨 " + title : "📢 " + title,
            body != null && body.length() > 100 ? body.substring(0, 97) + "…" : body,
            Map.of(
                "type",           "ANNOUNCEMENT",
                "announcementId", announcementId,
                "priority",       priority != null ? priority : "NORMAL"
            ),
            urgent ? "default" : "events"
        );
    }

    @Override
    public void notifyNewPost(List<Long> recipientIds, String authorName,
                              String contentPreview, String postType, Long postId) {
        if (recipientIds == null || recipientIds.isEmpty()) return;
        String typeLabel = switch (postType != null ? postType.toLowerCase() : "post") {
            case "poll"         -> "📊 New Poll";
            case "announcement" -> "📢 Announcement";
            case "event"        -> "📅 New Event";
            default             -> "📝 New Post";
        };
        sendToUsers(
            recipientIds,
            authorName + " — " + typeLabel,
            contentPreview,
            Map.of(
                "type",     "NEW_POST",
                "postType", postType != null ? postType : "post",
                "postId",   postId
            ),
            "default"
        );
    }

    // ── Internal dispatch ─────────────────────────────────────────────────

    /**
     * Converts PushToken rows to ExpoPushMessage objects.
     * Skips non-Expo tokens (e.g. leftover FCM tokens from an older build).
     */
    private List<ExpoPushMessage> buildMessages(List<PushToken> tokens,
                                                String title, String body,
                                                Map<String, Object> data,
                                                String channelId) {
        return tokens.stream()
                .filter(t -> t.getToken() != null && t.getToken().startsWith("ExponentPushToken["))
                .map(t -> ExpoPushMessage.builder()
                        .to(t.getToken())
                        .title(title)
                        .body(body)
                        .data(data)
                        .channelId(channelId)
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Batches messages into groups of BATCH_SIZE and sends each batch to Expo.
     * Invalid tokens discovered in the response are deactivated immediately.
     */
    private void dispatch(List<ExpoPushMessage> messages) {
        if (!pushEnabled || messages.isEmpty()) {
            NOTIF_LOG.debug("Push skipped — enabled={} messages={}", pushEnabled, messages.size());
            return;
        }

        // Split into batches
        for (int i = 0; i < messages.size(); i += BATCH_SIZE) {
            List<ExpoPushMessage> batch = messages.subList(i, Math.min(i + BATCH_SIZE, messages.size()));
            sendBatch(batch);
        }
    }

    @Transactional
    protected void sendBatch(List<ExpoPushMessage> batch) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/json");
            headers.set("Accept-Encoding", "gzip, deflate");

            HttpEntity<List<ExpoPushMessage>> request = new HttpEntity<>(batch, headers);

            ResponseEntity<ExpoPushResponse> response = restTemplate.exchange(
                EXPO_PUSH_URL,
                HttpMethod.POST,
                request,
                ExpoPushResponse.class
            );

            if (response.getBody() != null && response.getBody().getData() != null) {
                processTickets(batch, response.getBody().getData());
            }

            NOTIF_LOG.info("Push batch sent: count={} status={}",
                           batch.size(), response.getStatusCode());

        } catch (Exception ex) {
            // Never let a push failure break the calling business operation
            NOTIF_LOG.error("Push batch failed: count={} error={}", batch.size(), ex.getMessage());
        }
    }

    /**
     * Walks the Expo response tickets. For each "error" ticket with
     * DeviceNotRegistered, deactivates the corresponding token so we
     * don't waste future sends on dead tokens.
     */
    private void processTickets(List<ExpoPushMessage> batch,
                                List<ExpoPushResponse.TicketData> tickets) {
        int errorCount = 0;
        for (int i = 0; i < tickets.size() && i < batch.size(); i++) {
            ExpoPushResponse.TicketData ticket = tickets.get(i);
            if (!"ok".equals(ticket.getStatus())) {
                errorCount++;
                String failedToken = batch.get(i).getTo();
                if (ticket.getDetails() != null
                        && "DeviceNotRegistered".equals(ticket.getDetails().getError())) {
                    tokenRepo.deactivateByToken(failedToken);
                    NOTIF_LOG.info("Deactivated invalid push token: {}…", safePreview(failedToken));
                } else {
                    NOTIF_LOG.warn("Push ticket error for token {}… — message: {}",
                                   safePreview(failedToken), ticket.getMessage());
                }
            }
        }
        if (errorCount > 0) {
            NOTIF_LOG.warn("Push batch completed with {} error(s) out of {}",
                           errorCount, batch.size());
        }
    }

    private String formatAmount(long amount) {
        return String.format("%,d", amount);
    }

    private String safePreview(String token) {
        return token != null && token.length() > 24 ? token.substring(0, 24) : token;
    }

    // ── Sports ────────────────────────────────────────────────────────────

    @Override
    public void notifyMatchStartingSoon(List<Long> playerIds, String matchTitle,
                                        String sport, String venue,
                                        String scheduledTime, Long matchId) {
        if (playerIds == null || playerIds.isEmpty()) return;
        String sportEmoji = sportEmoji(sport);
        sendToUsers(
            playerIds,
            sportEmoji + " Match starting at " + scheduledTime,
            matchTitle + " • " + (venue != null ? venue : "Community ground"),
            Map.of("type", "MATCH_STARTING", "matchId", matchId),
            "events"
        );
    }

    @Override
    public void notifyRateYourPlayers(List<Long> participantIds,
                                      String matchTitle, Long matchId) {
        if (participantIds == null || participantIds.isEmpty()) return;
        sendToUsers(
            participantIds,
            "⭐ Rate your players",
            "How did your teammates do in " + matchTitle + "?",
            Map.of("type", "RATE_PLAYERS", "matchId", matchId),
            "default"
        );
    }

    @Override
    public void notifyMatchCompleted(Long communityId, String matchTitle,
                                     String result, Long matchId) {
        if (communityId == null) return;
        sendToCommunity(
            communityId,
            "🏁 Match Result",
            result != null ? result : matchTitle + " — match completed",
            Map.of("type", "MATCH_COMPLETED", "matchId", matchId),
            "events"
        );
    }

    @Override
    public void notifyBadgeEarned(Long userId, String badgeName,
                                   String badgeEmoji, String rarity) {
        if (userId == null) return;
        boolean isSpecial = "legendary".equalsIgnoreCase(rarity) || "epic".equalsIgnoreCase(rarity);
        sendToUser(
            userId,
            isSpecial ? "🎉 New " + rarity + " badge!" : "🏅 Badge earned!",
            badgeEmoji + " " + badgeName + " — added to your sports profile",
            Map.of("type", "BADGE_EARNED", "badgeName", badgeName),
            "default"
        );
    }

    private String sportEmoji(String sport) {
        if (sport == null) return "🏅";
        return switch (sport.toUpperCase()) {
            case "CRICKET"     -> "🏏";
            case "FOOTBALL"    -> "⚽";
            case "BADMINTON"   -> "🏸";
            case "TABLE_TENNIS"-> "🏓";
            case "BASKETBALL"  -> "🏀";
            case "VOLLEYBALL"  -> "🏐";
            default            -> "🏅";
        };
    }
}
