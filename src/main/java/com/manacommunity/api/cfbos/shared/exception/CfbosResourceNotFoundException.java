package com.manacommunity.api.cfbos.shared.exception;

import org.springframework.http.HttpStatus;

public class CfbosResourceNotFoundException extends CfbosException {
    public CfbosResourceNotFoundException(String entityName, Long id) {
        super(entityName + " not found with id: " + id, HttpStatus.NOT_FOUND, "CFBOS_NOT_FOUND");
    }
}
