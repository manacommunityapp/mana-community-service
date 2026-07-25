package com.manacommunity.api.cfbos.shared.exception;

import com.manacommunity.api.exception.ManaCommunityException;
import org.springframework.http.HttpStatus;

public class CfbosException extends ManaCommunityException {
    public CfbosException(String message, HttpStatus status, String errorCode) {
        super(message, status, errorCode);
    }

    public CfbosException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "CFBOS_ERROR");
    }
}
