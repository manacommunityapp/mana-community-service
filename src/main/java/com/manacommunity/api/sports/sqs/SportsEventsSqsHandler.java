package com.manacommunity.api.sports.sqs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.sports.scheduler.SportsTournamentSchedulerService;
import com.manacommunity.api.sports.service.SportsFeedService;
import com.manacommunity.platform.sqs.SqsEventEnvelope;
import com.manacommunity.platform.sqs.SqsMessageHandler;
import com.manacommunity.platform.sqs.SqsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * SQS-3 & SQS-6 consumer for {@code mana-sports-events}.
 * Handles immediate match-result feed post publication and knockout bracket seeding
 * upon match completion without blocking the admin match-status endpoint.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SportsEventsSqsHandler implements SqsMessageHandler {

    private final SportsFeedService sportsFeedService;
    private final SportsTournamentSchedulerService schedulerService;
    private final SqsProperties sqsProperties;
    private final ObjectMapper objectMapper;

    @Override
    public String queueName() {
        return sqsProperties.getQueues().getSportsEvents();
    }

    @Override
    public void handle(SqsEventEnvelope envelope) throws Exception {
        JsonNode root = objectMapper.readTree(envelope.payloadJson());
        String type = envelope.eventType();
        if ("MATCH_RESULT_FEED_POST".equals(type) && root.has("matchId")) {
            sportsFeedService.publishCompletedMatchPost(root.get("matchId").asLong());
        } else if ("SEED_KNOCKOUT_FROM_GROUPS".equals(type) && root.has("configId")) {
            schedulerService.seedKnockoutFromGroups(root.get("configId").asLong());
        } else {
            log.debug("Ignoring unknown sports SQS event type={} eventId={}", type, envelope.eventId());
        }
    }
}
