package com.manacommunity.api.sports.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.service.ExpoPushService;
import com.manacommunity.platform.sqs.SqsEventEnvelope;
import com.manacommunity.platform.sqs.SqsMessageHandler;
import com.manacommunity.platform.sqs.SqsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * SQS-4 consumer for {@code mana-auction-notifications}.
 * Delivers outbid and player-sold push notifications asynchronously outside
 * the bid/sold database transaction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionNotificationSqsHandler implements SqsMessageHandler {

    private final ExpoPushService expoPushService;
    private final SqsProperties sqsProperties;
    private final ObjectMapper objectMapper;

    public record AuctionPushPayload(
            String type,          // OUTBID | PLAYER_SOLD
            Long auctionId,
            String playerName,
            String teamName,
            Long amount,
            List<Long> recipientUserIds
    ) {}

    @Override
    public String queueName() {
        return sqsProperties.getQueues().getAuctionNotifications();
    }

    @Override
    public void handle(SqsEventEnvelope envelope) throws Exception {
        AuctionPushPayload payload = objectMapper.readValue(envelope.payloadJson(), AuctionPushPayload.class);
        deliverPush(payload);
    }

    public void deliverPush(AuctionPushPayload payload) {
        if (payload == null || payload.recipientUserIds() == null || payload.recipientUserIds().isEmpty()) {
            return;
        }
        String title;
        String body;
        if ("OUTBID".equals(payload.type())) {
            title = "You've been outbid!";
            body = payload.teamName() + " bid ₹" + payload.amount() + " for " + payload.playerName();
        } else if ("ITEM_WON".equals(payload.type())) {
            title = "Auction Won: " + payload.playerName();
            body = "You won " + payload.playerName() + " with a bid of ₹" + payload.amount();
        } else {
            title = "Player Sold: " + payload.playerName();
            body = payload.playerName() + " sold to " + payload.teamName() + " for ₹" + payload.amount();
        }
        expoPushService.sendToUsers(
                payload.recipientUserIds(),
                title,
                body,
                Map.of(
                        "type", "AUCTION_" + payload.type(),
                        "auctionId", String.valueOf(payload.auctionId())
                ),
                "events"
        );
    }
}
