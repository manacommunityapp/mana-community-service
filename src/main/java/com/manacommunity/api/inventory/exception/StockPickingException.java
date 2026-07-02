package com.manacommunity.api.inventory.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class StockPickingException extends RuntimeException {
    public StockPickingException(String message) {
        super(message);
    }
}
