package com.manacommunity.api.workflow;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class WorkflowNotificationListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransition(WorkflowTransitionEvent event) {
        WorkflowTransitionHistory h = event.getHistory();
        log.info("[WorkflowNotif] workflow={} entity={} {} → {} label='{}' — notify stakeholders",
                h.getWorkflowId(), h.getEntityId(),
                h.getFromStatus(), h.getToStatus(),
                event.getRule().getDisplayLabel());
        // TODO: wire into NotificationService when it supports template-based push
    }
}
