package com.manacommunity.api.homeservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class SlotConflictException extends HomeServiceException {
    public SlotConflictException(String message) {
        super(message);
    }
}
