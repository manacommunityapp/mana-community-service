package com.manacommunity.api.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

/**
 * Thrown when weight variation exceeds acceptable tolerance gate (e.g. 15%).
 * Requires explicit customer confirmation before billing or dispatch.
 */
@Getter
public class ToleranceExceededException extends ManaCommunityException {

    private final BigDecimal requestedWeightG;
    private final BigDecimal actualWeightG;
    private final BigDecimal diffPct;
    private final BigDecimal maxTolerancePct;

    public ToleranceExceededException(
            String message,
            BigDecimal requestedWeightG,
            BigDecimal actualWeightG,
            BigDecimal diffPct,
            BigDecimal maxTolerancePct) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, "TOLERANCE_EXCEEDED");
        this.requestedWeightG = requestedWeightG;
        this.actualWeightG = actualWeightG;
        this.diffPct = diffPct;
        this.maxTolerancePct = maxTolerancePct;
    }
}
