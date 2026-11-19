package com.manacommunity.api.workflow;

import org.springframework.context.ApplicationEvent;

public class WorkflowTransitionEvent extends ApplicationEvent {

    private final WorkflowTransitionHistory history;
    private final WorkflowTransitionRule rule;

    public WorkflowTransitionEvent(Object source, WorkflowTransitionHistory history, WorkflowTransitionRule rule) {
        super(source);
        this.history = history;
        this.rule = rule;
    }

    public WorkflowTransitionHistory getHistory() { return history; }
    public WorkflowTransitionRule getRule() { return rule; }
}
