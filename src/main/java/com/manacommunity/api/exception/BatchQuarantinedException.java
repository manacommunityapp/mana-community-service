package com.manacommunity.api.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an action is attempted on a quarantined or recalled production batch.
 */
@Getter
public class BatchQuarantinedException extends ManaCommunityException {

    private final Long batchId;
    private final String batchNumber;
    private final String reason;

    public BatchQuarantinedException(Long batchId, String batchNumber, String reason) {
        super(String.format("Batch %s (ID: %d) is QUARANTINED and cannot be fulfilled or released: %s",
                batchNumber, batchId, reason), HttpStatus.CONFLICT, "BATCH_QUARANTINED");
        this.batchId = batchId;
        this.batchNumber = batchNumber;
        this.reason = reason;
    }
}
