package com.manacommunity.api.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an illegal state transition is attempted on an order, fulfilment group, or batch.
 */
@Getter
public class InvalidStateTransitionException extends ManaCommunityException {

    private final String entityName;
    private final String currentStatus;
    private final String attemptedStatus;

    public InvalidStateTransitionException(String entityName, String currentStatus, String attemptedStatus) {
        super(String.format("Cannot transition %s from %s to %s", entityName, currentStatus, attemptedStatus),
                HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION");
        this.entityName = entityName;
        this.currentStatus = currentStatus;
        this.attemptedStatus = attemptedStatus;
    }
}
