package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.dto.BidAckMessage;
import com.manacommunity.api.events.dto.BidAckMessage.BidAckStatus;
import com.manacommunity.api.events.dto.EventAuctionItemResponse;
import com.manacommunity.api.events.service.EventAuctionService;
import com.manacommunity.platform.ratelimit.AuctionBidRateLimiter;
import com.manacommunity.platform.websocket.EventAuctionRedisPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
public class EventAuctionBidWebSocketController {

    private static final String ACK_DESTINATION = "/queue/auction/bid-result";

    private final EventAuctionService auctionService;
    private final SimpMessagingTemplate messaging;

    /** Redis Pub/Sub publisher — optional, falls back to local STOMP when null. */
    @Autowired(required = false)
    private EventAuctionRedisPublisher eventAuctionRedisPublisher;

    /** Distributed bid rate limiter — optional. */
    @Autowired(required = false)
    private AuctionBidRateLimiter bidRateLimiter;

    public EventAuctionBidWebSocketController(EventAuctionService auctionService,
                                              SimpMessagingTemplate messaging) {
        this.auctionService = auctionService;
        this.messaging = messaging;
    }

    @MessageMapping("/auction/{itemId}/bid")
    public void handleBid(
            @DestinationVariable Long itemId,
            @Payload Map<String, Object> payload,
            Principal principal) {

        if (principal == null) {
            log.warn("[AuctionWS] Unauthenticated bid attempt on item={}", itemId);
            return;
        }

        Long userId;
        try {
            userId = Long.parseLong(principal.getName());
        } catch (NumberFormatException e) {
            log.warn("[AuctionWS] Invalid principal '{}' item={}", principal.getName(), itemId);
            return;
        }

        BigDecimal amount;
        try {
            Object raw = payload.get("amount");
            if (raw == null) throw new IllegalArgumentException("amount is required");
            amount = new BigDecimal(raw.toString());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("amount must be positive");
        } catch (Exception e) {
            log.warn("[AuctionWS] Invalid amount item={} user={}: {}", itemId, userId, e.getMessage());
            sendAck(String.valueOf(userId), BidAckMessage.builder()
                    .status(BidAckStatus.REJECTED).auctionId(itemId)
                    .reason("Invalid bid amount: " + e.getMessage()).build());
            return;
        }

        try {
            if (bidRateLimiter != null && !bidRateLimiter.tryAcquireEventBid(itemId, userId)) {
                log.info("[AuctionWS] Bid RATE_LIMITED item={} user={}", itemId, userId);
                sendAck(String.valueOf(userId), BidAckMessage.builder()
                        .status(BidAckStatus.REJECTED).auctionId(itemId).amount(amount)
                        .reason("Too many bids. Please wait a moment before bidding again.").build());
                return;
            }

            EventAuctionItemResponse updated = auctionService.placeBidByUserId(itemId, amount, userId);

            Map<String, Object> event = new HashMap<>();
            event.put("type",       "BID_PLACED");
            event.put("auctionId",  updated.getId());
            event.put("amount",     amount);
            event.put("bidderId",   userId);
            event.put("bidderName", updated.getLeaderName() != null ? updated.getLeaderName() : "");
            event.put("bidCount",   updated.getBidCount());
            event.put("timestamp",  Instant.now().toString());

            // Route through Redis Pub/Sub so all nodes deliver the event
            if (eventAuctionRedisPublisher != null) {
                eventAuctionRedisPublisher.publishBidEvent(itemId, event);
            } else {
                messaging.convertAndSend("/topic/auction/" + itemId, event);
            }

            sendAck(String.valueOf(userId), BidAckMessage.builder()
                    .status(BidAckStatus.ACCEPTED).auctionId(itemId).amount(amount).build());

            log.info("[AuctionWS] Bid ACCEPTED item={} user={} amount={}", itemId, userId, amount);

        } catch (IllegalArgumentException e) {
            log.info("[AuctionWS] Bid REJECTED item={} user={}: {}", itemId, userId, e.getMessage());
            sendAck(String.valueOf(userId), BidAckMessage.builder()
                    .status(BidAckStatus.REJECTED).auctionId(itemId).amount(amount)
                    .reason(e.getMessage()).build());

        } catch (IllegalStateException e) {
            String msg = e.getMessage();
            BidAckStatus st = (msg != null && msg.toLowerCase().contains("closed"))
                    ? BidAckStatus.REJECTED : BidAckStatus.OUTBID;
            log.info("[AuctionWS] Bid {} item={} user={}: {}", st, itemId, userId, msg);
            sendAck(String.valueOf(userId), BidAckMessage.builder()
                    .status(st).auctionId(itemId).amount(amount).reason(msg).build());

        } catch (Exception e) {
            log.error("[AuctionWS] Unexpected error item={} user={}", itemId, userId, e);
            sendAck(String.valueOf(userId), BidAckMessage.builder()
                    .status(BidAckStatus.REJECTED).auctionId(itemId).amount(amount)
                    .reason("Server error. Please try again.").build());
        }
    }

    private void sendAck(String userId, BidAckMessage ack) {
        messaging.convertAndSendToUser(userId, ACK_DESTINATION, ack);
    }
}