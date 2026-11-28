package com.manacommunity.api.grouporder.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.grouporder.entity.GroupOrderContribution;
import com.manacommunity.api.grouporder.repository.GroupOrderContributionRepository;
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
 * SQS-9 consumer for {@code mana-grouporder-notifications}.
 * Delivers mobile push notifications to all group-order contributors when
 * order milestones change (MIN_REACHED, ORDERED, DELIVERED, CANCELLED).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupOrderNotificationSqsHandler implements SqsMessageHandler {

    private final GroupOrderContributionRepository contributionRepository;
    private final ExpoPushService expoPushService;
    private final SqsProperties sqsProperties;
    private final ObjectMapper objectMapper;

    public record GroupOrderNotificationPayload(
            Long groupOrderId,
            Long communityId,
            String status,
            String title,
            String message
    ) {}

    @Override
    public String queueName() {
        return sqsProperties.getQueues().getGroupOrderNotifications();
    }

    @Override
    public void handle(SqsEventEnvelope envelope) throws Exception {
        GroupOrderNotificationPayload payload = objectMapper.readValue(
                envelope.payloadJson(), GroupOrderNotificationPayload.class);
        deliverNotifications(payload);
    }

    public void deliverNotifications(GroupOrderNotificationPayload payload) {
        if (payload == null || payload.groupOrderId() == null) return;
        List<Long> memberUserIds = contributionRepository.findByGroupOrderId(payload.groupOrderId()).stream()
                .map(GroupOrderContribution::getResidentId)
                .distinct()
                .toList();
        if (memberUserIds.isEmpty()) return;

        expoPushService.sendToUsers(
                memberUserIds,
                "Group Order: " + payload.title(),
                payload.message(),
                Map.of(
                        "type", "GROUP_ORDER_STATUS",
                        "groupOrderId", String.valueOf(payload.groupOrderId()),
                        "status", payload.status() != null ? payload.status() : ""
                ),
                "events"
        );
    }
}
