package com.manacommunity.api.workflow;

import com.manacommunity.api.workflow.dto.AllowedTransitionDto;
import com.manacommunity.api.workflow.dto.TransitionHistoryDto;
import com.manacommunity.api.workflow.dto.WorkflowTransitionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowEngine {

    private final WorkflowTransitionRuleRepository ruleRepo;
    private final WorkflowTransitionHistoryRepository historyRepo;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Returns transitions the given role may take from currentStatus,
     * filtered and sorted by sort_order.
     */
    public List<AllowedTransitionDto> getAllowedTransitions(
            String workflowId, String currentStatus, String actorRole) {
        return ruleRepo.findByWorkflowIdAndFromStatus(workflowId, currentStatus)
                .stream()
                .filter(r -> r.getAllowedRolesList().contains(actorRole))
                .sorted(Comparator.comparingInt(r -> (int) r.getSortOrder()))
                .map(r -> new AllowedTransitionDto(r.getToStatus(), r.getDisplayLabel(), r.isReasonRequired()))
                .toList();
    }

    /**
     * Validates and records a state transition.
     * Callers (module services) are responsible for updating their own entity status after this returns.
     *
     * @param metadata optional context data stored as audit metadata (not persisted in this implementation)
     */
    @Transactional
    public WorkflowTransitionResult transition(
            String workflowId,
            String entityType,
            String entityId,
            Long communityId,
            String fromStatus,
            String toStatus,
            Long actorUserId,
            String actorRole,
            String reason,
            String idempotencyKey,
            Map<String, Object> metadata) {

        // Idempotency guard
        if (idempotencyKey != null && historyRepo.existsByIdempotencyKey(idempotencyKey)) {
            log.info("[Workflow] idempotent skip: key={}", idempotencyKey);
            return new WorkflowTransitionResult(true, toStatus, null);
        }

        // Validate rule exists
        WorkflowTransitionRule rule = ruleRepo
                .findByWorkflowIdAndFromStatusAndToStatus(workflowId, fromStatus, toStatus)
                .orElseThrow(() -> new WorkflowTransitionException(
                        "Transition " + fromStatus + " → " + toStatus
                                + " is not defined in workflow " + workflowId));

        // Role authorisation check
        if (!rule.getAllowedRolesList().contains(actorRole)) {
            throw new WorkflowTransitionException(
                    "Role " + actorRole + " is not permitted to perform "
                            + fromStatus + " → " + toStatus);
        }

        // Mandatory reason check
        if (rule.isReasonRequired() && (reason == null || reason.isBlank())) {
            throw new WorkflowTransitionException(
                    "A reason is required for transition " + fromStatus + " → " + toStatus);
        }

        // Persist history record
        WorkflowTransitionHistory history = new WorkflowTransitionHistory();
        history.setWorkflowId(workflowId);
        history.setEntityType(entityType);
        history.setEntityId(entityId);
        history.setCommunityId(communityId);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setActorUserId(actorUserId);
        history.setActorRole(actorRole);
        history.setReason(reason);
        history.setIdempotencyKey(idempotencyKey);
        history.setTransitionedAt(Instant.now());
        historyRepo.save(history);

        // Publish AFTER_COMMIT event for notification hooks
        eventPublisher.publishEvent(new WorkflowTransitionEvent(this, history, rule));

        log.info("[Workflow] {} {} {} → {} by {}({})",
                workflowId, entityId, fromStatus, toStatus, actorRole, actorUserId);

        TransitionHistoryDto dto = new TransitionHistoryDto(
                history.getId(), fromStatus, toStatus,
                actorRole, actorUserId, reason,
                history.getTransitionedAt(), rule.getDisplayLabel());

        return new WorkflowTransitionResult(true, toStatus, dto);
    }

    public List<TransitionHistoryDto> getHistory(String entityType, String entityId) {
        return historyRepo
                .findByEntityTypeAndEntityIdOrderByTransitionedAtDesc(entityType, entityId)
                .stream()
                .map(h -> new TransitionHistoryDto(
                        h.getId(), h.getFromStatus(), h.getToStatus(),
                        h.getActorRole(), h.getActorUserId(), h.getReason(),
                        h.getTransitionedAt(), null))
                .toList();
    }
}
