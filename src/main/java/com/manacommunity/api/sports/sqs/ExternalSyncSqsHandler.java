package com.manacommunity.api.sports.sqs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.sports.service.SportsCricHeroesService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.platform.sqs.SqsEventEnvelope;
import com.manacommunity.platform.sqs.SqsMessageHandler;
import com.manacommunity.platform.sqs.SqsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * SQS-7 consumer for {@code mana-external-sync}.
 * Offloads external CricHeroes HTML scraping jobs from web request threads and schedulers.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalSyncSqsHandler implements SqsMessageHandler {

    private final SportsCricHeroesService cricHeroesService;
    private final AppUserRepository userRepository;
    private final SqsProperties sqsProperties;
    private final ObjectMapper objectMapper;

    @Override
    public String queueName() {
        return sqsProperties.getQueues().getExternalSync();
    }

    @Override
    public void handle(SqsEventEnvelope envelope) throws Exception {
        if (!"CRICHEROES_REFRESH".equals(envelope.eventType())) {
            return;
        }
        JsonNode node = objectMapper.readTree(envelope.payloadJson());
        if (!node.has("playerId")) {
            log.warn("Malformed CRICHEROES_REFRESH SQS message eventId={}", envelope.eventId());
            return;
        }
        long playerId = node.get("playerId").asLong();
        AppUser user = node.has("userId") && !node.get("userId").isNull()
                ? userRepository.findById(node.get("userId").asLong()).orElse(null)
                : null;
        cricHeroesService.refreshProfile(playerId, user);
    }
}
