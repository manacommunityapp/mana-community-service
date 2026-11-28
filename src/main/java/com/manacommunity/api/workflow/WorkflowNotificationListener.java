package com.manacommunity.api.workflow;

import com.manacommunity.api.notification.orchestrator.NotificationDtos.UnifiedNotificationRequest;
import com.manacommunity.api.notification.orchestrator.NotificationEnums.NotificationCategory;
import com.manacommunity.api.notification.orchestrator.NotificationEnums.NotificationPriority;
import com.manacommunity.api.notification.orchestrator.UnifiedNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowNotificationListener {

    private final ObjectProvider<UnifiedNotificationService> notificationServiceProvider;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransition(WorkflowTransitionEvent event) {
        WorkflowTransitionHistory h = event.getHistory();
        log.info("[WorkflowNotif] workflow={} entity={} {} → {} label='{}' — notify stakeholders",
                h.getWorkflowId(), h.getEntityId(),
                h.getFromStatus(), h.getToStatus(),
                event.getRule().getDisplayLabel());

        if (h.getActorUserId() != null) {
            try {
                UnifiedNotificationService notif = notificationServiceProvider.getIfAvailable();
                if (notif != null) {
                    notif.orchestrate(UnifiedNotificationRequest.builder()
                            .recipientUserId(h.getActorUserId())
                            .category(NotificationCategory.GENERAL)
                            .priority(NotificationPriority.NORMAL)
                            .title("Workflow Status Update: " + event.getRule().getDisplayLabel())
                            .body(String.format("Workflow item #%d transitioned from %s to %s",
                                    h.getEntityId(), h.getFromStatus(), h.getToStatus()))
                            .data(Map.of("workflowId", h.getWorkflowId(), "entityId", h.getEntityId(), "status", h.getToStatus()))
                            .build());
                }
            } catch (Exception e) {
                log.warn("[WorkflowNotif] Failed to dispatch workflow transition alert: {}", e.getMessage());
            }
        }
    }
}
