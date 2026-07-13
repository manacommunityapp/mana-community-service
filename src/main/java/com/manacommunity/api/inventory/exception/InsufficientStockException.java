package com.manacommunity.api.inventory.exception;

import com.manacommunity.api.exception.ManaCommunityException;
import org.springframework.http.HttpStatus;

public class InsufficientStockException extends ManaCommunityException {
    public InsufficientStockException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INSUFFICIENT_STOCK");
    }
}
