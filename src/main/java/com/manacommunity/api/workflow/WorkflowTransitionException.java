package com.manacommunity.api.workflow;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class WorkflowTransitionException extends RuntimeException {

    public WorkflowTransitionException(String message) {
        super(message);
    }
}
