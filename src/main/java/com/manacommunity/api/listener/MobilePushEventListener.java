package com.manacommunity.api.listener;

import com.manacommunity.api.event.*;
import com.manacommunity.api.service.ExpoPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listens for domain events published via ApplicationEventPublisher
 * and dispatches the appropriate push notification to the mobile app.
 *
 * All handlers are:
 *  1. @Async — runs on the app's async executor, not the request thread.
 *     Push delivery latency (~50-200ms) does not block the HTTP response.
 *  2. Non-throwing — ExpoPushServiceImpl catches all exceptions internally.
 *     Any leak here is caught by the global @Async uncaught exception handler.
 *
 * To add a new event:
 *  1. Create a class extending ApplicationEvent in the event package.
 *  2. Publish it from the relevant service: events.publishEvent(new YourEvent(...))
 *  3. Add an @EventListener method here.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MobilePushEventListener {

    private final ExpoPushService pushService;

    // ── Chat ──────────────────────────────────────────────────────────────

    /**
     * Fires when a chat message is saved.
     * Notifies each offline participant individually so the notification
     * body shows "Priya: Hey, are you free tonight?" style (sender name as title).
     */
    @Async
    @EventListener
    public void onChatMessageSent(ChatMessageSentEvent event) {
        if (event.getRecipientIds().isEmpty()) return;

        log.debug("[Push] Chat message event: conversationId={} recipients={}",
                  event.getConversationId(), event.getRecipientIds().size());

        for (Long recipientId : event.getRecipientIds()) {
            pushService.notifyNewMessage(
                recipientId,
                event.getSenderName(),
                event.getContentPreview(),
                event.getConversationId()
            );
        }
    }

    // ── Auction ───────────────────────────────────────────────────────────

    /**
     * Fires when a new bid is placed.
     * Only the previous leader (who just got outbid) gets a notification —
     * sending to the whole community would be noise.
     */
    @Async
    @EventListener
    public void onAuctionBidPlaced(AuctionBidPlacedEvent event) {
        // Notify the outbid user (previous leader)
        if (event.getPreviousLeaderId() != null
                && !event.getPreviousLeaderId().equals(event.getNewBidderId())) {

            log.debug("[Push] Outbid notification: userId={} auctionId={}",
                      event.getPreviousLeaderId(), event.getAuctionId());

            pushService.notifyOutbid(
                event.getPreviousLeaderId(),
                event.getNewBidderName(),
                event.getBidAmount(),
                event.getAuctionTitle(),
                event.getAuctionId()
            );
        }
    }

    /**
     * Fires when an auction closes.
     * Notifies the winner and the seller with personalised messages.
     * Other bidders get a brief "auction ended" notification.
     */
    @Async
    @EventListener
    public void onAuctionEnded(AuctionEndedEvent event) {
        log.debug("[Push] Auction ended: id={} winner={}",
                  event.getAuctionId(), event.getWinnerId());

        // Notify winner
        if (event.getWinnerId() != null) {
            pushService.notifyAuctionWon(
                event.getWinnerId(),
                event.getAuctionTitle(),
                event.getFinalAmount(),
                event.getAuctionId()
            );
        }

        // Notify seller
        if (event.getSellerId() != null
                && !event.getSellerId().equals(event.getWinnerId())) {
            pushService.notifyAuctionSold(
                event.getSellerId(),
                event.getAuctionTitle(),
                event.getFinalAmount(),
                event.getWinnerName(),
                event.getAuctionId()
            );
        }

        // Notify other bidders (lost)
        if (!event.getOtherBidderIds().isEmpty()) {
            pushService.sendToUsers(
                event.getOtherBidderIds(),
                "🏁 Auction ended — " + event.getAuctionTitle(),
                event.getWinnerId() != null
                    ? event.getWinnerName() + " won with ₹" + String.format("%,d", event.getFinalAmount())
                    : "No winning bid — item unsold.",
                java.util.Map.of(
                    "type",      "AUCTION_ENDED",
                    "auctionId", event.getAuctionId()
                ),
                "default"
            );
        }
    }

    // ── Community Events ──────────────────────────────────────────────────

    /**
     * Fires when an event (match, social gathering, etc.) is created.
     * Broadcasts to all community members.
     */
    @Async
    @EventListener
    public void onCommunityEventCreated(CommunityEventCreatedEvent event) {
        log.debug("[Push] New community event: id={} community={}",
                  event.getEventId(), event.getCommunityId());

        pushService.notifyNewCommunityEvent(
            event.getCommunityId(),
            event.getEventTitle(),
            event.getVenue(),
            event.getEventId()
        );
    }

    // ── Announcements ─────────────────────────────────────────────────────

    /**
     * Fires after an admin posts an announcement (from AdminServiceImpl).
     * Broadcasts to the whole community. URGENT announcements use higher
     * visual prominence in the notification shade.
     */
    @Async
    @EventListener
    public void onAnnouncementCreated(AnnouncementCreatedEvent event) {
        log.debug("[Push] Announcement created: id={} priority={} community={}",
                  event.getAnnouncementId(), event.getPriority(), event.getCommunityId());

        // Truncate content for the notification body
        String body = event.getContent() != null && event.getContent().length() > 100
                ? event.getContent().substring(0, 97) + "…"
                : event.getContent();

        pushService.notifyAnnouncement(
            event.getCommunityId(),
            event.getTitle(),
            body,
            event.getPriority(),
            event.getAnnouncementId()
        );
    }

    // ── Posts / Polls ─────────────────────────────────────────────────────

    /**
     * Fires when a post, poll, or official announcement is created in the feed.
     * Regular member posts send to a specific target list (e.g. followers);
     * official posts and polls can be broadcast.
     */
    @Async
    @EventListener
    public void onNewPost(NewPostEvent event) {
        if (event.getTargetUserIds() == null || event.getTargetUserIds().isEmpty()) {
            return;
        }

        log.debug("[Push] New post: id={} type={} recipients={}",
                  event.getPostId(), event.getPostType(), event.getTargetUserIds().size());

        pushService.notifyNewPost(
            event.getTargetUserIds(),
            event.getAuthorName(),
            event.getContentPreview(),
            event.getPostType(),
            event.getPostId()
        );
    }

    // ── Sports ────────────────────────────────────────────────────────────

    /**
     * Match starting soon — fires from the scheduled job 30 min before kickoff.
     */
    @Async
    @EventListener
    public void onMatchStartingSoon(com.manacommunity.api.event.MatchStartingSoonEvent event) {
        log.debug("[Push] Match starting soon: matchId={} players={}",
                  event.getMatchId(), event.getAllPlayerIds().size());

        pushService.notifyMatchStartingSoon(
            event.getAllPlayerIds(),
            event.getMatchTitle(),
            event.getSport(),
            event.getVenue(),
            event.getScheduledAt(),
            event.getMatchId()
        );
    }

    /**
     * Match completed — send result to community + prompt players to rate peers.
     */
    @Async
    @EventListener
    public void onMatchCompleted(com.manacommunity.api.event.MatchCompletedEvent event) {
        log.debug("[Push] Match completed: matchId={} result='{}'",
                  event.getMatchId(), event.getResult());

        // Broadcast result to the whole community
        pushService.notifyMatchCompleted(
            event.getCommunityId(),
            event.getMatchTitle(),
            event.getResult(),
            event.getMatchId()
        );

        // Prompt participants to rate each other
        if (!event.getParticipantIds().isEmpty()) {
            pushService.notifyRateYourPlayers(
                event.getParticipantIds(),
                event.getMatchTitle(),
                event.getMatchId()
            );
        }
    }

    /**
     * Badge earned — personal congratulations to the player.
     */
    @Async
    @EventListener
    public void onBadgeEarned(com.manacommunity.api.event.BadgeEarnedEvent event) {
        log.debug("[Push] Badge earned: userId={} badge='{}'",
                  event.getUserId(), event.getBadgeName());

        pushService.notifyBadgeEarned(
            event.getUserId(),
            event.getBadgeName(),
            event.getBadgeEmoji(),
            event.getRarity()
        );
    }
}
