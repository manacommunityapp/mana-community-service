package com.cpn.infrastructure.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CpnException extends RuntimeException {
    
    private final CpnErrorCode errorCode;
    private final HttpStatus httpStatus;

    public CpnException(CpnErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.httpStatus = errorCode.getDefaultStatus();
    }

    public CpnException(CpnErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = errorCode.getDefaultStatus();
    }

    public CpnException(CpnErrorCode errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
