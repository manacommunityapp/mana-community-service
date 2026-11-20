package com.manacommunity.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a participant does not meet the category's age criteria.
 * Message focuses on eligibility criteria without echoing exact personal age values.
 */
public class AgeMismatchException extends ManaCommunityException {

    public AgeMismatchException(int userAge, int minAge, int maxAge, String sportName) {
        super("Age requirement not met for '" + sportName + "'. Required age: "
                + minAge + "–" + maxAge + " years.", HttpStatus.BAD_REQUEST, "AGE_MISMATCH");
    }
}
