package com.manacommunity.api.inventory.exception;

import com.manacommunity.api.exception.ManaCommunityException;
import org.springframework.http.HttpStatus;

public class StockPickingException extends ManaCommunityException {
    public StockPickingException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "STOCK_PICKING_ERROR");
    }
}
