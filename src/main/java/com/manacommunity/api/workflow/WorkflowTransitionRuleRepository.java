package com.manacommunity.api.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowTransitionRuleRepository extends JpaRepository<WorkflowTransitionRule, Long> {

    List<WorkflowTransitionRule> findByWorkflowIdAndFromStatus(String workflowId, String fromStatus);

    Optional<WorkflowTransitionRule> findByWorkflowIdAndFromStatusAndToStatus(
            String workflowId, String fromStatus, String toStatus);
}
