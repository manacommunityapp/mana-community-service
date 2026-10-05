package com.manacommunity.api.notification.orchestrator.engine;

import com.manacommunity.api.notification.orchestrator.NotificationAuditLog;
import com.manacommunity.api.notification.orchestrator.NotificationAuditLogRepository;
import com.manacommunity.api.notification.orchestrator.NotificationDtos.UnifiedNotificationRequest;
import com.manacommunity.api.notification.orchestrator.NotificationDtos.UnifiedNotificationResult;
import com.manacommunity.api.notification.orchestrator.NotificationEnums.DeliveryStatus;
import com.manacommunity.api.notification.orchestrator.NotificationEnums.DeliveryStrategy;
import com.manacommunity.api.notification.orchestrator.NotificationEnums.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRetryFallbackEngine {

    private final NotificationAuditLogRepository auditLogRepository;

    private static final List<NotificationChannel> CASCADE_ORDER = List.of(
            NotificationChannel.PUSH,
            NotificationChannel.WHATSAPP,
            NotificationChannel.SMS,
            NotificationChannel.EMAIL
    );

    public Optional<NotificationChannel> determineNextFallbackChannel(NotificationChannel failedChannel) {
        int idx = CASCADE_ORDER.indexOf(failedChannel);
        if (idx >= 0 && idx < CASCADE_ORDER.size() - 1) {
            return Optional.of(CASCADE_ORDER.get(idx + 1));
        }
        return Optional.empty();
    }

    public UnifiedNotificationRequest buildFallbackRequest(NotificationAuditLog failedLog, NotificationChannel nextChannel) {
        return UnifiedNotificationRequest.builder()
                .recipientUserId(failedLog.getUser().getId())
                .category(failedLog.getCategory())
                .priority(failedLog.getPriority())
                .strategy(DeliveryStrategy.FALLBACK_CASCADE)
                .explicitChannels(List.of(nextChannel))
                .title(failedLog.getTitle())
                .body(failedLog.getBody())
                .build();
    }
}
