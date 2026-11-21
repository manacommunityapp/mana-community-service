package com.manacommunity.api.homeservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class HomeServiceNotFoundException extends HomeServiceException {
    public HomeServiceNotFoundException(String message) {
        super(message);
    }
}
