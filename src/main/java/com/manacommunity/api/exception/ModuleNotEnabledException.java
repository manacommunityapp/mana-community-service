package com.manacommunity.api.exception;

import org.springframework.http.HttpStatus;

public class ModuleNotEnabledException extends ManaCommunityException {

    public ModuleNotEnabledException(Long communityId, String moduleKey) {
        super(
            "Module '" + moduleKey + "' is not enabled for community " + communityId,
            HttpStatus.FORBIDDEN,
            "MODULE_NOT_ENABLED"
        );
    }
}
